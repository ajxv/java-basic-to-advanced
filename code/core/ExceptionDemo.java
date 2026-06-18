import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ExceptionDemo {

    // Custom unchecked exception (most common in service layers)
    static class UserNotFoundException extends RuntimeException {
        private final String userId;

        UserNotFoundException(String userId) {
            super("User not found: " + userId);
            this.userId = userId;
        }

        UserNotFoundException(String userId, Throwable cause) {
            super("User not found: " + userId, cause);
            this.userId = userId;
        }

        String getUserId() { return userId; }
    }

    // Custom checked exception (forces caller to handle it)
    static class InsufficientFundsException extends Exception {
        private final double shortfall;

        InsufficientFundsException(double shortfall) {
            super(String.format("Insufficient funds: need %.2f more", shortfall));
            this.shortfall = shortfall;
        }

        double getShortfall() { return shortfall; }
    }

    public static void main(String[] args) {
        checkedVsUnchecked();
        tryCatchFinally();
        tryWithResources();
        customExceptions();
        multiCatch();
        exceptionChaining();
        bestPractices();
    }

    static void checkedVsUnchecked() {
        System.out.println("=== Checked vs Unchecked ===");

        // Unchecked: runtime failures from bad code — IndexOutOfBoundsException, NPE
        List<String> list = List.of("a", "b");
        try {
            String s = list.get(10); // IndexOutOfBoundsException
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Unchecked caught: " + e.getMessage());
        }

        // NullPointerException
        try {
            String s = null;
            System.out.println(s.length()); // NPE
        } catch (NullPointerException e) {
            System.out.println("NPE caught (Java 14+ message): " + e.getMessage());
        }

        // ClassCastException
        try {
            Object o = "hello";
            Integer i = (Integer) o; // ClassCastException
        } catch (ClassCastException e) {
            System.out.println("ClassCast caught: " + e.getMessage());
        }
    }

    static void tryCatchFinally() {
        System.out.println("\n=== try-catch-finally ===");

        System.out.println("Start");
        try {
            System.out.println("In try");
            if (true) throw new RuntimeException("Test error");
            System.out.println("Never reached");
        } catch (RuntimeException e) {
            System.out.println("In catch: " + e.getMessage());
        } finally {
            // Always runs, even if exception thrown or return executed
            System.out.println("In finally (always runs)");
        }
        System.out.println("After try-catch-finally");

        // finally with return: finally wins
        System.out.println("finallyWithReturn: " + finallyWithReturn());
    }

    static String finallyWithReturn() {
        try {
            return "from try";
        } finally {
            return "from finally"; // this wins! (generally avoid this pattern)
        }
    }

    static void tryWithResources() {
        System.out.println("\n=== try-with-resources ===");

        // Any AutoCloseable is auto-closed — even if exception is thrown
        class Resource implements AutoCloseable {
            private final String name;
            Resource(String name) {
                this.name = name;
                System.out.println("  " + name + " opened");
            }

            void use() {
                System.out.println("  " + name + " used");
            }

            @Override
            public void close() {
                System.out.println("  " + name + " closed"); // guaranteed to run
            }
        }

        // Multiple resources: closed in reverse order
        try (Resource r1 = new Resource("R1");
             Resource r2 = new Resource("R2")) {
            r1.use();
            r2.use();
        } // R2 closed first, then R1
        System.out.println("  (after try block)");
    }

    static void customExceptions() {
        System.out.println("\n=== Custom Exceptions ===");

        // Unchecked: doesn't need try-catch by the compiler
        try {
            findUser("UNKNOWN-999");
        } catch (UserNotFoundException e) {
            System.out.println("Unchecked caught: " + e.getMessage() + " (id=" + e.getUserId() + ")");
        }

        // Checked: compiler forces you to handle it
        try {
            withdraw(1000.0, 200.0);
        } catch (InsufficientFundsException e) {
            System.out.printf("Checked caught: %s (shortfall=%.2f)%n", e.getMessage(), e.getShortfall());
        }
    }

    static void findUser(String id) {
        Map<String, String> db = Map.of("USR-001", "Alice");
        if (!db.containsKey(id)) {
            throw new UserNotFoundException(id);
        }
    }

    static void withdraw(double balance, double amount) throws InsufficientFundsException {
        if (amount > balance) {
            throw new InsufficientFundsException(amount - balance);
        }
        System.out.println("Withdrew " + amount);
    }

    static void multiCatch() {
        System.out.println("\n=== Multi-catch ===");

        String[] inputs = {"42", "not-a-number", null};

        for (String input : inputs) {
            try {
                Objects.requireNonNull(input, "input must not be null");
                int value = Integer.parseInt(input);
                System.out.println("Parsed: " + value);
            } catch (NullPointerException | NumberFormatException e) {
                // Handle two exception types the same way
                System.out.println("Invalid input (" + e.getClass().getSimpleName() + "): " + e.getMessage());
            }
        }
    }

    static void exceptionChaining() {
        System.out.println("\n=== Exception Chaining (Cause) ===");

        try {
            callDatabase();
        } catch (RuntimeException e) {
            System.out.println("Top-level: " + e.getMessage());
            System.out.println("Caused by: " + e.getCause().getMessage());
        }
    }

    static void callDatabase() {
        try {
            simulateDbFailure();
        } catch (IOException e) {
            // Wrap low-level exception in domain exception — preserves cause
            throw new RuntimeException("Database call failed", e);
        }
    }

    static void simulateDbFailure() throws IOException {
        throw new IOException("Connection refused to db:5432");
    }

    static void bestPractices() {
        System.out.println("\n=== Best Practices ===");

        // 1. Validate early with clear messages
        try {
            validateAge(-5);
        } catch (IllegalArgumentException e) {
            System.out.println("validateAge(-5): " + e.getMessage());
        }

        // 2. Use Objects.requireNonNull for mandatory params
        try {
            Objects.requireNonNull(null, "name must not be null");
        } catch (NullPointerException e) {
            System.out.println("requireNonNull: " + e.getMessage());
        }

        // 3. Don't use exceptions for flow control
        String input = "abc";
        // GOOD: check before parsing
        if (input.matches("-?\\d+")) {
            System.out.println("Is number: " + Integer.parseInt(input));
        } else {
            System.out.println("Not a number: '" + input + "'");
        }
    }

    static void validateAge(int age) {
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("Age must be between 0 and 150, got: " + age);
        }
    }
}
