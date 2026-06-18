# Lambdas and Functional Programming

> Runnable example: [code/modern/LambdaDemo.java](../code/modern/LambdaDemo.java)

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
