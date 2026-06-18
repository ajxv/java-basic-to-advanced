import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadingDemo {

    public static void main(String[] args) throws Exception {
        raceConditionDemo();
        atomicDemo();
        executorServiceDemo();
        completableFutureDemo();
        producerConsumerDemo();
    }

    // === Race Condition — what goes wrong without synchronization ===
    static void raceConditionDemo() throws InterruptedException {
        System.out.println("=== Race Condition ===");

        // Unsafe counter (intentional — to show the bug)
        int[] unsafeCount = {0};
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    unsafeCount[0]++; // read-modify-write is NOT atomic
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) t.join();
        System.out.println("Unsafe count (expected 10000, actual): " + unsafeCount[0]);

        // Synchronized counter — correct
        class SafeCounter {
            private int count = 0;
            synchronized void increment() { count++; }
            synchronized int get() { return count; }
        }

        SafeCounter safe = new SafeCounter();
        Thread[] safeThreads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            safeThreads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) safe.increment();
            });
            safeThreads[i].start();
        }
        for (Thread t : safeThreads) t.join();
        System.out.println("Safe count (expected 10000, actual): " + safe.get());
    }

    // === AtomicInteger — lock-free for single values ===
    static void atomicDemo() throws InterruptedException {
        System.out.println("\n=== AtomicInteger ===");

        AtomicInteger counter = new AtomicInteger(0);
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) counter.incrementAndGet();
            });
            threads[i].start();
        }
        for (Thread t : threads) t.join();
        System.out.println("Atomic count: " + counter.get()); // always 10000
    }

    // === ExecutorService — thread pool ===
    static void executorServiceDemo() throws Exception {
        System.out.println("\n=== ExecutorService ===");

        ExecutorService pool = Executors.newFixedThreadPool(3);

        // Submit tasks and get Future for results
        Future<String> f1 = pool.submit(() -> {
            Thread.sleep(100);
            return "Task 1 done by " + Thread.currentThread().getName();
        });
        Future<String> f2 = pool.submit(() -> {
            Thread.sleep(50);
            return "Task 2 done by " + Thread.currentThread().getName();
        });
        Future<Integer> f3 = pool.submit(() -> {
            int sum = 0;
            for (int i = 1; i <= 100; i++) sum += i;
            return sum;
        });

        // get() blocks until result is ready
        System.out.println(f1.get());
        System.out.println(f2.get());
        System.out.println("Sum 1..100 = " + f3.get());

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }

    // === CompletableFuture — async chaining without blocking ===
    static void completableFutureDemo() throws Exception {
        System.out.println("\n=== CompletableFuture ===");

        // Chain async operations
        CompletableFuture<String> result = CompletableFuture
            .supplyAsync(() -> {
                simulateDelay(50);
                return "user-123";
            })
            .thenApply(userId -> "Fetched user: " + userId) // transform
            .thenApply(String::toUpperCase)                  // another transform
            .exceptionally(ex -> "Error: " + ex.getMessage()); // handle errors

        System.out.println(result.get()); // blocks here

        // Combine two independent async tasks
        CompletableFuture<String> nameFuture = CompletableFuture.supplyAsync(() -> {
            simulateDelay(80);
            return "Alice";
        });
        CompletableFuture<Integer> ageFuture = CompletableFuture.supplyAsync(() -> {
            simulateDelay(60);
            return 30;
        });

        CompletableFuture<String> combined = nameFuture.thenCombine(
            ageFuture,
            (name, age) -> name + " is " + age + " years old"
        );
        System.out.println(combined.get());

        // allOf: wait for multiple
        CompletableFuture<Void> all = CompletableFuture.allOf(
            CompletableFuture.runAsync(() -> simulateDelay(30)),
            CompletableFuture.runAsync(() -> simulateDelay(20)),
            CompletableFuture.runAsync(() -> simulateDelay(10))
        );
        all.get(); // all three complete before this returns
        System.out.println("All tasks completed");
    }

    // === BlockingQueue: Producer-Consumer pattern ===
    static void producerConsumerDemo() throws InterruptedException {
        System.out.println("\n=== Producer-Consumer with BlockingQueue ===");

        BlockingQueue<String> queue = new LinkedBlockingQueue<>(5); // capacity 5

        // Producer thread
        Thread producer = new Thread(() -> {
            String[] tasks = {"task-1", "task-2", "task-3", "task-4", "DONE"};
            for (String task : tasks) {
                try {
                    queue.put(task);    // blocks if queue is full
                    System.out.println("Produced: " + task);
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        // Consumer thread
        Thread consumer = new Thread(() -> {
            while (true) {
                try {
                    String task = queue.take(); // blocks if queue is empty
                    if ("DONE".equals(task)) break;
                    System.out.println("Consumed: " + task);
                    Thread.sleep(40); // consumer is slower than producer
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        System.out.println("Producer-Consumer done");
    }

    static void simulateDelay(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
