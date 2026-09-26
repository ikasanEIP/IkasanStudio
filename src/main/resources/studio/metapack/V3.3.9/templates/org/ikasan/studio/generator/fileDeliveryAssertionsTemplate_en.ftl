package org.ikasan.studio.flowtests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bounded receiver-side checks. Never writes/deletes files or treats remote paths as local paths. */
public final class FileDeliveryAssertions {
    private FileDeliveryAssertions() { }

    /** Waits for a regular final file containing exactly the expected UTF-8 text, including whitespace. */
    public static void assertFileContents(Path file, String expected, Duration timeout) throws Exception {
        long started = System.nanoTime();
        long limit = timeoutNanos(timeout);
        do {
            try {
                if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                        && expected.equals(Files.readString(file, StandardCharsets.UTF_8))) return;
            } catch (NoSuchFileException pendingRename) { /* Recheck an atomic delivery/rename. */ }
            if (System.nanoTime() - started >= limit)
                throw new AssertionError("Final file missing or UTF-8 contents differ: " + file + " (waited " + timeout + ")");
            Thread.sleep(25);
        } while (true);
    }

    /**
     * Waits for an exact multiset of UTF-8 contents in direct regular files matching the glob.
     * Order and filenames may vary; count and duplicate contents must match. No recursion or symlink following.
     * Use an isolated directory and a final-filename pattern. Unmatched files are deliberately ignored.
     * For two unique deliveries pass both expected contents after batch two; for overwrites use assertFileContents.
     */
    public static void assertDeliveredFileContents(Path directory, String glob, List<String> expected, Duration timeout) throws Exception {
        List<String> wanted = new ArrayList<>(expected);
        Collections.sort(wanted);
        long started = System.nanoTime();
        long limit = timeoutNanos(timeout);
        List<String> actual = List.of();
        do {
            if (Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    actual = readContents(directory, glob);
                    if (wanted.equals(actual)) return;
                } catch (NoSuchFileException pendingRename) { /* Retry if a file was renamed during the scan. */ }
            }
            if (System.nanoTime() - started >= limit)
                throw new AssertionError("Delivered files differ in " + directory + " matching " + glob
                        + ": expected " + wanted.size() + " files with specified UTF-8 contents; last completed scan found "
                        + actual.size() + " (waited " + timeout + ")");
            Thread.sleep(25);
        } while (true);
    }

    /** Reads only the selected regular files; IO errors other than an in-progress rename fail the test. */
    private static List<String> readContents(Path directory, String glob) throws IOException {
        List<String> contents = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, glob)) {
            for (Path file : files) {
                if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
                    contents.add(Files.readString(file, StandardCharsets.UTF_8));
            }
        }
        Collections.sort(contents);
        return contents;
    }

    private static long timeoutNanos(Duration timeout) {
        if (timeout.isNegative() || timeout.isZero()) throw new IllegalArgumentException("File assertion timeout must be positive");
        return timeout.toNanos();
    }
}
