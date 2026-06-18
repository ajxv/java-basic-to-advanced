import java.math.BigDecimal;
import java.util.Objects;

public class TypesDemo {

    public static void main(String[] args) {
        primitiveTypes();
        floatingPointTrap();
        integerOverflow();
        stackVsHeap();
        equalsVsDoubleEquals();
        casting();
    }

    static void primitiveTypes() {
        System.out.println("=== Primitive Types ===");
        byte b = 127;
        short s = 32767;
        int i = 2_147_483_647;       // underscores for readability (Java 7+)
        long l = 9_876_543_210L;     // L suffix for long literal
        float f = 3.14f;             // f suffix for float
        double d = 3.141592653589793;
        boolean flag = true;
        char c = 'A';

        System.out.println("int max: " + i);
        System.out.println("long:    " + l);
        System.out.println("double:  " + d);
        System.out.println("char:    " + c + " = " + (int) c); // 65 — char is a number
    }

    static void floatingPointTrap() {
        System.out.println("\n=== Floating Point Trap ===");
        double a = 0.1 + 0.2;
        System.out.println("0.1 + 0.2 = " + a); // 0.30000000000000004 — surprise!

        BigDecimal bd1 = new BigDecimal("0.10"); // use String constructor!
        BigDecimal bd2 = new BigDecimal("0.20");
        System.out.println("BigDecimal: 0.10 + 0.20 = " + bd1.add(bd2)); // 0.30 ✓

        // Bad BigDecimal constructor:
        BigDecimal bad = new BigDecimal(0.1); // inherits double imprecision
        System.out.println("BigDecimal(0.1) = " + bad); // still imprecise!
    }

    static void integerOverflow() {
        System.out.println("\n=== Integer Overflow ===");
        int max = Integer.MAX_VALUE;
        System.out.println("Integer.MAX_VALUE = " + max);
        System.out.println("MAX_VALUE + 1 = " + (max + 1)); // wraps to negative!

        long safe = (long) max + 1;
        System.out.println("(long) MAX_VALUE + 1 = " + safe); // correct
    }

    static void stackVsHeap() {
        System.out.println("\n=== Stack vs Heap ===");
        int x = 5; // on stack
        String s1 = new String("hello"); // reference on stack, object on heap
        String s2 = "hello"; // reference on stack, object in String Pool

        System.out.println("new String('hello') == 'hello': " + (s1 == s2)); // false
        System.out.println("'hello' == 'hello': " + (s2 == "hello"));         // true (pooled)
    }

    static void equalsVsDoubleEquals() {
        System.out.println("\n=== == vs .equals() ===");
        String a = new String("hello");
        String b = new String("hello");

        System.out.println("a == b: " + (a == b));          // false — different objects
        System.out.println("a.equals(b): " + a.equals(b));  // true  — same content
        System.out.println("Objects.equals(a, b): " + Objects.equals(a, b)); // true, null-safe
        System.out.println("Objects.equals(null, null): " + Objects.equals(null, null)); // true
        System.out.println("Objects.equals(null, a): " + Objects.equals(null, a));       // false
    }

    static void casting() {
        System.out.println("\n=== Type Casting ===");
        // Widening — automatic
        int i = 100;
        long l = i;   // int -> long: automatic
        double d = i; // int -> double: automatic
        System.out.println("int to double: " + d);

        // Narrowing — explicit, possible data loss
        double pi = 3.14159;
        int truncated = (int) pi; // fraction lost
        System.out.println("(int) 3.14159 = " + truncated); // 3

        long big = 300L;
        byte wrapped = (byte) big; // 300 % 256 = 44
        System.out.println("(byte) 300L = " + wrapped); // 44
    }
}
