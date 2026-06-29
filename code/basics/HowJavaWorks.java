/**
 * Demonstrates how Java actually runs: the JVM, runtime info, the
 * compiler-vs-runtime error boundary, and where bytecode lives.
 *
 * Compile and run:
 *   javac HowJavaWorks.java && java HowJavaWorks
 *
 * Then peek at the bytecode this file compiled to:
 *   javap -c HowJavaWorks
 */
public class HowJavaWorks {

    public static void main(String[] args) {
        runtimeInfo();
        compilerVsRuntime();
        jitWarmup();
    }

    static void runtimeInfo() {
        System.out.println("=== Who is running this code? ===");
        // These come from the JVM you launched with `java` — not baked into the .class file.
        System.out.println("Java version : " + System.getProperty("java.version"));
        System.out.println("JVM name     : " + System.getProperty("java.vm.name"));
        System.out.println("Vendor       : " + System.getProperty("java.vendor"));
        System.out.println("OS / arch    : " + System.getProperty("os.name")
                + " / " + System.getProperty("os.arch"));
        System.out.println("CPU cores    : " + Runtime.getRuntime().availableProcessors());
        System.out.println("Working dir  : " + System.getProperty("user.dir"));
        // The same HowJavaWorks.class would print different values on Windows/macOS/Linux —
        // that is "write once, run anywhere" in action.
    }

    static void compilerVsRuntime() {
        System.out.println("\n=== Compiler checks vs JVM checks ===");
        // The compiler trusts this cast at compile time...
        Object x = "I am actually a String";
        try {
            Integer i = (Integer) x; // ...but the JVM enforces the real type at runtime.
            System.out.println(i);
        } catch (ClassCastException e) {
            System.out.println("Runtime caught what the compiler allowed: " + e.getMessage());
        }
        // Note: `int n = "hello";` would NOT compile at all — the compiler stops it
        // before the program ever starts. That error is cheaper because users never see it.
    }

    static void jitWarmup() {
        System.out.println("\n=== JIT warm-up (hot code gets faster) ===");
        long firstRun = timeLoop();      // interpreted / cold
        for (int i = 0; i < 50; i++) {
            timeLoop();                  // run it many times so the JIT compiles the hot path
        }
        long warmRun = timeLoop();       // likely JIT-compiled now
        System.out.println("First run : " + firstRun + " ns");
        System.out.println("Warm run  : " + warmRun + " ns");
        System.out.println("(The warm run is usually faster once the JIT compiles the hot loop.)");
    }

    /** A trivial hot loop the JIT can optimize after enough invocations. */
    static long timeLoop() {
        long start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < 1_000_000; i++) {
            sum += i;
        }
        long elapsed = System.nanoTime() - start;
        if (sum == Long.MIN_VALUE) System.out.println(sum); // stop the JIT from deleting the loop
        return elapsed;
    }
}
