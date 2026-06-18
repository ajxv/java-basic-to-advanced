import java.util.*;
import java.util.function.*;

public class InterfaceDemo {

    // === Interface with default and static methods ===

    interface Printable {
        void print(); // abstract: must be implemented

        default void printWithBorder() { // optional to override
            System.out.println("┌──────────────────┐");
            print();
            System.out.println("└──────────────────┘");
        }

        static boolean isSupportedFormat(String format) { // utility on the interface
            return Set.of("txt", "csv", "json").contains(format.toLowerCase());
        }
    }

    interface Exportable {
        String export();
        default String filename() { return "export_" + System.currentTimeMillis() + ".txt"; }
    }

    interface Compressible {
        default String compress(String data) {
            return "[compressed:" + data.length() + "chars]";
        }
    }

    // One class implements MULTIPLE interfaces
    static class Report implements Printable, Exportable, Compressible {
        private final String title;
        private final List<String> rows;

        Report(String title, List<String> rows) {
            this.title = title;
            this.rows = rows;
        }

        @Override
        public void print() {
            System.out.println("  Report: " + title);
            rows.forEach(r -> System.out.println("  • " + r));
        }

        @Override
        public String export() {
            return title + "\n" + String.join("\n", rows);
        }
    }

    // === Abstract class: shared state + implementation ===

    static abstract class DataProcessor {
        protected final String name;
        private int processedCount = 0;

        DataProcessor(String name) { this.name = name; }

        // Template Method pattern: defines the skeleton, subclass fills the steps
        public final void process(List<String> items) {
            System.out.println("[" + name + "] Processing " + items.size() + " items");
            List<String> filtered = filter(items);
            List<String> transformed = filtered.stream().map(this::transform).toList();
            save(transformed);
            processedCount += transformed.size();
            System.out.println("[" + name + "] Done. Total processed: " + processedCount);
        }

        protected abstract List<String> filter(List<String> items);
        protected abstract String transform(String item);
        protected abstract void save(List<String> items);
    }

    static class UpperCaseProcessor extends DataProcessor {
        UpperCaseProcessor() { super("UpperCaseProcessor"); }

        @Override
        protected List<String> filter(List<String> items) {
            return items.stream().filter(s -> !s.isBlank()).toList();
        }

        @Override
        protected String transform(String item) { return item.trim().toUpperCase(); }

        @Override
        protected void save(List<String> items) {
            System.out.println("  Saved: " + items);
        }
    }

    // === Functional interface: exactly ONE abstract method ===

    @FunctionalInterface
    interface Check<T> {
        boolean test(T value);

        // Default methods compose checks
        default Check<T> and(Check<T> other) {
            return value -> this.test(value) && other.test(value);
        }

        default Check<T> or(Check<T> other) {
            return value -> this.test(value) || other.test(value);
        }

        default Check<T> negate() {
            return value -> !this.test(value);
        }
    }

    public static void main(String[] args) {
        interfaceBasics();
        abstractClassTemplate();
        functionalInterface();
        interfaceVsAbstractClass();
    }

    static void interfaceBasics() {
        System.out.println("=== Interface Basics ===");

        Report report = new Report("Sales Q1", List.of("Alice: $5000", "Bob: $3200", "Charlie: $4100"));

        // Printable interface
        report.print();
        System.out.println();
        report.printWithBorder();

        // Static method on interface
        System.out.println("isSupportedFormat('csv'): " + Printable.isSupportedFormat("csv"));
        System.out.println("isSupportedFormat('pdf'): " + Printable.isSupportedFormat("pdf"));

        // Exportable
        String exported = report.export();
        System.out.println("Exported length: " + exported.length());
        System.out.println("Filename: " + report.filename());

        // Compressible
        System.out.println("Compressed: " + report.compress(exported));

        // Polymorphism through interface
        Printable p = report;         // Report IS-A Printable
        Exportable e = report;        // Report IS-A Exportable
        p.printWithBorder();
    }

    static void abstractClassTemplate() {
        System.out.println("\n=== Abstract Class: Template Method ===");

        DataProcessor processor = new UpperCaseProcessor();
        processor.process(List.of("  hello  ", "", "  world  ", "java  ", "  "));
        processor.process(List.of("second", "batch"));
    }

    static void functionalInterface() {
        System.out.println("\n=== Functional Interface (Composable Checks) ===");

        Check<String> notEmpty = s -> !s.isEmpty();
        Check<String> notBlank = s -> !s.isBlank();
        Check<String> shortEnough = s -> s.length() <= 20;
        Check<String> noSpaces = s -> !s.contains(" ");

        // Compose: username must pass all checks
        Check<String> validUsername = notEmpty.and(shortEnough).and(noSpaces);

        String[] usernames = {"alice", "", "this username is way too long for us", "has spaces", "bob123"};
        for (String u : usernames) {
            System.out.printf("  %-45s valid=%b%n", "'" + u + "'", validUsername.test(u));
        }

        // Lambda as functional interface
        Check<Integer> positive = n -> n > 0;
        Check<Integer> lessThan100 = n -> n < 100;
        Check<Integer> even = n -> n % 2 == 0;
        Check<Integer> validScore = positive.and(lessThan100);

        int[] scores = {-5, 0, 50, 100, 75};
        for (int score : scores) {
            System.out.printf("  score=%-4d valid=%b%n", score, validScore.test(score));
        }
    }

    static void interfaceVsAbstractClass() {
        System.out.println("\n=== Interface vs Abstract Class Decision ===");
        System.out.println("""
                Use INTERFACE when:
                  ✓ Defining a capability (can-do) — Printable, Sortable, Runnable
                  ✓ Multiple unrelated classes need the same behavior
                  ✓ You want multiple inheritance of behavior
                  ✓ You want to enable lambda usage (@FunctionalInterface)

                Use ABSTRACT CLASS when:
                  ✓ Subclasses share state (private/protected fields)
                  ✓ You need constructors with initialization logic
                  ✓ You want to provide a template (Template Method pattern)
                  ✓ Strong 'is-a' relationship with substantial shared implementation

                When in doubt: start with an interface.
                  You can always add a default method later.
                  You can't add an abstract class if they already extend something.
                """);
    }
}
