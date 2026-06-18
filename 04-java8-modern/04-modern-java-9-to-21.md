# Modern Java: 9 to 21

> Runnable example: [code/modern/ModernJavaDemo.java](../code/modern/ModernJavaDemo.java)

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
