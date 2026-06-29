# Classes and Objects

> Runnable example: [code/oop/BankAccountDemo.java](../code/oop/BankAccountDemo.java)

---

## The Big Picture

> **In plain terms** — Object-oriented programming is about bundling *data* and the *behavior that operates on it* into one unit (an object), instead of having data floating around and functions poking at it from outside. A **class** is the blueprint; an **object** is a thing built from that blueprint. The single most important habit is **encapsulation**: keep an object's data private and force the outside world to go through methods, so the object can guarantee it's always in a valid state.

> **Why this matters** — A `BankAccount` that lets anyone write `account.balance = -999` can't protect its own rules. By hiding the field and exposing `deposit`/`withdraw`, the object becomes the *single place* those rules live — you can't forget to check them. This is the foundation the rest of OOP ([inheritance](02-inheritance-and-polymorphism.md), [interfaces](03-interfaces-and-abstract-classes.md)) builds on: trustworthy objects with clear, minimal public surfaces.

---

## What is a Class?

A class is a blueprint. An object is an instance of that blueprint.

```java
// Blueprint (class)
public class Car {
    // Fields: what the object HAS (state)
    private String model;
    private int year;
    private boolean running;

    // Constructor: how to BUILD the object
    public Car(String model, int year) {
        this.model = model;
        this.year = year;
        this.running = false;
    }

    // Methods: what the object CAN DO (behavior)
    public void start() { this.running = true; }
    public void stop()  { this.running = false; }
    public boolean isRunning() { return running; }
    public String getModel() { return model; }
}

// Creating objects from the blueprint:
Car myCar = new Car("Toyota", 2022);
Car yourCar = new Car("Honda", 2021);
// myCar and yourCar are two independent objects in memory
```

> **In plain terms** — One class, many objects. The class describes *what every car has* (a model, a year) and *what every car can do* (start, stop); each `new Car(...)` is a separate car with its own values. Fields are the nouns (state), methods are the verbs (behavior), and the constructor is the assembly line that builds a valid one.

> **Going deeper** — `new` allocates the object on the heap and returns a reference to it (revisit [stack vs heap](../01-basics/02-variables-and-types.md#stack-vs-heap)); `myCar` and `yourCar` are independent references to independent objects, so changing one never affects the other. Each object also carries a header (used for its class pointer, identity hash, and locking) on top of its fields — which is why millions of tiny objects cost more memory than you'd guess. Classes themselves are loaded lazily by the classloader the first time they're used.

---

## Encapsulation — The Most Important OOP Principle

Encapsulation means hiding your object's internal state and only allowing controlled access.

```java
// BAD: exposing fields directly
public class BankAccount {
    public double balance; // anyone can write account.balance = -999999
}

// GOOD: private fields + controlled access through methods
public class BankAccount {
    private double balance;
    private final String accountId;

    public BankAccount(String accountId, double initialBalance) {
        if (initialBalance < 0) throw new IllegalArgumentException("Balance can't be negative");
        this.accountId = accountId;
        this.balance = initialBalance;
    }

    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit must be positive");
        this.balance += amount;
    }

    public void withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Withdrawal must be positive");
        if (amount > balance) throw new IllegalStateException("Insufficient funds");
        this.balance -= amount;
    }

    public double getBalance() { return balance; }   // read-only — no setter
    public String getAccountId() { return accountId; }
}
```

The outside world can only interact with a `BankAccount` through its methods. The methods enforce all the business rules. This is the core idea of OOP.

> **In plain terms** — Encapsulation is the difference between a vending machine and an open cash box. You don't reach inside and grab money/snacks directly; you use the buttons and slot, and the machine enforces the rules (correct change, item in stock). Private fields are the locked interior; public methods are the buttons.

> **Going deeper** — The real payoff is that you can change the *implementation* (store balance in cents, add logging, switch to a database) without breaking any caller, because they only ever touched the public methods — your *invariants* (rules that must always hold) are protected at a single chokepoint. Note that getters returning mutable objects quietly break encapsulation: `getItems()` handing back the internal `List` lets callers mutate your state behind your back — return a copy or `List.copyOf(...)`. The strongest form is *immutability*: no setters at all, so the object can never enter a bad state.

---

## Constructors In Depth

```java
public class Employee {
    private final String name;
    private final String id;
    private int salary;

    // Primary constructor
    public Employee(String name, String id, int salary) {
        this.name = name;
        this.id = id;
        this.salary = salary;
    }

    // Convenience constructor — delegates to primary
    public Employee(String name, String id) {
        this(name, id, 50000); // must be the FIRST statement
    }

    // Copy constructor
    public Employee(Employee other) {
        this(other.name, other.id, other.salary);
    }
}

Employee e1 = new Employee("Alice", "E001", 75000);
Employee e2 = new Employee("Bob", "E002"); // salary defaults to 50000
Employee e3 = new Employee(e1);            // deep copy
```

> **In plain terms** — A constructor's job is to hand back a fully-built, valid object — so do all your validation here. You can offer several constructors for different convenience levels, and have the simpler ones *delegate* to the main one with `this(...)` so the real logic lives in exactly one place.

> **Going deeper** — If you write no constructor, Java gives you a no-arg default; write *any* constructor and that freebie disappears. `this(...)` (constructor chaining) must be the first statement; `super(...)` calls the parent constructor and runs before your body (see [inheritance](02-inheritance-and-polymorphism.md)). Two cautions: never call an *overridable* method from a constructor — the subclass override runs before the subclass is initialized, seeing half-built state; and the "copy constructor" here is shallow — if `Employee` held a mutable `List`, both copies would share it unless you copy that too. For many constructor parameters, prefer the [Builder pattern](../05-advanced/04-design-patterns.md) or a `record`.

---

## `this` Keyword

```java
public class Point {
    private int x;
    private int y;

    public Point(int x, int y) {
        this.x = x; // `this.x` = field, `x` = parameter
        this.y = y;
    }

    public Point translate(int x, int y) {
        return new Point(this.x + x, this.y + y); // `this.x` to be explicit
    }

    public void copyFrom(Point other) {
        this.x = other.x;
        this.y = other.y;
    }
}
```

`this` refers to the **current instance** of the object the method is called on.

> **In plain terms** — `this` means "the specific object this method was called on." Its most common use is disambiguating a field from a parameter with the same name (`this.x = x`): left side is the object's field, right side is the incoming value.

> **Going deeper** — Every instance method has an invisible `this` parameter passed in automatically — that's literally why `static` methods (which belong to no instance) can't use it. `this` also enables a *fluent* style: methods that `return this` can be chained (`builder.name("a").age(30)`). Inside an inner class, `OuterClass.this` reaches the enclosing instance. Lambdas, notably, capture the *enclosing* `this` rather than creating their own — a deliberate difference from anonymous classes (see [lambdas](../04-java8-modern/01-lambdas-and-functional.md)).

---

## `static` — Belongs to the Class, Not an Instance

```java
public class Employee {
    private static int totalCount = 0;  // ONE copy shared across all instances
    private final int id;
    private String name;

    public Employee(String name) {
        totalCount++;            // every new Employee increments the class-level counter
        this.id = totalCount;
        this.name = name;
    }

    // static method — no instance needed, no `this`
    public static int getTotalCount() {
        return totalCount;
    }

    // instance method — needs an object
    public String getName() {
        return name;
    }
}

// Call static method on the class:
System.out.println(Employee.getTotalCount()); // 0

Employee e1 = new Employee("Alice");
Employee e2 = new Employee("Bob");
System.out.println(Employee.getTotalCount()); // 2
```

> **In plain terms** — A `static` field is shared by *every* object of the class — there's exactly one copy, like a whiteboard the whole class writes on (here, a running count of how many `Employee`s exist). Instance fields are per-object; static fields are per-class.

> **Going deeper** — Static fields live for the lifetime of the class (effectively the whole program), which makes mutable static state a classic source of memory leaks and *thread-safety* bugs: the `totalCount++` above is not atomic, so concurrent constructors can lose increments — use `AtomicInteger` if that matters. Use `static final` for genuine constants, and a `static {}` initializer block for one-time setup. Avoid mutable global static state as a general design; it's effectively a hidden global variable and makes code hard to test in isolation.

---

## Access Modifiers

| Modifier    | Same Class | Same Package | Subclass | Any Class |
|-------------|------------|--------------|----------|-----------|
| `private`   | ✓          | ✗            | ✗        | ✗         |
| (none)      | ✓          | ✓            | ✗        | ✗         |
| `protected` | ✓          | ✓            | ✓        | ✗         |
| `public`    | ✓          | ✓            | ✓        | ✓         |

**Default rule:** make everything `private` until you have a reason to expose it.

> **In plain terms** — These four levels control *who can see what*, from `private` (only this class) out to `public` (everyone). Start locked down and open up only when a real caller needs access — it's far easier to widen access later than to take it back once code depends on it.

> **Going deeper** — Note the often-forgotten one: *package-private* (no keyword) is broader than `private` — any class in the same package can reach it — which is great for letting collaborating classes and tests in without going fully `public`. `protected` also leaks to *any* subclass, even in other packages, so it's a real part of your API contract, not "almost private." The Java Platform Module System (Java 9+) adds a layer on top: a package isn't visible outside its module unless explicitly `exports`-ed, regardless of `public`.

---

## `final` — Preventing Changes

```java
// final field: must be assigned once (in declaration or constructor), never changed
private final String name;  // can't change after construction

// final method: cannot be overridden by subclasses
public final void auditLog() { ... }

// final class: cannot be subclassed (String, Integer are final)
public final class ImmutablePoint {
    private final int x;
    private final int y;
    // ...
}

// final variable: can't be reassigned after first assignment
final int MAX = 100;
MAX = 200; // compile error
```

> **In plain terms** — `final` means "this can't be reassigned after it's set." On a field it makes the value fixed once the object is built; on a method it blocks subclasses from changing the behavior; on a class it blocks subclassing entirely. It's a promise to readers (and the compiler) that something won't change.

> **Going deeper** — A crucial subtlety: `final` freezes the *reference*, not the *object*. `final List<String> xs` can't be reassigned, but you can still `xs.add(...)` — for true immutability you need `List.copyOf(...)` or an unmodifiable view. `final` fields also carry a JMM (Java Memory Model) guarantee: their values are safely visible to other threads after construction without synchronization, which is part of why immutable objects are inherently thread-safe. `final` classes/methods also give the JIT more freedom to inline. Records and `enum` constants are implicitly final.

---

## `equals()` and `hashCode()`

If you override `equals()`, you **must** also override `hashCode()`. They are a contract.

```java
public class Point {
    private final int x;
    private final int y;

    // Two Points are equal if they have the same x and y
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;                // same reference
        if (!(o instanceof Point other)) return false; // null or wrong type
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y); // use java.util.Objects — consistent with equals
    }
}

// Why hashCode matters:
Set<Point> set = new HashSet<>();
set.add(new Point(1, 2));
set.contains(new Point(1, 2)); // true ONLY if hashCode is consistent with equals
```

In practice, use **records** (Java 16+) for simple data classes — they generate all of this automatically.

> **In plain terms** — By default Java thinks two objects are equal only if they're literally the *same* object. If you want "same contents = equal" (two `Point(1,2)` should match), you override `equals`. And you *must* also override `hashCode`, because hash-based collections (`HashMap`, `HashSet`) first use the hash code to find the right bucket — if equal objects had different hash codes, the set would never find your item.

> **Going deeper** — The contract: equal objects **must** have equal hash codes (the reverse isn't required — collisions are fine). Breaking it means `set.contains(equalObject)` returns `false` and map lookups silently miss. The full `equals` contract is also reflexive, symmetric, transitive, and consistent — subclassing while preserving symmetry is genuinely hard (a `ColorPoint` vs `Point` can't satisfy it cleanly), which is one reason to *favor composition over inheritance* and to use `record`s, which generate a correct `equals`/`hashCode`/`toString` for you. Also avoid putting *mutable* fields in `equals`/`hashCode`: mutating an object after it's in a `HashSet` corrupts the set.
