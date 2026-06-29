import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

/**
 * Runnable tour of modern file I/O (NIO.2, java.nio.file).
 *
 * Everything happens inside a self-cleaning temp directory, so running this
 * never touches your real files.
 *
 * Compile and run:
 *   javac FileIODemo.java && java FileIODemo
 */
public class FileIODemo {

    public static void main(String[] args) throws IOException {
        Path sandbox = Files.createTempDirectory("fileio-demo-");
        System.out.println("Working inside temp sandbox: " + sandbox);
        try {
            pathsBasics(sandbox);
            writeAndRead(sandbox);
            streamLines(sandbox);
            metadataAndCopy(sandbox);
            walkTree(sandbox);
        } finally {
            deleteRecursively(sandbox); // clean up so we leave no trace
            System.out.println("\nCleaned up sandbox.");
        }
    }

    static void pathsBasics(Path dir) {
        System.out.println("\n=== Building paths ===");
        Path file = dir.resolve("notes").resolve("data.txt"); // OS-correct separators for free
        System.out.println("Full path : " + file);
        System.out.println("Parent    : " + file.getParent());
        System.out.println("File name : " + file.getFileName());
    }

    static void writeAndRead(Path dir) throws IOException {
        System.out.println("\n=== Write then read ===");
        Path file = dir.resolve("greeting.txt");
        Files.writeString(file, "Hello, World!\n", StandardCharsets.UTF_8);
        Files.writeString(file, "Appended line\n", StandardCharsets.UTF_8,
                StandardOpenOption.APPEND);

        String whole = Files.readString(file);
        System.out.print("readString:\n" + whole);

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        System.out.println("readAllLines -> " + lines.size() + " lines");
    }

    static void streamLines(Path dir) throws IOException {
        System.out.println("\n=== Stream lines (memory-efficient) ===");
        Path file = dir.resolve("log.txt");
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            for (int i = 1; i <= 5; i++) {
                w.write("event " + i);
                w.newLine();
            }
            w.write("   "); // a blank-ish line to filter out
            w.newLine();
        }
        // try-with-resources closes the stream so the file handle is released.
        try (Stream<String> lines = Files.lines(file)) {
            lines.filter(l -> !l.isBlank())
                 .map(String::trim)
                 .forEach(l -> System.out.println("  " + l));
        }
    }

    static void metadataAndCopy(Path dir) throws IOException {
        System.out.println("\n=== Metadata, copy, move ===");
        Path src = dir.resolve("greeting.txt");
        Path copy = dir.resolve("greeting-copy.txt");

        System.out.println("size of greeting.txt : " + Files.size(src) + " bytes");
        Files.copy(src, copy, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("copy exists          : " + Files.exists(copy));

        Path moved = dir.resolve("greeting-renamed.txt");
        Files.move(copy, moved, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("after move, copy gone: " + Files.notExists(copy));
        System.out.println("moved exists         : " + Files.exists(moved));
    }

    static void walkTree(Path dir) throws IOException {
        System.out.println("\n=== Walking the directory tree ===");
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile)
                .forEach(p -> System.out.println("  " + dir.relativize(p)));
        }
    }

    /** Deletes a directory and everything in it (children before parents). */
    static void deleteRecursively(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()) // deepest first
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        System.err.println("Failed to delete " + p + ": " + e.getMessage());
                    }
                });
        }
    }
}
