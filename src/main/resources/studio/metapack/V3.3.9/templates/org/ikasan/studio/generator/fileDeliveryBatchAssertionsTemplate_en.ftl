package org.ikasan.studio.flowtests.support.utils;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scenario-owned delivery history for append-only output directories. Each call supplies only
 * this batch's expected UTF-8 contents; previous successful expectations are retained internally.
 * Exact total counts still prevent an earlier matching file from satisfying a later batch.
 * Files are never deleted. Use a fresh directory, final-file glob and distinct filenames;
 * for intentional overwrites use FileDeliveryAssertions.assertFileContents instead.
 * Instances are confined to the scenario's assertion thread, not shared between tests.
 */
public final class FileDeliveryBatchAssertions {
    private final Map<String, Batch> batches = new HashMap<>();

    /** Discards expectation history when starting a new scenario; does not change files. */
    public void reset() { batches.clear(); }

    /**
     * Checks the current batch and all previously verified deliveries for this directory/glob.
     * Batch numbers start at 1 and increase by one for each directory/glob pair. Multiple files
     * and duplicate contents are supported. Failed assertions do not advance the batch number.
     * @param directory isolated local output directory
     * @param glob final-filename glob; use the same pattern for successive batches
     * @param batch one-based batch number
     * @param expected only this batch's expected contents, including duplicates
     * @param timeout positive delivery timeout
     * @throws Exception on file-reading errors or interruption
     * @throws AssertionError if cumulative count or contents differ at the deadline
     * @throws IllegalArgumentException if batch numbers are skipped or repeated
     */
    public void assertBatch(Path directory, String glob, int batch, List<String> expected, Duration timeout) throws Exception {
        String key = directory.toAbsolutePath().normalize() + "\u0000" + glob;
        Batch previous = batches.get(key);
        int next = previous == null ? 1 : previous.number + 1;
        if (batch != next) throw new IllegalArgumentException("Expected delivery batch " + next + " for " + directory + " matching " + glob + ", got " + batch);
        List<String> cumulative = new ArrayList<>();
        if (previous != null) cumulative.addAll(previous.contents);
        cumulative.addAll(expected);
        FileDeliveryAssertions.assertDeliveredFileContents(directory, glob, cumulative, timeout);
        batches.put(key, new Batch(batch, List.copyOf(cumulative)));
    }

    private static final class Batch {
        final int number;
        final List<String> contents;
        Batch(int number, List<String> contents) { this.number = number; this.contents = contents; }
    }
}
