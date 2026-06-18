# Classes and Objects

> Runnable example: [code/oop/BankAccountDemo.java](../code/oop/BankAccountDemo.java)

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

---

## Access Modifiers

| Modifier    | Same Class | Same Package | Subclass | Any Class |
|-------------|------------|--------------|----------|-----------|
| `private`   | ✓          | ✗            | ✗        | ✗         |
| (none)      | ✓          | ✓            | ✗        | ✗         |
| `protected` | ✓          | ✓            | ✓        | ✗         |
| `public`    | ✓          | ✓            | ✓        | ✓         |

**Default rule:** make everything `private` until you have a reason to expose it.

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
