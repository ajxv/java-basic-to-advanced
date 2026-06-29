# Operators and Control Flow

> Runnable example: [code/basics/ControlFlowDemo.java](../code/basics/ControlFlowDemo.java)

---

## The Big Picture

> **In plain terms** — *Operators* are the verbs that combine values (`+`, `&&`, `==`), and *control flow* is how you steer which lines run and how often: take a branch (`if`/`switch`), repeat (`for`/`while`), or jump out (`break`/`continue`). Together they're the basic grammar of "do this, then maybe that, over and over." Master these and you can express any algorithm.

> **Why this matters** — Two themes run through this whole topic and bite real-world code: (1) **types quietly change results** — `7 / 2` is `3`, not `3.5`, because both sides are `int`; and (2) **short-circuiting** (`&&`, `||`) lets you guard against errors by ordering checks deliberately. Modern Java also turned `switch` from an error-prone statement into a clean, value-returning *expression* — a small change that removes a whole class of fall-through bugs.

---

## Arithmetic and Assignment

```java
int a = 10, b = 3;
a + b   // 13
a - b   // 7
a * b   // 30
a / b   // 3  — integer division, remainder discarded!
a % b   // 1  — modulo (remainder)

// Integer division trap:
int x = 7 / 2;       // 3, not 3.5
double y = 7.0 / 2;  // 3.5 — promote to double first
double z = (double) 7 / 2; // 3.5 — cast one operand

// Compound assignment:
a += 5;  // a = a + 5
a++;     // a = a + 1 (post-increment)
++a;     // same for assignment purposes; matters in expressions
```

> **In plain terms** — The classic surprise: dividing two whole numbers throws away the fraction (`7 / 2` → `3`). To get `3.5`, at least one side must be a decimal. `a++` and `++a` both add one, but in a bigger expression `a++` hands back the *old* value first while `++a` hands back the *new* one.

> **Going deeper** — Compound assignments hide a cast: `a += 5` is really `a = (T)(a + 5)`, so `byte b = 10; b += 5;` compiles even though `b = b + 5` wouldn't (the latter produces an `int`). `%` works on doubles too (`5.5 % 2 == 1.5`) and can be negative (`-7 % 3 == -1` — the sign follows the dividend; use `Math.floorMod` for a non-negative result). Java has no `**` power operator — use `Math.pow`. And bitwise operators (`&`, `|`, `^`, `<<`, `>>`, `>>>`) are the non-short-circuiting, integer-level cousins of the logical ones below.

---

## Comparison and Logical

```java
a == b   // equal (use .equals() for objects)
a != b   // not equal
a > b    // greater than
a >= b   // greater than or equal

// Logical operators:
true && false  // false — AND (short-circuits: if left is false, right not evaluated)
true || false  // true  — OR  (short-circuits: if left is true, right not evaluated)
!true          // false — NOT

// Short-circuit is important:
if (list != null && list.size() > 0) { // if list is null, size() never called
    ...
}
```

> **In plain terms** — "Short-circuit" means Java stops evaluating as soon as the answer is certain: with `&&`, if the left side is false the whole thing is false, so it never bothers checking the right side. That's not just a speed trick — it's how you write safe guards like `obj != null && obj.isValid()` where checking the right side first would crash.

> **Going deeper** — `&&`/`||` short-circuit; their single-character cousins `&`/`|` are *bitwise/eager* and always evaluate both sides — occasionally useful when the right side has a needed side effect, but a frequent typo-bug otherwise. Because evaluation order is guaranteed left-to-right, order your conditions cheapest-and-most-likely-to-fail first. `==` on objects compares identity, not content — see [== vs .equals()](02-variables-and-types.md#-vs-equals--the-most-common-java-bug).

---

## if / else if / else

```java
int score = 75;

if (score >= 90) {
    grade = "A";
} else if (score >= 80) {
    grade = "B";
} else if (score >= 70) {
    grade = "C";
} else {
    grade = "F";
}

// Ternary: concise for simple two-branch conditions
String result = score >= 60 ? "pass" : "fail";

// Avoid nested ternaries — they destroy readability:
// BAD: String x = a ? b ? "both" : "a only" : "neither";
```

> **In plain terms** — `if/else if` checks conditions top to bottom and runs the *first* match, so order matters — put the strictest condition first (check `>= 90` before `>= 80`). The ternary `cond ? x : y` is a one-line `if/else` that *produces a value*, perfect for "assign one of two things."

> **Going deeper** — `if` branches on arbitrary boolean conditions; `switch` (next section) branches on the *value* of one expression and the compiler can verify you've covered everything (exhaustiveness) — prefer it when you're matching one variable against many constants. The ternary is an *expression* (yields a value) whereas `if` is a *statement* (doesn't), which is exactly why `String s = if (...)` is illegal but `String s = cond ? a : b` works. Deeply nested `if`s are often better flattened with early `return`s ("guard clauses").

---

## switch — Old vs New

```java
// OLD switch (statement) — fall-through is a common bug
switch (day) {
    case "MON":
    case "TUE":
        System.out.println("weekday"); // both MON and TUE fall through here
        break; // forget this and execution continues into next case!
    case "SAT":
        System.out.println("weekend");
        break;
    default:
        System.out.println("unknown");
}

// NEW switch expression (Java 14+) — no fall-through, returns a value
String type = switch (day) {
    case "MON", "TUE", "WED", "THU", "FRI" -> "weekday";
    case "SAT", "SUN" -> "weekend";
    default -> throw new IllegalArgumentException("Unknown day: " + day);
};

// Arrow syntax doesn't fall-through. Each case is independent.
// If you need a block of code:
String message = switch (status) {
    case "ACTIVE" -> {
        log("Active user");
        yield "User is active"; // yield returns from a block in a switch expression
    }
    case "INACTIVE" -> "User is inactive";
    default -> "Unknown status";
};
```

> **In plain terms** — The old `switch` "falls through": once a `case` matches, execution keeps running into the next case until it hits a `break` — forget the `break` and you get sneaky bugs. The new arrow (`->`) form fixes this: each case is self-contained, no `break` needed, and the whole thing can return a value you assign in one go.

> **Going deeper** — The arrow form is also *exhaustive*: when switching over an enum or sealed type, the compiler errors if you miss a case, so adding a new enum constant forces you to handle it everywhere — a huge maintenance win over `if/else` chains. Modern `switch` also does **pattern matching** (Java 21): `case Circle c -> …`, `case String s when s.isBlank() -> …`, and even `null` handling. Use `yield` to return from a multi-line `{ }` case. See [sealed types & pattern matching](../04-java8-modern/04-modern-java-9-to-21.md) for where this shines.

---

## Loops

> **In plain terms** — Four flavors, picked by what you know up front: a counting `for` when you know how many times; `for-each` to visit every item in a collection (cleanest, but no index); `while` when some condition decides when to stop; `do-while` when the body must run at least once before checking. When in doubt, reach for `for-each`.

> **Going deeper** — `for-each` is compiler sugar over an `Iterator` (or a plain index for arrays), which is why you can't safely add/remove from the collection mid-loop — doing so triggers `ConcurrentModificationException` (see the Iterator section below). For transforming or filtering data, a [Stream](../04-java8-modern/02-streams.md) often reads better than any loop. Micro-note: hoist invariant work (like `list.size()`) out of loop conditions, and prefer `i < n` bounds the JIT can optimize.

### for loop — when you know the count

```java
for (int i = 0; i < 10; i++) {
    System.out.println(i);
}

// Counting down:
for (int i = 10; i >= 0; i--) { ... }

// Multiple variables:
for (int i = 0, j = 10; i < j; i++, j--) { ... }
```

### for-each — preferred for iterating collections

```java
List<String> names = List.of("Alice", "Bob", "Charlie");
for (String name : names) {
    System.out.println(name);
}

// Also works for arrays:
int[] numbers = {1, 2, 3, 4, 5};
for (int n : numbers) {
    System.out.println(n);
}

// Limitation: you can't get the index, and you can't modify the collection.
// If you need those, use a regular for or Iterator.
```

### while — when condition drives the loop

```java
Scanner scanner = new Scanner(System.in);
while (scanner.hasNextLine()) {
    String line = scanner.nextLine();
    process(line);
}
```

### do-while — always runs at least once

```java
String input;
do {
    System.out.print("Enter a number: ");
    input = scanner.nextLine();
} while (!isValidNumber(input));
```

### Iterator — safe removal during iteration

```java
List<String> items = new ArrayList<>(List.of("a", "", "b", "", "c"));

// This throws ConcurrentModificationException:
for (String item : items) {
    if (item.isEmpty()) items.remove(item); // BAD

// Use Iterator for safe removal:
Iterator<String> it = items.iterator();
while (it.hasNext()) {
    if (it.next().isEmpty()) {
        it.remove(); // safe
    }
}

// Or use removeIf (Java 8+) — cleanest:
items.removeIf(String::isEmpty);
```

> **In plain terms** — You can't rearrange a collection while a for-each loop is walking through it — Java notices and throws `ConcurrentModificationException` to protect you from skipping or double-processing elements. To delete while looping, either use the `Iterator`'s own `remove()`, or just say what you want with `removeIf`.

> **Going deeper** — That exception is *fail-fast*: most collections track a `modCount`, and the iterator throws the instant it sees an unexpected change — it's a bug detector, not a thread-safety guarantee. The truly concurrent collections (`CopyOnWriteArrayList`, `ConcurrentHashMap`) iterate without throwing but give weakly-consistent snapshots. Across threads, even `modCount` won't always save you — use the `java.util.concurrent` collections from [multithreading](../05-advanced/02-multithreading.md).

---

## break, continue, and Labels

```java
// break exits the current loop
for (int i = 0; i < 10; i++) {
    if (i == 5) break;
    System.out.println(i); // 0 1 2 3 4
}

// continue skips the rest of the current iteration
for (int i = 0; i < 10; i++) {
    if (i % 2 == 0) continue; // skip even numbers
    System.out.println(i); // 1 3 5 7 9
}

// Labels: break/continue for nested loops (use sparingly — hurts readability)
outer:
for (int i = 0; i < 3; i++) {
    for (int j = 0; j < 3; j++) {
        if (j == 1) continue outer; // skip to next iteration of outer loop
        System.out.println(i + "," + j);
    }
}
```

> **In plain terms** — `break` leaves the loop entirely; `continue` skips just the rest of *this* round and starts the next. By default they only affect the innermost loop — a *label* lets you aim them at an outer loop instead.

> **Going deeper** — Labelled `break`/`continue` are Java's only "structured goto," and they're a readability smell: if you need them, the loop body is usually begging to be extracted into a method where a plain `return` does the same job more clearly. Note Java's labels attach to *loops/blocks*, not arbitrary lines, so they can't create spaghetti jumps the way C's `goto` can.
