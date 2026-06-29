# How Java Actually Works

> Runnable example: [code/basics/HowJavaWorks.java](../code/basics/HowJavaWorks.java)

---

## The Big Picture

> **In plain terms** — You write Java in plain text. A tool called the *compiler* translates it into a universal "in-between" language called **bytecode**. A program called the **JVM** then reads that bytecode and runs it on whatever computer it's installed on. That extra middle step is the whole reason "write once, run anywhere" works.

> **Why this matters** — Most languages (C, C++, Rust) compile straight to machine code for *one* type of CPU, so you must rebuild for each platform. Java compiles to bytecode for an *imaginary* CPU (the JVM), and a real JVM exists for every platform. The trade-off: a tiny startup cost and a running JVM, in exchange for portability and runtime optimization.

---

## The Compilation Pipeline

When you write Java, here is what happens before it runs:

```
YourCode.java
     │
  javac          ← Java Compiler (part of JDK)
     │
YourCode.class   ← Bytecode (platform-independent instructions)
     │
    JVM          ← Java Virtual Machine (platform-specific)
     │
 Native OS       ← Runs on your machine
```

The key insight: `.class` files are NOT machine code. They are bytecode — instructions for an imaginary machine (the JVM). This is why the same `.jar` file runs on Windows, Linux, and macOS without recompiling.

> **Going deeper** — Bytecode is a compact, *stack-based* instruction set (operands are pushed and popped from an operand stack rather than living in named CPU registers). This keeps it simple and portable, but it's why the JVM can't run it directly at full speed — it first interprets, then JIT-compiles hot code into real register-based machine instructions (see below). You can see the bytecode of any class with `javap -c YourClass`.

---

## JDK vs JRE vs JVM

```
JDK (Java Development Kit)
├── javac         ← compiler
├── java          ← launcher
├── javadoc       ← doc generator
├── jdb           ← debugger
├── jar           ← archive tool
└── JRE
    ├── Class libraries (java.util, java.io, etc.)
    └── JVM
        └── Executes your bytecode
```

- **JDK** — install this to write and compile Java
- **JRE** — install this to only run Java programs (no compiler)
- **JVM** — the engine that runs bytecode; part of JRE

As a developer, you always install the **JDK**.

> **In plain terms** — Think of it like cooking: the **JDK** is the full kitchen (knives, oven, recipes) for *making* the dish, the **JRE** is just a microwave for *reheating* a finished meal, and the **JVM** is the heating element inside that actually does the work. Developers need the whole kitchen.

> **Going deeper** — Since Java 11, Oracle no longer ships a standalone JRE; you bundle just the modules your app needs using `jlink` to produce a slim custom runtime. So "JDK vs JRE" is now more of a conceptual split than two separate downloads. Knowing the layers still matters when you size a Docker image or debug a "command not found: javac" on a runtime-only box.

---

## JIT Compilation

The JVM doesn't just read bytecode line by line (that would be slow). It uses a **Just-In-Time (JIT) compiler** to convert frequently-used bytecode ("hot paths") into native machine code at runtime.

```
Start: JVM interprets bytecode        → slow
After warmup: JIT compiles hot code   → as fast as C
```

This is why:
- Java takes a few seconds to "warm up" (common in microservices)
- Long-running Java servers get *faster* over time
- Benchmarks on small programs make Java look slower than it actually is in production

> **In plain terms** — The JVM starts by *reading* your code one step at a time (slow but instant to begin). While it runs, it watches which parts are used the most and quietly rewrites those into fast native code in the background — like a translator who, after hearing the same phrase 100 times, just memorizes it instead of re-translating every time.

> **Going deeper** — HotSpot uses *tiered compilation*: C1 (the client compiler) kicks in fast for quick-but-modest speedups, then C2 (the server compiler) recompiles the hottest methods with aggressive optimizations like inlining and loop unrolling. It even makes *speculative* optimizations based on observed behavior and "deoptimizes" (falls back to the interpreter) if its assumptions break. This runtime profiling is something an ahead-of-time C compiler simply can't do — and it's why a warmed-up JVM can match or beat native code on long-running workloads. The flip side is cold-start latency, which is what GraalVM native-image and AOT compilation aim to fix.

---

## What the Compiler Checks vs What the JVM Checks

The compiler catches **syntax and compile-time type errors**. The JVM catches **runtime errors**.

```java
// This compiles fine:
Object x = "hello";
Integer i = (Integer) x; // compiler trusts you — but JVM throws ClassCastException at runtime

// This is a compile error (compiler catches it):
int n = "hello"; // incompatible types: String cannot be converted to int
```

Understanding this boundary helps you know when to add `instanceof` checks.

> **In plain terms** — There are two safety nets at two different times. The *compiler* checks your code before it ever runs (spelling, types, structure) — catch a problem here and your program never even starts with the bug. The *JVM* checks things that can only be known while running (Is this object really an Integer? Is this array index valid?). Compile-time errors are the cheap, friendly ones; runtime errors are the ones that surprise users.

> **Going deeper** — This split exists because Java's type system is partly *erased* and partly *dynamic*. Generics are checked at compile time then erased, so the JVM can't always know the real runtime type — hence casts that "compile fine" but throw `ClassCastException`. The rule of thumb: push as many errors as possible to compile time (with generics, `Optional`, sealed types, enums) because every error you move *left* is one your users never hit.

---

## Classpath

The classpath tells the JVM where to find your `.class` files and `.jar` libraries.

```bash
# Compile:
javac -cp lib/gson.jar src/Main.java -d out/

# Run:
java -cp out:lib/gson.jar Main
```

In real projects, a build tool (Maven, Gradle) handles this for you. But knowing what the classpath is prevents confusion when you see `ClassNotFoundException`.

---

## Key Takeaways

1. Java is compiled to bytecode, then interpreted/JIT-compiled by the JVM — not compiled directly to machine code.
2. Install the JDK (not just JRE) when developing.
3. The compiler is your first line of defence; the JVM catches what the compiler can't.
4. JIT means Java is not inherently slow — benchmark properly.
