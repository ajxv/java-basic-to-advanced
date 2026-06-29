# Collections Framework

> Runnable example: [code/core/CollectionsDemo.java](../code/core/CollectionsDemo.java)

---

## The Big Picture

> **In plain terms** — Collections are Java's built-in containers for groups of objects, and they come in three big shapes: a **List** (an ordered sequence, duplicates allowed — like a numbered to-do list), a **Set** (a bag of unique items, no duplicates — like a guest list), and a **Map** (key→value lookups — like a dictionary). Almost every program is mostly moving data between these three. Pick the shape that matches your *access pattern*, and the right implementation falls out from there.

> **Why this matters** — Choosing the wrong collection is one of the most common causes of slow code. "Is X in this list?" is slow on an `ArrayList` (scan everything) but instant on a `HashSet`. The framework is built around *interfaces* (`List`, `Set`, `Map`) with multiple *implementations* that trade off speed, ordering, and memory — so you write code against the interface and swap implementations freely. Understanding the Big-O costs in the hierarchy below is what separates code that scales from code that crawls.

---

## The Hierarchy

```
Iterable
└── Collection
    ├── List     — ordered, allows duplicates, index access
    │   ├── ArrayList      O(1) get, O(n) insert at middle
    │   └── LinkedList     O(1) insert at ends, O(n) get by index
    ├── Set      — no duplicates
    │   ├── HashSet        O(1) add/remove/contains, no order
    │   ├── LinkedHashSet  O(1) ops, insertion order preserved
    │   └── TreeSet        O(log n) ops, always sorted
    └── Queue / Deque
        ├── ArrayDeque     Fast stack and queue, no nulls
        └── PriorityQueue  Min-heap, elements ordered by priority

Map (not a Collection, but part of the framework)
    ├── HashMap        O(1) ops, no order
    ├── LinkedHashMap  O(1) ops, insertion order
    └── TreeMap        O(log n) ops, sorted by key
```

> **In plain terms** — Notice the pattern in the names: `Hash*` = fast but unordered, `Linked*` = keeps insertion order, `Tree*` = always sorted (but a bit slower). So once you know whether you need a List/Set/Map, the prefix tells you the ordering behavior. `Map` sits slightly apart — it's part of the framework but isn't a `Collection` because it holds *pairs*, not single elements.

> **Going deeper** — The Big-O annotations are the whole point of this diagram. `HashMap`/`HashSet` give *amortized* O(1) — "amortized" because occasional resizes (rehashing all entries) are O(n), and a bad `hashCode` degrades buckets toward O(n) (modern `HashMap` upgrades a heavily-collided bucket to a balanced tree, capping it at O(log n)). `Tree*` are backed by red-black trees giving guaranteed O(log n) *and* range queries (`headSet`, `subMap`). `ArrayList` is contiguous memory (cache-friendly, fast index access), while `LinkedList`'s O(1) middle-insert is mostly theoretical — pointer-chasing and per-node objects make it lose to `ArrayList` in practice almost always.

---

## List

### ArrayList — Your Default List

```java
List<String> names = new ArrayList<>(); // initial capacity 10, grows automatically
names.add("Alice");
names.add("Bob");
names.add(0, "Zara");  // insert at index 0: O(n) — shifts everything

names.get(1);          // "Alice" — O(1)
names.set(1, "Anna");  // replace at index 1
names.remove(0);       // remove by index
names.remove("Bob");   // remove by value (first occurrence)
names.contains("Anna");// true — O(n) linear scan
names.size();          // count
names.isEmpty();       // boolean

// Sorting
Collections.sort(names);                               // natural order
names.sort(Comparator.comparingInt(String::length));   // by length
```

> **In plain terms** — `ArrayList` is your default list — it's a growable array, so getting item #5 is instant. The cost is inserting/removing in the *middle*: everything after it has to shift over. If you mostly add to the end and read by index (the common case), `ArrayList` is exactly right.

> **Going deeper** — Backing array grows by ~50% when full, copying everything — so if you know the size, `new ArrayList<>(capacity)` avoids repeated reallocation. Declare variables as the `List` interface (`List<String> x = new ArrayList<>()`), not the concrete class, so you can swap implementations. `list.remove(int)` removes by *index* while `list.remove(Object)` removes by *value* — a real trap with `List<Integer>` (see [overloading](../01-basics/04-methods.md#method-overloading)). For iteration-while-removing pitfalls, revisit the [Iterator section](../01-basics/03-operators-and-control-flow.md).

### LinkedList — Queue and Deque

```java
// LinkedList as Deque (double-ended queue)
Deque<String> deque = new LinkedList<>();
deque.addFirst("first");     // add to front
deque.addLast("last");       // add to back
deque.peekFirst();           // view front without removing
deque.pollFirst();           // remove from front

// As Stack (use ArrayDeque instead — faster)
Deque<String> stack = new ArrayDeque<>();
stack.push("a");             // push to top
stack.pop();                 // remove from top
stack.peek();                // view top
```

> **In plain terms** — A `Deque` ("deck") is a double-ended queue: you can add and remove from *both* ends, which makes it serve as either a queue (FIFO) or a stack (LIFO). For stack/queue needs, reach for `ArrayDeque`, not the old `Stack` or `LinkedList`.

> **Going deeper** — `ArrayDeque` is the recommended general-purpose stack *and* queue — it's a circular array, so it's faster and more memory-compact than `LinkedList` and avoids the legacy `Stack` class (which extends `Vector` and is synchronized/slow, the inheritance mistake from [the OOP doc](../02-oop/02-inheritance-and-polymorphism.md#common-inheritance-mistakes)). Caveat: `ArrayDeque` forbids `null` elements (it uses null internally as an empty marker), so use the `peek`/`poll` methods that return `null` to signal "empty" rather than throwing. For producer/consumer across threads, use a `BlockingQueue` from `java.util.concurrent` instead — see [multithreading](../05-advanced/02-multithreading.md).

---

## Set

```java
// HashSet: fastest, no guaranteed order
Set<String> set = new HashSet<>();
set.add("banana");
set.add("apple");
set.add("banana"); // silently ignored — sets don't allow duplicates
set.contains("apple"); // true — O(1)

// LinkedHashSet: preserves insertion order
Set<String> linked = new LinkedHashSet<>();
linked.add("banana");
linked.add("apple");
linked.add("cherry");
System.out.println(linked); // [banana, apple, cherry] — always in insertion order

// TreeSet: always sorted
Set<String> tree = new TreeSet<>();
tree.add("banana");
tree.add("apple");
System.out.println(tree); // [apple, banana]
tree.first(); // "apple"
tree.last();  // "banana"
((TreeSet<String>) tree).headSet("b"); // elements < "b": [apple]
```

> **In plain terms** — A `Set` automatically throws away duplicates — adding the same value twice does nothing. Pick by ordering need: `HashSet` (fastest, no order), `LinkedHashSet` (remembers insertion order), `TreeSet` (always sorted). Great for "have I seen this already?" and deduplication.

> **Going deeper** — A `Set` decides "duplicate" using `equals` + `hashCode` (for `HashSet`) or `compareTo`/`Comparator` (for `TreeSet`) — so your element type *must* implement these correctly, or dedup silently breaks (tie back to [equals/hashCode](../02-oop/01-classes-and-objects.md#equals-and-hashcode)). A subtle `TreeSet` trap: it judges equality purely by the comparator, so a comparator that returns 0 for "different" objects will treat them as duplicates and drop one. Set algebra is built in: `addAll` (union), `retainAll` (intersection), `removeAll` (difference). Most JDK sets are literally backed by the matching map (`HashSet` wraps a `HashMap`).

---

## Map

```java
Map<String, Integer> scores = new HashMap<>();
scores.put("Alice", 95);
scores.put("Bob", 87);
scores.put("Alice", 99);          // replaces previous value for "Alice"

scores.get("Alice");              // 99
scores.get("Unknown");            // null — risky if you forget to check
scores.getOrDefault("X", 0);     // 0 — safer

scores.containsKey("Bob");        // true
scores.containsValue(87);         // true — O(n) scan

scores.remove("Bob");             // removes the entry

// Safe operations:
scores.putIfAbsent("Charlie", 70);         // only puts if key not present
scores.merge("Alice", 5, Integer::sum);    // 99 + 5 = 104
scores.computeIfAbsent("Dan", k -> 50);   // computes and puts if absent
scores.computeIfPresent("Alice", (k, v) -> v + 10); // update only if exists

// Iterating:
for (Map.Entry<String, Integer> e : scores.entrySet()) {
    System.out.println(e.getKey() + " → " + e.getValue());
}

// Java 8+ forEach:
scores.forEach((name, score) -> System.out.println(name + ": " + score));

// Get all keys / values:
Set<String> keys = scores.keySet();
Collection<Integer> values = scores.values();
```

> **In plain terms** — A `Map` stores key→value pairs for instant lookup by key — the workhorse of most programs. Two habits worth forming early: use `getOrDefault` instead of `get` (so a missing key doesn't hand you a `null` that crashes later), and learn `computeIfAbsent`/`merge` — they replace the clunky "check if key exists, then update" pattern with one clean call.

> **Going deeper** — `merge` and `computeIfAbsent` are transformative for two everyday tasks: counting (`map.merge(word, 1, Integer::sum)` for a frequency map) and grouping (`map.computeIfAbsent(key, k -> new ArrayList<>()).add(item)` for a multimap). Iterate via `entrySet()` (one lookup per entry), never by looping keys and calling `get` (two lookups). `HashMap` allows one `null` key and `null` values; `TreeMap` and `ConcurrentHashMap` forbid `null` keys. `HashMap` is *not* thread-safe — concurrent writes can corrupt it or (historically) infinite-loop — use `ConcurrentHashMap` across threads. For grouping straight from a stream, `Collectors.groupingBy` is even cleaner — see [streams](../04-java8-modern/02-streams.md).

---

## Choosing the Right Collection

| Need                                  | Use                  |
|---------------------------------------|----------------------|
| Fast lookup by key                    | `HashMap`            |
| Sorted by key                         | `TreeMap`            |
| Key insertion order preserved         | `LinkedHashMap`      |
| No duplicates, fast lookup            | `HashSet`            |
| Sorted unique elements                | `TreeSet`            |
| Ordered list with index               | `ArrayList`          |
| Queue or stack                        | `ArrayDeque`         |
| Thread-safe map                       | `ConcurrentHashMap`  |
| Rarely written, often iterated list   | `CopyOnWriteArrayList` |

> **In plain terms** — This table is the cheat sheet for the whole topic: start from what you need to *do* (look up by key? keep sorted? forbid duplicates?) and read off the collection. When totally unsure, the safe defaults are `ArrayList` for sequences and `HashMap` for lookups — then specialize only when a real requirement (ordering, sorting, concurrency) demands it.

> **Going deeper** — Two columns people forget until production bites: the *concurrent* ones (`ConcurrentHashMap`, `CopyOnWriteArrayList`) exist because the plain versions break under multi-threaded writes — never share a `HashMap`/`ArrayList` across threads without external locking. `CopyOnWriteArrayList` copies the whole backing array on every write, so it's only for read-heavy/write-rare cases (listener lists, config). Also consider `EnumMap`/`EnumSet` when keys are enum constants — they're array-backed and dramatically faster/smaller than the hash versions.

---

## Immutable Collections (Java 9+)

```java
// These throw UnsupportedOperationException on any mutation attempt
List<String> names = List.of("Alice", "Bob", "Charlie");
Set<Integer> ids = Set.of(1, 2, 3);
Map<String, Integer> map = Map.of("a", 1, "b", 2);

// Set.of throws IllegalArgumentException for duplicate values — use to catch bugs
Set.of("a", "a"); // exception at creation time

// For a mutable copy:
List<String> mutable = new ArrayList<>(names);
Map<String, Integer> mutableMap = new HashMap<>(map);
```

> **In plain terms** — `List.of(...)`, `Set.of(...)`, `Map.of(...)` create compact, *unmodifiable* collections in one line — perfect for constants and for returning data you don't want callers mucking with. Trying to `add`/`remove` on one throws an exception, which is a feature: it prevents accidental mutation.

> **Going deeper** — These are genuinely immutable (and reject `null` elements), unlike the older `Collections.unmodifiableList`, which is just a *read-only view* over a list someone else can still change underneath you. Returning `List.of(...)` or `List.copyOf(existing)` from getters is the clean way to preserve [encapsulation](../02-oop/01-classes-and-objects.md#encapsulation--the-most-important-oop-principle). Immutable collections are also inherently thread-safe. One gotcha: immutable means the *collection structure* is fixed — if it holds mutable objects, those objects can still change.

---

## Sorting and Ordering

```java
List<Employee> employees = new ArrayList<>(getEmployees());

// Sort by salary ascending:
employees.sort(Comparator.comparingInt(Employee::getSalary));

// Sort by department then name:
employees.sort(
    Comparator.comparing(Employee::getDepartment)
              .thenComparing(Employee::getName)
);

// Reverse order:
employees.sort(Comparator.comparingInt(Employee::getSalary).reversed());

// Natural order requires implementing Comparable:
public class Employee implements Comparable<Employee> {
    @Override
    public int compareTo(Employee other) {
        return this.name.compareTo(other.name);
    }
}
Collections.sort(employees); // uses compareTo
```

> **In plain terms** — There are two ways to sort: give a type a *natural order* by implementing `Comparable` (its one "default" way to sort), or pass a `Comparator` to sort however you like right now (by salary, then by name, reversed…). The fluent `Comparator.comparing(...).thenComparing(...)` builder reads almost like English and replaces fiddly hand-written comparison logic.

> **Going deeper** — Rule of thumb: `Comparable` for the *one* obvious ordering (a number's value, a date's chronology), `Comparator` for everything situational — and keep `compareTo` *consistent with `equals`*, or `TreeSet`/`TreeMap` will behave oddly. Prefer `comparingInt`/`comparingLong`/`comparingDouble` over `comparing` for primitives to avoid boxing in the hot path. Build descending or null-tolerant orders with `.reversed()` and `Comparator.nullsFirst(...)`. Never implement `compareTo` as `a - b` on ints — it overflows for large/negative values; use `Integer.compare(a, b)`.

---

## Collections Utility Methods

```java
List<Integer> nums = new ArrayList<>(List.of(3, 1, 4, 1, 5, 9));

Collections.sort(nums);              // [1, 1, 3, 4, 5, 9]
Collections.reverse(nums);           // [9, 5, 4, 3, 1, 1]
Collections.shuffle(nums);           // random order
Collections.min(nums);               // 1
Collections.max(nums);               // 9
Collections.frequency(nums, 1);      // 2 — how many times 1 appears
Collections.nCopies(3, "hello");    // ["hello", "hello", "hello"]

// Thread-safe wrappers (prefer ConcurrentHashMap over this for maps):
List<String> syncList = Collections.synchronizedList(new ArrayList<>());
```

> **In plain terms** — The `Collections` utility class (plural — different from the `Collection` interface) bundles handy one-liners: sort, reverse, shuffle, min/max, frequency counts, and more. Before hand-rolling a loop for something basic, check whether `Collections` already has it.

> **Going deeper** — Two cautions. `Collections.synchronizedList/Map` only make *individual* operations atomic — a check-then-act sequence (`if (!list.contains(x)) list.add(x)`) still needs you to hold the lock manually, and you must synchronize on the collection while iterating; that's why `ConcurrentHashMap`/`CopyOnWriteArrayList` are usually better. Also `Collections.unmodifiableList` returns a *view*, not a copy (contrast with `List.copyOf`). For bulk number-crunching and transformations over collections, [streams](../04-java8-modern/02-streams.md) are typically clearer than these imperative helpers.
