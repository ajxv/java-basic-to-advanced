# Strings

> Runnable example: [code/core/StringDemo.java](../code/core/StringDemo.java)

---

## The Big Picture

> **In plain terms** — A `String` is a piece of text, and the single most important fact about it is that it's **immutable**: once created, its characters never change. Every method that "modifies" a string (`toUpperCase`, `replace`, `trim`) actually returns a *brand-new* string and leaves the original untouched. Almost every string surprise — "why didn't my change stick?", "why is `==` false?", "why is my loop slow?" — traces back to that one property.

> **Why this matters** — Immutability is a deliberate trade. The upside is huge: strings are automatically thread-safe, safe to use as `Map` keys, and cacheable (the String Pool). The downside is that building strings by repeated concatenation creates mountains of throwaway objects — which is why `StringBuilder` exists for heavy text building. Get immutability, the pool, and `StringBuilder` straight, and strings stop being mysterious.

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

> **In plain terms** — Think of a string as written in permanent ink. `s.toUpperCase()` doesn't edit `s` — it photocopies the text, uppercases the copy, and hands you the copy. If you don't catch that copy (`s = s.toUpperCase()`), it's lost and `s` still says "hello." Beginners trip on this constantly because it *looks* like a method that changes `s`.

> **Going deeper** — Immutability is what makes a string's `hashCode` cacheable (computed once, reused forever — great for `HashMap` keys) and what makes sharing across threads safe with zero locking. Internally a `String` wraps a private `final byte[]` (since Java 9, *Compact Strings* store Latin-1 text in 1 byte/char instead of 2, halving memory for typical ASCII). Security APIs deliberately use `char[]` for passwords instead of `String` precisely *because* you can zero out a `char[]` after use, but an immutable `String` lingers in memory (and possibly the pool) until GC.

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

> **In plain terms** — Because the same literal text appears all over a program, Java stores one shared copy of each *literal* in a "pool" and reuses it — so `"hello" == "hello"` happens to be `true`. But `new String("hello")` forces a separate object, so `==` is `false` even though the text matches. The lesson isn't "learn the pool rules" — it's "never compare string content with `==`; always use `.equals()`."

> **Going deeper** — Only *compile-time constant* strings are auto-pooled; values computed at runtime (concatenation of variables, `new String(...)`) land on the regular heap and must be `.intern()`-ed to join the pool. `intern()` can save memory when you have massive numbers of duplicate runtime strings, but it's a sharp tool — interned strings live a long time and the pool itself is a `HashMap`, so over-interning can hurt. The String Pool lives in the regular heap (since Java 7; it was in PermGen before), so it's garbage-collected like anything else.

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

> **In plain terms** — Because each `+=` on a string makes a *whole new string*, doing it inside a loop is like rewriting an entire document every time you add a word — work piles up fast. `StringBuilder` is a mutable scratchpad: it appends into one growing buffer and you call `toString()` once at the end. Use it whenever you're assembling text in a loop.

> **Going deeper** — The "O(n²)" warning is real: n concatenations copy ~n²/2 characters total. `StringBuilder` amortizes to O(n) by doubling its internal array as needed — preallocate with `new StringBuilder(expectedSize)` to skip the regrowth copies. The compiler optimizes simple `a + b + c` into a single builder (modern JDKs use the even faster `invokedynamic`/`StringConcatFactory`), so manual builders for one-liners add no value. Two more notes: `StringBuilder` is *not* thread-safe (its synchronized cousin `StringBuffer` is, but you almost never need it); and inside a stream, `Collectors.joining()` beats a manual builder.

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

> **In plain terms** — Pick by the job: `+` for quick joins, `String.format`/`.formatted` when you want a template with placeholders (`%s`, `%d`), and **text blocks** (`"""..."""`) for multi-line content like JSON or SQL where escaping quotes and newlines by hand is miserable.

> **Going deeper** — `%`-format placeholders are powerful (width, precision, locale: `%,.2f` → `1,234.57`) but unchecked at compile time — a type mismatch throws at runtime, so reserve `format` for genuine templating, not hot loops. Text blocks strip *incidental* leading whitespace based on the closing `"""` position, so indentation is determined by alignment, not luck. Note: the older "String Templates" preview (`STR."..."`) was *removed* and reworked, so for now stick to `formatted`/text blocks. For user-facing localized messages, `MessageFormat`/resource bundles beat hard-coded format strings.

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

> **In plain terms** — You don't need to memorize these — just know the categories (trim, case, search, extract, replace, split, check, join) so you know what to reach for. A couple of modern favorites: `strip()` over the older `trim()` (it understands Unicode whitespace), and `isBlank()` to catch strings that are empty *or* only spaces.

> **Going deeper** — Watch the gotchas: `replace` takes literal text, but `replaceAll`/`matches`/`split` take **regex**, so `"a.b".split(".")` returns nothing (`.` matches everything) — escape it as `"\\."`. `substring`'s end index is *exclusive*. `split` drops trailing empty strings unless you pass a negative limit. `indexOf` returns `-1` when not found (not an exception). And `toLowerCase()`/`toUpperCase()` are locale-sensitive — the infamous "Turkish-I" bug means you should use `toLowerCase(Locale.ROOT)` for case-insensitive *logic* (keys, protocol tokens) rather than display.

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

> **In plain terms** — Calling `.equals()` on a possibly-null variable crashes. Flip it so the *known* value goes first (`"expected".equals(input)`) — a constant is never null, so it can't throw — or use `Objects.equals(a, b)`, which safely handles nulls on either side.

> **Going deeper** — The `"literal".equals(var)` idiom (sometimes called a "Yoda condition") trades a little readability for guaranteed null-safety and is widely used in defensive code. `Objects.equals` is the cleaner choice when *both* sides might be null. For case-insensitive comparison use `equalsIgnoreCase` rather than lowercasing both sides (it avoids allocating two new strings and the locale pitfalls noted above).

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

> **In plain terms** — Default string sorting is by character codes, so uppercase letters sort *before* lowercase ones (`"Apple"` before `"banana"`) — usually not what a human expects. Use `String.CASE_INSENSITIVE_ORDER` for human-friendly order, and chain `Comparator` methods (`comparingInt(...).thenComparing(...)`) to sort by length, then alphabetically, etc.

> **Going deeper** — Character-code ordering is *lexicographic by UTF-16 code unit*, which also mis-sorts accented characters and non-English text — for real human-language sorting use `java.text.Collator` with a `Locale`. The `Comparator` builder methods (`comparing`, `thenComparing`, `reversed`, `nullsFirst`) are the same fluent toolkit you'll use everywhere objects are sorted — see [collections](03-collections.md) and [lambdas](../04-java8-modern/01-lambdas-and-functional.md). Sorting is `O(n log n)`, so for "top 3" prefer a partial approach over a full sort.

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

> **In plain terms** — Turning `"42"` into a number uses `Integer.parseInt` (and its `Double`/`Long` cousins), but if the text isn't a valid number it throws `NumberFormatException`. So either validate first or wrap the parse in a try/catch — never assume user input is well-formed.

> **Going deeper** — `parseInt` is strict: leading/trailing spaces, commas, `+`/underscores, or empty strings all throw — `strip()` first if needed, and remember it also throws on values outside `int` range. Per the [exceptions guide](01-exception-handling.md#exception-best-practices), avoid using the `catch` as routine flow control on hot paths (each throw builds a stack trace); validate with a regex or a small tolerant parser when invalid input is *expected*. For locale-aware parsing of grouped numbers (`"1,234.56"`), use `NumberFormat`; for money, parse into `BigDecimal`, not `double`.
