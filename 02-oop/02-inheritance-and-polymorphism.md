# Inheritance and Polymorphism

> Runnable example: [code/oop/AnimalDemo.java](../code/oop/AnimalDemo.java)

---

## Inheritance

A subclass (`extends`) inherits all non-private fields and methods from the parent (superclass).

```java
public class Animal {
    protected String name; // protected: accessible to subclasses

    public Animal(String name) {
        this.name = name;
    }

    public String speak() {
        return name + " makes a sound";
    }

    public String describe() {
        return "I am " + name;
    }
}

public class Dog extends Animal {
    private String breed;

    public Dog(String name, String breed) {
        super(name); // MUST call parent constructor first, or compiler errors
        this.breed = breed;
    }

    @Override // always write this — compiler catches typos
    public String speak() {
        return name + " barks!";
    }

    public String getBreed() { return breed; }
}

public class Cat extends Animal {
    public Cat(String name) {
        super(name);
    }

    @Override
    public String speak() {
        return name + " meows!";
    }
}
```

---

## Polymorphism — One Reference, Many Behaviors

Polymorphism means "many forms." A parent type variable can hold any subtype object, and the correct method is called at runtime.

```java
// The array type is Animal, but it holds Dog and Cat objects
Animal[] animals = {
    new Dog("Rex", "Labrador"),
    new Cat("Whiskers"),
    new Dog("Buddy", "Poodle")
};

for (Animal a : animals) {
    System.out.println(a.speak()); // calls Dog or Cat version — decided at RUNTIME
}
// Rex barks!
// Whiskers meows!
// Buddy barks!
```

This is **dynamic dispatch**: the JVM looks at the actual object type (not the variable type) to decide which method to call.

This is the power of polymorphism — you can add a `Bird` class later, and the loop above still works without modification.

---

## Method Overriding Rules

```java
// @Override tells the compiler: "this must override a parent method"
// Without it, a typo silently creates a NEW method instead of overriding
@Override
public String speak() { ... } // correct

public String speek() { ... } // WITHOUT @Override: new method, silent bug
                               // WITH @Override: compile error — caught early

// Rules for overriding:
// 1. Same method signature (name + parameters)
// 2. Return type must be the same or a subtype (covariant return)
// 3. Can't reduce access (public can't become private)
// 4. Can't throw new checked exceptions
```

---

## `super` — Accessing Parent

```java
public class Dog extends Animal {
    @Override
    public String speak() {
        return super.speak() + " (woof!)"; // call parent's version, then add to it
    }

    @Override
    public String describe() {
        return super.describe() + ", breed: " + breed; // reuse parent's describe()
    }
}
```

---

## `instanceof` and Safe Casting

```java
Animal a = new Dog("Rex", "Labrador");

// Old style — check before cast
if (a instanceof Dog) {
    Dog d = (Dog) a;
    System.out.println(d.getBreed());
}

// Java 16+ — pattern matching: check + cast in one step
if (a instanceof Dog d) {
    System.out.println(d.getBreed()); // d is already Dog type here
}

// Casting without checking throws ClassCastException:
Cat c = (Cat) a; // ClassCastException at runtime — a is a Dog, not a Cat
```

---

## `final` Methods and Classes

```java
public class Animal {
    // final method: subclasses CANNOT override this
    public final void breathe() {
        System.out.println(name + " is breathing");
    }
}

// final class: CANNOT be subclassed (used for security or immutability)
public final class Species {
    private final String name;
    // String, Integer, and other wrapper classes are final
}

// Any attempt to extend a final class is a compile error:
// public class ExoticSpecies extends Species {} // compile error
```

---

## Common Inheritance Mistakes

```java
// 1. Calling overridable methods in constructor — dangerous!
public class Base {
    public Base() {
        init(); // if subclass overrides init(), it runs before subclass fields are set!
    }
    public void init() {}
}

public class Child extends Base {
    private final String name = "Alice";
    @Override
    public void init() {
        System.out.println(name.toUpperCase()); // name is null here — NullPointerException!
    }
}

// 2. Deep inheritance chains — prefer composition
// BAD: Animal → Pet → Dog → TrainedDog → ServiceDog → GuideServiceDog
// Hard to reason about. Use interfaces + composition instead.

// 3. Overusing inheritance when composition is better
// If B "has an A" → composition (B has a field of type A)
// If B "is an A" → inheritance (B extends A)
```

---

## Abstract Methods

```java
// Force subclasses to implement a method:
public abstract class Shape {
    protected String color;

    public Shape(String color) { this.color = color; }

    public abstract double area(); // no body — must be implemented by subclass

    public String describe() { // concrete — subclasses inherit this
        return color + " shape, area=" + area();
    }
}

// Can't instantiate abstract class directly:
// Shape s = new Shape("red"); // compile error

public class Circle extends Shape {
    private double radius;

    public Circle(String color, double radius) {
        super(color);
        this.radius = radius;
    }

    @Override
    public double area() {
        return Math.PI * radius * radius;
    }
}
```
