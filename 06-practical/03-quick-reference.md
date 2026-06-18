# Quick Reference and Decision Trees

For the moments when you need a fast answer.

---

## Debugging Checklist — When Code is Broken

1. **NullPointerException** → Something is null. Add null check, use `Optional`, or use `Objects.requireNonNull()` at method entry.
2. **ClassCastException** → Wrong cast. Add `instanceof` check before casting.
3. **ConcurrentModificationException** → Modified collection while iterating it. Use `Iterator.remove()` or `removeIf()`.
4. **IndexOutOfBoundsException** → Off-by-one or empty list. Check `list.isEmpty()` before `list.get(0)`.
5. **StackOverflowError** → Infinite recursion. Check your base case.
6. **OutOfMemoryError** → Heap exhausted. Check for memory leaks, increase `-Xmx`, or fix the leak.
7. **Result is wrong but no exception** → Integer overflow? Floating-point imprecision? Wrong type casting?

---

## Choosing a Collection

```
Need fast lookup by key?
  ├── Yes: HashMap (unordered), TreeMap (sorted), LinkedHashMap (insertion order)
  └── No: What order matters?
         ├── Don't care: HashSet (unique) or ArrayList (duplicates OK)
         ├── Insertion order: LinkedHashSet or ArrayList
         └── Sorted: TreeSet (unique) or ArrayList + sort

Need thread safety?
  ├── Map: ConcurrentHashMap
  ├── List (read-heavy): CopyOnWriteArrayList
  └── Queue: ArrayBlockingQueue, LinkedBlockingQueue

Need a queue or stack?
  └── ArrayDeque (faster than LinkedList)
```

---

## Choosing a Number Type

| Situation | Use |
|-----------|-----|
| Money / exact decimals | `BigDecimal` (string constructor) |
| Very large integers | `BigInteger` |
| General integer math | `int` |
| Large numbers / timestamps | `long` |
| General decimal math | `double` |
| Performance-critical numeric arrays | `int[]`, `long[]`, `double[]` |

---

## Choosing Exception Type

```
Is the error a programming bug (wrong argument, illegal state)?
  └── Throw unchecked: IllegalArgumentException, IllegalStateException, etc.

Is the error from external systems the caller should handle (file not found, DB down)?
  └── Throw checked: IOException, SQLException, or your own checked exception

Is it a domain "not found" that the service layer should translate?
  └── Throw unchecked runtime: UserNotFoundException, OrderNotFoundException, etc.
```

---

## Concurrency Decision Tree

```
Is the state only read, never written?
  └── No synchronization needed

One value modified by multiple threads?
  └── AtomicInteger / AtomicLong / AtomicReference

Multiple related fields that must be consistent?
  └── synchronized block or method

High-contention map?
  └── ConcurrentHashMap

Background async work, don't want to block?
  └── CompletableFuture + ExecutorService

I/O bound with many concurrent tasks (Java 21)?
  └── Virtual threads (Executors.newVirtualThreadPerTaskExecutor())
```

---

## When to Use What (OOP)

| Situation | Solution |
|-----------|----------|
| Object with many optional fields | Builder pattern |
| Multiple implementations of same behavior | Interface + Strategy |
| One global instance | Singleton (enum or holder idiom) |
| Create objects without specifying exact type | Factory |
| Add behavior without changing class | Decorator |
| Notify multiple components of an event | Observer / EventBus |
| Hide data access details | Repository pattern |
| Share code AND state between related classes | Abstract class |
| Define a capability any class can implement | Interface |

---

## String Method Cheat Sheet

```java
s.equals(other)          // content equality (always use, not ==)
s.equalsIgnoreCase(other)
Objects.equals(s1, s2)   // null-safe

s.isEmpty()              // s.length() == 0
s.isBlank()              // all whitespace (Java 11+)
s.contains("sub")
s.startsWith("pre")
s.endsWith("suf")
s.indexOf("x")           // -1 if not found

s.trim()                 // remove leading/trailing whitespace
s.strip()                // Unicode-aware trim (Java 11+)
s.toLowerCase()
s.toUpperCase()
s.replace("old", "new")  // replaces all occurrences
s.replaceAll("regex", "new")
s.split(",")             // returns String[]
s.substring(start, end)  // [start, end)
s.charAt(i)
String.join(", ", list)  // join elements
s.repeat(3)              // Java 11+

// Parse:
Integer.parseInt(s)
Double.parseDouble(s)
// Convert:
String.valueOf(42)
Integer.toString(42)
```

---

## Stream Operation Reference

```java
// Intermediate (lazy):
.filter(predicate)       // keep matching
.map(function)           // transform 1-to-1
.flatMap(function)       // transform 1-to-many and flatten
.distinct()              // remove duplicates
.sorted()                // natural order
.sorted(comparator)      // custom order
.limit(n)               // take first n
.skip(n)                // skip first n
.peek(consumer)          // tap for debugging

// Terminal (triggers execution):
.forEach(consumer)
.collect(Collectors.toList())
.collect(Collectors.toSet())
.collect(Collectors.joining(", "))
.collect(Collectors.groupingBy(keyFn))
.count()
.findFirst()             // Optional
.anyMatch(predicate)     // boolean, short-circuits
.allMatch(predicate)
.noneMatch(predicate)
.min(comparator)         // Optional
.max(comparator)         // Optional
.reduce(identity, accumulator)
.toList()                // Java 16+, unmodifiable
```

---

## Common @Override and Equals Contract

```java
// Always implement equals + hashCode together
// Objects.equals used in: HashMap, HashSet, Stream.distinct(), List.contains()

@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MyClass other)) return false;
    return Objects.equals(this.field1, other.field1)
        && Objects.equals(this.field2, other.field2);
}

@Override
public int hashCode() {
    return Objects.hash(field1, field2);
}

@Override
public String toString() {
    return "MyClass{field1=" + field1 + ", field2=" + field2 + "}";
}

// Or: use a Record and get all three for free
```

---

## Java Version at a Glance

```
Java 8  → lambdas, streams, Optional, default interface methods
Java 11 → String.strip/isBlank/repeat, Files.readString (LTS)
Java 17 → sealed classes, records (stable), switch expressions (LTS)
Java 21 → virtual threads, switch pattern matching (LTS)
```

Use an LTS version (11, 17, or 21) for production.
