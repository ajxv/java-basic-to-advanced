# Methods: The Building Blocks

> Runnable example: [code/basics/MethodsDemo.java](../code/basics/MethodsDemo.java)

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
