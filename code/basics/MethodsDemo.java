public class MethodsDemo {

    public static void main(String[] args) {
        passByValue();
        overloading();
        varargs();
        staticVsInstance();
    }

    // === Pass by Value ===

    static void passByValue() {
        System.out.println("=== Pass by Value ===");

        // Primitive: local copy — caller unchanged
        int n = 5;
        tryChangePrimitive(n);
        System.out.println("After tryChangePrimitive: " + n); // still 5

        // Object reference: mutation is visible, reassignment is not
        StringBuilder sb = new StringBuilder("hello");
        tryChangeObject(sb);
        System.out.println("After tryChangeObject: " + sb); // "hello world"
    }

    static void tryChangePrimitive(int x) {
        x = 100; // changes only local copy
    }

    static void tryChangeObject(StringBuilder sb) {
        sb.append(" world");           // mutates the OBJECT — visible to caller
        sb = new StringBuilder("new"); // reassigns LOCAL variable only — caller unaffected
    }

    // === Overloading ===

    static int add(int a, int b)       { return a + b; }
    static double add(double a, double b) { return a + b; }
    static String add(String a, String b) { return a + b; }
    static int add(int a, int b, int c) { return a + b + c; }

    static void overloading() {
        System.out.println("\n=== Overloading ===");
        System.out.println("add(1, 2) = " + add(1, 2));             // int version
        System.out.println("add(1.5, 2.5) = " + add(1.5, 2.5));    // double version
        System.out.println("add('a','b') = " + add("a", "b"));      // String version
        System.out.println("add(1,2,3) = " + add(1, 2, 3));         // 3-param version
    }

    // === Varargs ===

    static int sum(int... numbers) {
        int total = 0;
        for (int n : numbers) total += n;
        return total;
    }

    static void log(String level, String... messages) {
        for (String msg : messages) {
            System.out.println("[" + level + "] " + msg);
        }
    }

    static void varargs() {
        System.out.println("\n=== Varargs ===");
        System.out.println("sum() = " + sum());               // 0
        System.out.println("sum(1,2,3) = " + sum(1, 2, 3));   // 6
        System.out.println("sum(1..5) = " + sum(1, 2, 3, 4, 5)); // 15

        log("INFO", "Server started");
        log("WARN", "Disk 80% full", "Memory pressure detected");
    }

    // === Static vs Instance ===

    static int instanceCount = 0;
    int id;

    MethodsDemo() {
        this.id = ++instanceCount;
    }

    static int getCount() { return instanceCount; } // no instance needed
    String identify() { return "Instance #" + id; } // needs `this`

    static void staticVsInstance() {
        System.out.println("\n=== Static vs Instance ===");
        System.out.println("Before: count=" + getCount());

        MethodsDemo a = new MethodsDemo();
        MethodsDemo b = new MethodsDemo();
        MethodsDemo c = new MethodsDemo();

        System.out.println("After creating 3: count=" + getCount());
        System.out.println(a.identify());
        System.out.println(b.identify());
        System.out.println(c.identify());
    }
}
