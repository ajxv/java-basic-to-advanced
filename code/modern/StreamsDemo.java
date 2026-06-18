import java.util.*;
import java.util.stream.*;

public class StreamsDemo {

    record Employee(String name, String dept, int salary, boolean active) {}

    public static void main(String[] args) {
        List<Employee> employees = List.of(
            new Employee("Alice",   "Engineering", 90000, true),
            new Employee("Bob",     "Engineering", 85000, true),
            new Employee("Charlie", "HR",          65000, false),
            new Employee("Diana",   "HR",          70000, true),
            new Employee("Eve",     "Marketing",   72000, true),
            new Employee("Frank",   "Engineering", 95000, true)
        );

        filterAndMap(employees);
        sorting(employees);
        collecting(employees);
        reducing(employees);
        grouping(employees);
        flatMapping();
        shortCircuit(employees);
    }

    static void filterAndMap(List<Employee> employees) {
        System.out.println("=== filter + map ===");

        // Active engineers earning > 87k
        List<String> highEarners = employees.stream()
            .filter(Employee::active)
            .filter(e -> e.dept().equals("Engineering"))
            .filter(e -> e.salary() > 87000)
            .map(Employee::name)
            .sorted()
            .collect(Collectors.toList());

        System.out.println("Active engineers earning > 87k: " + highEarners);
    }

    static void sorting(List<Employee> employees) {
        System.out.println("\n=== sorting ===");

        // Sort by salary descending, then name
        employees.stream()
            .sorted(Comparator.comparingInt(Employee::salary).reversed()
                .thenComparing(Employee::name))
            .limit(3)
            .forEach(e -> System.out.printf("  %s: %d (%s)%n", e.name(), e.salary(), e.dept()));
    }

    static void collecting(List<Employee> employees) {
        System.out.println("\n=== collecting ===");

        // toList (Java 16+, unmodifiable)
        List<String> names = employees.stream().map(Employee::name).toList();
        System.out.println("Names: " + names);

        // joining
        String namesCsv = employees.stream()
            .map(Employee::name)
            .collect(Collectors.joining(", "));
        System.out.println("CSV: " + namesCsv);

        // toMap: name → salary
        Map<String, Integer> salaryMap = employees.stream()
            .collect(Collectors.toMap(Employee::name, Employee::salary));
        System.out.println("Alice salary: " + salaryMap.get("Alice"));
    }

    static void reducing(List<Employee> employees) {
        System.out.println("\n=== reducing / statistics ===");

        // Total salary using mapToInt + sum
        int totalSalary = employees.stream()
            .mapToInt(Employee::salary)
            .sum();
        System.out.println("Total salary: " + totalSalary);

        // IntSummaryStatistics
        IntSummaryStatistics stats = employees.stream()
            .mapToInt(Employee::salary)
            .summaryStatistics();
        System.out.printf("Min: %d, Max: %d, Avg: %.0f, Count: %d%n",
            stats.getMin(), stats.getMax(), stats.getAverage(), stats.getCount());

        // reduce to find longest name
        Optional<String> longest = employees.stream()
            .map(Employee::name)
            .reduce((a, b) -> a.length() >= b.length() ? a : b);
        System.out.println("Longest name: " + longest.orElse("none"));
    }

    static void grouping(List<Employee> employees) {
        System.out.println("\n=== grouping ===");

        // Group by department
        Map<String, List<Employee>> byDept = employees.stream()
            .collect(Collectors.groupingBy(Employee::dept));
        byDept.forEach((dept, emps) ->
            System.out.printf("  %s: %s%n", dept, emps.stream().map(Employee::name).toList()));

        // Count by department
        Map<String, Long> countByDept = employees.stream()
            .collect(Collectors.groupingBy(Employee::dept, Collectors.counting()));
        System.out.println("Count by dept: " + countByDept);

        // Average salary by department
        Map<String, Double> avgSalary = employees.stream()
            .collect(Collectors.groupingBy(Employee::dept,
                Collectors.averagingInt(Employee::salary)));
        System.out.println("Avg salary: " + avgSalary);

        // Partition active vs inactive
        Map<Boolean, List<Employee>> partition = employees.stream()
            .collect(Collectors.partitioningBy(Employee::active));
        System.out.println("Active: " + partition.get(true).stream().map(Employee::name).toList());
        System.out.println("Inactive: " + partition.get(false).stream().map(Employee::name).toList());
    }

    static void flatMapping() {
        System.out.println("\n=== flatMap ===");

        // Each department has a list of skills
        Map<String, List<String>> skills = Map.of(
            "Engineering", List.of("Java", "Docker", "Kubernetes"),
            "HR",          List.of("Communication", "Excel"),
            "Marketing",   List.of("Analytics", "Excel", "Communication")
        );

        // All unique skills across all departments
        List<String> allSkills = skills.values().stream()
            .flatMap(Collection::stream) // List<List<String>> → Stream<String>
            .distinct()
            .sorted()
            .toList();
        System.out.println("All unique skills: " + allSkills);
    }

    static void shortCircuit(List<Employee> employees) {
        System.out.println("\n=== short-circuit operations ===");

        boolean anyHighEarner = employees.stream()
            .anyMatch(e -> e.salary() > 100_000);
        System.out.println("Any earning > 100k: " + anyHighEarner); // false

        boolean allActive = employees.stream().allMatch(Employee::active);
        System.out.println("All active: " + allActive); // false (Charlie)

        Optional<Employee> firstEngineer = employees.stream()
            .filter(e -> e.dept().equals("Engineering"))
            .findFirst();
        System.out.println("First engineer: " + firstEngineer.map(Employee::name).orElse("none"));
    }
}
