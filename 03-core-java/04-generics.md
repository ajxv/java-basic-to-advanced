# Generics

> Runnable example: [code/core/GenericsDemo.java](../code/core/GenericsDemo.java)

---

## Why Generics Exist

Without generics (pre-Java 5), collections held `Object`. You got runtime errors instead of compile-time errors.

```java
// Without generics:
List list = new ArrayList();
list.add("hello");
list.add(42);
String s = (String) list.get(1); // ClassCastException at runtime — bug found too late

// With generics:
List<String> list = new ArrayList<>();
list.add("hello");
list.add(42); // compile error — caught immediately ✓
String s = list.get(0); // no cast needed
```

Generics give you type safety at **compile time** with **zero runtime overhead** (due to type erasure).

---

## Generic Classes

```java
public class Box<T> { // T is a type parameter — a placeholder for any type
    private T value;

    public Box(T value) { this.value = value; }
    public T get() { return value; }
    public void set(T value) { this.value = value; }

    @Override
    public String toString() { return "Box[" + value + "]"; }
}

// Usage:
Box<String> strBox = new Box<>("hello");
Box<Integer> intBox = new Box<>(42);

strBox.get();    // "hello" — type is String, no cast
intBox.get();    // 42 — type is Integer

// Diamond operator (<>) infers type from context:
Box<List<String>> nested = new Box<>(new ArrayList<>());
```

---

## Multiple Type Parameters

```java
public class Pair<A, B> {
    private final A first;
    private final B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }
    public A getFirst() { return first; }
    public B getSecond() { return second; }

    @Override
    public String toString() { return "(" + first + ", " + second + ")"; }
}

Pair<String, Integer> nameAge = new Pair<>("Alice", 30);
nameAge.getFirst();   // "Alice" — String
nameAge.getSecond();  // 30 — Integer
```

---

## Generic Methods

The type parameter is declared on the method, not the class.

```java
// Swap two elements in an array
public static <T> void swap(T[] array, int i, int j) {
    T temp = array[i];
    array[i] = array[j];
    array[j] = temp;
}

String[] words = {"hello", "world"};
swap(words, 0, 1); // words is now ["world", "hello"]

// Return a list filled with n copies of an item
public static <T> List<T> repeat(T item, int times) {
    List<T> list = new ArrayList<>();
    for (int i = 0; i < times; i++) list.add(item);
    return list;
}

List<String> greetings = repeat("hi", 3); // ["hi", "hi", "hi"]
```

---

## Bounded Type Parameters

Restrict which types are allowed.

```java
// T must be Number or a subclass (Integer, Double, Long, etc.)
public static <T extends Number> double sum(List<T> list) {
    double total = 0;
    for (T item : list) {
        total += item.doubleValue(); // can call Number methods because T extends Number
    }
    return total;
}

sum(List.of(1, 2, 3));      // works — Integer extends Number
sum(List.of(1.5, 2.5));     // works — Double extends Number
// sum(List.of("a", "b")); // compile error — String doesn't extend Number

// Multiple bounds: T must extend both Comparable and Serializable
public static <T extends Comparable<T> & Serializable> T max(List<T> list) {
    return Collections.max(list);
}
```

---

## Wildcards

Wildcards (`?`) represent an unknown type. Used with existing generic types, not when defining them.

```java
// ? extends T — "some subtype of T" — READ-only (producer)
public double sumOfList(List<? extends Number> list) {
    double total = 0;
    for (Number n : list) total += n.doubleValue(); // can read as Number
    return total;
    // list.add(1); // compile error — don't know exact type, can't safely add
}

sumOfList(List.of(1, 2, 3));     // List<Integer> — works
sumOfList(List.of(1.5, 2.5));   // List<Double> — works

// ? super T — "some supertype of T" — WRITE-able (consumer)
public void addNumbers(List<? super Integer> list) {
    list.add(1);   // safe — Integer is a subtype of whatever ? is
    list.add(2);
    // Integer n = list.get(0); // compile error — only Object guaranteed
}

addNumbers(new ArrayList<Integer>());
addNumbers(new ArrayList<Number>());
addNumbers(new ArrayList<Object>());
```

**PECS — Produces Extends, Consumes Super:**
- List is a **producer** (you read from it) → `List<? extends T>`
- List is a **consumer** (you write to it) → `List<? super T>`

---

## Type Erasure

Generics are a compile-time feature only. At runtime, type information is erased.

```java
List<String> strings = new ArrayList<>();
List<Integer> ints = new ArrayList<>();

// At runtime, both are just List — same class
System.out.println(strings.getClass() == ints.getClass()); // true!

// You CANNOT do:
// if (list instanceof List<String>) // compile error — can't check erased type

// You CAN do:
if (list instanceof List<?>) // works — wildcard
```

This also means:
```java
// Can't create generic arrays:
T[] arr = new T[10]; // compile error

// Workaround:
@SuppressWarnings("unchecked")
T[] arr = (T[]) new Object[10];

// Or better: use List<T> instead of T[]
```

---

## Common Generic Patterns in Java APIs

```java
// Comparable<T> — for natural ordering
public class Employee implements Comparable<Employee> {
    @Override
    public int compareTo(Employee other) {
        return this.name.compareTo(other.name);
    }
}

// Iterable<T> — makes your class work in for-each
public class Range implements Iterable<Integer> {
    private final int start, end;
    public Range(int start, int end) { this.start = start; this.end = end; }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<>() {
            int current = start;
            public boolean hasNext() { return current < end; }
            public Integer next() { return current++; }
        };
    }
}

for (int n : new Range(1, 5)) {
    System.out.println(n); // 1 2 3 4
}
```
