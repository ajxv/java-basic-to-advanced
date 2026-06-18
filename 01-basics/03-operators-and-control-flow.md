# Operators and Control Flow

> Runnable example: [code/basics/ControlFlowDemo.java](../code/basics/ControlFlowDemo.java)

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

---

## Loops

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
