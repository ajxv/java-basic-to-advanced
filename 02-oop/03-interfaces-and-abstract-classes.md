# Interfaces and Abstract Classes

> Runnable example: [code/oop/InterfaceDemo.java](../code/oop/InterfaceDemo.java)

---

## The Big Picture

> **In plain terms** — Both interfaces and abstract classes let you program against a *general type* without caring about the concrete one. The mental split: an **interface** describes *what something can do* (a capability/contract — "anything Comparable can be sorted"), while an **abstract class** is a *half-finished class* that shares real state and code with its children. A class can implement many interfaces but extend only one (abstract) class.

> **Why this matters** — Interfaces are the seam that makes code testable and swappable: depend on `PaymentGateway` (the interface), and you can drop in a real Stripe implementation in production and a fake in tests without changing a line of the calling code. That's *dependency inversion* — high-level code depends on abstractions, not concrete classes. The modern default is "**code to interfaces**," reaching for an abstract class only when subtypes genuinely share state and logic.

---

## The Fundamental Difference

| Feature                    | Abstract Class        | Interface                  |
|----------------------------|-----------------------|----------------------------|
| Fields                     | Yes (any type)        | Only `public static final` |
| Constructors               | Yes                   | No                         |
| Concrete methods           | Yes                   | Yes (`default`, Java 8+)   |
| Multiple inheritance       | No (single extends)   | Yes (multiple implements)  |
| Use case                   | Shared state + logic  | Defining a contract/capability |

> **In plain terms** — Read this table as: abstract classes can hold *stuff* (mutable fields, constructors) and so model a shared *identity*; interfaces hold only constants and method signatures (plus `default` bodies since Java 8) and so model a shared *ability*. The killer difference is the last two rows — multiple interfaces, single parent class.

> **Going deeper** — Since Java 8–9 the line has blurred: interfaces now have `default` and `static` methods, and even `private` helper methods (Java 9) to share code between defaults — so an interface can carry behavior, just not *instance state*. That "no instance fields" rule is the real dividing line: an interface can't remember anything per-object. The other true-but-subtle one: interface fields are implicitly `public static final` (constants), so don't try to use an interface as a place to stash mutable shared data.

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

> **In plain terms** — `ReportGenerator` does the parts every report shares (hold the title/data, print to console, build a header) and leaves only the truly format-specific part (`generate()`) for each subclass. The CSV and HTML generators write *only* what's different. That's the abstract class's sweet spot: real shared code plus a few required blanks.

> **Going deeper** — This is the **Template Method** pattern: a concrete method (`printToConsole`) defines the fixed algorithm and calls an abstract step (`generate`) that subclasses fill in — the parent controls the *flow*, children control the *details*. It only works because of dynamic dispatch (the parent's call to `generate()` reaches the subclass override). The trade-off is the inheritance coupling from the [previous doc](02-inheritance-and-polymorphism.md): subclasses are bound to this base's lifecycle and `protected` surface, so keep that shared surface small and stable.

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

> **In plain terms** — An interface is a checklist of abilities. `JsonExporter` promises to be both `Exportable` *and* `Compressible`, so any code that needs "something that can export" or "something that can compress" will accept it. One class, many capabilities — something single inheritance can't give you.

> **Going deeper** — This composability is exactly why the JDK is built on small interfaces (`Comparable`, `Iterable`, `AutoCloseable`, `Runnable`): a type opts into each capability independently. `default` methods let an interface ship behavior (handy for evolving an API without breaking implementors), and `static` methods put related factories/helpers right on the interface (`Comparator.comparing`, `List.of`). Keep interfaces focused — the more abilities you cram into one, the fewer types can honestly implement it (the segregation point at the end of this doc).

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

> **In plain terms** — Decide with two questions. "Is X a kind of Y?" leans toward an abstract class (a true family relationship with shared guts). "Can X do Z?" leans toward an interface (a capability X opts into). When unsure, pick the interface — it keeps your options open because a class can add more interfaces later but can never gain a second parent.

> **Going deeper** — A powerful combo is *both*: publish an interface (`List`) and provide an abstract *skeletal* implementation (`AbstractList`) that does the boring parts, so implementors get the contract's flexibility plus most of the code for free — the JDK uses this everywhere. Interfaces also win for testing (trivially mockable) and for the dependency-inversion seam mentioned up top. The one thing only an abstract class can do remains decisive: hold per-instance mutable state and a constructor that enforces invariants across all subtypes.

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

> **In plain terms** — A `default` method is a method *with a body* inside an interface. It lets the language designers (or you) add a new method to an interface and supply a ready-made implementation, so the thousands of existing classes that implement that interface don't suddenly fail to compile.

> **Going deeper** — Default methods are why `Collection.stream()` and `forEach` could be added in Java 8 without breaking the world — the single biggest reason they exist. They reintroduce a mild "diamond problem": if you inherit the same default from two interfaces, the compiler forces you to resolve it explicitly with `Interface.super.method()` (as shown). Resolution rules: a concrete method in a *class* always beats an interface default, and a more-specific interface beats a less-specific one. Use defaults for genuine convenience methods, not as a backdoor to multiple inheritance of behavior-plus-state (they still can't hold state).

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

> **In plain terms** — If an interface has exactly *one* method that needs implementing, you can supply it with a tiny lambda (`s -> !s.isEmpty()`) instead of a whole class. That one-method shape is called a *functional interface*, and it's the bridge between OOP and the functional style in the next part of this guide.

> **Going deeper** — `@FunctionalInterface` is optional but worth adding: it makes the compiler enforce the single-abstract-method rule, so a teammate can't accidentally add a second abstract method and break every lambda. Default and static methods don't count against the limit, which is how `Validator` can offer a fluent `.and(...)` combinator. The JDK ships ready-made functional interfaces (`Function`, `Predicate`, `Consumer`, `Supplier`) so you rarely need to declare your own — see [lambdas & functional interfaces](../04-java8-modern/01-lambdas-and-functional.md).

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

> **In plain terms** — Don't cram unrelated abilities into one big interface. If a `Robot` is forced to implement `eat()` and `sleep()` just to be a `Worker`, you end up with meaningless empty methods. Split the fat interface into small focused ones so each class implements only what genuinely applies to it.

> **Going deeper** — This is the "I" in SOLID — the *Interface Segregation Principle*: no client should be forced to depend on methods it doesn't use. Empty stub implementations are the tell-tale smell; they often throw `UnsupportedOperationException` at runtime, which is a compile-time problem in disguise (the old `java.util` `Collection` immutability stubs are a cautionary example). Small, role-based interfaces also compose better (a class mixes exactly the roles it needs) and make mocking in tests trivial. Rule of thumb: name interfaces by capability (`-able`) and keep them as narrow as the callers that consume them.
