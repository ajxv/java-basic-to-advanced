# How Java Actually Works

> Runnable example: [code/basics/HowJavaWorks.java](../code/basics/HowJavaWorks.java)

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
