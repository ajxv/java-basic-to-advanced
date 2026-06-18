# Interfaces and Abstract Classes

> Runnable example: [code/oop/InterfaceDemo.java](../code/oop/InterfaceDemo.java)

---

## The Fundamental Difference

| Feature                    | Abstract Class        | Interface                  |
|----------------------------|-----------------------|----------------------------|
| Fields                     | Yes (any type)        | Only `public static final` |
| Constructors               | Yes                   | No                         |
| Concrete methods           | Yes                   | Yes (`default`, Java 8+)   |
| Multiple inheritance       | No (single extends)   | Yes (multiple implements)  |
| Use case                   | Shared state + logic  | Defining a contract/capability |

---

## Abstract Class — Shared State + Partial Implementation

Use when subclasses share common state (fields) or implementation.

```java
public abstract class ReportGenerator {
    protected String title;
    protected List<String> data;

    // Constructor: shared initialization
    public ReportGenerator(String title, List<String> data) {
        this.title = title;
        this.data = data;
    }

    // Abstract: subclass decides HOW to format
    public abstract String generate();

    // Concrete: shared logic all subclasses use
    public void printToConsole() {
        System.out.println("=== " + title + " ===");
        System.out.println(generate());
    }

    protected String header() {
        return "Report: " + title + "\nDate: " + LocalDate.now();
    }
}

public class CsvReportGenerator extends ReportGenerator {
    public CsvReportGenerator(String title, List<String> data) {
        super(title, data);
    }

    @Override
    public String generate() {
        return header() + "\n" + String.join(",", data);
    }
}

public class HtmlReportGenerator extends ReportGenerator {
    public HtmlReportGenerator(String title, List<String> data) {
        super(title, data);
    }

    @Override
    public String generate() {
        return "<h1>" + title + "</h1><ul>" +
               data.stream().map(d -> "<li>" + d + "</li>").collect(Collectors.joining()) +
               "</ul>";
    }
}
```

---

## Interface — Defining a Contract

Use when you want to define what something *can do*, regardless of what it *is*.

```java
public interface Exportable {
    byte[] export(); // abstract: implementors must define this
    String mimeType();

    // default: optional override — gives behavior for free
    default String filename() {
        return "export_" + System.currentTimeMillis();
    }

    // static: utility method on the interface itself
    static boolean isSupportedFormat(String format) {
        return Set.of("json", "csv", "xml").contains(format.toLowerCase());
    }
}

public interface Compressible {
    byte[] compress(byte[] data);
}

// One class can implement MULTIPLE interfaces:
public class JsonExporter implements Exportable, Compressible {
    @Override
    public byte[] export() { return toJson().getBytes(); }

    @Override
    public String mimeType() { return "application/json"; }

    @Override
    public byte[] compress(byte[] data) { return gzip(data); }

    private String toJson() { /* ... */ return "{}"; }
    private byte[] gzip(byte[] data) { /* ... */ return data; }
}
```

---

## When to Use Which

```
Dog IS-A Animal            → extends Animal (abstract class)
Dog CAN-DO things          → implements Runnable, Serializable (interfaces)

Car IS-A Vehicle           → extends Vehicle
Car CAN-DO things          → implements Refuelable, Registerable
```

**Prefer interfaces** in most cases, especially for new code.
- They're more flexible (multiple implementation)
- They allow mocking in tests
- They decouple code

**Use abstract class** when:
- You truly have shared mutable state
- You need a constructor
- You want to provide non-trivial default behavior that involves fields

---

## Interface Default Methods (Java 8+)

Default methods let you add new behavior to an interface without breaking existing implementors.

```java
public interface Collection<E> {
    // Added in Java 8 — existing classes like ArrayList don't need to implement it
    default void forEach(Consumer<? super E> action) {
        for (E e : this) action.accept(e);
    }
}

// If two interfaces have the same default method, the class must override it:
public interface A {
    default String hello() { return "Hello from A"; }
}
public interface B {
    default String hello() { return "Hello from B"; }
}
public class C implements A, B {
    @Override
    public String hello() {
        return A.super.hello(); // explicitly choose which one
    }
}
```

---

## Functional Interfaces

Any interface with exactly ONE abstract method is a **functional interface**. It can be used with a lambda.

```java
@FunctionalInterface // optional but documents intent and enforces one-abstract-method rule
public interface Validator<T> {
    boolean validate(T value);

    // Can still have default methods — doesn't count as abstract
    default Validator<T> and(Validator<T> other) {
        return value -> this.validate(value) && other.validate(value);
    }
}

// Usage with lambda:
Validator<String> notEmpty = s -> !s.isEmpty();
Validator<String> notTooLong = s -> s.length() <= 100;
Validator<String> combined = notEmpty.and(notTooLong);

combined.validate("hello"); // true
combined.validate("");      // false
```

---

## Interface Segregation — Don't Make Fat Interfaces

```java
// BAD: one fat interface forces classes to implement irrelevant methods
public interface Worker {
    void work();
    void eat();
    void sleep();
}

// Robot can work but doesn't eat or sleep — forced to add empty methods:
public class Robot implements Worker {
    public void work() { /* real */ }
    public void eat() { /* empty stub */ }
    public void sleep() { /* empty stub */ }
}

// GOOD: separate interfaces
public interface Workable { void work(); }
public interface Eatable  { void eat(); }
public interface Sleepable { void sleep(); }

public class Human implements Workable, Eatable, Sleepable { ... }
public class Robot implements Workable { ... } // only what's relevant
```
