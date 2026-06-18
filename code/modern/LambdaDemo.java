import java.util.*;
import java.util.function.*;

public class LambdaDemo {

    public static void main(String[] args) {
        lambdaBasics();
        builtInFunctionalInterfaces();
        methodReferences();
        composingFunctions();
        closures();
    }

    static void lambdaBasics() {
        System.out.println("=== Lambda Basics ===");

        // Before lambdas: anonymous class
        Runnable r1 = new Runnable() {
            @Override
            public void run() { System.out.println("Old way: anonymous class"); }
        };

        // Lambda
        Runnable r2 = () -> System.out.println("Lambda: no params");
        Runnable r3 = () -> {
            System.out.println("Lambda:");
            System.out.println("  block body");
        };

        r1.run(); r2.run(); r3.run();

        // With Comparator
        List<String> words = new ArrayList<>(List.of("banana", "apple", "cherry", "date"));

        // Anonymous class (old)
        words.sort(new Comparator<String>() {
            @Override
            public int compare(String a, String b) { return a.length() - b.length(); }
        });
        System.out.println("Anon class sort by length: " + words);

        // Lambda (clean)
        words.sort((a, b) -> a.compareTo(b));
        System.out.println("Lambda sort alphabetical: " + words);
    }

    static void builtInFunctionalInterfaces() {
        System.out.println("\n=== Built-in Functional Interfaces ===");

        // Function<T, R>: T in, R out
        Function<String, Integer> length = s -> s.length();
        Function<Integer, String> describe = n -> "length=" + n;
        Function<String, String> combined = length.andThen(describe);
        System.out.println("Function: " + combined.apply("hello")); // length=5

        // Predicate<T>: T in, boolean out
        Predicate<String> notEmpty = s -> !s.isEmpty();
        Predicate<String> short10 = s -> s.length() <= 10;
        Predicate<String> valid = notEmpty.and(short10);
        System.out.println("Predicate 'hello': " + valid.test("hello")); // true
        System.out.println("Predicate '': " + valid.test(""));             // false

        // Consumer<T>: T in, void (side effect)
        Consumer<String> shout = s -> System.out.println(s.toUpperCase() + "!");
        List.of("hello", "world").forEach(shout);

        // Supplier<T>: void in, T out
        Supplier<List<String>> factory = ArrayList::new;
        List<String> list = factory.get();
        list.add("from supplier");
        System.out.println("Supplier: " + list);

        // BiFunction<T, U, R>: two params in, R out
        BiFunction<String, Integer, String> repeat = (s, n) -> s.repeat(n);
        System.out.println("BiFunction: " + repeat.apply("ha", 3)); // hahaha
    }

    static void methodReferences() {
        System.out.println("\n=== Method References ===");

        List<String> words = List.of("hello", "world", "java");

        // Lambda vs method reference
        words.forEach(s -> System.out.println(s));  // lambda
        words.forEach(System.out::println);           // method ref — same thing

        // 1. Static method: ClassName::staticMethod
        Function<String, Integer> parseInt = Integer::parseInt;
        System.out.println("parseInt: " + parseInt.apply("42"));

        // 2. Instance method on specific instance
        String prefix = "Hello";
        Predicate<String> startWith = prefix::startsWith; // wait, let me fix this
        // More accurate:
        Consumer<String> log = System.out::println; // System.out is specific instance

        // 3. Instance method on arbitrary instance: ClassName::instanceMethod
        Function<String, String> upper = String::toUpperCase; // s -> s.toUpperCase()
        Predicate<String> isEmpty = String::isEmpty;
        System.out.println("upper.apply('hello'): " + upper.apply("hello"));

        // 4. Constructor: ClassName::new
        Supplier<ArrayList<String>> listFactory = ArrayList::new;
        Function<String, StringBuilder> sbFactory = StringBuilder::new;
        System.out.println("constructor ref: " + sbFactory.apply("test"));
    }

    static void composingFunctions() {
        System.out.println("\n=== Composing Functions ===");

        Function<Integer, Integer> times2 = n -> n * 2;
        Function<Integer, Integer> plus3 = n -> n + 3;

        // andThen: times2 first, then plus3
        Function<Integer, Integer> times2ThenPlus3 = times2.andThen(plus3);
        System.out.println("(5*2)+3 = " + times2ThenPlus3.apply(5)); // 13

        // compose: plus3 first, then times2
        Function<Integer, Integer> plus3ThenTimes2 = times2.compose(plus3);
        System.out.println("(5+3)*2 = " + plus3ThenTimes2.apply(5)); // 16

        // Predicate composition
        Predicate<Integer> positive = n -> n > 0;
        Predicate<Integer> lessThan100 = n -> n < 100;
        Predicate<Integer> inRange = positive.and(lessThan100);

        System.out.println("50 in range (0,100): " + inRange.test(50));   // true
        System.out.println("-1 in range (0,100): " + inRange.test(-1));   // false
        System.out.println("150 in range (0,100): " + inRange.test(150)); // false
    }

    static void closures() {
        System.out.println("\n=== Closures (Capturing Variables) ===");

        String greeting = "Hello"; // effectively final — never reassigned
        Consumer<String> greet = name -> System.out.println(greeting + ", " + name + "!");
        greet.accept("Alice");
        greet.accept("Bob");

        // You can capture effectively-final local variables:
        int multiplier = 3; // effectively final
        Function<Integer, Integer> triple = n -> n * multiplier;
        System.out.println("triple(7) = " + triple.apply(7)); // 21

        // This would NOT compile:
        // int counter = 0;
        // counter++; // now counter is NOT effectively final
        // Runnable r = () -> System.out.println(counter); // compile error
    }
}
