# Multithreading and Concurrency

> Runnable example: [code/advanced/ThreadingDemo.java](../code/advanced/ThreadingDemo.java)

---

## The Big Picture

> **In plain terms** — A thread is an independent line of execution. Multiple threads let your program do several things at once — handle many web requests, keep a UI responsive while working, use all your CPU cores. The catch is that threads *share memory*, and when two of them touch the same data at the same time, the result depends on unpredictable timing. Concurrency is the discipline of coordinating that sharing so the answer is always correct.

> **Why this matters** — Concurrency bugs are the worst kind: they're intermittent (1-in-1000), timing-dependent, and often invisible until production load. You can't reliably test them away — you have to *design* them out. The whole topic boils down to two problems: **atomicity** (an operation like `count++` must happen all-at-once or another thread corrupts it) and **visibility** (a change one thread makes must be seen by others). The tools below — `synchronized`, atomics, `volatile`, concurrent collections, executors — are different trade-offs for solving those two problems. The golden rule: *share immutable data, or don't share mutable data*.

---

## Why Threads Are Hard

Multiple threads sharing memory can interleave in unpredictable ways. A bug might happen 1 in 1000 runs, only under load. Understanding the rules prevents these bugs at design time.

> **In plain terms** — Imagine two people editing the same shared shopping list with no coordination. One reads "eggs: 1," the other reads "eggs: 1," both cross it off and write "eggs: 2" — but you wanted 3. Neither did anything wrong individually; the *interleaving* lost an update. That's exactly what threads do to shared variables, except millions of times a second and only sometimes.

> **Going deeper** — The reason it's genuinely hard is that the *Java Memory Model* allows the compiler, JIT, and CPU to reorder and cache operations for speed — so without explicit synchronization, there's no guarantee one thread ever sees another's writes, or sees them in order. "It worked in my tests" means nothing: the bug depends on timing, core count, and load. This is why the advice is always to *minimize shared mutable state* first (immutability, message passing, thread-confinement) and only reach for locks when you must coordinate.

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

> **In plain terms** — Three ways to run code on a thread, in increasing order of how you should actually do it. Extending `Thread` ties your task to a thread (and wastes your one allowed superclass). Implementing `Runnable` separates the *task* from the *thread* — better. But in real code you almost never create threads by hand: you hand tasks to an `ExecutorService`, which manages a reusable pool for you.

> **Going deeper** — Creating raw threads is an anti-pattern in production: threads are expensive OS resources (~1MB stack each), and unbounded creation under load exhausts memory. A *pool* reuses a fixed set of threads, bounding resource use and decoupling "how many tasks" from "how many threads." `start()` (not `run()`!) is what actually launches a new thread — calling `run()` directly just executes it on the current thread, a classic beginner bug. Since Java 21, [virtual threads](../04-java8-modern/04-modern-java-9-to-21.md#java-21--virtual-threads-project-loom) change the economics for I/O-bound work — but the executor *abstraction* stays the same (`newVirtualThreadPerTaskExecutor`).

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

> **In plain terms** — `count++` *looks* like one action but is really three: read the current value, add one, write it back. Two threads can both read `5`, both compute `6`, and both write `6` — so two increments produce one. That's a *race condition*: the outcome depends on who wins the timing race. The lost-update is invisible until it isn't.

> **Going deeper** — A race condition needs three ingredients: shared state, at least one writer, and no coordination — remove any one and it's gone (hence "don't share mutable state"). Beyond lost updates, unsynchronized access can expose *partially constructed* objects or *stale* cached values due to the visibility issue above. The fixes below address the two facets separately: locks (`synchronized`, `ReentrantLock`) give *mutual exclusion* (atomicity), atomics give lock-free atomicity for single variables, and `volatile` gives visibility only. Match the tool to whether you need atomicity, visibility, or both.

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

> **In plain terms** — `synchronized` is the simplest fix: it puts a lock around a method or block so only *one* thread can be inside at a time — others wait their turn. This makes the read-add-write of `count++` indivisible. Keep the locked section as small as possible (lock the critical part, not the whole method) so threads aren't needlessly waiting.

> **Going deeper** — Every Java object has an intrinsic *monitor lock*; `synchronized` acquires it on entry and releases on exit (even if an exception is thrown — automatic, unlike `ReentrantLock`). Beyond mutual exclusion, it also establishes a *happens-before* relationship, so it fixes *visibility* too — changes made under a lock are guaranteed visible to the next thread that acquires it. Pitfalls: never lock on `this` or a public object (outside code could lock it too and deadlock you) — use a `private final Object lock`; and never synchronize on a `String` literal or boxed `Integer` (they're shared/cached). Holding a lock during slow I/O serializes your whole app — lock data, not operations.

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

> **In plain terms** — For a single counter or flag, a full lock is overkill. `AtomicInteger` does atomic operations (`incrementAndGet`) without locking, so threads don't block each other — faster under contention for simple values. There's a whole family: `AtomicLong`, `AtomicBoolean`, `AtomicReference<T>` for any object.

> **Going deeper** — Atomics are built on a CPU instruction called *compare-and-swap* (CAS): "if the value is still what I read, set it to the new value; otherwise retry." This is *lock-free* and *optimistic* — great when contention is low, but under heavy contention the retry loop can spin and waste CPU (then a lock or `LongAdder`, which shards the counter across cells, wins). `AtomicReference` with `compareAndSet` is how you build lock-free data structures. Atomics solve *single-variable* atomicity only — if you need two fields to change together consistently, you still need a lock.

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

> **In plain terms** — `ReentrantLock` does what `synchronized` does but with extra abilities: you can *try* to get the lock and give up if it's busy (`tryLock`), wait with a timeout, or make it fair (first-come-first-served). The price is that you must `unlock()` yourself — always in a `finally` block, or a thrown exception leaves the lock held forever (a guaranteed hang).

> **Going deeper** — Reach for `ReentrantLock` over `synchronized` only when you need its features: timed/interruptible acquisition (avoids permanent hangs), `tryLock` (avoids deadlock by backing off), fairness, or condition variables (`newCondition()` for fine-grained wait/notify). For the common case, `synchronized` is simpler and auto-releases — prefer it. "Reentrant" means the *same* thread can re-acquire a lock it already holds (both `synchronized` and `ReentrantLock` are reentrant), which is why a synchronized method can call another synchronized method on the same object without deadlocking itself. Also know `ReadWriteLock`/`StampedLock` for read-heavy data where many readers can proceed concurrently.

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

> **In plain terms** — `volatile` solves *only* the visibility half of the problem. Without it, a thread may keep reading a cached copy of `stopped` and loop forever even after another thread sets it `true`. Marking the field `volatile` forces every read to see the latest write. But it does *not* make `count++` safe — that needs atomicity, which `volatile` doesn't provide. Rule: `volatile` for simple flags, atomics/locks for anything you modify based on its current value.

> **Going deeper** — Mechanically, `volatile` reads/writes go straight to main memory (no caching) and act as memory barriers that prevent reordering across them, establishing happens-before. The decision tree: just publishing a value one thread writes and others read (a `stopped` flag, a lazily-built config reference)? → `volatile`. Read-modify-write of one variable? → `Atomic*`. Coordinating multiple variables or compound actions? → a lock. A subtle correct use is the *double-checked locking* singleton, which requires the instance field to be `volatile` to be safe — though an `enum` or holder-class singleton avoids the whole issue (see [design patterns](04-design-patterns.md)).

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

> **In plain terms** — Deadlock is a standoff: thread A holds lock 1 and wants lock 2; thread B holds lock 2 and wants lock 1. Neither will let go, so both wait forever and that part of your app freezes. The simplest cure is *lock ordering* — if every thread always grabs locks in the same agreed order, the circular wait can't form.

> **Going deeper** — Deadlock needs four conditions simultaneously (mutual exclusion, hold-and-wait, no preemption, circular wait); breaking any one prevents it, and lock ordering breaks the circular-wait condition. When you can't guarantee an order, use `tryLock` with a timeout and back off/retry on failure. Related hazards: *livelock* (threads keep reacting to each other and make no progress) and *starvation* (a thread never gets the lock — fairness settings help). Best defense overall: hold fewer locks, hold them briefly, and prefer higher-level concurrency tools (concurrent collections, executors, `CompletableFuture`) that manage coordination for you. The JDK's `jstack`/thread dumps will pinpoint a deadlock's exact lock cycle.

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

> **In plain terms** — An `ExecutorService` is a managed pool of worker threads you submit tasks to. Submit a task and you get a `Future` — a placeholder for a result that isn't ready yet; call `future.get()` to wait for it. Pick the pool to match the work: a *fixed* pool sized to your cores for CPU-heavy work, a *cached* pool for bursty short I/O tasks.

> **Going deeper** — Pool sizing is the key decision: CPU-bound work peaks at ~number-of-cores threads (more just adds context-switching); I/O-bound work wants many more (threads sit idle waiting). `newCachedThreadPool` is unbounded — convenient but dangerous under load (it can spawn thousands of threads); production systems often configure a `ThreadPoolExecutor` directly with a bounded queue and a rejection policy. Always `shutdown()` (executors are non-daemon and keep the JVM alive) and call `future.get()` — it rethrows task exceptions wrapped in `ExecutionException`, so a task that fails silently in a pool will surface there. For I/O-heavy work on Java 21+, a virtual-thread-per-task executor sidesteps the sizing dilemma entirely.

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

> **In plain terms** — A plain `Future` only lets you *block and wait*. `CompletableFuture` lets you say "when this finishes, *then* do that" — chaining steps (`thenApply`, `thenCompose`), combining results from parallel tasks (`thenCombine`), and handling errors (`exceptionally`) — all without a thread sitting around blocked. It's how you express async pipelines that read top-to-bottom like the [stream](../04-java8-modern/02-streams.md) chains they resemble.

> **Going deeper** — The method-name grammar: `thenApply` (transform the result, like `map`), `thenCompose` (chain another async call, like `flatMap` — avoids nested futures), `thenCombine` (merge two independent futures), `*Async` variants (run the callback on a pool instead of the completing thread). Default async ops run on the shared `ForkJoinPool.commonPool`, so pass your *own* `Executor` for blocking work to avoid starving it (same trap as parallel streams). Don't forget error handling — an unhandled exception in the chain stays buried in the future until you call `get`/`join`; use `exceptionally`/`handle`. On Java 21+, virtual threads make simple blocking code a viable alternative to deep `CompletableFuture` chains for many cases.

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

> **In plain terms** — Don't hand-roll locking around a normal `HashMap`/`ArrayList` to share it — use the purpose-built concurrent collections. `ConcurrentHashMap` is a thread-safe map that stays fast under many threads; `BlockingQueue` is the clean way to hand work from producer threads to consumer threads (it blocks automatically when full/empty); `CopyOnWriteArrayList` suits read-mostly lists.

> **Going deeper** — `ConcurrentHashMap` shines because it locks only small segments (not the whole map) and offers *atomic* compound ops — `computeIfAbsent`, `merge` — that replace racy check-then-act sequences (a plain `synchronizedMap` can't do those atomically). `BlockingQueue` is the backbone of the producer-consumer pattern and of thread pools themselves; a *bounded* queue also provides backpressure (producers slow down when consumers fall behind). `CopyOnWriteArrayList` copies the entire array on every write, so it's only for read-heavy, rarely-written data (event listeners, config). Note: making individual operations thread-safe doesn't make *sequences* of them atomic — "get then put" still needs `compute`/`merge` or external coordination.

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

> **In plain terms** — A grab-bag of real-world traps: some classes that *look* harmless (`SimpleDateFormat`) secretly aren't thread-safe; letting `this` escape a constructor exposes a half-built object to other threads; and swallowing `InterruptedException` quietly breaks the mechanism Java uses to ask a thread to stop. Each is a "works in dev, fails in prod" bug.

> **Going deeper** — `InterruptedException` deserves emphasis: interruption is Java's *cooperative* cancellation signal, and catching it clears the thread's interrupt flag — so swallowing it makes the thread un-cancellable (pools and shutdown logic rely on it). Either propagate it or restore the flag with `Thread.currentThread().interrupt()`. The thread-safe modern replacements matter too: prefer the immutable `java.time` types (`LocalDate`, `DateTimeFormatter`) over the mutable legacy `Date`/`SimpleDateFormat`/`Calendar`. The "`this` escaping" trap is the same initialization-order hazard as [calling overridable methods in a constructor](../02-oop/02-inheritance-and-polymorphism.md#common-inheritance-mistakes) — finish construction *before* publishing the object anywhere another thread can see it. Above all: immutable objects are automatically thread-safe — making that your default eliminates most of this list.
