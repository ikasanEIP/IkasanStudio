package org.ikasan.studio.flowtests.support;

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

    /**
     * Waits for a regular final file with exactly the expected UTF-8 contents, including whitespace.
     * No timestamp comparison is used. To avoid accepting a stale matching file, use a fresh
     * directory or unique filename per scenario. Does not follow symbolic links or fetch remote files.
     * @param file local destination file, or a previously downloaded remote delivery
     * @param expected complete expected text
     * @param timeout positive maximum wait; successful comparison returns immediately
     * @throws AssertionError if the file is missing or differs at the deadline
     * @throws Exception on interruption, decoding failure or other filesystem errors
     */
    public static void assertFileContents(Path file, String expected, Duration timeout) throws Exception {
        long started = System.nanoTime();
        long limit = timeoutNanos(timeout);
        String actual = null;
        do {
            try {
                actual = Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                        ? Files.readString(file, StandardCharsets.UTF_8) : null;
                if (expected.equals(actual)) return;
            } catch (NoSuchFileException pendingRename) { /* Recheck an atomic delivery/rename. */ }
            if (System.nanoTime() - started >= limit)
                throw new AssertionError("Final file missing or UTF-8 contents differ: " + file + " (waited " + timeout + "): " + difference(expected, actual));
            Thread.sleep(25);
        } while (true);
    }

    /**
     * Waits for an exact multiset of UTF-8 contents in direct regular files matching the glob.
     * Order and filenames may vary; count and duplicate contents must match. No recursion or symlink following.
     * Use an isolated directory and a final-filename pattern. Unmatched files are deliberately ignored.
     * For two unique deliveries pass both expected contents after batch two; for overwrites use assertFileContents.
     * @param directory local output directory owned by this scenario
     * @param glob final-file glob such as {@code *.xml}; unmatched files are ignored
     * @param expected cumulative file contents, including duplicate entries when expected
     * @param timeout positive maximum wait
     * @throws AssertionError when file count or contents do not match at the deadline
     * @throws Exception on interruption or filesystem/decoding errors
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
                        + actual.size() + " (waited " + timeout + "): " + firstDifference(wanted, actual));
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

    /** Shows bounded context around the first differing UTF-16 character; never dumps a large fixture. */
    private static String difference(String expected, String actual) {
        if (actual == null) return "file is missing";
        int offset = 0;
        while (offset < expected.length() && offset < actual.length()
                && expected.charAt(offset) == actual.charAt(offset)) offset++;
        int start = Math.max(0, offset - 40);
        return "first difference at character " + offset + "; expected length " + expected.length()
                + ", actual length " + actual.length() + "; expected ["
                + expected.substring(start, Math.min(expected.length(), offset + 40))
                + "]; actual [" + actual.substring(start, Math.min(actual.length(), offset + 40)) + "]";
    }

    private static String firstDifference(List<String> expected, List<String> actual) {
        for (int i = 0; i < Math.min(expected.size(), actual.size()); i++) {
            if (!expected.get(i).equals(actual.get(i))) return difference(expected.get(i), actual.get(i));
        }
        return "file count differs";
    }

    private static long timeoutNanos(Duration timeout) {
        if (timeout.isNegative() || timeout.isZero()) throw new IllegalArgumentException("File assertion timeout must be positive");
        return timeout.toNanos();
    }
}
