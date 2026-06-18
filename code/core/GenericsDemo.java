import java.util.*;
import java.util.function.*;

public class GenericsDemo {

    // Generic class: T is a type parameter
    static class Box<T> {
        private T value;

        Box(T value) { this.value = value; }
        T get() { return value; }
        void set(T value) { this.value = value; }

        // Generic method on a generic class (U is separate from T)
        <U> Box<U> map(Function<T, U> mapper) {
            return new Box<>(mapper.apply(value));
        }

        @Override
        public String toString() { return "Box[" + value + "]"; }
    }

    // Multiple type parameters
    static class Pair<A, B> {
        private final A first;
        private final B second;

        Pair(A first, B second) { this.first = first; this.second = second; }

        A getFirst() { return first; }
        B getSecond() { return second; }
        Pair<B, A> swap() { return new Pair<>(second, first); }

        @Override
        public String toString() { return "(" + first + ", " + second + ")"; }
    }

    // Generic interface
    interface Transformer<T, R> {
        R transform(T input);

        default <V> Transformer<T, V> andThen(Transformer<R, V> after) {
            return input -> after.transform(this.transform(input));
        }
    }

    public static void main(String[] args) {
        genericClass();
        genericMethods();
        boundedTypeParams();
        wildcards();
        realWorldGenerics();
    }

    static void genericClass() {
        System.out.println("=== Generic Class ===");

        Box<String> strBox = new Box<>("hello");
        Box<Integer> intBox = new Box<>(42);
        Box<List<String>> nestedBox = new Box<>(List.of("a", "b", "c"));

        System.out.println(strBox);
        System.out.println(intBox);
        System.out.println(nestedBox);

        // map transforms the value inside the box
        Box<Integer> lengthBox = strBox.map(String::length);
        System.out.println("map string→length: " + lengthBox);

        // Pair
        Pair<String, Integer> nameAge = new Pair<>("Alice", 30);
        System.out.println("Pair: " + nameAge);
        System.out.println("Swapped: " + nameAge.swap());
    }

    static void genericMethods() {
        System.out.println("\n=== Generic Methods ===");

        // Swap elements in array
        String[] words = {"hello", "world", "java"};
        swap(words, 0, 2);
        System.out.println("After swap(0,2): " + Arrays.toString(words));

        // Create a repeated list
        List<String> thrice = repeat("go", 3);
        System.out.println("repeat: " + thrice);

        // Find first matching element
        List<Integer> nums = List.of(5, 2, 8, 1, 9, 3);
        Optional<Integer> first = findFirst(nums, n -> n > 7);
        System.out.println("First > 7: " + first);

        // Transformer composition
        Transformer<String, Integer> wordCount = s -> s.split("\\s+").length;
        Transformer<Integer, String> describe = n -> n + " word(s)";
        Transformer<String, String> pipeline = wordCount.andThen(describe);

        System.out.println("pipeline: " + pipeline.transform("hello world java"));
    }

    static <T> void swap(T[] array, int i, int j) {
        T temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    static <T> List<T> repeat(T item, int times) {
        List<T> list = new ArrayList<>(times);
        for (int i = 0; i < times; i++) list.add(item);
        return list;
    }

    static <T> Optional<T> findFirst(List<T> list, Predicate<T> predicate) {
        for (T item : list) {
            if (predicate.test(item)) return Optional.of(item);
        }
        return Optional.empty();
    }

    static void boundedTypeParams() {
        System.out.println("\n=== Bounded Type Parameters ===");

        // T extends Number: can call .doubleValue() on each element
        System.out.println("sum ints: " + sum(List.of(1, 2, 3, 4, 5)));
        System.out.println("sum doubles: " + sum(List.of(1.5, 2.5, 3.0)));

        // T extends Comparable: can compare elements
        System.out.println("max string: " + max(List.of("banana", "apple", "cherry")));
        System.out.println("max int: " + max(List.of(5, 3, 9, 1)));
    }

    static <T extends Number> double sum(List<T> list) {
        double total = 0;
        for (T item : list) total += item.doubleValue();
        return total;
    }

    static <T extends Comparable<T>> T max(List<T> list) {
        if (list.isEmpty()) throw new NoSuchElementException("Empty list");
        T result = list.get(0);
        for (T item : list) {
            if (item.compareTo(result) > 0) result = item;
        }
        return result;
    }

    static void wildcards() {
        System.out.println("\n=== Wildcards ===");

        List<Integer> ints = List.of(1, 2, 3);
        List<Double> doubles = List.of(1.5, 2.5, 3.5);

        // ? extends Number — read items as Number (PRODUCER)
        System.out.println("printNumbers(ints): ");
        printNumbers(ints);
        System.out.println("printNumbers(doubles): ");
        printNumbers(doubles);

        // ? super Integer — can add Integers (CONSUMER)
        List<Number> nums = new ArrayList<>();
        addIntegers(nums);
        System.out.println("addIntegers result: " + nums);

        // Unbounded wildcard ?  — least restrictive, only Object methods available
        printSize(List.of("a", "b", "c"));
        printSize(List.of(1, 2));
    }

    // Producer Extends: list produces (gives) values
    static void printNumbers(List<? extends Number> list) {
        for (Number n : list) {
            System.out.print(n.doubleValue() + " ");
        }
        System.out.println();
        // list.add(1); // compile error — don't know exact subtype
    }

    // Consumer Super: list consumes (accepts) values
    static void addIntegers(List<? super Integer> list) {
        list.add(1);
        list.add(2);
        list.add(3);
        // Integer n = list.get(0); // compile error — only Object guaranteed when reading
    }

    static void printSize(List<?> list) {
        System.out.println("size: " + list.size()); // only Object methods available
    }

    static void realWorldGenerics() {
        System.out.println("\n=== Real World: Result<T> ===");

        // A common pattern: Result type that holds success or failure
        Result<Integer> success = Result.success(42);
        Result<Integer> failure = Result.failure("Something went wrong");

        System.out.println("success: " + success);
        System.out.println("failure: " + failure);

        // Map transforms the value if successful
        Result<String> mapped = success.map(n -> "Value is " + n);
        System.out.println("mapped: " + mapped);

        Result<String> failedMap = failure.map(n -> "Value is " + n);
        System.out.println("failedMap: " + failedMap); // stays failed, no mapping
    }

    // A simple generic Result type (similar to Optional but with error message)
    static class Result<T> {
        private final T value;
        private final String error;

        private Result(T value, String error) {
            this.value = value;
            this.error = error;
        }

        static <T> Result<T> success(T value) { return new Result<>(value, null); }
        static <T> Result<T> failure(String error) { return new Result<>(null, error); }

        boolean isSuccess() { return error == null; }

        <R> Result<R> map(Function<T, R> mapper) {
            if (isSuccess()) return Result.success(mapper.apply(value));
            return Result.failure(error);
        }

        T getOrElse(T defaultValue) { return isSuccess() ? value : defaultValue; }

        @Override
        public String toString() {
            return isSuccess() ? "Success(" + value + ")" : "Failure(" + error + ")";
        }
    }
}
