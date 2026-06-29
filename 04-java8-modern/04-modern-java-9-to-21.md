# Modern Java: 9 to 21

> Runnable example: [code/modern/ModernJavaDemo.java](../code/modern/ModernJavaDemo.java)

---

## The Big Picture

> **In plain terms** — Java didn't stop at Java 8. Since 2018 it ships a new version every six months, and the years since have steadily removed boilerplate and added safety: `records` (data classes in one line), `sealed` types (a fixed, known set of subtypes), pattern matching (test-and-extract in one step), text blocks (sane multi-line strings), and virtual threads (cheap concurrency). The throughline is "say more with less, and let the compiler check more for you."

> **Why this matters** — Several of these features combine into a genuinely new style: `sealed` interfaces + `records` + pattern-matching `switch` let you model data and handle every case *exhaustively*, with the compiler erroring if you forget one — a functional "algebraic data type" approach that was awkward in old Java. Knowing what exists per version also guides practical choices: target an **LTS** release (8, 11, 17, 21), and reach for the modern construct (`record` over a hand-written DTO, `switch` patterns over `instanceof` chains) when it makes intent clearer.

---

## Java 9 — Collection Factory Methods

```java
// Immutable collections — concise and clear
List<String> names = List.of("Alice", "Bob", "Charlie");
Set<Integer> ids = Set.of(1, 2, 3);
Map<String, Integer> scores = Map.of("Alice", 95, "Bob", 87);

// Larger maps: use Map.ofEntries
Map<String, Integer> bigMap = Map.ofEntries(
    Map.entry("Alice", 95),
    Map.entry("Bob", 87),
    Map.entry("Charlie", 92),
    Map.entry("Diana", 88)
);

// For a mutable copy:
List<String> mutable = new ArrayList<>(names);
```

> **In plain terms** — `List.of(...)`/`Set.of(...)`/`Map.of(...)` create small unmodifiable collections in one readable line — perfect for constants and test data. Covered in depth in [collections](../03-core-java/03-collections.md#immutable-collections-java-9); the takeaway here is just "this is the modern, concise way to make a fixed collection."

> **Going deeper** — `Map.of` is overloaded up to 10 key/value pairs; beyond that use `Map.ofEntries`. These reject `null` elements and duplicate keys (fail-fast), and have no guaranteed iteration order (`Map.of` is deliberately randomized between runs to stop you depending on order). They're genuinely immutable, unlike the older `Collections.unmodifiableList` view.

---

## Java 10 — `var` (Local Variable Type Inference)

```java
// var lets the compiler infer the type from the right-hand side
var list = new ArrayList<String>();       // inferred: ArrayList<String>
var map = new HashMap<String, Integer>(); // inferred: HashMap<String, Integer>
var name = "Alice";                       // inferred: String

// Works in for-each:
for (var entry : map.entrySet()) {
    System.out.println(entry.getKey() + "=" + entry.getValue());
}

// Use when the type is obvious from the right side.
// Avoid when it makes reading harder:
var x = process(data); // what type is x? Unclear without checking process()
```

> **In plain terms** — `var` infers a local variable's type from the right side, cutting repetition (`var users = new HashMap<String, List<User>>()`). It's still fully, statically typed — just less typing. Full treatment in [variables & types](../01-basics/02-variables-and-types.md#var--type-inference-java-10); here it's listed as the Java 10 milestone.

> **Going deeper** — `var` pairs especially well with the other modern features on this page: it shines on the long generic types and `record`/builder results common in modern code, and Java 11 even allows `var` in lambda parameters. Remember its limits — locals only, never fields/parameters/returns — and the readability rule: keep it where the type is obvious from the right-hand side.

---

## Java 11 — String and File Improvements

```java
// New String methods
" hello ".strip();      // "hello" (Unicode-aware, unlike trim())
" ".isBlank();          // true (checks only whitespace)
"ha".repeat(3);         // "hahaha"
"a\nb\nc".lines()       // Stream<String> of each line

// Read/write entire file as String
String content = Files.readString(Path.of("file.txt"));
Files.writeString(Path.of("out.txt"), "hello");
```

> **In plain terms** — Java 11 (the previous LTS, and still hugely common in industry) polished everyday APIs: `strip()`/`isBlank()`/`repeat()`/`lines()` on strings, and one-call file read/write. Small, but they remove boilerplate you used to write by hand. Details in [strings](../03-core-java/02-strings.md) and [file I/O](../05-advanced/01-file-io.md).

> **Going deeper** — Java 11 also made `java SomeFile.java` run a single source file directly (no separate `javac` step) — great for scripts and learning. It's the version a *lot* of production still targets, so when writing libraries, check whether you can rely on 17/21 features or must stay 11-compatible.

---

## Java 14 — Records

Records are immutable data classes. The compiler generates constructor, accessors, `equals`, `hashCode`, and `toString` automatically.

```java
// All the boilerplate below is generated automatically:
public record Point(int x, int y) {}

// Equivalent to:
// public final class Point {
//   private final int x;
//   private final int y;
//   public Point(int x, int y) { this.x = x; this.y = y; }
//   public int x() { return x; }
//   public int y() { return y; }
//   public boolean equals(Object o) { ... }
//   public int hashCode() { ... }
//   public String toString() { return "Point[x=3, y=4]"; }
// }

// Usage:
Point p = new Point(3, 4);
p.x();         // 3 (accessor, NOT getX())
p.y();         // 4
p.toString();  // "Point[x=3, y=4]"
p.equals(new Point(3, 4)); // true — structural equality

// Custom validation in compact constructor:
public record Range(int min, int max) {
    public Range { // compact constructor: no parens, no this.min = min
        if (min > max) throw new IllegalArgumentException("min must be ≤ max");
    }
}

// Records can have methods:
public record Circle(double radius) {
    public double area() { return Math.PI * radius * radius; }
    public double circumference() { return 2 * Math.PI * radius; }
}

// Records can implement interfaces:
public record UserEvent(String userId, String action, Instant timestamp)
    implements Comparable<UserEvent> {
    @Override
    public int compareTo(UserEvent other) {
        return this.timestamp.compareTo(other.timestamp);
    }
}
```

**When to use records:** Anywhere you'd write a plain data carrier class (DTOs, value objects, response/request objects, event payloads).

> **In plain terms** — A `record` is a one-line way to declare an immutable "just holds data" class. You write `record Point(int x, int y) {}` and get the constructor, accessors, and correct `equals`/`hashCode`/`toString` for free — the 30 lines of boilerplate (and the bugs in hand-written `equals`) simply vanish. It's the modern default for DTOs, value objects, and event payloads.

> **Going deeper** — Records are implicitly `final`, their fields are `final`, and they enforce *structural* equality (two records with equal components are equal) — exactly the [equals/hashCode contract](../02-oop/01-classes-and-objects.md#equals-and-hashcode) done right. The *compact constructor* (`public Range { ... }`, no parameter list) is the place for validation and normalization, running before the fields are assigned. Limits: a record can't extend a class (it already extends `Record`) but *can* implement interfaces, and it's for immutable data — if you need mutability or inheritance, use a regular class. Records also pair with sealed types and pattern matching (below) to form algebraic data types, and support *record patterns* (Java 21) for destructuring: `case Point(int x, int y) -> ...`.

---

## Java 16 — Pattern Matching for instanceof

```java
// Old:
if (obj instanceof String) {
    String s = (String) obj;
    System.out.println(s.toUpperCase());
}

// New (Java 16): check and bind in one step
if (obj instanceof String s) {
    System.out.println(s.toUpperCase()); // s is already String
}

// Also works in expressions:
String result = (obj instanceof String s && s.length() > 5) ? s.toUpperCase() : "short";
```

> **In plain terms** — Pattern matching merges the two steps you always did together — *check the type* and *cast to it* — into one. `if (obj instanceof String s)` both tests and gives you a ready `String s`, so you can't get the cast wrong and there's no redundant `(String) obj`.

> **Going deeper** — The bound variable's scope is *flow-sensitive*: in `obj instanceof String s && s.length() > 5`, `s` is in scope for the right side of `&&` because that only runs when the test passed; and `if (!(obj instanceof String s)) return;` makes `s` available for the rest of the method. This is the same pattern-matching machinery that powers the `switch` patterns below — `instanceof` was just where it landed first. It makes the occasional, legitimate type-branch clean while the [polymorphism guidance](../02-oop/02-inheritance-and-polymorphism.md#instanceof-and-safe-casting) still applies: prefer overriding to type-switching where you can.

---

## Java 17 — Sealed Classes

Sealed classes restrict which classes can extend or implement them. Useful for modeling closed type hierarchies.

```java
// Only the listed classes are permitted to extend Shape
public sealed interface Shape permits Circle, Rectangle, Triangle {}

public record Circle(double radius) implements Shape {}
public record Rectangle(double width, double height) implements Shape {}
public record Triangle(double base, double height) implements Shape {}

// The real power: exhaustive switch — no default needed
double area = switch (shape) {
    case Circle c     -> Math.PI * c.radius() * c.radius();
    case Rectangle r  -> r.width() * r.height();
    case Triangle t   -> 0.5 * t.base() * t.height();
    // Compiler knows all possibilities — warns if you add a new Shape type and forget a case
};
```

> **In plain terms** — A `sealed` type declares *exactly* which classes are allowed to extend/implement it (`permits Circle, Rectangle, Triangle`). It's the opposite of an open interface anyone can implement: a closed, known set. The big payoff shows up in `switch` — the compiler knows the full list, so it can verify you handled every case and you can drop the `default`.

> **Going deeper** — Sealed types let you express a real *closed* domain ("a payment is exactly Cash, Card, or Transfer") and get *exhaustiveness checking* for free: add a fourth subtype later and every `switch` that didn't handle it becomes a compile error, pointing you to each spot to update — a maintenance superpower over `if/else` or open hierarchies. Permitted subclasses must each be `final`, `sealed`, or `non-sealed` (explicitly reopening). Combined with `records`, this is Java's version of *algebraic data types* (sum types), and it's why the [switch in control flow](../01-basics/03-operators-and-control-flow.md#switch--old-vs-new) doc points here.

---

## Java 21 — Pattern Matching in Switch (Finalized)

```java
// Switch on any type (not just int/String/enum)
String describe(Object obj) {
    return switch (obj) {
        case Integer i when i < 0  -> "negative int: " + i;
        case Integer i             -> "positive int: " + i;
        case String s when s.isEmpty() -> "empty string";
        case String s              -> "string: " + s;
        case null                  -> "null";
        default                    -> "something else: " + obj;
    };
}
```

> **In plain terms** — `switch` can now match on an object's *type* (not just int/String/enum), add `when` guards for extra conditions, and even handle `null` as a case. It turns long `if (x instanceof A) ... else if (x instanceof B) ...` chains into one clean, readable block that returns a value.

> **Going deeper** — Order matters: cases are tried top-to-bottom, so put guarded/more-specific cases before their unguarded/general versions (the compiler rejects a dominated case). Over a [sealed type](#java-17--sealed-classes) the switch is *exhaustive* without a `default`; over open types you still need one. Combined with *record patterns* you can destructure in the case label itself — `case Point(int x, int y) when x == y -> "diagonal"` — extracting fields and testing them in one line. Handling `null` as an explicit `case null` is opt-in; without it, a null still throws `NullPointerException` as before, preserving old behavior.

---

## Java 21 — Virtual Threads (Project Loom)

Virtual threads are lightweight threads managed by the JVM, not the OS. You can create millions of them without exhausting system resources.

```java
// Classic OS thread: each one is expensive (~2MB stack, OS scheduling)
// Virtual thread: tiny (few KB), managed by JVM, can run millions concurrently

// Start a virtual thread:
Thread.ofVirtual().start(() -> {
    String result = fetchFromApi(); // blocks here — but virtual thread parks, not OS thread
    System.out.println(result);
});

// With ExecutorService (production pattern):
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<String>> futures = new ArrayList<>();
    for (String url : urls) {
        futures.add(executor.submit(() -> fetch(url)));
    }
    for (Future<String> f : futures) {
        System.out.println(f.get());
    }
}

// Why it matters:
// Old model: one thread per request → limited to ~few thousand concurrent requests
// Virtual threads: one virtual thread per request → millions of concurrent requests
// Great for: REST APIs, DB queries, any I/O-heavy work
// Requires: Java 21+, no Thread.sleep() in synchronized blocks
```

> **In plain terms** — A virtual thread looks and codes *exactly* like a normal thread, but it's almost free. The JVM can run millions of them, so instead of clever async/callback code, you can write plain blocking code ("call the database, wait for the answer") for each request and just spin up a virtual thread per task. Simple thread-per-request code that finally scales.

> **Going deeper** — The trick: when a virtual thread blocks on I/O, the JVM *unmounts* it from its underlying OS "carrier" thread and parks it cheaply, freeing the OS thread to run another virtual thread — so a few OS threads serve millions of virtual ones. This makes the simple blocking style competitive with reactive/async frameworks, without their complexity. Caveats matter: virtual threads help *I/O-bound* work, not CPU-bound (you still only have N cores); pre-Java-24, holding a `synchronized` lock or a native call across a blocking point "pins" the carrier thread and kills the benefit (use `ReentrantLock` instead); and you should never *pool* virtual threads — create one per task (`newVirtualThreadPerTaskExecutor`). See [multithreading](../05-advanced/02-multithreading.md) for the threading model they build on.

---

## Java 14 — Text Blocks

```java
// Without text blocks: escaping nightmare
String json = "{\n" +
              "    \"name\": \"Alice\",\n" +
              "    \"age\": 30\n" +
              "}";

// With text blocks (Java 15, stable):
String json = """
        {
            "name": "Alice",
            "age": 30
        }
        """;

// The closing """ determines indentation (whitespace stripping)
// Works great for SQL, HTML, JSON, YAML in tests
String sql = """
        SELECT u.name, o.total
        FROM users u
        JOIN orders o ON u.id = o.user_id
        WHERE u.active = true
        """;
```

> **In plain terms** — Text blocks (`"""..."""`) let you write multi-line strings — JSON, SQL, HTML — exactly as they look, without `\n` and escaped quotes everywhere. Covered with the formatting gotchas in [strings](../03-core-java/02-strings.md#string-formatting); listed here as the Java 15 milestone.

> **Going deeper** — The indentation rule is the one thing to internalize: Java strips the *common* leading whitespace, measured from the least-indented line (often the closing `"""`), so you indent the block to match surrounding code without that indentation leaking into the value. Combine with `.formatted(...)` for templating. They're a quality-of-life win especially in tests and embedded queries.

---

## Quick Version Cheat Sheet

| Version | Key Feature                        |
|---------|------------------------------------|
| Java 8  | Lambdas, Streams, Optional, default methods |
| Java 9  | Collection.of(), modules           |
| Java 10 | `var`                              |
| Java 11 | String.strip(), Files.readString() |
| Java 14 | Records (preview), switch expression |
| Java 15 | Text blocks (stable)               |
| Java 16 | Records (stable), instanceof pattern |
| Java 17 | Sealed classes (LTS)               |
| Java 21 | Virtual threads, switch patterns (LTS) |

**Recommendation:** Use Java 21 (LTS) for new projects. Most enterprise projects run on 11 or 17.

> **In plain terms** — Java ships a release every 6 months, but most of those are short-lived. The ones to care about are **LTS** (Long-Term Support) versions — 8, 11, 17, 21 — which get years of updates and are what companies actually standardize on. Pick the latest LTS you can; jump on by LTS, not by every six-month release.

> **Going deeper** — LTS releases get ~8 years of vendor support (Oracle, plus free builds like Eclipse Temurin/Adoptium), so they're the safe production target; interim releases (18, 19, 20…) are fine for trying features but lose support fast. Features often arrive as *preview* (enable with `--enable-preview`, may change) before finalizing in a later release — records and pattern matching both took this path. When choosing a target version, weigh the libraries/runtime you must support against the modern features you'd gain; greenfield → newest LTS (21), existing systems → match what's deployed (often 11 or 17).
