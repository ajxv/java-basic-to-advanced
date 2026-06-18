import java.util.*;

public class StringDemo {

    public static void main(String[] args) {
        immutability();
        stringPool();
        stringBuilder();
        essentialMethods();
        formatting();
        comparison();
        parsing();
        textBlocks();
    }

    static void immutability() {
        System.out.println("=== Immutability ===");
        String s = "hello";
        s.toUpperCase();           // creates a new string — doesn't change s
        System.out.println(s);     // still "hello"

        s = s.toUpperCase();       // reassign to see the result
        System.out.println(s);     // "HELLO"

        // String is safe to share — immutability guarantees no one can change it
        String shared = "config-value";
        useInMultiplePlaces(shared, shared);
    }

    static void useInMultiplePlaces(String a, String b) {
        // Can't accidentally change `shared` through a or b
        System.out.println("Same object: " + (a == b)); // true
    }

    static void stringPool() {
        System.out.println("\n=== String Pool ===");
        String a = "hello";         // pool
        String b = "hello";         // same pool object
        String c = new String("hello"); // new heap object, outside pool

        System.out.println("literal == literal: " + (a == b));       // true
        System.out.println("literal == new: " + (a == c));            // false
        System.out.println("literal.equals(new): " + a.equals(c));    // true

        // intern() puts the string into the pool (or returns the pooled copy)
        String d = c.intern();
        System.out.println("literal == interned: " + (a == d));        // true
    }

    static void stringBuilder() {
        System.out.println("\n=== StringBuilder vs Concatenation ===");

        // In a loop, += creates a new String each time — exponential memory
        long start = System.nanoTime();
        String bad = "";
        for (int i = 0; i < 5000; i++) bad += "x";
        long badTime = System.nanoTime() - start;

        // StringBuilder reuses the same buffer — O(n)
        start = System.nanoTime();
        StringBuilder sb = new StringBuilder(5000);
        for (int i = 0; i < 5000; i++) sb.append("x");
        String good = sb.toString();
        long goodTime = System.nanoTime() - start;

        System.out.printf("Concatenation: %d ms%n", badTime / 1_000_000);
        System.out.printf("StringBuilder: %d ms%n", goodTime / 1_000_000);
        System.out.println("Same length: " + (bad.length() == good.length()));

        // Chaining:
        String result = new StringBuilder()
            .append("Hello, ")
            .append("World")
            .append("!")
            .toString();
        System.out.println("Chained: " + result);

        // StringBuilder is also useful for building conditionally:
        StringBuilder out = new StringBuilder("User: ");
        String name = "Alice";
        String role = "admin";
        out.append(name);
        if (role != null) out.append(" [").append(role).append("]");
        System.out.println("Conditional: " + out);
    }

    static void essentialMethods() {
        System.out.println("\n=== Essential Methods ===");
        String s = "  Hello, World!  ";

        System.out.println("trim:           '" + s.trim() + "'");
        System.out.println("strip:          '" + s.strip() + "'");       // Java 11
        System.out.println("toLowerCase:    '" + s.toLowerCase() + "'");
        System.out.println("toUpperCase:    '" + s.toUpperCase() + "'");
        System.out.println("contains World: " + s.contains("World"));
        System.out.println("startsWith '  H': " + s.startsWith("  H"));
        System.out.println("indexOf 'o':    " + s.indexOf('o'));
        System.out.println("lastIndexOf 'o':" + s.lastIndexOf('o'));
        System.out.println("replace:        '" + s.replace("World", "Java") + "'");
        System.out.println("substring(2,7): '" + s.substring(2, 7) + "'");
        System.out.println("charAt(2):      " + s.charAt(2));
        System.out.println("isEmpty:        " + s.isEmpty());
        System.out.println("isBlank:        " + s.isBlank());           // Java 11
        System.out.println("'  '.isBlank:   " + "  ".isBlank());
        System.out.println("repeat:         " + "ha".repeat(3));        // Java 11

        // split
        String csv = "Alice,Bob,Charlie,Diana";
        String[] parts = csv.split(",");
        System.out.println("split: " + Arrays.toString(parts));

        // join
        System.out.println("join: " + String.join(" | ", parts));
        System.out.println("join list: " + String.join(", ", List.of("x", "y", "z")));
    }

    static void formatting() {
        System.out.println("\n=== Formatting ===");
        String name = "Alice";
        int age = 30;
        double score = 98.5;

        // String.format
        System.out.println(String.format("Name: %s, Age: %d, Score: %.1f", name, age, score));

        // .formatted() (Java 15+)
        System.out.println("Name: %s, Age: %d".formatted(name, age));

        // Padding and alignment
        System.out.println(String.format("|%-10s|%5d|%8.2f|", name, age, score));
        //                                  left-pad  right  decimal

        // Common format specifiers:
        // %s  — String
        // %d  — int/long
        // %f  — float/double    %.2f = 2 decimal places
        // %b  — boolean
        // %n  — newline (platform-aware)
        // %10s — right-pad to 10   %-10s — left-pad to 10
    }

    static void comparison() {
        System.out.println("\n=== Comparison ===");
        String a = "Hello";
        String b = "hello";
        String c = null;

        System.out.println("equals:             " + a.equals(b));              // false
        System.out.println("equalsIgnoreCase:   " + a.equalsIgnoreCase(b));    // true
        System.out.println("compareTo:          " + a.compareTo(b));           // negative (H < h)
        System.out.println("Objects.equals:     " + Objects.equals(a, c));     // false, null-safe
        System.out.println("Objects.equals(null,null): " + Objects.equals(c, c)); // true

        // Null-safe comparison in practice:
        String userInput = null;
        // BAD: if (userInput.equals("admin")) — NPE if null
        // GOOD: put the literal first
        if ("admin".equals(userInput)) {
            System.out.println("Is admin");
        } else {
            System.out.println("Not admin (safe, no NPE)");
        }

        // Sorting strings
        List<String> words = new ArrayList<>(List.of("Banana", "apple", "Cherry"));
        words.sort(String.CASE_INSENSITIVE_ORDER);
        System.out.println("Case-insensitive sort: " + words);
    }

    static void parsing() {
        System.out.println("\n=== Parsing Strings ===");

        // String → number
        int n = Integer.parseInt("42");
        double d = Double.parseDouble("3.14");
        long l = Long.parseLong("9876543210");
        boolean b = Boolean.parseBoolean("true"); // case-insensitive
        System.out.printf("int=%d, double=%.2f, long=%d, bool=%b%n", n, d, l, b);

        // Safe parsing with validation
        String[] inputs = {"123", "abc", "", "99.5"};
        for (String input : inputs) {
            if (input.matches("-?\\d+")) {
                System.out.println("'" + input + "' → int: " + Integer.parseInt(input));
            } else {
                System.out.println("'" + input + "' → not an integer");
            }
        }

        // number → String
        System.out.println("valueOf: " + String.valueOf(42));
        System.out.println("toString: " + Integer.toString(255, 16)); // hex: ff
    }

    static void textBlocks() {
        System.out.println("\n=== Text Blocks (Java 15+) ===");

        String json = """
                {
                    "name": "Alice",
                    "role": "admin"
                }
                """;
        System.out.print("JSON:\n" + json);

        // Strip indent based on closing """
        String sql = """
                SELECT *
                FROM users
                WHERE active = true
                """;
        System.out.print("SQL:\n" + sql);
    }
}
