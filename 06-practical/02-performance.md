# Performance Tips

> Measure first, optimize second. Guessing wastes time and often makes things worse.

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
