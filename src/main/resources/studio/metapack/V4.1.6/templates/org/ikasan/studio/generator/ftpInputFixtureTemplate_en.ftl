package org.ikasan.studio.flowtests.support.utils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/** Publishes complete UTF-8 fixture files into an isolated test FTP home. */
public final class FtpInputFixture {
    private FtpInputFixture() { }

    /**
     * Copies a classpath fixture to a test FTP home without decoding or altering its bytes.
     * The resource basename becomes the remote filename. A staging file outside the scanned home
     * prevents consumers from seeing a partial copy. Existing destinations are never overwritten.
     *
     * @param directory existing test FTP home, normally {@code localFtpDirectory(context)}
     * @param regex consumer's runtime filename regular expression (a full-name match)
     * @param resourcePath path relative to {@code src/test/resources}; a leading slash is optional
     * @return the published local file, for additional business assertions
     * @throws IllegalArgumentException if the resource is missing or its basename does not match
     * @throws IllegalStateException if the destination already exists
     * @throws Exception if opening, copying or publishing the fixture fails
     */
    public static Path copyResource(Path directory, String regex, String resourcePath) throws Exception {
        return FileInputFixture.copyResource(directory, regex, resourcePath);
    }

    /**
     * Publishes inline UTF-8 text using an explicit filename or a matching batch-name default.
     * Like {@link #copyResource}, stages outside the scanned home and refuses overwrite.
     * @param directory existing test FTP home
     * @param regex full-name consumer filename pattern
     * @param filename plain filename, or null/blank to try common {@code batch-N} extensions
     * @param batch batch number used only when choosing a default filename
     * @param text exact fixture contents; whitespace is retained
     * @return the published file
     * @throws Exception if the filename is invalid, already exists or cannot be written
     */
    public static Path write(Path directory, String regex, String filename, int batch, String text) throws Exception {
        Pattern pattern = Pattern.compile(regex);
        if (filename == null || filename.isBlank()) {
            for (String extension : new String[]{".txt", ".csv", ".xml", ".json", ".dat", ""}) {
                String candidate = "batch-" + batch + extension;
                if (pattern.matcher(candidate).matches()) { filename = candidate; break; }
            }
        }
        if (filename == null || filename.isBlank() || filename.contains("/") || filename.contains("\\")
                || filename.equals(".") || filename.equals("..") || !pattern.matcher(filename).matches()) {
            throw new IllegalArgumentException("Set test.ftp.input.filename.batch" + batch
                    + " in module-test.properties to a plain filename matching the consumer pattern: " + regex);
        }
        Path destination = directory.resolve(filename);
        if (Files.exists(destination)) throw new IllegalStateException("Input fixture already exists: " + destination
                + "; use distinct filenames for each batch and an isolated test directory");
        // Stage outside the scanned directory so scheduled scans cannot read partially written input.
        Path staging = Files.createTempFile(directory.getParent(), "ftp-input-", ".tmp");
        try {
            Files.writeString(staging, text, StandardCharsets.UTF_8);
            Files.move(staging, destination);
        } finally { Files.deleteIfExists(staging); }
        return destination;
    }
}
