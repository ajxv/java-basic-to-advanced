# File I/O and NIO

> Runnable example: [code/advanced/FileIODemo.java](../code/advanced/FileIODemo.java)

---

## The Big Picture

> **In plain terms** — File I/O is reading and writing data that lives outside your program — on disk. The modern toolkit is `java.nio.file`, built around two ideas: a **`Path`** (a location, like "/home/user/data.txt") and the **`Files`** helper class (static methods that *do* things to paths — read, write, copy, delete, list). For most tasks you'll combine a `Path` with one `Files` call.

> **Why this matters** — I/O is where programs meet the messy real world: files go missing, disks fill up, encodings differ, and operations are *slow* relative to memory. Two habits prevent most I/O bugs: always close what you open (use [try-with-resources](../03-core-java/01-exception-handling.md#try-with-resources-preferred) — file handles are a limited OS resource), and always be explicit about character encoding (`UTF_8`) so text doesn't corrupt across platforms. Get those right and `java.nio.file` makes the rest concise.

---

## NIO.2 — The Modern Way (Java 7+)

Use `java.nio.file` for all file operations. The old `java.io.File` class is largely superseded.

```java
import java.nio.file.*;

// Path represents a file or directory location
Path path = Path.of("/home/user/data.txt");      // Java 11+
Path path2 = Paths.get("/home/user/data.txt");   // Java 7+ equivalent

// Build paths safely (handles separators for you)
Path config = Path.of("config", "app", "settings.json");
Path absolute = Path.of("/var/log").resolve("app.log");
Path parent = path.getParent();    // "/home/user"
Path filename = path.getFileName(); // "data.txt"
```

> **In plain terms** — A `Path` is just a *location*; creating one doesn't touch the disk or require the file to exist. Build paths with `Path.of("a", "b", "c")` or `.resolve(...)` rather than gluing strings with `"/"` — Java inserts the correct separator for the OS, so your code works on Windows and Linux alike.

> **Going deeper** — `Path` replaces the legacy `java.io.File` (convert with `file.toPath()` if you hit old APIs). Useful operations: `resolve` (append a child), `relativize` (the path *from* one to another), `normalize` (collapse `..`/`.`), and `toAbsolutePath`. A security note: when building paths from user input, `normalize()` and validate against a base directory to prevent *path traversal* (`../../etc/passwd`) attacks. `Path` is also tied to a `FileSystem`, which is why the same API can transparently address files inside a ZIP archive via `FileSystems.newFileSystem`.

---

## Reading Files

```java
// Read entire file as String (only for small files — loads everything into memory)
String content = Files.readString(path);        // Java 11+

// Read as list of lines
List<String> lines = Files.readAllLines(path);  // Java 7+

// Stream lines — memory efficient for large files (closes the stream on exit)
try (Stream<String> lineStream = Files.lines(path)) {
    lineStream
        .filter(line -> !line.isBlank())
        .map(String::trim)
        .forEach(System.out::println);
}

// Read as bytes
byte[] bytes = Files.readAllBytes(path);

// Buffered reader — for large files with custom processing
try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
    String line;
    while ((line = reader.readLine()) != null) {
        process(line);
    }
}
```

> **In plain terms** — Reading has a "size spectrum." For small files, `readString`/`readAllLines` slurp the whole thing into memory in one line — easy. For large files, that would blow up your heap, so stream them: `Files.lines(path)` hands you one line at a time, processed lazily, so a 10GB log uses only a few KB of memory.

> **Going deeper** — The rule: load-all is fine up to a few MB; beyond that, stream. `Files.lines` returns a *lazy* [Stream](../04-java8-modern/02-streams.md) that holds an open file handle, so it **must** be in try-with-resources or you leak descriptors (unlike a collection stream). `BufferedReader` is the manual equivalent when you need custom control. For non-text/binary data use `readAllBytes` or an `InputStream`. And as the gotchas section stresses, pass an explicit `StandardCharsets.UTF_8` — relying on the platform default is a classic "works on my machine" bug.

---

## Writing Files

```java
// Write a String (overwrites by default)
Files.writeString(path, "Hello, World!\n");

// Append to existing file
Files.writeString(path, "More content\n", StandardOpenOption.APPEND);

// Write with specific charset and options
Files.writeString(path, content, StandardCharsets.UTF_8,
    StandardOpenOption.CREATE,
    StandardOpenOption.TRUNCATE_EXISTING);

// Write bytes
Files.write(path, content.getBytes(StandardCharsets.UTF_8));

// Write list of lines
Files.write(path, List.of("line 1", "line 2", "line 3"));

// Buffered writer — for large amounts of data
try (BufferedWriter writer = Files.newBufferedWriter(path)) {
    for (String line : largeDataSet) {
        writer.write(line);
        writer.newLine();
    }
} // auto-flushed and closed
```

> **In plain terms** — Writing mirrors reading: `writeString`/`write` for one-shot small content, a `BufferedWriter` for streaming lots of lines. By default writing *overwrites* the file; pass `StandardOpenOption.APPEND` to add to the end instead. The `StandardOpenOption` flags (`CREATE`, `TRUNCATE_EXISTING`, `APPEND`) are how you control exactly what happens.

> **Going deeper** — *Buffering* is the performance key: writing byte-by-byte to disk is brutally slow because each call may hit the OS, so a `BufferedWriter` batches writes into a memory buffer and flushes in chunks (try-with-resources flushes and closes for you). For durability-critical data (you must survive a crash), a buffer flush isn't enough — the OS may still cache it; force it to physical disk with `FileChannel.force(true)` or `StandardOpenOption.SYNC`. To avoid readers seeing a half-written file, write to a temp file and `Files.move(tmp, target, ATOMIC_MOVE)` — an atomic swap.

---

## File and Directory Operations

```java
// Check existence / type
Files.exists(path)
Files.notExists(path)
Files.isDirectory(path)
Files.isRegularFile(path)
Files.isReadable(path)
Files.isWritable(path)

// File size and metadata
long size = Files.size(path);                        // bytes
FileTime modified = Files.getLastModifiedTime(path);

// Copy and move
Files.copy(source, target);                          // fails if target exists
Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES); // preserve timestamps

Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); // best effort atomic

// Delete
Files.delete(path);           // throws NoSuchFileException if not found
Files.deleteIfExists(path);   // silent if not found

// Create directories
Files.createDirectory(path);            // fails if parent doesn't exist
Files.createDirectories(path);          // creates all missing parent directories
Files.createTempDirectory("prefix-");   // temp dir in system temp folder
Files.createTempFile("prefix-", ".txt");
```

> **In plain terms** — `Files` has a method for every common file-system chore: check existence/type, get size and timestamps, copy, move, delete, make directories. Note the safer variants — `deleteIfExists` won't throw if the file's already gone, and `createDirectories` makes the whole parent chain at once.

> **Going deeper** — These operations are subject to *races* (TOCTOU — time-of-check-to-time-of-use): checking `Files.exists()` then acting is not atomic, because another process can change things in between. Prefer atomic operations that act-and-report — e.g. `Files.createFile` throws if it already exists rather than checking first, and copy/move take `REPLACE_EXISTING`/`ATOMIC_MOVE` options. Cross-filesystem `move` may fall back to copy-then-delete (not atomic). Use `Files.createTempFile/Directory` for scratch space — and clean them up (the [FileIODemo](../code/advanced/FileIODemo.java) does this in a `finally`).

---

## Listing and Walking Directories

```java
// List immediate children of a directory
try (Stream<Path> entries = Files.list(dirPath)) {
    entries
        .filter(Files::isRegularFile)
        .filter(p -> p.toString().endsWith(".java"))
        .forEach(System.out::println);
}

// Walk entire directory tree (depth-first)
try (Stream<Path> walk = Files.walk(rootDir)) {
    walk
        .filter(p -> p.toString().endsWith(".log"))
        .forEach(logFile -> {
            try { Files.delete(logFile); }
            catch (IOException e) { System.err.println("Failed to delete: " + logFile); }
        });
}

// Walk with max depth
try (Stream<Path> walk = Files.walk(rootDir, 2)) { // max 2 levels deep
    ...
}

// Find files matching a pattern (glob)
try (Stream<Path> matches = Files.find(rootDir, Integer.MAX_VALUE,
        (path, attrs) -> attrs.isRegularFile() && path.toString().endsWith(".java"))) {
    matches.forEach(System.out::println);
}
```

> **In plain terms** — To process the contents of a directory you get a `Stream<Path>`: `Files.list` for the immediate children, `Files.walk` to recurse through the entire tree, `Files.find` to recurse with a filter. Then it's just normal [stream](../04-java8-modern/02-streams.md) operations — `filter` by extension, `forEach` to act on each.

> **Going deeper** — All three return lazy, resource-holding streams — wrap them in try-with-resources or you leak directory handles (the most common bug here). `Files.walk` is simple but throws if it hits an unreadable directory mid-traversal; for robust, large, or error-tolerant traversals use `Files.walkFileTree` with a `FileVisitor`, which gives you per-file error callbacks and control over symlink following and depth. Beware symlink cycles — `walk` doesn't follow links by default (opt in with `FOLLOW_LINKS`, which can then loop infinitely).

---

## Reading Config Files (Properties)

```java
// Java .properties file format: key=value
Properties props = new Properties();
try (InputStream in = Files.newInputStream(Path.of("app.properties"))) {
    props.load(in);
}

String dbUrl = props.getProperty("db.url");
int port = Integer.parseInt(props.getProperty("server.port", "8080")); // with default
```

> **In plain terms** — `.properties` files are the simplest config format: plain `key=value` lines. `Properties.load` reads them into a lookup object, and `getProperty(key, default)` gives you a value or a fallback. Great for small app settings without pulling in a library.

> **Going deeper** — `Properties` is technically a `Hashtable<Object,Object>` (a legacy quirk) — treat it as string→string. Load from the *classpath* (`getResourceAsStream`) rather than a hard-coded file path so config travels with your jar. Two limits push teams toward libraries: properties files are flat (no nesting) and ISO-8859-1 by default for the byte form. Modern apps often prefer YAML/JSON or a config framework (Spring `@ConfigurationProperties`, Typesafe Config), and pull secrets from environment variables rather than checked-in files.

---

## Watching a Directory for Changes

```java
WatchService watcher = FileSystems.getDefault().newWatchService();

Path dir = Path.of("/var/log/app");
dir.register(watcher,
    StandardWatchEventKinds.ENTRY_CREATE,
    StandardWatchEventKinds.ENTRY_MODIFY,
    StandardWatchEventKinds.ENTRY_DELETE);

// Poll for events
while (true) {
    WatchKey key = watcher.take(); // blocks until event
    for (WatchEvent<?> event : key.pollEvents()) {
        System.out.println(event.kind() + ": " + event.context());
    }
    key.reset(); // must reset to receive further events
}
```

> **In plain terms** — A `WatchService` lets your program react when files in a folder are created, changed, or deleted — instead of repeatedly polling "did anything change?" You register a directory, then block on `take()` until the OS notifies you of an event. Handy for config hot-reload or "process files as they arrive" workflows.

> **Going deeper** — It's powered by native OS file-notification APIs (inotify on Linux, etc.), so it's efficient — but quirky: it watches a *single* directory (not recursively — register each subdir yourself), can *coalesce* rapid events into one, and may briefly fire `ENTRY_MODIFY` mid-write before a file is fully written (debounce or check size stability before reading). You must call `key.reset()` after processing or you stop receiving events. For heavy-duty needs, libraries like Apache Commons IO or directory-watcher wrappers smooth over these edges.

---

## Common Gotchas

```java
// 1. Always specify charset explicitly — default charset varies by platform
Files.readAllLines(path);                         // uses UTF-8 since Java 11
Files.readAllLines(path, StandardCharsets.UTF_8); // explicit is safer for Java 8/10

// 2. Close streams from Files.lines() — they hold file handles
Stream<String> lines = Files.lines(path); // if not closed, file handle leaks
// Always use try-with-resources

// 3. Path.of() vs new File() — prefer Path.of()
// File.toPath() converts old File to Path if needed

// 4. Relative paths are relative to the JVM's working directory (where you launched from)
Path relative = Path.of("data/file.txt"); // could be anywhere
Path fromClasspath = Path.of(getClass().getClassLoader()
    .getResource("config.properties").toURI()); // safe for resources in classpath
```

> **In plain terms** — The recurring traps: always state the charset (`UTF_8`), always close streams from `Files.lines`/`walk`/`list` (try-with-resources), prefer `Path.of` over the old `new File`, and never assume *where* a relative path points — it's relative to wherever the JVM was launched, which is rarely what you expect.

> **Going deeper** — The relative-path gotcha is the sneakiest: `Path.of("data/file.txt")` resolves against the JVM's *working directory*, which differs between your IDE, a `java -jar` run, and a container — so files that "exist" in dev vanish in prod. Two robust fixes: load read-only data as a *classpath resource* (`getResourceAsStream`), which is packaged inside your jar and location-independent; and for writable data, use an explicit absolute base directory from config/env rather than a relative path. When something "can't find the file," print `System.getProperty("user.dir")` first — it's almost always the working-directory assumption.
