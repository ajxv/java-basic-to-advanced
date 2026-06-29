# Streams API

> Runnable example: [code/modern/StreamsDemo.java](../code/modern/StreamsDemo.java)

---

## The Big Picture

> **In plain terms** — A stream lets you describe data processing as a *pipeline* — "take these items, keep the ones over 50k, grab their names, uppercase them, collect into a list" — reading top-to-bottom like a sentence, instead of a hand-written loop with temporary variables and `if`s. You say *what* you want; the stream handles *how* to iterate. It's the single biggest readability upgrade Java 8 brought.

> **Why this matters** — Streams turn multi-step data wrangling (filter → transform → group → summarize) into clear, composable steps built from [lambdas](01-lambdas-and-functional.md). Two ideas make them powerful and occasionally surprising: they're **lazy** (nothing happens until a terminal operation asks for a result, so the pipeline can short-circuit and skip work) and **single-use** (consume once, then it's spent). Master `filter`/`map`/`collect` and `groupingBy` and you'll replace dozens of loops with a few readable lines.

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

> **In plain terms** — Every stream has three parts: a *source* (where data comes from), zero or more *intermediate* steps that each return a new stream (`filter`, `map`, `sorted`), and exactly one *terminal* step that produces a result and kicks everything off (`count`, `collect`, `forEach`). Until the terminal step, you've only written a *recipe* — no cooking has happened.

> **Going deeper** — Laziness isn't just a curiosity; it enables real optimizations. The pipeline is processed element-by-element (not stage-by-stage), so `filter` then `map` makes one pass, not two, and `findFirst`/`limit` can stop early without touching the rest. The elements themselves aren't stored — a stream is a *view over* a source, not a new collection — which is why a stream is single-use and why mutating the source mid-stream is illegal. This is fundamentally different from chaining list operations, where each step would build a full intermediate list.

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

> **In plain terms** — Streams come from many places: collections (`.stream()`), loose values (`Stream.of`), numeric ranges (`IntStream.range`), generators, and files. The everyday one is `collection.stream()`; the rest are handy for specific jobs like "process the numbers 1 to 100" or "read a file line by line."

> **Going deeper** — Two cautions. `Stream.iterate`/`Stream.generate` are *infinite* — always pair them with `limit` (or the bounded `iterate(seed, hasNext, next)` form, Java 9+) or they never terminate. And stream sources backed by I/O (`Files.lines`) hold resources, so they implement `AutoCloseable` and must be used in [try-with-resources](../03-core-java/01-exception-handling.md#try-with-resources-preferred) or you leak file handles — collection streams need no closing. For numeric work, start with a primitive stream (`IntStream.range`) rather than `Stream<Integer>` to avoid boxing.

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

> **In plain terms** — These are the verbs of the pipeline. `filter` keeps items that pass a test, `map` transforms each item into something else, `sorted` orders them, `distinct` drops duplicates, `limit`/`skip` slice. The one that confuses people is `flatMap`: use it when each item *expands into several* (a list of lists → one flat stream) and you want them merged into a single stream.

> **Going deeper** — `map` is one-to-one (N in, N out); `flatMap` is one-to-many flattened (each element becomes a sub-stream, all concatenated) — reach for it whenever a `map` would give you a `Stream<Stream<X>>` or `Stream<List<X>>`. Prefer `mapToInt/mapToLong/mapToDouble` over `map` for numbers to drop into primitive streams and skip boxing. `peek` is strictly a debugging tap — never put real logic in it (it may be skipped entirely when the pipeline short-circuits or optimizes). All these return a *new* stream and run lazily — they queue work, they don't do it.

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

> **In plain terms** — The terminal operation is what actually *runs* the pipeline and gives you something back: a list (`collect`), a number (`count`, `sum`), a single item (`findFirst`, `max`), a yes/no (`anyMatch`), or a side effect (`forEach`). Many return an [`Optional`](03-optional.md) because the answer might not exist (an empty stream has no `max`).

> **Going deeper** — `reduce` is the general engine behind `sum`/`count`/`max` — it folds elements pairwise into one value; provide an *identity* and an *associative* combiner so it also works in parallel. The `*Match` and `find*` ops *short-circuit* — they stop the moment the answer is known, which is why `anyMatch(p)` beats `filter(p).count() > 0`. `findAny` exists to give parallel streams freedom to return the cheapest match rather than the first. Prefer `collect(toList())`/`.toList()` to mutating an external list in `forEach` (the parallel-safety reason appears below).

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

> **In plain terms** — `Collectors` is where streams get genuinely powerful for real data work. `groupingBy` is the star: "group employees by department" in one line gives you a `Map<Department, List<Employee>>`. Add a *downstream* collector (`counting()`, `averagingInt(...)`) to summarize each group — "average salary per department" without a single loop or temporary map.

> **Going deeper** — The downstream-collector parameter is the unlock: `groupingBy(dept, counting())`, `groupingBy(dept, mapping(Employee::getName, toList()))`, even nested `groupingBy(dept, groupingBy(level))` for multi-level grouping. `partitioningBy` is the boolean special case (always returns both `true` and `false` keys, even when empty — unlike `groupingBy`). With `toMap`, *always* supply the merge function if keys can collide — the two-arg form throws `IllegalStateException` on a duplicate key, a very common production surprise. These collectors mirror SQL's `GROUP BY`/aggregate functions, which is a useful mental model.

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

> **In plain terms** — `IntStream`/`LongStream`/`DoubleStream` are streams of raw numbers. They exist so numeric work avoids the cost of wrapping every `int` in an `Integer` object, and they come with built-in `sum()`, `average()`, `max()`, `summaryStatistics()` that the generic `Stream` doesn't have. Use `mapToInt` to drop into one, and `boxed()`/`mapToObj` to come back.

> **Going deeper** — Boxing isn't free: a `Stream<Integer>` allocates an object per element and chases pointers, so primitive streams can be several times faster and far lighter on GC for large numeric workloads. Note the numeric terminals return primitive optionals (`OptionalInt`, `OptionalDouble`) — handle the empty case with `orElse`. `summaryStatistics()` is a hidden gem: one pass gives you count, sum, min, max, and average together. The same boxing concern is why the [function package](01-lambdas-and-functional.md#functional-interfaces-javautilfunction) ships `IntFunction`, `ToIntFunction`, etc.

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

> **In plain terms** — Adding `.parallel()` (or using `parallelStream()`) splits the work across CPU cores automatically — tempting, but it's not free and not always faster. It only pays off for large datasets doing CPU-heavy, independent work. For small lists or anything touching I/O, it's usually *slower*, and any shared mutable state turns it into a data-corruption bug.

> **Going deeper** — Parallel streams run on the *shared* common `ForkJoinPool` (sized to your CPU cores), so a blocking/IO task in one parallel stream can starve every other parallel stream in the JVM — a notorious production trap; isolate such work in your own pool or avoid parallel streams for I/O. Performance needs the data to *split cheaply* (`ArrayList`/arrays split well; `LinkedList`/`Iterator` sources don't) and the per-element work to outweigh the splitting/merging overhead. Always `collect` into the result rather than `forEach`-ing into a shared collection — collectors are designed to merge partial results safely. Measure before trusting parallelism; it frequently loses.

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

> **In plain terms** — Streams aren't always the answer. A stream is *spent* after one terminal op — reuse it and you get an exception. For a plain "do this for each element" with no filtering or transforming, a regular `for-each` loop is clearer and easier to debug. And when you only need to know "does any match?", use `anyMatch` (stops early) rather than counting all matches.

> **Going deeper** — Debuggability is a real cost: a stack trace through a long stream pipeline is harder to read than a loop, and you can't step through it line-by-line as naturally. Rule of thumb — use streams when they make the *intent* clearer (filter/map/group/reduce chains), use loops for simple iteration, early exits with complex control flow, or when you need to mutate the source. Avoid side effects inside stream operations (the whole model assumes each stage is a pure function); if you find yourself fighting that with shared state, it's a sign a loop fits better. Never reuse a consumed stream — create a fresh one from the source.
