# Memory Management and Garbage Collection

> Understanding GC helps you write code that doesn't fight it.

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

### 2. Event Listeners Not Removed

```java
// BAD: listener keeps a reference to 'this', preventing GC
button.addActionListener(this);
// If button outlives 'this' in design, 'this' is never collected

// GOOD: remove the listener when done
button.removeActionListener(listener);

// GOOD: use WeakReference listeners (listener is GC'd when no other strong ref exists)
```

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
