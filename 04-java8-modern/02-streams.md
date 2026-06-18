# Streams API

> Runnable example: [code/modern/StreamsDemo.java](../code/modern/StreamsDemo.java)

---

## What is a Stream?

A Stream is a pipeline for processing a sequence of elements declaratively. You describe WHAT you want to happen, not HOW to iterate.

```
Source → [Intermediate Operations] → Terminal Operation
(lazy, evaluated only when terminal op is called)
```

```java
List<String> names = List.of("Alice", "Bob", "Charlie", "Diana", "Eve");

long count = names.stream()          // create stream from list
    .filter(n -> n.length() > 3)     // keep: Alice, Charlie, Diana
    .map(String::toUpperCase)         // transform: ALICE, CHARLIE, DIANA
    .sorted()                         // order: ALICE, CHARLIE, DIANA
    .count();                         // terminal: triggers execution, returns 3
```

**Key rule:** Intermediate operations are **lazy** — nothing runs until a terminal operation is called.

---

## Creating Streams

```java
// From a collection
list.stream()
list.parallelStream() // parallel execution

// From values
Stream.of("a", "b", "c")
Stream.empty()

// From array
Arrays.stream(new int[]{1, 2, 3}) // IntStream
Arrays.stream(new String[]{"a", "b"})

// Range (IntStream)
IntStream.range(0, 10)        // 0..9 (exclusive end)
IntStream.rangeClosed(1, 10)  // 1..10 (inclusive end)

// Infinite streams (use limit!)
Stream.iterate(0, n -> n + 2).limit(5) // 0, 2, 4, 6, 8
Stream.generate(Math::random).limit(3) // 3 random doubles

// From file
Files.lines(Path.of("data.txt")) // Stream<String> — each line
```

---

## Intermediate Operations

These transform the stream but don't trigger execution.

```java
List<Employee> employees = getEmployees();

// filter — keep elements matching a predicate
employees.stream()
    .filter(e -> e.getSalary() > 50000)
    .filter(e -> e.getDepartment().equals("Engineering"))

// map — transform each element
employees.stream()
    .map(Employee::getName)       // Stream<String>
    .map(String::toUpperCase)

// mapToInt / mapToDouble — specialized for primitives (avoids boxing)
employees.stream()
    .mapToInt(Employee::getSalary) // IntStream — more efficient for numeric ops

// flatMap — flatten nested structures
List<List<String>> nested = List.of(List.of("a","b"), List.of("c","d"));
nested.stream()
    .flatMap(Collection::stream)  // flatten: "a","b","c","d"

// distinct — remove duplicates (uses equals())
stream.distinct()

// sorted — natural order or with Comparator
stream.sorted()
stream.sorted(Comparator.comparingInt(Employee::getSalary).reversed())

// peek — side effect tap for debugging (don't use in production logic)
stream.peek(e -> System.out.println("Before filter: " + e))
      .filter(...)
      .peek(e -> System.out.println("After filter: " + e))

// limit / skip
stream.limit(10)    // take only first 10
stream.skip(5)      // skip first 5
```

---

## Terminal Operations

These trigger the pipeline and produce a result.

```java
// forEach — consume each element (no return value)
stream.forEach(System.out::println);

// collect — gather into a container (most flexible terminal op)
List<String> list = stream.collect(Collectors.toList());
List<String> list2 = stream.collect(Collectors.toUnmodifiableList()); // immutable
Set<String> set = stream.collect(Collectors.toSet());
String joined = stream.collect(Collectors.joining(", ", "[", "]")); // "[a, b, c]"

// toList() shorthand (Java 16+)
List<String> list3 = stream.toList(); // unmodifiable

// count, sum, average, min, max
long count = stream.count();
int sum = intStream.sum();
OptionalDouble avg = intStream.average();
Optional<Employee> richest = employees.stream()
    .max(Comparator.comparingInt(Employee::getSalary));

// reduce — combine elements into a single value
int total = IntStream.rangeClosed(1, 100).reduce(0, Integer::sum); // 5050
Optional<String> longest = stream.reduce((a, b) -> a.length() >= b.length() ? a : b);

// findFirst / findAny
Optional<Employee> first = employees.stream()
    .filter(e -> e.getDepartment().equals("HR"))
    .findFirst(); // first match

// anyMatch / allMatch / noneMatch (short-circuit — stop early)
boolean anyHighEarner = employees.stream().anyMatch(e -> e.getSalary() > 100_000);
boolean allActive = employees.stream().allMatch(Employee::isActive);
boolean noneNegative = intStream.noneMatch(n -> n < 0);
```

---

## Collectors — Powerful Grouping and Partitioning

```java
// groupingBy: partition elements into groups
Map<String, List<Employee>> byDept = employees.stream()
    .collect(Collectors.groupingBy(Employee::getDepartment));

// groupingBy with downstream collector
Map<String, Long> countByDept = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.counting()
    ));

Map<String, Double> avgSalaryByDept = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.averagingInt(Employee::getSalary)
    ));

Map<String, Optional<Employee>> highestPaidByDept = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.maxBy(Comparator.comparingInt(Employee::getSalary))
    ));

// partitioningBy: split into true/false groups
Map<Boolean, List<Employee>> seniorJunior = employees.stream()
    .collect(Collectors.partitioningBy(e -> e.getSalary() > 70_000));
List<Employee> senior = seniorJunior.get(true);
List<Employee> junior = seniorJunior.get(false);

// toMap: build a Map from stream elements
Map<String, Employee> byId = employees.stream()
    .collect(Collectors.toMap(Employee::getId, e -> e));

// toMap with merge function (for duplicate keys):
Map<String, Integer> totalSalaryByDept = employees.stream()
    .collect(Collectors.toMap(
        Employee::getDepartment,
        Employee::getSalary,
        Integer::sum // merge: add salaries for same department
    ));
```

---

## Primitive Streams

Avoid boxing overhead for numeric operations.

```java
// IntStream, LongStream, DoubleStream
IntStream.of(1, 2, 3).sum();         // 6
IntStream.rangeClosed(1, 100).sum(); // 5050

// mapToInt converts Stream<T> to IntStream
int totalSalary = employees.stream()
    .mapToInt(Employee::getSalary)
    .sum();

int maxSalary = employees.stream()
    .mapToInt(Employee::getSalary)
    .max()
    .orElse(0); // max returns OptionalInt

// Back to object stream:
IntStream.of(1, 2, 3).boxed()     // Stream<Integer>
IntStream.of(1, 2, 3).mapToObj(n -> "item-" + n) // Stream<String>
```

---

## Parallel Streams

```java
// parallelStream() splits work across CPU threads automatically
long count = largeList.parallelStream()
    .filter(n -> isPrime(n))
    .count();

// Rules for safe parallel streams:
// 1. Operations must be STATELESS (no shared mutable state)
// 2. Operations must be NON-INTERFERING (don't modify the source)
// 3. Order is not guaranteed unless you use forEachOrdered

// BAD: shared mutable state in parallel stream
List<Integer> results = new ArrayList<>(); // not thread-safe!
list.parallelStream().forEach(results::add); // data corruption

// GOOD: collect instead
List<Integer> results = list.parallelStream()
    .filter(...)
    .collect(Collectors.toList()); // Collectors are thread-safe

// Parallel streams have overhead — only worth it for:
// - Large datasets (thousands+ elements)
// - CPU-intensive operations (not I/O)
// - Stateless pipelines
```

---

## Stream Pitfalls

```java
// 1. A stream can only be consumed ONCE
Stream<String> stream = list.stream();
stream.forEach(...);   // first terminal op
stream.forEach(...);   // IllegalStateException: stream has already been operated upon

// 2. Don't use streams for simple loops — overkill and harder to debug
// OVERKILL:
list.stream().forEach(item -> System.out.println(item));
// SIMPLE:
for (String item : list) System.out.println(item);

// 3. Short-circuit ops can save work — use them
// This stops as soon as it finds a match:
boolean found = list.stream().anyMatch(s -> s.startsWith("A"));

// Without short-circuit, this processes ALL elements:
list.stream().filter(s -> s.startsWith("A")).count() > 0; // worse
```
