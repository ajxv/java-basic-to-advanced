import java.util.*;
import java.util.stream.Collectors;

public class CollectionsDemo {

    record Employee(String name, String dept, int salary) {}

    public static void main(String[] args) {
        listDemo();
        setDemo();
        mapDemo();
        sortingDemo();
        realWorldGrouping();
    }

    static void listDemo() {
        System.out.println("=== List ===");

        List<String> names = new ArrayList<>();
        names.add("Charlie");
        names.add("Alice");
        names.add("Bob");
        names.add(0, "Zara"); // insert at index 0 — O(n)

        System.out.println("List: " + names);
        System.out.println("Get index 1: " + names.get(1)); // O(1)
        System.out.println("Size: " + names.size());
        System.out.println("Contains Alice: " + names.contains("Alice"));

        names.remove("Charlie");
        System.out.println("After remove Charlie: " + names);

        // sort
        Collections.sort(names);
        System.out.println("Sorted: " + names);

        // removeIf
        List<Integer> nums = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
        nums.removeIf(n -> n % 2 == 0);
        System.out.println("Odds only: " + nums);
    }

    static void setDemo() {
        System.out.println("\n=== Set ===");

        // HashSet: no order guaranteed, O(1) ops
        Set<String> hashSet = new HashSet<>();
        hashSet.add("banana");
        hashSet.add("apple");
        hashSet.add("banana"); // duplicate — ignored
        System.out.println("HashSet (no guaranteed order): " + hashSet);

        // LinkedHashSet: insertion order preserved
        Set<String> linked = new LinkedHashSet<>();
        linked.add("banana");
        linked.add("apple");
        linked.add("cherry");
        System.out.println("LinkedHashSet (insertion order): " + linked);

        // TreeSet: always sorted
        Set<String> tree = new TreeSet<>();
        tree.add("banana");
        tree.add("apple");
        tree.add("cherry");
        System.out.println("TreeSet (sorted): " + tree);

        // Set operations
        Set<Integer> a = new HashSet<>(Set.of(1, 2, 3, 4, 5));
        Set<Integer> b = new HashSet<>(Set.of(3, 4, 5, 6, 7));

        Set<Integer> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        System.out.println("Intersection: " + new TreeSet<>(intersection)); // sorted for display

        Set<Integer> union = new HashSet<>(a);
        union.addAll(b);
        System.out.println("Union: " + new TreeSet<>(union));
    }

    static void mapDemo() {
        System.out.println("\n=== Map ===");

        Map<String, Integer> scores = new HashMap<>();
        scores.put("Alice", 95);
        scores.put("Bob", 87);
        scores.put("Charlie", 92);
        scores.put("Alice", 99); // replaces 95

        System.out.println("Alice's score: " + scores.get("Alice"));
        System.out.println("Unknown score: " + scores.get("Unknown")); // null!
        System.out.println("Unknown (default): " + scores.getOrDefault("Unknown", 0));

        // Safe operations
        scores.putIfAbsent("Diana", 88);      // only puts if absent
        scores.merge("Alice", 5, Integer::sum); // Alice = 99 + 5 = 104

        System.out.println("All scores:");
        scores.forEach((name, score) -> System.out.printf("  %s: %d%n", name, score));

        // Frequency count — common interview pattern
        String text = "hello world hello java hello";
        Map<String, Long> frequency = Arrays.stream(text.split(" "))
            .collect(Collectors.groupingBy(w -> w, Collectors.counting()));
        System.out.println("Word frequency: " + frequency);
    }

    static void sortingDemo() {
        System.out.println("\n=== Sorting ===");

        List<Employee> employees = new ArrayList<>(List.of(
            new Employee("Bob", "Engineering", 85000),
            new Employee("Alice", "HR", 65000),
            new Employee("Charlie", "Engineering", 95000),
            new Employee("Diana", "HR", 70000)
        ));

        // Sort by salary
        employees.sort(Comparator.comparingInt(Employee::salary));
        System.out.println("By salary:");
        employees.forEach(e -> System.out.printf("  %s: %d%n", e.name(), e.salary()));

        // Sort by dept then name
        employees.sort(Comparator.comparing(Employee::dept).thenComparing(Employee::name));
        System.out.println("By dept then name:");
        employees.forEach(e -> System.out.printf("  [%s] %s%n", e.dept(), e.name()));
    }

    static void realWorldGrouping() {
        System.out.println("\n=== Real World: Grouping ===");

        List<Employee> employees = List.of(
            new Employee("Alice", "Engineering", 90000),
            new Employee("Bob", "Engineering", 85000),
            new Employee("Charlie", "HR", 65000),
            new Employee("Diana", "HR", 70000),
            new Employee("Eve", "Marketing", 72000)
        );

        // Group by department
        Map<String, List<Employee>> byDept = employees.stream()
            .collect(Collectors.groupingBy(Employee::dept));

        System.out.println("Employees by dept:");
        byDept.forEach((dept, emps) -> {
            System.out.println("  " + dept + ":");
            emps.forEach(e -> System.out.println("    " + e.name()));
        });

        // Average salary by dept
        Map<String, Double> avgSalary = employees.stream()
            .collect(Collectors.groupingBy(Employee::dept, Collectors.averagingInt(Employee::salary)));
        System.out.println("Avg salary by dept: " + avgSalary);
    }
}
