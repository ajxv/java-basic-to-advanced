import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ControlFlowDemo {

    public static void main(String[] args) {
        ifElse();
        switchExpressions();
        loops();
        iteratorSafeRemoval();
    }

    static void ifElse() {
        System.out.println("=== if / else if / else ===");
        int score = 75;

        String grade;
        if (score >= 90) grade = "A";
        else if (score >= 80) grade = "B";
        else if (score >= 70) grade = "C";
        else grade = "F";

        System.out.println("Score " + score + " → Grade: " + grade);

        // Ternary
        String result = score >= 60 ? "pass" : "fail";
        System.out.println("Result: " + result);

        // Short-circuit: if list is null, .size() never called
        List<String> list = null;
        if (list != null && list.size() > 0) {
            System.out.println("Not reached");
        } else {
            System.out.println("Short-circuit protected from NPE");
        }
    }

    static void switchExpressions() {
        System.out.println("\n=== Switch ===");
        String[] days = {"MON", "SAT", "SUN", "WED"};

        for (String day : days) {
            // Modern switch expression (Java 14+): no fall-through, returns value
            String type = switch (day) {
                case "MON", "TUE", "WED", "THU", "FRI" -> "weekday";
                case "SAT", "SUN" -> "weekend";
                default -> "unknown";
            };
            System.out.println(day + " is a " + type);
        }

        // Switch with block body using yield
        int code = 2;
        String message = switch (code) {
            case 1 -> "one";
            case 2 -> {
                String prefix = "num";
                yield prefix + "-two"; // yield returns the value from a block
            }
            default -> "other";
        };
        System.out.println("code " + code + " → " + message);
    }

    static void loops() {
        System.out.println("\n=== Loops ===");

        // for loop
        System.out.print("for: ");
        for (int i = 0; i < 5; i++) System.out.print(i + " ");
        System.out.println();

        // for-each
        List<String> names = List.of("Alice", "Bob", "Charlie");
        System.out.print("for-each: ");
        for (String name : names) System.out.print(name + " ");
        System.out.println();

        // while
        System.out.print("while: ");
        int n = 0;
        while (n < 5) { System.out.print(n + " "); n++; }
        System.out.println();

        // do-while
        System.out.print("do-while: ");
        int m = 0;
        do { System.out.print(m + " "); m++; } while (m < 3);
        System.out.println();

        // break and continue
        System.out.print("break at 5: ");
        for (int i = 0; i < 10; i++) {
            if (i == 5) break;
            System.out.print(i + " ");
        }
        System.out.println();

        System.out.print("skip evens: ");
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) continue;
            System.out.print(i + " ");
        }
        System.out.println();
    }

    static void iteratorSafeRemoval() {
        System.out.println("\n=== Safe Removal During Iteration ===");
        List<String> items = new ArrayList<>(List.of("a", "", "b", "", "c"));
        System.out.println("Before: " + items);

        // Safe removal using Iterator
        Iterator<String> it = items.iterator();
        while (it.hasNext()) {
            if (it.next().isEmpty()) it.remove();
        }
        System.out.println("After Iterator.remove(): " + items);

        // Even cleaner: removeIf (Java 8+)
        List<String> items2 = new ArrayList<>(List.of("x", "", "y", "", "z"));
        items2.removeIf(String::isEmpty);
        System.out.println("After removeIf: " + items2);
    }
}
