# Memory Management and Garbage Collection

> Understanding GC helps you write code that doesn't fight it.

---

## The Big Picture

> **In plain terms** — In Java you create objects with `new`, but you never free them — the *garbage collector* (GC) does that automatically, reclaiming objects nothing points to anymore. This eliminates a whole class of C/C++ bugs (use-after-free, manual-free mistakes). The one rule you must understand: an object is "garbage" the moment it's *unreachable* — when no live reference chain can reach it. Memory leaks in Java aren't forgotten frees; they're references you accidentally *keep*.

> **Why this matters** — Most of the time you can ignore GC entirely. You start caring when (a) your app pauses unexpectedly, or (b) it runs out of memory (`OutOfMemoryError`). Both come from the same root: the GC's central bet is that **most objects die young**, so it's optimized for short-lived garbage. Code that respects this (tightly-scoped, short-lived objects) is fast; code that fights it (accidental long-lived references, huge allocations in loops) causes pauses and leaks. This doc is about working *with* the GC, not tuning it to death.

---

## Heap Structure

```
JVM Heap
├── Young Generation (most objects die here)
│   ├── Eden Space           ← new objects created here
│   ├── Survivor Space S0    ← survived 1 GC
│   └── Survivor Space S1    ← survived multiple GCs
│
└── Old Generation (tenured / long-lived objects)

Off-Heap:
└── Metaspace (class metadata — not in heap, Java 8+)
```

> **In plain terms** — The heap (where all objects live) is split into a *young* area for brand-new objects and an *old* area for ones that have stuck around. New objects start in "Eden"; the ones that survive a cleanup get moved aside, and the rare long-lived survivors graduate to the old generation. This split exists purely so the GC can clean the busy young area cheaply and often, and the stable old area rarely.

> **Going deeper** — This is the *generational hypothesis* made concrete: because most objects die almost immediately, collecting only the young gen ("minor GC") reclaims most garbage while scanning a tiny region. Promotion across survivor spaces uses a *copying* collector, which also compacts (no fragmentation). Note class metadata moved out of the heap into native *Metaspace* in Java 8 (replacing the old fixed-size PermGen, killing the infamous "PermGen OutOfMemoryError") — it grows dynamically but can still leak if you load classes endlessly (dynamic proxies, repeated redeploys).

---

## How Garbage Collection Works

```
1. New object created → goes to Eden (Young Gen)
2. Minor GC triggers when Eden fills:
   - Objects still referenced → move to Survivor space
   - Unreferenced objects → collected (fast, few ms)
3. Objects that survive many minor GCs → promoted to Old Gen
4. Old Gen fills → Major/Full GC triggers:
   - Scans entire heap
   - Longer pause (can be hundreds of ms)
   - "Stop-the-world" in most GC algorithms
```

The key insight: **most objects die young**. Design your code so short-lived objects are created in tightly-scoped methods — they'll be collected in the cheap minor GC.

> **In plain terms** — Cleanup comes in two sizes. A *minor GC* tidies just the young area — frequent but fast (a few milliseconds). A *full GC* scans the entire heap — rare but slow, and in most algorithms it "stops the world," freezing all application threads while it runs. Your goal is to keep work in the cheap minor GCs and avoid triggering full GCs.

> **Going deeper** — "Stop-the-world" pauses are why GC matters for latency: a full GC on a multi-GB heap can freeze your app for hundreds of ms, which is catastrophic for a trading system or a real-time API. The GC finds live objects by tracing references from *GC roots* (stack variables, statics, active threads) — anything not reachable from a root is collectable. Two things force premature promotion and full GCs: allocating objects too big for Eden, and holding references longer than needed so objects survive into old gen. Modern collectors (below) shrink or eliminate these pauses; older ones (ParallelGC, the ancient CMS) trade pause time against throughput.

---

## GC Algorithms

| GC Algorithm | When to Use | Characteristics |
|--------------|-------------|-----------------|
| G1GC (default, Java 9+) | Most applications | Balances throughput and latency |
| ZGC (Java 15+) | Low-latency apps | Pauses < 1ms, good for large heaps |
| Shenandoah | Low-latency, OpenJDK | Similar to ZGC |
| ParallelGC | Batch processing | Max throughput, higher pauses |

```bash
# Set GC algorithm:
-XX:+UseG1GC        # default for most
-XX:+UseZGC         # for latency-sensitive (Java 15+)
```

> **In plain terms** — There isn't one garbage collector — there are several, each tuned for a different priority. **G1** (the default) is the sensible all-rounder. **ZGC**/**Shenandoah** chase ultra-low pause times for latency-sensitive apps with huge heaps. **ParallelGC** maximizes raw throughput for batch jobs that don't mind occasional pauses. For most apps, the default is the right answer.

> **Going deeper** — The real choice is *throughput vs latency*: ParallelGC does the least total GC work (best throughput) but with the longest pauses; ZGC/Shenandoah do most of their work *concurrently* with your app, keeping pauses sub-millisecond even on terabyte heaps, at some throughput cost. G1 splits the heap into regions and targets a *pause-time goal* you can set (`-XX:MaxGCPauseMillis`). Critically: **measure before switching.** Most "GC problems" are actually allocation problems (your code creating too much garbage) that no collector swap will fix — profile first, tune the GC last, and never cargo-cult flags from a blog.

---

## Common Memory Leaks

### 1. Static Collections That Grow Forever

```java
// BAD: this map never loses entries
static Map<String, UserSession> activeSessions = new HashMap<>();

void addSession(String token, UserSession session) {
    activeSessions.put(token, session); // grows forever
}

// GOOD: eviction policy
static Map<String, UserSession> activeSessions =
    Collections.synchronizedMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, UserSession> eldest) {
            return size() > 10_000; // auto-evict when too large
        }
    });

// Or use a proper cache: Caffeine, Guava Cache
```

> **In plain terms** — The #1 Java memory leak: a `static` (or otherwise long-lived) collection you keep adding to but never remove from. Because it lives for the whole program and holds strong references to everything inside, the GC can never reclaim those objects — the map just grows until you run out of memory. Any unbounded cache or registry is a leak waiting to happen.

> **Going deeper** — The fix is *bounded* growth: an eviction policy (size or time based). The `LinkedHashMap` + `removeEldestEntry` trick is a quick LRU, but in real code use a purpose-built cache (Caffeine, Guava) with size/TTL limits, statistics, and concurrency built in. Statics are the usual culprit because their lifetime is the JVM's, but the same leak happens with any object that outlives the entries it holds. Watch for the subtler version too: keys whose `equals`/`hashCode` change after insertion, which strand entries you can never look up *or* remove.

### 2. Event Listeners Not Removed

```java
// BAD: listener keeps a reference to 'this', preventing GC
button.addActionListener(this);
// If button outlives 'this' in design, 'this' is never collected

// GOOD: remove the listener when done
button.removeActionListener(listener);

// GOOD: use WeakReference listeners (listener is GC'd when no other strong ref exists)
```

> **In plain terms** — When you register a callback/listener with something long-lived (a button, an event bus, a singleton), that thing now holds a reference to your object — keeping it alive even after you're "done" with it. The cure is symmetry: whatever you register, *un*register when finished (`removeListener`), or register a weak reference so the GC can still collect it.

> **Going deeper** — This is the "observer" leak, and it's everywhere: caches, pub/sub buses, scheduled tasks, callbacks. The rule of thumb is *lifecycle symmetry* — every `add`/`register`/`subscribe` needs a matching `remove`/`unregister`/`unsubscribe`, ideally in a `close()`/`dispose()` or `finally`. Weak listeners help but are tricky (a listener with no *other* strong reference vanishes unexpectedly). The same pattern bites with `ScheduledExecutorService` tasks that capture `this` and outlive it — cancel them on shutdown.

### 3. Non-static Inner Classes Holding Outer Reference

```java
class Outer {
    private byte[] bigData = new byte[1024 * 1024]; // 1MB

    class Inner implements Runnable { // non-static: holds implicit reference to Outer
        @Override
        public void run() { /* ... */ }
    }
}

// If Inner is submitted to a thread pool, Outer can't be GC'd even though
// you don't need bigData anymore.

// Fix: make Inner static if it doesn't need Outer's state
class Outer {
    static class Inner implements Runnable { // no reference to Outer
        @Override
        public void run() { /* ... */ }
    }
}
```

> **In plain terms** — A non-`static` inner class secretly holds a reference to its enclosing object — that's how it can use the outer class's fields. But it means if the inner object outlives its purpose (e.g. it's a long-running task), it drags the *entire* outer object (and its 1MB `bigData`) along, preventing GC. Fix: make the inner class `static` if it doesn't actually need the outer instance.

> **Going deeper** — The same hidden capture happens with non-static anonymous classes and (more subtly) lambdas that reference `this` or an instance field — they capture the enclosing instance too. The rule: make nested classes `static` by default, and only capture what you actually use. A lambda that needs one field of the outer object still captures the whole object via `this`; copy the field to a local first if you want to avoid retaining the outer. This connects back to [lambdas capturing the enclosing `this`](../04-java8-modern/01-lambdas-and-functional.md#closures--capturing-variables).

### 4. ThreadLocal Not Cleaned Up

```java
// ThreadLocal values survive as long as the thread lives
// In thread pools (threads live forever), ThreadLocal can leak
static ThreadLocal<Connection> localConn = new ThreadLocal<>();

// Always clean up:
try {
    localConn.set(getConnection());
    doWork();
} finally {
    localConn.remove(); // crucial — prevents leak in thread pool
}
```

> **In plain terms** — A `ThreadLocal` stores a value tied to the current thread. The trap: in a *thread pool*, threads are reused forever, so a value you set and forget stays attached to that thread between unrelated tasks — both a memory leak and a correctness bug (the next task sees stale data). Always `remove()` it in a `finally` block once you're done.

> **Going deeper** — `ThreadLocal` is genuinely useful for per-request context (a transaction, a user ID, a `DateTimeFormatter`) without passing it through every method — but it's effectively thread-scoped global state, so use it sparingly. The leak mechanism is subtle: the thread's internal map keys are weak references to the `ThreadLocal`, but the *values* are strong, so a forgotten value lingers until the thread dies (which, in a pool, is never). On Java 21+, `ScopedValue` is a safer, immutable alternative designed for the structured-concurrency/virtual-thread world.

---

## WeakReference and SoftReference

```java
// Strong reference: prevents GC
User user = new User("Alice"); // GC won't collect this

// WeakReference: GC can collect it anytime there are no strong references
WeakReference<User> weakUser = new WeakReference<>(new User("Alice"));
User u = weakUser.get(); // null if GC collected it
if (u != null) {
    // use u
}

// SoftReference: GC collects only when memory is low (good for caches)
SoftReference<byte[]> cache = new SoftReference<>(loadLargeData());
byte[] data = cache.get(); // null if GC collected it due to memory pressure
if (data == null) {
    data = loadLargeData(); // reload
    cache = new SoftReference<>(data);
}
```

> **In plain terms** — Normally a reference is "strong" — as long as you hold it, the object is safe from GC. A `WeakReference` says "keep this only if someone *else* needs it; otherwise feel free to collect it" (great for canonical-instance maps). A `SoftReference` says "keep this until memory gets tight, then collect it" (great for caches you'd *like* to keep but can rebuild). Both can return `null` from `.get()`, so always null-check.

> **Going deeper** — The reference strength ladder: strong > soft > weak > phantom. `WeakHashMap` uses weak *keys* so entries vanish when the key is otherwise unreferenced — handy for associating metadata with objects without preventing their collection. `SoftReference` for caching is a blunt instrument (the JVM decides when to clear, often all at once under pressure) — a real cache library with proper eviction usually beats it. `PhantomReference` (plus a `ReferenceQueue`) is for advanced post-mortem cleanup, replacing the unreliable `finalize()` (deprecated) — modern code uses `Cleaner` instead. Don't reach for these until a profiler proves you need them; strong references and a bounded cache cover most needs.

---

## JVM Memory Flags

```bash
# Heap size
-Xms512m           # initial heap size (start with this)
-Xmx2g             # maximum heap size (don't grow past this)
# Rule: set Xms = Xmx in production to avoid resizing pauses

# Metaspace (class metadata)
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=512m

# GC logging (Java 11+)
-Xlog:gc*:file=gc.log:time,uptime,level

# Heap dump on OOM (invaluable for debugging)
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/app/
```

> **In plain terms** — These flags control how much memory the JVM may use and what it does when things go wrong. `-Xmx` caps the heap (the most important one — too low causes `OutOfMemoryError`, too high starves the OS). `-Xms` sets the starting size. And `-XX:+HeapDumpOnOutOfMemoryError` is the one to *always* set in production: when it crashes, it leaves a snapshot you can analyze to find the leak.

> **Going deeper** — In containers/cloud, prefer `-XX:MaxRAMPercentage` over a hard `-Xmx` so the JVM sizes itself to the (cgroup-aware) container memory — a hard `-Xmx` bigger than the container limit gets the process OOM-killed by the kernel with no heap dump. Setting `-Xms` equal to `-Xmx` in production avoids heap-resize pauses and surfaces memory problems at startup, not under load. Remember the heap isn't the whole story: thread stacks, Metaspace, direct/native buffers, and the GC's own structures all add up, so total JVM footprint exceeds `-Xmx` — `Native Memory Tracking` (below) reveals the rest.

---

## Diagnosing Memory Issues

```bash
# JVM tools (included in JDK):
jps                              # list Java processes
jmap -heap <pid>                 # heap summary
jmap -dump:format=b,file=heap.hprof <pid> # heap dump
jstat -gcutil <pid> 1s 10        # GC stats every 1 second, 10 times
jcmd <pid> VM.native_memory      # native memory usage

# Analyze heap dumps:
# - JVisualVM (included in JDK < 9, downloadable separately)
# - Eclipse MAT (Memory Analyzer Tool) — most powerful
# - IntelliJ IDEA profiler

# Flight Recorder (production-safe profiling):
java -XX:StartFlightRecording=duration=60s,filename=recording.jfr MyApp
jfr print recording.jfr
```

> **In plain terms** — When memory misbehaves, don't guess — measure. The JDK ships free tools: `jps` to find your process, `jstat` to watch GC live, `jmap` to grab a heap dump, and **Eclipse MAT** to open that dump and see exactly what's eating memory (and what's holding the references). For production, **Java Flight Recorder** profiles with almost no overhead.

> **Going deeper** — The decision tree: rising memory + frequent full GCs → take a heap dump (`jmap` or auto-on-OOM) and open it in MAT; MAT's "Leak Suspects" report and "dominator tree" show which objects retain the most memory and the *reference chain* keeping them alive — that chain is your bug. For *allocation* problems (too much garbage rather than retained memory), JFR's allocation profiling shows which call sites churn objects. `jstat -gcutil` quickly tells you if you're spending too much time in GC (a healthy app spends <5%). JFR is safe to leave running in production; full heap dumps pause the JVM, so take them deliberately.

---

## Writing GC-Friendly Code

```java
// 1. Scope objects tightly — short-lived objects = cheap GC
public void processItems(List<Item> items) {
    for (Item item : items) {
        Result r = compute(item); // r is eligible for GC after each loop iteration
        save(r);
    }
}

// 2. Avoid large object creation in loops
// BAD: creates new array on every call in a hot loop
for (Item item : items) {
    byte[] buffer = new byte[8192]; // allocated and thrown away each iteration
    ...
}
// GOOD: create once, reuse
byte[] buffer = new byte[8192];
for (Item item : items) {
    Arrays.fill(buffer, (byte) 0); // reset
    ...
}

// 3. Prefer primitives over wrapper types in performance-critical paths
// int[], long[] are much more GC-friendly than Integer[], Long[]

// 4. Use StringBuilder not String concatenation in loops
// (Already covered in Strings section)

// 5. Null out large objects when done, especially in long-lived objects
public class DataProcessor {
    private byte[] cache;

    public void process() {
        cache = loadLargeData();
        doWork();
        cache = null; // allow GC to reclaim — don't hold it longer than needed
    }
}
```

> **In plain terms** — You don't manage memory directly, but you *influence* the GC. The themes: keep objects short-lived and tightly scoped (so they die in cheap minor GCs), don't allocate big things repeatedly in hot loops (reuse a buffer), prefer primitive arrays over wrapper arrays, and drop references to large objects when you're truly done with them.

> **Going deeper** — The honest caveat: **don't micro-optimize allocation prematurely.** The JVM is extremely good at cheap young-gen allocation and collection (bump-pointer allocation is nearly free), and the JIT's *escape analysis* can even eliminate allocations entirely for objects that never leave a method. So buffer-reuse and nulling-out matter only in proven hot paths — profile first. The reliable wins are architectural: avoid accidental long-lived references (the leaks above), choose the right [collection](../03-core-java/03-collections.md) to avoid boxing, and never call `System.gc()` (it forces a full GC and almost always hurts). Write clear code; reach for these tricks only when a profiler points at allocation.
