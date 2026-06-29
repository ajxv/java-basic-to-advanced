# Inheritance and Polymorphism

> Runnable example: [code/oop/AnimalDemo.java](../code/oop/AnimalDemo.java)

---

## The Big Picture

> **In plain terms** — *Inheritance* lets one class build on another: a `Dog` is an `Animal`, so it gets all of `Animal`'s fields and methods for free and can add or change its own. *Polymorphism* is the payoff: you can treat a whole pile of different animals as plain `Animal`s and call `speak()` on each — and every one does *its own* thing. You write code against the general type, and the specific behavior is filled in automatically.

> **Why this matters** — Polymorphism is what lets you add a new `Bird` class next month without touching the loop that processes animals — the existing code keeps working. That's the open/closed principle in action. The catch, drilled into this whole topic: inheritance is the *tightest* coupling between two classes, so reach for it only on a genuine "is-a" relationship. When you're tempted to inherit just to reuse code, prefer **composition** ("has-a") or [interfaces](03-interfaces-and-abstract-classes.md) instead.

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

> **In plain terms** — `extends` means "is a kind of." `Dog extends Animal` says every dog *is an* animal, so a dog automatically has a name and can `describe()` itself, while overriding `speak()` to bark. `super(name)` runs the parent's setup first so the inherited parts are ready before the dog adds its own.

> **Going deeper** — Java has *single* inheritance for classes (one parent only) — it sidesteps the "diamond problem" of multiple inheritance, and you get multiple-type flexibility from interfaces instead. Every class implicitly extends `Object`, so `toString`/`equals`/`hashCode` are always there to override. `protected` exposes a field to subclasses but also to the whole package — and it makes that field part of your subclassing contract, so prefer `private` fields with `protected` accessors. Constructors are *not* inherited; each subclass must define its own and chain up with `super(...)`.

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

> **In plain terms** — The *variable's* type (`Animal`) decides what methods you're *allowed* to call; the *object's* real type (`Dog`) decides which *version* actually runs. So even though you're looping over `Animal`s, each one barks or meows correctly. This is why coding to the general type doesn't cost you the specific behavior.

> **Going deeper** — This runtime selection is *dynamic dispatch*, implemented via a per-class method table (vtable) the JVM consults by the object's actual type — which is exactly why `static`, `private`, and `final` methods (not overridable) are dispatched statically and slightly cheaper. Contrast this with [overloading](../01-basics/04-methods.md#method-overloading), resolved at *compile time* by declared argument types — mixing the two up causes subtle bugs. Fields are *not* polymorphic either: they're resolved by the reference type, so a subclass field with the same name *shadows* rather than overrides. The whole mechanism underpins the Liskov Substitution Principle: a subtype must be safely usable anywhere its supertype is expected.

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

> **In plain terms** — Always put `@Override` on a method meant to replace a parent's. It costs nothing and turns a silent, baffling bug (you misspelled the name, so you accidentally created a brand-new method and the parent's version still runs) into an instant compile error.

> **Going deeper** — The rules exist to preserve substitutability: an override can *widen* access and *narrow* the return type (covariant returns, e.g. override `Object clone()` to return `Dog`) and may drop or narrow checked exceptions — but never the reverse, or callers holding the parent type could be surprised. `@Override` also works for interface methods (since Java 6). You *cannot* override `static`, `final`, or `private` methods — a same-named `static` in a subclass *hides* the parent's (resolved by declared type, not the object), which looks like overriding but isn't.

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

> **In plain terms** — `super.method()` calls the parent's version of a method, letting you *extend* rather than *replace* it — do what the parent did, then add your bit. It's how `Dog.describe()` reuses `Animal.describe()` instead of copy-pasting it.

> **Going deeper** — `super` bypasses dynamic dispatch and calls the *exact* parent implementation, which is the one safe way to invoke an overridden method without re-triggering the override. Unlike `this(...)`/`super(...)` *constructor* calls (which must be the first statement), `super.method()` can appear anywhere. There's no `super.super` — you can only reach one level up, by design, so a class can't skip its parent to bind to a grandparent's behavior.

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

> **In plain terms** — `instanceof` asks "is this object actually a Dog?" before you treat it like one. The modern `if (a instanceof Dog d)` form checks *and* gives you a ready-to-use `Dog` variable in one line, so you can't forget the cast or get it wrong.

> **Going deeper** — Reaching for `instanceof` chains is often a smell — it usually means behavior that *should* live as an overridden method on each subtype (let polymorphism choose). The legitimate uses are `equals` implementations and handling genuinely unrelated types. Where you *do* branch by type, modern Java makes it safe and exhaustive: pattern matching in `switch` over a **sealed** hierarchy (Java 21) lets the compiler verify you've covered every case — see [sealed types & pattern matching](../04-java8-modern/04-modern-java-9-to-21.md). The bound variable (`d`) is also scope-aware: `if (!(a instanceof Dog d)) return;` makes `d` usable for the rest of the method.

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

> **In plain terms** — `final` here is the "do not extend / do not override" sign. Marking a method `final` locks its behavior so no subclass can change it; marking a class `final` says "this class is complete, don't build on it." Use it to protect logic that must stay exactly as written.

> **Going deeper** — *Effective Java*'s advice: "design and document for inheritance, or else prohibit it." Subclassing is fragile — a parent that changes how its own methods call each other can silently break subclasses (the "fragile base class" problem). So either carefully document which methods are overridable and how they interact, or make the class `final` and offer composition instead. `final` classes/methods also let the JIT inline and devirtualize more aggressively, a minor performance win.

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

> **In plain terms** — The big traps: don't call methods that subclasses might override from inside a constructor (the override runs before the child is fully built, so it sees half-set fields), don't stack inheritance five levels deep, and don't inherit just to grab some code. The simple test: "B *is a* kind of A" → inheritance; "B *has an* A" → composition (hold it as a field).

> **Going deeper** — Composition is usually the better default because it's *flexible at runtime* (swap the held object, even via an interface) and *loosely coupled* (you depend on a small surface, not a whole parent's internals). The constructor-calls-overridable-method bug is the same one from [classes & objects](01-classes-and-objects.md#constructors-in-depth): initialization order is parent-then-child, so the child's override fires against uninitialized `final` fields — note even the `name = "Alice"` initializer hasn't run yet. The `java.util.Stack extends Vector` mistake (a stack is *not* a vector, yet exposes `add(index)`) is the textbook example of inheriting for reuse and leaking the wrong API.

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

> **In plain terms** — An `abstract` method is a promise with no body: "every concrete subclass must provide this." `Shape` knows every shape *has* an area but can't compute it generically, so it declares `area()` abstract and lets each shape fill it in. You can't create a `Shape` directly — only real shapes like `Circle`.

> **Going deeper** — An abstract class is the middle ground between a normal class and an interface: it can hold state (fields), constructors, and *some* implemented methods, while forcing subclasses to supply the rest — perfect for the *Template Method* pattern, where a concrete method defines the skeleton and calls abstract steps. Choosing between abstract class and [interface](03-interfaces-and-abstract-classes.md): use an abstract class when subtypes share state and a clear "is-a" identity (single inheritance only); use an interface when you want a capability many unrelated types can opt into (multiple allowed). Since Java 8, interfaces have `default` methods, narrowing the gap — but only abstract classes can carry instance fields.
