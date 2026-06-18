# File I/O and NIO

> Runnable example: [code/advanced/FileIODemo.java](../code/advanced/FileIODemo.java)

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
