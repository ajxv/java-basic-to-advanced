# Strings

> Runnable example: [code/core/StringDemo.java](../code/core/StringDemo.java)

---

## Immutability — The Root of Most String Confusion

`String` objects in Java are **immutable**. Every "modification" creates a new object.

```java
String s = "hello";
s.toUpperCase();         // creates a NEW string — s is still "hello"
System.out.println(s);  // "hello"

s = s.toUpperCase();    // now s points to the new string
System.out.println(s);  // "HELLO"
```

Immutability makes strings safe to share across threads and in hash maps, but means you must always reassign to see the result.

---

## String Pool

Java interns string literals in a shared pool to save memory.

```java
String a = "hello";          // goes into the pool
String b = "hello";          // reuses the same pool object
String c = new String("hello"); // explicitly creates a new heap object

System.out.println(a == b);           // true — same pooled object
System.out.println(a == c);           // false — different objects
System.out.println(a.equals(c));      // true — same content
System.out.println(c.intern() == a);  // true — intern() adds c to pool
```

**Rule:** Use `.equals()` to compare content. Never use `==` on strings except by accident.

---

## StringBuilder — For String Building

```java
// BAD: creates a new String object on every += in a loop — O(n²) memory
String result = "";
for (int i = 0; i < 10000; i++) {
    result += i; // slow: allocates new string each iteration
}

// GOOD: StringBuilder mutates the same buffer — O(n)
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 10000; i++) {
    sb.append(i);
}
String result = sb.toString();

// Chaining:
String formatted = new StringBuilder()
    .append("Hello, ")
    .append(name)
    .append("! You are ")
    .append(age)
    .append(" years old.")
    .toString();
```

**Note:** For simple single-line concatenation (outside loops), the compiler converts it to StringBuilder automatically — no need to do it manually.

---

## String Formatting

```java
// String.format — readable but slower (uses reflection)
String msg = String.format("User %s has %d points", username, points);

// + concatenation — for simple cases
String greeting = "Hello, " + name + "!";

// Text blocks (Java 13+) — multi-line strings without escaping
String json = """
        {
            "name": "Alice",
            "age": 30
        }
        """;

// Formatted (Java 15+, on String itself)
String msg2 = "User %s has %d points".formatted(username, points);
```

---

## Essential String Methods

```java
String s = "  Hello, World!  ";

// Whitespace
s.trim()                    // "Hello, World!" — removes leading/trailing whitespace
s.strip()                   // Java 11: same but handles Unicode whitespace
s.stripLeading()            // removes only leading
s.stripTrailing()           // removes only trailing

// Case
s.toLowerCase()             // "  hello, world!  "
s.toUpperCase()             // "  HELLO, WORLD!  "

// Search
s.contains("World")         // true
s.startsWith("  He")        // true
s.endsWith("  ")            // true
s.indexOf("o")              // 5 — first occurrence, -1 if not found
s.lastIndexOf("o")          // 9
s.indexOf("o", 6)           // 9 — search from index 6

// Extract
s.substring(2, 7)           // "Hello" — [2, 7)
s.charAt(2)                 // 'H'
s.toCharArray()             // char[]

// Replace
s.replace("World", "Java") // "  Hello, Java!  " — replaces all occurrences
s.replaceAll("\\s+", " ")  // regex replace — "Hello, World!" (collapses whitespace)
s.replaceFirst("l", "L")   // "  HeLlo, World!  "

// Split
"a,b,c".split(",")         // ["a", "b", "c"]
"a,b,c".split(",", 2)      // ["a", "b,c"] — limit 2 parts

// Check
"".isEmpty()                // true
" ".isEmpty()               // false — not empty
" ".isBlank()               // true — Java 11, checks only whitespace
s.matches("\\s*Hello.*")    // true — full string regex match

// Join
String.join(", ", "a", "b", "c")          // "a, b, c"
String.join("-", List.of("x", "y", "z"))  // "x-y-z"

// Repeat (Java 11+)
"ha".repeat(3)              // "hahaha"

// Convert
String.valueOf(42)          // "42"
String.valueOf(true)        // "true"
Integer.toString(42)        // "42"
```

---

## Null-Safe Comparison

```java
String s1 = getUserInput();  // might be null
String s2 = "expected";

// NPE if s1 is null:
s1.equals(s2); // NullPointerException!

// Put the literal first (known non-null):
"expected".equals(s1); // safe — returns false if s1 is null

// Or use Objects.equals (null-safe):
Objects.equals(s1, s2); // returns false if either is null, true if both null
```

---

## Sorting Strings

```java
List<String> words = new ArrayList<>(List.of("banana", "Apple", "cherry"));

// Default: lexicographic, case-sensitive ('A' < 'a' in ASCII)
Collections.sort(words);
// ["Apple", "banana", "cherry"]

// Case-insensitive:
words.sort(String.CASE_INSENSITIVE_ORDER);
// ["Apple", "banana", "cherry"]

// By length then alphabetical:
words.sort(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));
```

---

## Parsing Strings

```java
// String → number
int n = Integer.parseInt("42");
double d = Double.parseDouble("3.14");
long l = Long.parseLong("9876543210");

// Common trap: throws NumberFormatException for invalid input
try {
    int x = Integer.parseInt(userInput);
} catch (NumberFormatException e) {
    System.err.println("Not a valid number");
}

// Safer: check first
if (input.matches("-?\\d+")) {
    int x = Integer.parseInt(input);
}

// number → String
String s = String.valueOf(42);   // "42"
String s2 = 42 + "";            // "42" — works but less clear
```
