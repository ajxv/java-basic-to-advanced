# Performance Tips

> Measure first, optimize second. Guessing wastes time and often makes things worse.

---

## The Big Picture

> **In plain terms** — Performance work is the art of making code faster *without* guessing. The cardinal rule: **measure first.** Programmers are notoriously bad at predicting what's slow — the bottleneck is almost never where you think. So you profile to find the actual hot spot, fix that one thing, and measure again. Optimizing code that wasn't slow is wasted effort that often makes the code uglier *and* no faster.

> **Why this matters** — Most of the tips here are about *not creating needless work*: don't build strings the O(n²) way, don't box millions of integers, don't scan a list when a `HashSet` would do, don't re-create expensive objects in loops. These are cheap to get right from the start and rarely need "optimization" later. The expensive, risky tuning (GC flags, micro-benchmarks, caching) comes *last* and only with profiler data justifying it. The throughline: write clear code, measure, and optimize the proven hot path — in that order.

---

## Rule 1: Profile Before Optimizing

```bash
# Java Flight Recorder (built into JDK, production-safe):
java -XX:StartFlightRecording=duration=60s,filename=recording.jfr MyApp
jfr print --events CPULoad,GarbageCollection recording.jfr

# async-profiler (best for CPU/allocation profiling, low overhead):
./asprof -d 30 -f profile.html <pid>

# JVisualVM: GUI tool, good for quick heap analysis
```

The profiler will tell you exactly where time is spent. Don't optimize what isn't slow.

> **In plain terms** — A profiler watches your running program and reports where it actually spends its time and memory. Instead of guessing "this loop looks slow," you get hard data: "62% of CPU is in this one method." Then you know exactly what to fix — and you can confirm the fix worked by profiling again.

> **Going deeper** — This is *Amdahl's Law* in practice: speeding up code that takes 2% of runtime can at best give a 2% improvement, no matter how clever the fix — so always attack the biggest contributor first. Profile under *realistic* conditions (production-like data and load); a profile of toy input lies. Prefer low-overhead, *sampling* profilers (JFR, async-profiler) for production — they statistically sample the stack rather than instrumenting every call, so they don't distort the very thing they measure. Watch for both CPU *and* allocation profiles: excessive object churn (GC pressure) is a common hidden cost that a pure CPU view misses (see [memory & GC](../05-advanced/03-memory-and-gc.md)).

---

## String Performance

```java
// Concatenation in a loop — O(n²), creates n strings
String s = "";
for (int i = 0; i < 10_000; i++) s += i; // SLOW

// StringBuilder — O(n), single buffer
StringBuilder sb = new StringBuilder(50_000); // pre-size avoids resizing
for (int i = 0; i < 10_000; i++) sb.append(i);
String s = sb.toString();

// String.format is slow (reflection-based) — avoid in hot paths
// Use + or StringBuilder in tight loops

// String.intern() deduplicates, but fills the pool — profile before using
```

> **In plain terms** — The big string win is using `StringBuilder` instead of `+=` when building text in a loop (covered in depth in [strings](../03-core-java/02-strings.md#stringbuilder--for-string-building)). Pre-sizing the builder avoids internal regrowth copies, and avoiding reflection-based `String.format` in tight loops keeps things fast.

> **Going deeper** — The compiler already optimizes simple `a + b + c` into a single builder, so don't manually "optimize" one-line concatenation — the only real hotspot is the *loop* case. `String.format` is convenient but parses the format string and uses reflection each call, so it's fine for logging/messages but wasteful in a million-iteration loop. Pre-sizing matters because a builder doubling its array repeatedly does O(log n) copies of growing size — knowing the rough final length skips them.

---

## Collection Sizing

```java
// ArrayList resizes by 50% when full — causes array copy
// If you know the size, pre-allocate:
List<String> list = new ArrayList<>(expectedSize);
Map<String, Object> map = new HashMap<>(expectedSize * 2); // load factor 0.75, so *2 avoids resize

// HashMap default capacity is 16. Each resize doubles and rehashes everything.
// For maps with >12 entries on creation, specify capacity
```

> **In plain terms** — Collections that grow (`ArrayList`, `HashMap`) start small and copy everything into a bigger array each time they fill up. If you already know roughly how many elements you'll add, tell the constructor up front — `new ArrayList<>(expectedSize)` — and skip all that repeated copying.

> **Going deeper** — For `HashMap`, account for the 0.75 *load factor*: it resizes when ~75% full, so to hold N entries without a resize, size it to `N / 0.75` (the `* 2` rule of thumb is a safe over-estimate; Java 19+ added `HashMap.newHashMap(n)` to do this correctly for you). Each resize rehashes *every* entry, so it's costly for large maps. This is a cheap, safe optimization — but only meaningful for large or hot collections; don't clutter code pre-sizing a 3-element list.

---

## Primitive vs Boxed Types

```java
// Boxed types (Integer, Long, Double) are objects — heap allocation + GC pressure
List<Integer> boxed = new ArrayList<>();
for (int i = 0; i < 1_000_000; i++) boxed.add(i); // 1M Integer objects!

// Prefer primitive arrays and streams for numeric processing:
int[] array = new int[1_000_000]; // no boxing
IntStream.range(0, 1_000_000).sum(); // IntStream avoids boxing

// Auto-unboxing in expressions can cause NPE:
Integer count = map.get("missing"); // null
int x = count; // NullPointerException — auto-unbox of null
// Fix:
int x = map.getOrDefault("missing", 0); // safe default
```

> **In plain terms** — Every `Integer`/`Long`/`Double` is a full heap object wrapping a single number. A `List<Integer>` of a million values means a million little objects for the GC to manage — slow and memory-hungry. For heavy numeric work, use primitive arrays (`int[]`) and primitive streams (`IntStream`) that skip the wrapping entirely.

> **Going deeper** — Boxing costs both *memory* (an `Integer` is ~16 bytes vs 4 for an `int`, plus pointer indirection that wrecks cache locality) and *GC pressure* (all those short-lived objects). Generics force boxing (`List<int>` is illegal), which is exactly why the primitive [stream](../04-java8-modern/02-streams.md#primitive-streams) and [function](../04-java8-modern/01-lambdas-and-functional.md) specializations exist. The auto-unbox NPE is a correctness trap, not just performance: `int x = nullInteger` throws with no visible null — see [variables & types](../01-basics/02-variables-and-types.md#-vs-equals--the-most-common-java-bug). For primitive-keyed maps/large numeric collections, specialized libraries (Eclipse Collections, fastutil) avoid boxing entirely.

---

## Choosing the Right Data Structure

```java
// contains() on List = O(n). On HashSet = O(1).
List<String> allowed = List.of("admin", "user", "guest");
allowed.contains(role); // scans entire list — O(n)

Set<String> allowedSet = Set.of("admin", "user", "guest");
allowedSet.contains(role); // hash lookup — O(1) ✓

// If you frequently check membership, use Set/Map
// If you need ordering + deduplication, use TreeSet / LinkedHashSet

// Prefer ArrayDeque over LinkedList for queue/stack — ArrayDeque is faster (no node allocation)
Deque<String> stack = new ArrayDeque<>(); // not LinkedList
```

> **In plain terms** — The single biggest algorithmic win is picking the right container. Checking membership (`contains`) in a `List` scans every element (O(n)); in a `HashSet` it's a near-instant hash lookup (O(1)). If you repeatedly ask "is X in this group?", that's a `Set`/`Map`, not a `List`. The right data structure often beats any amount of micro-tuning.

> **Going deeper** — This is where Big-O actually bites: an O(n) `contains` inside a loop over m items is O(n·m) — fine for tens of elements, catastrophic for millions. The fix is free at design time and brutal to retrofit. Revisit the [collections decision table](../03-core-java/03-collections.md#choosing-the-right-collection): `HashSet`/`HashMap` for membership/lookup, `TreeMap` for sorted range queries, `ArrayDeque` for stacks/queues (it avoids `LinkedList`'s per-node object allocation and pointer-chasing, which also hurts CPU cache). Algorithmic improvements (better data structure or algorithm) routinely deliver 100× gains where constant-factor tuning gives 2×.

---

## Object Creation Costs

```java
// 1. Reuse expensive objects — don't create in hot paths
// BAD: compiled inside a loop
for (String s : list) {
    Pattern pattern = Pattern.compile("\\d+"); // compiled each iteration
    Matcher m = pattern.matcher(s);
}

// GOOD: compile once
private static final Pattern DIGITS = Pattern.compile("\\d+");
for (String s : list) {
    Matcher m = DIGITS.matcher(s); // reuse pattern
}

// 2. Object pooling for expensive resources
// DB connections: always use a connection pool (HikariCP)
// HTTP clients: create once, reuse
// Thread pools: use ExecutorService, don't create new Thread()
```

> **In plain terms** — Some objects are expensive to build (a compiled regex `Pattern`, a DB connection, an HTTP client). Creating them fresh inside a loop or per-request wastes huge effort. Build them *once* — as a `static final` constant or a shared instance — and reuse them. The classic offender is `Pattern.compile` inside a loop; hoist it out.

> **Going deeper** — Compiling a regex, opening a DB connection, or constructing an HTTP client involves real setup work that dwarfs the actual use — reuse turns repeated cost into one-time cost. For *expensive, limited* resources (DB connections), go further and *pool* them (HikariCP) so threads share a fixed set rather than each opening its own. Caveat tying back to [memory & GC](../05-advanced/03-memory-and-gc.md): don't over-cache — the JVM allocates cheap short-lived objects almost for free and the JIT can elide them via escape analysis, so hoisting only pays for *genuinely* expensive constructions. And shared mutable instances must be thread-safe (`Pattern` is; many builders aren't) — see [multithreading](../05-advanced/02-multithreading.md).

---

## I/O Performance

```java
// Always buffer I/O — unbuffered reads call the OS on every byte
// BAD:
InputStream in = new FileInputStream(file); // unbuffered

// GOOD:
InputStream in = new BufferedInputStream(new FileInputStream(file)); // batches reads
// Or: Files.newBufferedReader() / Files.newBufferedWriter() — already buffered

// Batch DB operations instead of one-by-one
// BAD: 1000 individual inserts
for (User user : users) repo.save(user);

// GOOD: batch insert (one network round trip)
repo.saveAll(users); // single statement or JDBC batch
```

> **In plain terms** — Two I/O rules. First, always *buffer*: reading or writing one byte at a time hits the operating system constantly, which is brutally slow — a buffered stream batches it. Second, *batch* round trips: 1,000 separate database inserts means 1,000 network conversations; one batched insert is a single trip. Both turn many small expensive operations into a few big cheap ones.

> **Going deeper** — I/O is orders of magnitude slower than CPU/memory, so it dominates the performance of most real applications — meaning I/O efficiency usually matters far more than any in-memory micro-optimization. The recurring theme is *amortizing fixed costs*: each OS syscall or network round trip has a high fixed overhead, so doing fewer, larger operations wins. The [NIO `Files` helpers](../05-advanced/01-file-io.md) (`newBufferedReader`, `readString`) are already buffered. The classic database killer is the *N+1 query problem* (one query to get a list, then one per item) — batch or join instead. For I/O-bound concurrency, [virtual threads](../04-java8-modern/04-modern-java-9-to-21.md#java-21--virtual-threads-project-loom) let you keep simple blocking code while scaling.

---

## Caching Results

```java
// Cache expensive computations that are called frequently with the same input
// Simple in-memory cache:
private final Map<String, Result> cache = new ConcurrentHashMap<>();

public Result compute(String key) {
    return cache.computeIfAbsent(key, k -> expensiveComputation(k)); // atomic
}

// For production: use Caffeine (in-process cache with eviction)
// Add dependency: com.github.ben-manes.caffeine:caffeine
LoadingCache<String, Result> cache = Caffeine.newBuilder()
    .maximumSize(10_000)
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build(key -> expensiveComputation(key));

Result r = cache.get("myKey"); // computes on miss, returns cached on hit
```

> **In plain terms** — If a computation is expensive and you keep calling it with the same inputs, remember the answer instead of recomputing it. A cache (even a simple `ConcurrentHashMap` with `computeIfAbsent`) stores results by key — first call computes, later calls return the saved result instantly. For real use, a library like Caffeine adds size limits and expiry.

> **Going deeper** — Caching is a powerful but *double-edged* tool — "there are only two hard things in computer science: cache invalidation and naming things." An unbounded cache is a [memory leak](../05-advanced/03-memory-and-gc.md#1-static-collections-that-grow-forever) (the entries never leave); always bound it by size and/or TTL, which is exactly what Caffeine/Guava provide. `computeIfAbsent` on `ConcurrentHashMap` is atomic, so two threads asking for the same missing key won't both compute it (avoiding a "cache stampede" for that key). Only cache *pure*, expensive, repeatedly-requested computations — caching cheap or rarely-repeated work just adds complexity and stale-data risk. And remember stale data: cached results must be invalidated when the underlying data changes.

---

## Lazy Initialization

```java
// Only create expensive objects when first needed
public class ReportService {
    private volatile PdfRenderer renderer; // null until needed

    public PdfRenderer getRenderer() {
        if (renderer == null) {
            synchronized (this) {
                if (renderer == null) { // double-checked locking — thread-safe
                    renderer = new PdfRenderer(); // expensive creation
                }
            }
        }
        return renderer;
    }
}
```

> **In plain terms** — Lazy initialization means "don't build it until someone actually needs it." If an object is expensive to create and might never be used, defer its creation to the first access. This speeds up startup and saves resources — at the cost of a little extra code to handle "is it built yet?".

> **Going deeper** — In multithreaded code, lazy init is surprisingly tricky — the *double-checked locking* shown here is correct **only** because `renderer` is `volatile` (without it, a thread could see a half-constructed object — the visibility issue from [multithreading](../05-advanced/02-multithreading.md#volatile--visibility-without-atomicity)). Honestly, prefer simpler alternatives: the [holder-class idiom](../05-advanced/04-design-patterns.md#singleton--one-instance-only) gives lazy, thread-safe init with no locking and no `volatile`, and `Suppliers.memoize` (Guava) wraps it cleanly. Reserve lazy init for genuinely expensive, possibly-unused objects — eager init is simpler and fine for cheap ones; over-lazying just adds null-checks and bugs.

---

## JVM Tuning Cheat Sheet

```bash
# Memory
-Xms2g -Xmx2g        # Set equal to avoid resizing (no heap growth pauses)
-XX:+UseG1GC          # Good default GC for most apps

# Low latency (Java 15+):
-XX:+UseZGC           # <1ms pauses, great for > 4GB heaps

# Crash on OOM (to get heap dump for analysis):
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/logs/

# Container-aware JVM (Docker/Kubernetes — detects container limits):
-XX:+UseContainerSupport    # default in Java 8u191+
-XX:MaxRAMPercentage=75.0   # use 75% of container memory for heap
```

> **In plain terms** — These flags tune the JVM itself: how much memory it may use, which garbage collector, and how it behaves in a container. The safe, high-value ones are setting heap size, enabling a heap dump on out-of-memory (so you can diagnose crashes), and — in Docker/Kubernetes — letting the JVM see the container's memory limit.

> **Going deeper** — Most apps need only a handful of these; resist copy-pasting long flag lists from blogs, which often hurt more than help on a modern JVM. The container flags are essential in cloud: a JVM unaware of its cgroup limit will size the heap to the *host's* RAM and get OOM-killed — `MaxRAMPercentage` fixes that (prefer it over a hard `-Xmx` that can exceed the limit). Always set `HeapDumpOnOutOfMemoryError` in production — it's free insurance for the day you leak. Beyond that, GC choice and sizing should follow *measurement* (see [memory & GC](../05-advanced/03-memory-and-gc.md)), not folklore.

---

## Micro-benchmarking with JMH

For measuring small code differences properly:

```java
// Add dependency: org.openjdk.jmh:jmh-core
import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class StringBenchmark {

    private String[] words = {"hello", "world", "foo", "bar"};

    @Benchmark
    public String concatenation() {
        String result = "";
        for (String w : words) result += w;
        return result;
    }

    @Benchmark
    public String stringBuilder() {
        StringBuilder sb = new StringBuilder();
        for (String w : words) sb.append(w);
        return sb.toString();
    }
}
// Run: java -jar benchmarks.jar
```

Don't write micro-benchmarks without JMH — the JVM optimizes things in ways that make naive benchmarks completely wrong.

> **In plain terms** — When you want to measure which of two small code snippets is faster, you can't just wrap them in `System.nanoTime()` — the JVM warms up, optimizes hot code, and may even delete code whose result you ignore, giving wildly misleading numbers. JMH is the proper tool: it handles warmup, runs many iterations, and prevents the JVM from cheating.

> **Going deeper** — Recall [JIT warmup](../01-basics/01-how-java-works.md#jit-compilation): the first runs are interpreted and slow, then the JIT compiles hot paths — so a naive benchmark mostly measures the un-optimized version. The JVM also does *dead-code elimination* (drops computations whose results aren't used) and *constant folding*, which is why JMH provides `Blackhole` and `@State` to consume results and defeat these optimizations. Rule: never trust a hand-rolled `nanoTime` micro-benchmark — but also remember micro-benchmarks measure code *in isolation*, which can mislead about real application behavior. Profile the whole app (top of this doc) to find *what* to optimize; use JMH only to compare specific alternatives once you know where the hot spot is.
