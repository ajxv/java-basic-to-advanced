# Methods: The Building Blocks

> Runnable example: [code/basics/MethodsDemo.java](../code/basics/MethodsDemo.java)

---

## The Big Picture

> **In plain terms** — A method is a named, reusable recipe: you hand it some ingredients (parameters), it does work, and it usually hands something back (the return value). Methods let you write a piece of logic once and call it from everywhere, and they let you read code at a high level ("validate, then save") without drowning in detail. Good programs are mostly small, well-named methods calling each other.

> **Why this matters** — Two ideas in this file trip up even experienced developers: (1) **how arguments are passed** — Java *always* copies the value you pass, which behaves very differently for primitives vs object references; and (2) **overloading vs overriding** — same-named methods chosen by the *compiler* (by argument types) vs by the *runtime* (by actual object type). Getting these straight prevents a surprising number of "why didn't my change take effect?" bugs.

---

## Method Anatomy

```java
//  access   static?   return   name      parameters
    public   static    int      add(      int a, int b) {
        return a + b;
    }
```

- **access modifier** — `public`, `private`, `protected`, or package-private (no keyword)
- **static** — belongs to the class, not an instance. Optional.
- **return type** — the type of value the method sends back. `void` means nothing is returned.
- **name** — camelCase by convention
- **parameters** — inputs; can be zero or more

> **In plain terms** — Read a signature left to right as "who can call it, whether it needs an object, what it gives back, its name, and what it takes in." The *signature* (name + parameter types) is the method's identity — it's what callers depend on and what overloading and overriding key off of.

> **Going deeper** — A method's signature for overload resolution is **name + parameter types only** — the return type is *not* part of it, which is why you can't have two methods differing only by return type. Methods can also carry a `throws` clause (checked exceptions), type parameters (`<T> T first(List<T> xs)`), and annotations like `@Override`. Prefer the *narrowest* access that works (default to `private`) — it shrinks your public surface and keeps refactors cheap.

---

## Pass by Value — Java's Most Misunderstood Rule

**Java always passes by value.** For objects, the value being passed is the *reference* (memory address), not a copy of the object.

```java
// Primitive: the local copy changes, original is unaffected
public static void tryChange(int x) {
    x = 100; // only the local copy changes
}

int n = 5;
tryChange(n);
System.out.println(n); // 5 — unchanged

// Object: you can MUTATE the object (because you have the address)
// but you cannot REASSIGN the caller's reference
public static void tryMutate(StringBuilder sb) {
    sb.append(" world");          // mutates the object — visible to caller
    sb = new StringBuilder("new"); // reassigns LOCAL variable only — caller unaffected
}

StringBuilder sb = new StringBuilder("hello");
tryMutate(sb);
System.out.println(sb); // "hello world" — mutation visible, reassignment not
```

**Mental model:** Java passes a copy of the pointer, not a pointer to the pointer.

> **In plain terms** — Imagine handing someone a *photocopy* of a sticky note that has a house address on it. They can drive to that house and rearrange the furniture (mutate the object) — you'll see those changes. But if they scribble a *different* address on their copy (reassign the reference), your original note is untouched. That's exactly why `sb.append(...)` is visible to the caller but `sb = new StringBuilder(...)` is not.

> **Going deeper** — This is why "Java is pass-by-reference" is a persistent myth: it passes references *by value*. Practical consequences: a method can never swap two of the caller's variables, and a method that mutates its argument is doing something invisible from the call site — a common source of bugs. Favor returning new values over mutating parameters; mark parameters you don't intend to reassign as `final` for clarity; and lean on immutable types (`record`, `String`, `List.copyOf`) so "can this method change my object?" is answerable from the type alone.

---

## Return Types

```java
// Single value
public int square(int n) { return n * n; }

// Void (no return)
public void printName(String name) {
    System.out.println(name);
    // no return statement needed
}

// Returning multiple values: use a record or a custom class
public record Pair<A, B>(A first, B second) {}

public Pair<String, Integer> getNameAndAge() {
    return new Pair<>("Alice", 30);
}
```

> **In plain terms** — A method gives back at most one thing. Need to return "no value at all"? Use `void`. Need to return "several values"? Bundle them into one object — a `record` is the cleanest way to do that in modern Java.

> **Going deeper** — Prefer returning an empty collection over `null` (callers can loop without a null check), and an [`Optional<T>`](../04-java8-modern/03-optional.md) over `null` when "nothing" is a real, expected outcome — it makes the absence part of the type. Avoid returning `null` from methods whose name implies a collection or count. A method that returns a value *and* has side effects (mutates state, does I/O) is harder to reason about and test — keep "command" methods (do something, return `void`) separate from "query" methods (compute and return) where you can.

---

## Method Overloading

Same method name, different parameter lists. Resolved at **compile time**.

```java
public int add(int a, int b)             { return a + b; }
public double add(double a, double b)    { return a + b; }
public int add(int a, int b, int c)      { return a + b + c; }
public String add(String a, String b)    { return a + b; }

// The compiler picks the right version based on argument types:
add(1, 2);         // int version
add(1.5, 2.5);     // double version
add("Hello", " World"); // String version
```

Overloading is resolved at **compile time** (unlike overriding, which is runtime).

> **In plain terms** — Overloading is reusing a method *name* for variations that take different inputs (`add(int,int)` vs `add(double,double)`). The compiler picks the matching version by looking at the argument types you passed — it's decided while compiling, not while running. Keep overloads doing the *same conceptual thing*; surprising callers with wildly different behavior per overload is a trap.

> **Going deeper** — Resolution uses the *compile-time (declared)* types of arguments, applying the most specific match and considering widening, then autoboxing, then varargs — in that priority order. This static nature is the key contrast with [overriding](../02-oop/02-inheritance-and-polymorphism.md), which is chosen at runtime by the object's actual type. The classic gotcha: `null` is ambiguous between two reference overloads (you must cast), and `remove(int)` vs `remove(Object)` on `List` famously bites people — `list.remove(2)` removes *index* 2, `list.remove(Integer.valueOf(2))` removes the *value* 2.

---

## Varargs

Accept a variable number of arguments.

```java
public int sum(int... numbers) { // numbers is an int[] internally
    int total = 0;
    for (int n : numbers) total += n;
    return total;
}

sum(1, 2, 3);        // 6
sum(1, 2, 3, 4, 5);  // 15
sum();               // 0 — empty array

// Rules:
// 1. Only one varargs parameter per method
// 2. Must be the LAST parameter
public void log(String prefix, String... messages) {
    for (String msg : messages) System.out.println(prefix + ": " + msg);
}
```

> **In plain terms** — Varargs (`int... numbers`) lets callers pass any number of arguments — zero, one, or fifty — and inside the method you just treat them as an array. It's what makes convenience methods like `List.of(...)` and `String.format(...)` possible.

> **Going deeper** — Under the hood varargs *is* an array, so the JVM allocates one on every call — fine for occasional use, worth avoiding on a hot path (this is why APIs often provide fixed-arity overloads for 0–2 args plus a varargs catch-all). Two sharp edges: passing an existing array works directly (`sum(arr)`), but passing a single `null` is ambiguous and triggers an "unchecked/varargs" warning with generic element types — hence `@SafeVarargs` on methods like `List.of`. You can call a varargs method with zero arguments, so always handle the empty-array case.

---

## Static vs Instance Methods

```java
public class MathUtils {
    // Static: call on the class, no instance needed
    public static int square(int n) { return n * n; }

    // Instance: call on an object
    private int base;
    public int squareWithBase() { return base * base; }
}

// Usage:
MathUtils.square(5);     // static — no object needed

MathUtils util = new MathUtils();
util.squareWithBase();   // instance — needs an object
```

**When to make a method static:**
- It doesn't use any instance fields
- It's a utility function (pure function — same input always gives same output)
- Factory methods

> **In plain terms** — An *instance* method needs an object to work on (it reads/writes that object's fields), so you call it on the object: `util.squareWithBase()`. A *static* method belongs to the class itself and doesn't touch any particular object, so you call it on the class: `MathUtils.square(5)`. Rule of thumb: if the method never uses `this`, it can be static.

> **Going deeper** — Static methods can't be overridden (they're bound at compile time to the declared type — "hiding," not overriding) and can't see `this`, so they're invisible to polymorphism — a `static` method on an interface can't be a strategy you swap out. That makes heavy static utility classes hard to mock in tests; prefer injecting a small instance (a dependency) when you need to substitute behavior. Statics shine for genuinely stateless pure functions (`Math.max`) and `static` factory methods (`List.of`, `Optional.of`) that read better than constructors.

---

## Method Design Tips

```java
// 1. Methods should do ONE thing — if you're using "and" to describe it, split it
// BAD: validateAndSaveUser()
// GOOD: validateUser() + saveUser()

// 2. Keep parameter lists short (≤ 4). Use a builder or object if you need more.
// BAD: createUser(String name, String email, int age, String dept, String role)
// GOOD: createUser(UserRequest request)

// 3. Return early to avoid deep nesting
// BAD:
public String process(String input) {
    if (input != null) {
        if (!input.isEmpty()) {
            if (input.length() < 100) {
                return input.trim();
            }
        }
    }
    return null;
}

// GOOD: guard clauses
public String process(String input) {
    if (input == null) return null;
    if (input.isEmpty()) return null;
    if (input.length() >= 100) return null;
    return input.trim();
}
```

> **In plain terms** — Small methods that each do one thing are easier to name, test, and reuse. The "guard clause" trick — handle the bad/edge cases up front and `return` early — keeps the happy path flat and readable instead of buried inside nested `if`s.

> **Going deeper** — These tips are the day-to-day face of well-known principles: single-responsibility (one reason to change), low cyclomatic complexity (fewer branches → fewer test cases needed), and the [Builder pattern](../05-advanced/04-design-patterns.md) for taming long parameter lists. A practical heuristic: if a method doesn't fit on one screen, or you can't name it without "and", split it. Watch for *boolean parameters* (`save(true)`) — they're a sign one method is secretly two; prefer two well-named methods or an enum.
