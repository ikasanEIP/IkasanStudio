package org.ikasan.studio.flowtests.support;

import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/** Publishes complete UTF-8 fixture files into an isolated test FTP home. */
public final class FtpInputFixture {
    private FtpInputFixture() { }

    /** Streams a classpath resource unchanged, publishing only the complete file under its basename. */
    public static Path copyResource(Path directory, String regex, String resourcePath) throws Exception {
        String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        String filename = path.substring(path.lastIndexOf('/') + 1);
        if (filename.isBlank() || filename.equals(".") || filename.equals("..") || filename.contains("\\")
                || !Pattern.compile(regex).matcher(filename).matches()) {
            throw new IllegalArgumentException("Input resource " + resourcePath
                    + " must have a filename matching the consumer pattern: " + regex);
        }
        Path destination = directory.resolve(filename);
        if (Files.exists(destination)) throw new IllegalStateException("Input fixture already exists: " + destination);
        try (InputStream input = FtpInputFixture.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalArgumentException("Missing test resource " + path
                    + "; add it under user-flow-tests/src/test/resources");
            // Stage outside the scanned home so consumers cannot read a partial copy.
            Path staging = Files.createTempFile(directory.getParent(), "ftp-input-", ".tmp");
            try {
                Files.copy(input, staging, StandardCopyOption.REPLACE_EXISTING);
                Files.move(staging, destination);
            } finally { Files.deleteIfExists(staging); }
        }
        return destination;
    }

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
