package org.ikasan.studio.flowtests.support.utils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Classpath file fixtures shared by local-file and FTP consumer tests. Never modifies source resources. */
public final class FileInputFixture {
    private FileInputFixture() { }

    /**
     * Returns the resource basename used when publishing input. Directory names are not copied.
     * @param resourcePath classpath resource path, with or without a leading slash
     * @return a plain filename suitable for a local test directory
     * @throws IllegalArgumentException for an empty, traversal or unsupported filename
     */
    public static String filename(String resourcePath) {
        String name = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
        if (name.isBlank() || name.equals(".") || name.equals("..") || name.contains("\\") || name.contains(","))
            throw new IllegalArgumentException("Invalid input resource filename: " + resourcePath);
        return name;
    }

    /**
     * Builds the local consumer's filename expression, matching only the specified fixture basenames.
     * The directory is literal; regex metacharacters in basenames are quoted. Duplicate basenames
     * are rejected so batches remain distinguishable. Use this only for a test-owned directory.
     * @param directory existing JUnit temporary input directory
     * @param resources resource paths for all batches
     * @return absolute directory followed by a filename regular expression
     */
    public static String localFilenames(Path directory, String... resources) {
        String[] names = Arrays.stream(resources).map(FileInputFixture::filename).toArray(String[]::new);
        if (names.length == 0 || Arrays.stream(names).distinct().count() != names.length)
            throw new IllegalArgumentException("Use distinct resource filenames for the input batches");
        return directory.toAbsolutePath().toString().replace('\\', '/') + "/(?:"
                + Arrays.stream(names).map(Pattern::quote).collect(Collectors.joining("|")) + ")";
    }

    /**
     * Streams a fixture unchanged to the directory using its basename. Stages outside the scanned
     * directory and publishes only a complete copy; never overwrites an existing destination.
     * @param directory existing test input directory
     * @param regex required full-name regular expression
     * @param resourcePath resource path relative to src/test/resources; leading slash optional
     * @return published input file
     * @throws Exception if the resource is absent, filename does not match, destination exists or IO fails
     */
    public static Path copyResource(Path directory, String regex, String resourcePath) throws Exception {
        String name = filename(resourcePath);
        if (!Pattern.compile(regex).matcher(name).matches())
            throw new IllegalArgumentException("Input resource " + resourcePath
                    + " must have a filename matching the consumer pattern: " + regex);
        Path destination = directory.resolve(name);
        if (Files.exists(destination)) throw new IllegalStateException("Input fixture already exists: " + destination);
        String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        try (InputStream input = FileInputFixture.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalArgumentException("Missing test resource " + path
                    + "; add it under user-flow-tests/src/test/resources");
            Path staging = Files.createTempFile(directory.toAbsolutePath().getParent(), "file-input-", ".tmp");
            try {
                Files.copy(input, staging, StandardCopyOption.REPLACE_EXISTING);
                Files.move(staging, destination);
            } finally { Files.deleteIfExists(staging); }
        }
        return destination;
    }

    /**
     * Publishes one batch for a local consumer that may leave its previous input on disk.
     * Removes only the named test-owned batch files, preventing re-reading old input on the next scan.
     * @param directory private JUnit input directory, never an application or shared input directory
     * @param batch one-based batch index into resources
     * @param resources distinct resource paths for all batches
     * @return the complete file for this batch
     * @throws Exception if the batch, resource or filesystem operation is invalid
     */
    public static Path prepareLocalBatch(Path directory, int batch, String... resources) throws Exception {
        localFilenames(directory, resources); // Validate all names before deleting anything.
        if (batch < 1 || batch > resources.length) throw new IllegalArgumentException("Invalid input batch " + batch);
        for (String resource : resources) Files.deleteIfExists(directory.resolve(filename(resource)));
        return copyResource(directory, ".*", resources[batch - 1]);
    }
}
