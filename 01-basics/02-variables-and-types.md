# Variables, Types, and Memory

> Runnable example: [code/basics/TypesDemo.java](../code/basics/TypesDemo.java)

---

## The Big Picture

> **In plain terms** — A variable is a labelled box that holds a value. In Java, every box has a fixed *type* that says what can go in it (a number, a true/false, a piece of text). There are two families of types: **primitives** (the value lives directly in the box — fast and simple) and **objects** (the box holds a *reference*, like a street address pointing to the real thing stored elsewhere). Almost every confusing Java moment for beginners — `==` giving the "wrong" answer, `null` errors, surprising number results — comes down to that one distinction: *am I holding the value, or an address to it?*

> **Why this matters** — Java is statically and strongly typed: types are checked at compile time and rarely change underneath you. That's what gives you autocomplete, refactoring safety, and errors before users ever see them. The cost is verbosity, which `var` (later in this doc) softens. Knowing where a value physically lives — stack vs heap — also explains performance, thread-safety, and garbage collection later on.

---

## The 8 Primitive Types

| Type    | Size   | Default | Typical Use                        |
|---------|--------|---------|------------------------------------|
| byte    | 8-bit  | 0       | Raw binary data, file I/O          |
| short   | 16-bit | 0       | Rarely used directly               |
| int     | 32-bit | 0       | Default for integer math           |
| long    | 64-bit | 0L      | IDs, timestamps — add `L` suffix   |
| float   | 32-bit | 0.0f    | Low-precision decimals — add `f`   |
| double  | 64-bit | 0.0     | Default for decimal math           |
| boolean | 1-bit  | false   | True/false flags                   |
| char    | 16-bit | '\u0000'| Single Unicode character        |

```java
int age = 25;
long userId = 9876543210L;  // L suffix required for long literals > int range
double price = 19.99;
boolean isActive = true;
char grade = 'A';
```

> **In plain terms** — These 8 are the only types that aren't objects. Pick by *range* and *purpose*: use `int` for everyday whole numbers, `long` when values get huge (IDs, timestamps, byte counts), `double` for general decimals, `boolean` for yes/no. You'll rarely reach for `byte`/`short` directly. A `char` is secretly just a number (its Unicode code point), which is why `(int) 'A'` gives `65`.

> **Going deeper** — Each primitive has a boxed object twin (`int`/`Integer`, `double`/`Double`, …). The compiler silently converts between them — *autoboxing*. It's convenient but has two sharp edges: (1) an `Integer` can be `null`, so unboxing one into an `int` can throw `NullPointerException`; (2) boxing in a tight loop allocates millions of throwaway objects and wrecks performance. Also note the JLS only guarantees `boolean` is "1 bit" *logically* — the JVM typically stores it as a full byte, and a `boolean[]` uses one byte per element.

---

## The Floating-Point Trap

```java
double a = 0.1 + 0.2;
System.out.println(a); // 0.30000000000000004 — NOT 0.3

// IEEE 754 floating point can't represent 0.1 exactly in binary.
// This affects EVERY language, not just Java.
```

**For money and precision: always use `BigDecimal`**

```java
import java.math.BigDecimal;

BigDecimal price = new BigDecimal("0.10"); // use String constructor!
BigDecimal tax   = new BigDecimal("0.20");
BigDecimal total = price.add(tax);
System.out.println(total); // 0.30 ✓

// BigDecimal("0.10") is exact; new BigDecimal(0.10) inherits the double imprecision
```

> **In plain terms** — Computers store decimals in binary, and just like `1/3` can't be written exactly in decimal (0.333…), `0.1` can't be written exactly in binary. So `0.1 + 0.2` lands a hair off `0.3`. It's not a Java bug — every language using standard floating point does this. For anything where being off by a fraction of a cent matters (money!), use `BigDecimal`.

> **Going deeper** — `double`/`float` follow the IEEE 754 standard: a sign, an exponent, and a fraction, giving ~15–17 significant decimal digits for `double`. Because the error is *relative*, never compare floats with `==` — compare within a tolerance (`Math.abs(a - b) < 1e-9`). `BigDecimal` trades speed for exactness and arbitrary precision, but watch out: division can produce a non-terminating result and throw `ArithmeticException` unless you supply a `MathContext` or rounding mode. For high-performance money math, some teams instead store amounts as `long` cents.

---

## Integer Overflow — Silent and Dangerous

```java
int max = Integer.MAX_VALUE; // 2,147,483,647
System.out.println(max + 1); // -2,147,483,648 — wraps around, no exception!

// Fix: use long when values can exceed int range
long bigNum = (long) max + 1; // 2,147,483,648 ✓

// Common trap: calculating file sizes, timestamps, or IDs with int
long fileSize = 3L * 1024 * 1024 * 1024; // 3 GB — use L to force long arithmetic
```

> **In plain terms** — An `int` is like a car's odometer with a fixed number of digits: go past the maximum and it silently rolls over to the most negative number instead of erroring. The fix is to use a bigger container (`long`) *before* the math happens — note `(long) max + 1` casts first, whereas `(long)(max + 1)` overflows first and casts the already-broken result.

> **Going deeper** — Java integers are two's-complement and wrap on overflow with no exception — fast, but dangerous in size/time/financial calculations (this class of bug grounded an Ariane 5 rocket and broke many systems at the "year 2038" `int`-seconds boundary). If you want overflow to *fail loudly*, use `Math.addExact` / `Math.multiplyExact`, which throw `ArithmeticException`. For numbers beyond even `long`, use `BigInteger`. And remember `3 * 1024 * 1024 * 1024` is computed in `int` and overflows *before* it's ever assigned to a `long` — force `long` math early with an `L` literal.

---

## Stack vs Heap

Where your variables actually live in memory:

```
Stack (per thread)               Heap (shared)
─────────────────                ─────────────
• Primitives                     • Objects (new X())
• Object references              • Arrays
• Method call frames             • String Pool
• Fixed size, fast               • Large, GC managed
• Auto-freed when method exits   • Lives until GC collects
```

```java
public void example() {
    int x = 5;                     // x lives on the stack
    String s = new String("hi");   // reference `s` on stack, object "hi" on heap
    String s2 = "hi";              // reference on stack, "hi" in String Pool (heap)
}
// When example() returns, x and the references are gone from stack.
// The heap objects remain until GC finds they're unreachable.
```

> **In plain terms** — Think of the **stack** as a stack of sticky notes for the method you're currently in: quick to add, automatically thrown away the moment the method returns. The **heap** is a big shared warehouse where actual objects live; a sticky note (a reference) just holds the warehouse shelf number. Many notes can point to the same object in the warehouse.

> **Going deeper** — Each thread gets its own stack (hence "per thread" and why stack data is inherently thread-safe); the heap is shared across all threads (hence the need for synchronization). Infinite recursion overflows the stack → `StackOverflowError`; allocating more objects than the heap can hold (or leaking them) → `OutOfMemoryError`. The garbage collector only manages the heap — it reclaims objects nothing references anymore. Modern JITs can even *escape-analyze* an object that never leaves a method and allocate it on the stack to skip GC entirely. This stack/heap split is the foundation for the [memory & GC](../05-advanced/03-memory-and-gc.md) deep dive.

---

## `==` vs `.equals()` — The Most Common Java Bug

```java
// Primitives: == compares VALUES
int a = 5, b = 5;
System.out.println(a == b); // true ✓

// Objects: == compares REFERENCES (memory addresses)
String s1 = new String("hello");
String s2 = new String("hello");
System.out.println(s1 == s2);      // false — two different objects in heap
System.out.println(s1.equals(s2)); // true  — same content ✓

// Tricky: String literals are pooled
String s3 = "hello";
String s4 = "hello";
System.out.println(s3 == s4); // true — same pooled object (don't rely on this)
```

**Rule: always use `.equals()` for object content comparison. Use `==` only for primitives.**

For null safety:
```java
// BAD: throws NullPointerException if s1 is null
s1.equals(s2);

// GOOD: null-safe
Objects.equals(s1, s2); // returns false if either is null

// Or: put the known non-null value first
"expected".equals(userInput); // userInput can be null — no NPE
```

> **In plain terms** — `==` asks "are these the *same box*?" while `.equals()` asks "do the boxes *contain the same thing*?" For primitives there's only the value, so `==` is right. For objects, two different objects can hold identical content, so you almost always want `.equals()`. The string-literal pooling that makes `"hello" == "hello"` happen to be `true` is an optimization you must never rely on.

> **Going deeper** — `==` on references compares identity (the address). `.equals()` does whatever a class *defines* it to — and `Object`'s default `.equals()` is just `==`, so a class that doesn't override it gets reference comparison anyway. The iron rule: **if you override `equals`, you must override `hashCode`** to the same contract, or the object will misbehave in `HashMap`/`HashSet`. `record` types (Java 16+) generate both correctly for you. Bonus gotcha: autoboxing caches small `Integer` values (−128..127), so `Integer a = 100; Integer b = 100; a == b` is `true`, but at `1000` it's `false` — another reason to never use `==` on boxed types.

---

## Type Casting

```java
// Widening (automatic, no data loss):
int i = 100;
long l = i;    // int → long: automatic
double d = i;  // int → double: automatic

// Narrowing (explicit cast, possible data loss):
double pi = 3.14159;
int truncated = (int) pi; // 3 — fractional part lost silently!

long bigNum = 300L;
byte b = (byte) bigNum; // 44 — wraps! 300 % 256 = 44

// Never cast without knowing the range.
```

> **In plain terms** — *Widening* (small type → big type) is safe and automatic, like pouring a cup into a bucket. *Narrowing* (big → small) needs an explicit cast because it might not fit — and Java won't warn you when data is lost; it just silently truncates or wraps. The cast is you telling the compiler "I know what I'm doing."

> **Going deeper** — Two casts to keep straight: *primitive* casts convert the value's bits (and may lose data); *reference* casts (`(String) obj`) don't change the object at all — they just re-label its type, and the JVM verifies it at runtime, throwing `ClassCastException` if you're wrong. Guard reference casts with `instanceof` — or better, use pattern matching: `if (obj instanceof String s) { … }` tests and binds in one step (Java 16+). Watch the silent traps: `(int) 3.9` truncates toward zero to `3` (not rounding), and `int`/`int` division floors, so `5 / 2 == 2` — write `5.0 / 2` to get `2.5`.

---

## `var` — Type Inference (Java 10+)

```java
// compiler infers the type from the right side
var list = new ArrayList<String>();  // type is ArrayList<String>
var result = getUserById(id);        // type is whatever getUserById returns

// Use when the type is obvious from context.
// Avoid when it makes the code harder to read:
var x = process(data); // what type is x? Unclear without looking at process()
```

> **In plain terms** — `var` lets the compiler figure out the type from the right-hand side so you don't repeat yourself (`var list = new ArrayList<String>()` instead of `ArrayList<String> list = …`). The variable is *still* strongly typed — `var` is not "anything goes" like JavaScript's `var`; the type is just inferred once and fixed forever.

> **Going deeper** — `var` is pure compile-time sugar: the `.class` file is byte-for-byte identical to writing the type out. Limits: it works only for local variables with an initializer — not fields, method parameters, or return types — because those are part of an API contract that shouldn't silently shift. Style guidance: prefer it when the type is obvious from the right side (constructors, casts) and skip it when the right side is an opaque method call that hides the type from the reader.
