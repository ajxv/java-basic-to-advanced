# Lambdas and Functional Programming

> Runnable example: [code/modern/LambdaDemo.java](../code/modern/LambdaDemo.java)

---

## The Big Picture

> **In plain terms** — A lambda lets you treat a *piece of behavior* as a value — something you can store in a variable, pass to a method, and call later. Before Java 8, "do this for each item" meant writing a whole class; a lambda compresses that to one line (`s -> s.length()`). This unlocks a more declarative style: you tell a method *what* to do (sort by length, keep the non-empty ones) by handing it a small function, instead of writing the loop yourself.

> **Why this matters** — Lambdas are the foundation everything modern in Java is built on: [streams](02-streams.md), `Optional`, `CompletableFuture`, and most fluent APIs all take lambdas. The key insight that makes it all click: a lambda is just a compact implementation of a **functional interface** (an interface with one abstract method). So `s -> s.isEmpty()` *is* a `Predicate<String>`. Once you see lambdas as "instances of one-method interfaces," method references and function composition stop being magic.

---

## What is a Lambda?

A lambda is a concise way to write an anonymous function — a block of code you can pass around like a value.

```java
// Before lambdas: anonymous class (verbose)
Runnable r = new Runnable() {
    @Override
    public void run() {
        System.out.println("Running");
    }
};

// With lambda: same thing, much less noise
Runnable r = () -> System.out.println("Running");
```

A lambda can only be used where a **functional interface** is expected — an interface with exactly one abstract method.

> **In plain terms** — Compare the two snippets: they do the *exact* same thing, but the lambda drops all the ceremony (`new Runnable() { @Override public void run() {...} }`) and keeps only the part that matters — the body. The compiler knows `r` is a `Runnable`, that `Runnable` has one method, so `() -> ...` can only mean "implement that method."

> **Going deeper** — A lambda is *not* just shorthand for an anonymous class. It has no separate `this` (it captures the *enclosing* `this`, whereas an anonymous class's `this` is the anonymous object), it doesn't create a new scope that can shadow variables, and the compiler implements it via `invokedynamic` rather than generating a hidden `$1.class` file — leaner and often faster. Use an anonymous class only when you need state, multiple methods, or to extend a class; reach for a lambda for everything else.

---

## Lambda Syntax

```java
// No parameters
() -> System.out.println("Hello")

// One parameter (parens optional for one param)
name -> System.out.println("Hello, " + name)
(name) -> System.out.println("Hello, " + name)  // same thing

// Multiple parameters
(a, b) -> a + b

// With explicit types (usually not needed — inferred)
(String a, String b) -> a + b

// Block body (when you need multiple statements)
(a, b) -> {
    int sum = a + b;
    System.out.println("Sum: " + sum);
    return sum;
}

// Single expression: no braces, no return keyword
(a, b) -> a + b  // return is implicit
```

> **In plain terms** — The syntax flexes to fit: drop the parens for a single parameter, drop the braces and `return` for a single expression, add them back when you need multiple statements. Aim for the shortest form that stays readable — a one-line expression lambda is the sweet spot.

> **Going deeper** — The single-expression form has an *implicit return*, but the moment you add `{ }` you're writing a statement block and must write `return` explicitly — a frequent beginner compile error. Parameter types are inferred from the target functional interface, so you rarely write them; the exception is `var` parameters (`(var a, var b) -> ...`, Java 11+), useful only to attach annotations. Keep lambdas tiny — if a lambda grows past a few lines, extract it into a named method and use a [method reference](#method-references); long lambdas wreck stack traces and readability.

---

## Functional Interfaces (java.util.function)

Java 8 added a package of ready-to-use functional interfaces:

```java
// Function<T, R> — takes T, returns R
Function<String, Integer> length = s -> s.length();
length.apply("hello"); // 5

// Predicate<T> — takes T, returns boolean
Predicate<String> isEmpty = s -> s.isEmpty();
isEmpty.test("");      // true

// Consumer<T> — takes T, returns nothing (side effects)
Consumer<String> printer = s -> System.out.println(s);
printer.accept("hello");

// Supplier<T> — takes nothing, returns T
Supplier<List<String>> newList = () -> new ArrayList<>();
List<String> list = newList.get();

// BiFunction<T, U, R> — takes two params, returns R
BiFunction<String, Integer, String> repeat = (s, n) -> s.repeat(n);

// UnaryOperator<T> — Function<T, T>
UnaryOperator<String> shout = s -> s.toUpperCase() + "!";

// BinaryOperator<T> — BiFunction<T, T, T>
BinaryOperator<Integer> add = (a, b) -> a + b;
```

> **In plain terms** — You almost never need to write your own functional interface — Java ships the handful you'll use constantly. Remember them by shape: **Function** (input → output), **Predicate** (input → true/false), **Consumer** (input → does something, returns nothing), **Supplier** (nothing → output). Stream and collection methods are built around exactly these.

> **Going deeper** — Each method's name is the "single abstract method" you implement: `Function.apply`, `Predicate.test`, `Consumer.accept`, `Supplier.get`. There are primitive specializations (`IntPredicate`, `ToIntFunction`, `IntСonsumer`...) that exist purely to avoid autoboxing in hot loops — prefer them when working with `int`/`long`/`double` streams. Many carry handy default methods too: `Predicate.and/or/negate`, `Function.andThen/compose`, `Consumer.andThen` (see [composition](#composing-functions)). When none fits exactly, define your own with `@FunctionalInterface`, but check `java.util.function` first.

---

## Method References

A shorthand for lambdas that simply call an existing method.

```java
// Lambda vs Method Reference:
list.forEach(s -> System.out.println(s));   // lambda
list.forEach(System.out::println);           // method reference — cleaner

// Four forms:

// 1. Static method reference: ClassName::staticMethod
Function<String, Integer> parse = Integer::parseInt; // s -> Integer.parseInt(s)

// 2. Instance method on a particular instance: instance::method
String prefix = "Hello, ";
Predicate<String> starts = prefix::startsWith; // wrong direction example, see below
// More common:
Consumer<String> print = System.out::println; // System.out is a specific instance

// 3. Instance method on an arbitrary instance of a type: ClassName::instanceMethod
Function<String, String> upper = String::toUpperCase; // s -> s.toUpperCase()
Predicate<String> empty = String::isEmpty;             // s -> s.isEmpty()

// 4. Constructor reference: ClassName::new
Supplier<ArrayList<String>> factory = ArrayList::new;           // () -> new ArrayList<>()
Function<String, StringBuilder> sbFactory = StringBuilder::new; // s -> new StringBuilder(s)
```

> **In plain terms** — When a lambda does nothing but call one existing method, you can name that method directly: `System.out::println` instead of `s -> System.out.println(s)`. It's shorter and says the intent plainly ("just print each one"). The `::` reads as "the method ... of ...".

> **Going deeper** — The four forms boil down to *where the receiver comes from*. The tricky pair: `String::toUpperCase` (unbound — the stream element *becomes* the receiver, so it's `s -> s.toUpperCase()`) vs `someString::length` (bound — a fixed instance is the receiver). The compiler picks based on the target type, which is why the same `ClassName::method` can mean different things in different contexts. Constructor references (`ArrayList::new`) pair beautifully with `Supplier`/`Function` for factories and stream `collect`. Prefer a method reference over a lambda when it doesn't hurt clarity — but a lambda can be clearer when arguments are reordered or transformed.

---

## Composing Functions

```java
Function<Integer, Integer> times2 = n -> n * 2;
Function<Integer, Integer> plus3 = n -> n + 3;

// andThen: apply times2, then apply plus3 to the result
Function<Integer, Integer> times2ThenPlus3 = times2.andThen(plus3);
times2ThenPlus3.apply(5); // (5*2)+3 = 13

// compose: apply plus3 first, then times2
Function<Integer, Integer> plus3ThenTimes2 = times2.compose(plus3);
plus3ThenTimes2.apply(5); // (5+3)*2 = 16

// Predicate composition:
Predicate<String> notEmpty = s -> !s.isEmpty();
Predicate<String> short100 = s -> s.length() < 100;
Predicate<String> valid = notEmpty.and(short100);    // both must be true
Predicate<String> either = notEmpty.or(short100);    // at least one must be true
Predicate<String> invalid = valid.negate();           // opposite
```

> **In plain terms** — Small functions snap together like Lego. `andThen` chains them ("do A, then feed the result into B"), and predicates combine with `and`/`or`/`negate` to build complex conditions from simple ones. Instead of one big tangled lambda, you assemble readable, reusable pieces.

> **Going deeper** — Watch the direction: `f.andThen(g)` runs `f` first then `g`; `f.compose(g)` runs `g` first then `f` (matching the math `f∘g`) — mixing them up is a classic bug. Composition shines for building validation/transformation pipelines from named, individually testable rules (`notEmpty.and(maxLength).and(matchesPattern)`). This is functional programming's core idea — programs as compositions of small functions — and it pairs naturally with [streams](02-streams.md), where each pipeline stage is one of these functions.

---

## Custom Functional Interface

```java
@FunctionalInterface // optional but enforces single abstract method
public interface Transformer<T, R> {
    R transform(T input);

    // Can have default and static methods
    default <V> Transformer<T, V> andThen(Transformer<R, V> after) {
        return input -> after.transform(this.transform(input));
    }
}

// Use with lambda:
Transformer<String, Integer> wordCount = s -> s.split("\\s+").length;
Transformer<Integer, String> describe = n -> "Has " + n + " words";

Transformer<String, String> pipeline = wordCount.andThen(describe);
pipeline.transform("hello world foo"); // "Has 3 words"
```

> **In plain terms** — When the built-in interfaces don't fit (you want a domain-specific name, or a method that throws a checked exception), declare your own. Add `@FunctionalInterface` so the compiler guarantees it stays single-method, and you can give it `default` helpers like `andThen` so it composes just like the JDK ones.

> **Going deeper** — The most common real reason to roll your own is *checked exceptions*: `Function` can't throw them, so an interface like `ThrowingFunction<T,R> { R apply(T t) throws Exception; }` lets you use lambdas that do I/O without ugly try/catch inside every lambda. A custom name also documents intent at call sites far better than a generic `Function<String,Integer>`. Keep the single-abstract-method rule sacred (that's what `@FunctionalInterface` enforces) — default and static methods don't count against it, so add as many helpers as you like.

---

## Closures — Capturing Variables

Lambdas can capture variables from the surrounding scope, but only if those variables are **effectively final** (never reassigned after first assignment).

```java
String prefix = "Hello, "; // effectively final — never reassigned below
Consumer<String> greet = name -> System.out.println(prefix + name);
greet.accept("Alice"); // "Hello, Alice"

// This fails:
String prefix2 = "Hello, ";
prefix2 = "Hi, "; // now it's NOT effectively final
Consumer<String> greet2 = name -> System.out.println(prefix2 + name); // compile error
```

This prevents a class of threading bugs where a lambda holds a reference to a variable that changes under it.

> **In plain terms** — A lambda can "remember" variables from where it was created (a *closure*), but Java requires those variables to be **effectively final** — set once and never reassigned. So you can read `prefix` inside the lambda, but you can't reassign `prefix` elsewhere. This rule looks annoying at first but stops a real category of bugs.

> **Going deeper** — The restriction exists because a lambda may outlive the method that created it (stored, returned, run on another thread), and Java captures the variable's *value*, not a live link — allowing reassignment would make "which value?" ambiguous and unsafe across threads. The classic workaround people reach for — a mutable field, a one-element array, or `AtomicInteger` to "mutate" from a lambda — usually signals you should be using a stream `reduce`/`collect` instead. Note the capture is by-value for the *reference*, so a captured mutable object can still be mutated (a frequent source of subtle concurrency bugs) — see [multithreading](../05-advanced/02-multithreading.md).
