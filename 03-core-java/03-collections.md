# Collections Framework

> Runnable example: [code/core/CollectionsDemo.java](../code/core/CollectionsDemo.java)

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
