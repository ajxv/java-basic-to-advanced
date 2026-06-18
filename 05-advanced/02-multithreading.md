# Multithreading and Concurrency

> Runnable example: [code/advanced/ThreadingDemo.java](../code/advanced/ThreadingDemo.java)

---

## Why Threads Are Hard

Multiple threads sharing memory can interleave in unpredictable ways. A bug might happen 1 in 1000 runs, only under load. Understanding the rules prevents these bugs at design time.

---

## Creating and Starting Threads

```java
// Approach 1: extend Thread (limited — can't extend anything else)
class PrintThread extends Thread {
    private final String message;
    PrintThread(String message) { this.message = message; }
    @Override
    public void run() { System.out.println(message); }
}
new PrintThread("hello").start();

// Approach 2: Runnable (separates task from thread — preferred)
Runnable task = () -> System.out.println("hello from thread");
Thread t = new Thread(task);
t.start();
t.join(); // wait for t to finish before continuing

// Approach 3: ExecutorService (production code — manages a pool of threads)
ExecutorService pool = Executors.newFixedThreadPool(4);
pool.submit(() -> System.out.println("task 1"));
pool.submit(() -> System.out.println("task 2"));
pool.shutdown();                       // no more submissions
pool.awaitTermination(10, TimeUnit.SECONDS); // wait for all tasks to complete
```

---

## Thread Safety Problem: Race Condition

```java
class Counter {
    int count = 0; // shared state

    void increment() {
        count++; // NOT atomic — three steps: read, add, write
                 // two threads can read the same value and both write back 1, losing an increment
    }
}

Counter c = new Counter();
// Two threads both call increment() 1000 times
// Expected: 2000. Actual: some number < 2000 — data loss
```

---

## Fixing Race Conditions

### Option 1: `synchronized` — coarse lock, simplest

```java
class SafeCounter {
    private int count = 0;

    public synchronized void increment() { count++; } // only one thread at a time
    public synchronized int getCount() { return count; }
}

// Synchronized block (finer-grained than synchronized method):
class SafeCounter2 {
    private int count = 0;
    private final Object lock = new Object();

    public void increment() {
        synchronized (lock) {  // lock just this critical section
            count++;
        }
        // other code outside the lock can run concurrently
    }
}
```

### Option 2: `AtomicInteger` — lock-free, faster for simple values

```java
class AtomicCounter {
    private final AtomicInteger count = new AtomicInteger(0);

    public void increment() { count.incrementAndGet(); } // atomic CAS operation
    public int getCount() { return count.get(); }
}

// Other atomic types:
AtomicLong, AtomicBoolean, AtomicReference<T>
```

### Option 3: `ReentrantLock` — more control than `synchronized`

```java
class LockCounter {
    private int count = 0;
    private final ReentrantLock lock = new ReentrantLock();

    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock(); // must unlock in finally — never forget this
        }
    }

    public boolean tryIncrement() {
        if (lock.tryLock(100, TimeUnit.MILLISECONDS)) { // don't wait forever
            try { count++; return true; }
            finally { lock.unlock(); }
        }
        return false; // couldn't acquire lock in time
    }
}
```

---

## `volatile` — Visibility Without Atomicity

```java
class Worker {
    private volatile boolean stopped = false; // guaranteed visible to all threads immediately

    public void stop() { stopped = true; }

    public void run() {
        while (!stopped) { // without volatile, the thread might cache its own copy of stopped
            doWork();
        }
    }
}

// volatile guarantees: changes are immediately visible to all threads
// volatile does NOT guarantee: atomicity (count++ is still not safe with volatile)
// Use: for simple flags, not counters
```

---

## Deadlock

A deadlock happens when two threads each hold a lock the other needs.

```java
// Thread A holds lockA, waits for lockB
// Thread B holds lockB, waits for lockA
// Both wait forever

// Prevention: always acquire locks in the same global order
// If every thread acquires lockA before lockB, deadlock is impossible.
```

---

## ExecutorService Patterns

```java
// Fixed thread pool: good for CPU-bound tasks (size = number of CPUs)
int cpus = Runtime.getRuntime().availableProcessors();
ExecutorService cpuPool = Executors.newFixedThreadPool(cpus);

// Cached pool: creates threads as needed, reuses idle ones — good for short I/O tasks
ExecutorService ioPool = Executors.newCachedThreadPool();

// Single thread: all tasks run sequentially in one background thread
ExecutorService single = Executors.newSingleThreadExecutor();

// Submit tasks and get Future for the result:
Future<String> future = pool.submit(() -> {
    Thread.sleep(1000); // simulates slow work
    return "result";
});

// get() blocks until the task is done:
String result = future.get();          // blocks forever
String result2 = future.get(5, TimeUnit.SECONDS); // TimeoutException if too slow

// Cancel a task:
future.cancel(true); // true = interrupt the thread if running
```

---

## CompletableFuture — Async Without Blocking

```java
// Run async task, chain transformations, no blocking
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> fetchUserFromDB(userId))     // runs in ForkJoinPool
    .thenApply(user -> enrichUser(user))             // transform result
    .thenCompose(user -> fetchOrders(user.getId()))  // chain another async op
    .exceptionally(ex -> {                           // handle any error in the chain
        log.error("Failed", ex);
        return Collections.emptyList();
    });

// Combine two async tasks:
CompletableFuture<User> userFuture = fetchUser(id);
CompletableFuture<List<Order>> ordersFuture = fetchOrders(id);

CompletableFuture<UserWithOrders> combined = userFuture.thenCombine(
    ordersFuture,
    (user, orders) -> new UserWithOrders(user, orders)
);

// Wait for all:
CompletableFuture.allOf(f1, f2, f3).join();

// Wait for any:
CompletableFuture.anyOf(f1, f2, f3).thenAccept(result -> System.out.println(result));
```

---

## Thread-Safe Collections

```java
// ConcurrentHashMap: thread-safe map, much faster than Collections.synchronizedMap()
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("a", 1);
map.computeIfAbsent("b", k -> expensiveComputation()); // atomic
map.merge("counter", 1, Integer::sum);                  // atomic increment

// CopyOnWriteArrayList: thread-safe list for read-heavy workloads
// Writes create a full copy of the array — expensive but safe
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

// BlockingQueue: thread-safe queue with blocking operations
// Perfect for producer-consumer patterns
BlockingQueue<Task> queue = new LinkedBlockingQueue<>(100); // max 100 tasks

// Producer:
queue.put(task);   // blocks if queue is full

// Consumer:
Task task = queue.take(); // blocks if queue is empty
```

---

## Common Mistakes

```java
// 1. Sharing SimpleDateFormat across threads — it's not thread-safe
// BAD:
static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

// GOOD: Use DateTimeFormatter (Java 8+) — immutable and thread-safe
static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

// 2. Iterating a regular collection while another thread modifies it
// GOOD: Use ConcurrentHashMap or snapshot

// 3. "this" escaping constructor
public class Server {
    public Server() {
        EventBus.register(this); // `this` is not fully constructed yet!
    }
}
// Fix: use a static factory method, complete construction before publishing

// 4. Catching InterruptedException and swallowing it
// BAD:
try { Thread.sleep(1000); } catch (InterruptedException e) {} // lost interrupt!

// GOOD: restore the interrupt flag
try { Thread.sleep(1000); }
catch (InterruptedException e) {
    Thread.currentThread().interrupt(); // restore interrupt status
    throw new RuntimeException("Interrupted", e);
}
```
