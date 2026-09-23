package org.ikasan.studio.intellij.testing;

import org.ikasan.studio.core.generator.FlowTestScaffold;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Background-only writer. Existing tests are preserved unless explicitly archived for regeneration. */
public final class FlowTestFiles {
    private FlowTestFiles() { }
    public static final class ExistingTestException extends IOException {
        private final Path path;
        private final byte[] contents;
        ExistingTestException(Path path) throws IOException {
            super("Test already exists: " + path);
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Expected a test file: " + path);
            this.path = path;
            this.contents = Files.readAllBytes(path);
        }
        public Path path() { return path; }
    }
    public static final class ExistingTestsException extends IOException {
        private final List<ExistingTestException> tests;
        ExistingTestsException(List<ExistingTestException> tests) { super("Selected tests already exist"); this.tests = List.copyOf(tests); }
        public List<ExistingTestException> tests() { return tests; }
    }
    public static void checkExisting(Path root, List<FlowTestScaffold.Scaffold> scaffolds) throws IOException {
        List<ExistingTestException> existing = new ArrayList<>();
        for (var scaffold : scaffolds) {
            Path test = safe(root.toAbsolutePath().normalize(), scaffold.testPath());
            if (Files.exists(test)) existing.add(new ExistingTestException(test));
        }
        if (!existing.isEmpty()) throw new ExistingTestsException(existing);
    }
    public static List<Path> archiveAndWriteAll(Path root, String originalPom, List<FlowTestScaffold.Scaffold> scaffolds,
                                               List<ExistingTestException> approved) throws IOException {
        root = root.toAbsolutePath().normalize();
        Set<Path> selected = new HashSet<>();
        for (var scaffold : scaffolds) {
            if (!selected.add(safe(root, scaffold.testPath()))) throw new IOException("Selected flows produce the same test class.");
        }
        Set<Path> approvals = new HashSet<>();
        for (var test : approved) {
            if (!selected.contains(test.path) || !approvals.add(test.path)
                    || !Arrays.equals(Files.readAllBytes(test.path), test.contents))
                throw new IOException("The existing tests changed. Review them before regenerating.");
        }
        if (!Files.readString(safe(root, "pom.xml")).equals(originalPom)) throw new IOException("pom.xml changed. Generate the tests again.");
        Map<Path, Path> backups = new LinkedHashMap<>();
        try {
            for (var test : approved) {
                String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").format(java.time.LocalDateTime.now());
                Path backup = test.path.resolveSibling(test.path.getFileName() + ".bak" + timestamp + "-" + UUID.randomUUID());
                Files.move(test.path, backup);
                backups.put(test.path, backup);
                if (!Arrays.equals(Files.readAllBytes(backup), test.contents)) throw new IOException("An existing test changed during archiving.");
            }
            return writeAll(root, originalPom, scaffolds);
        } catch (IOException | RuntimeException failure) {
            for (var entry : backups.entrySet()) {
                try { Files.copy(entry.getValue(), entry.getKey()); }
                catch (IOException restore) { failure.addSuppressed(restore); }
            }
            throw new IOException("Could not regenerate the tests. Archived originals are preserved beside the tests: " + backups.values(), failure);
        }
    }

    /** The approved bytes must still match; backup names never replace an earlier backup. */
    public static Path archiveAndWrite(Path root, String originalPom, FlowTestScaffold.Scaffold scaffold,
                                       ExistingTestException approved) throws IOException {
        root = root.toAbsolutePath().normalize();
        Path test = safe(root, scaffold.testPath());
        if (!test.equals(approved.path) || !Arrays.equals(Files.readAllBytes(test), approved.contents))
            throw new IOException("The existing test changed. Review it before regenerating.");
        if (!Files.readString(safe(root, "pom.xml")).equals(originalPom)) throw new IOException("pom.xml changed. Generate the test again.");
        String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
                .format(java.time.LocalDateTime.now());
        Path backup = test.resolveSibling(test.getFileName() + ".bak" + timestamp + "-" + UUID.randomUUID());
        // Rename preserves the exact file; regeneration creates a new file at the original path.
        Files.move(test, backup);
        try {
            if (!Arrays.equals(Files.readAllBytes(backup), approved.contents))
                throw new IOException("The existing test changed. Review it before regenerating.");
            return write(root, originalPom, scaffold);
        } catch (IOException | RuntimeException failure) {
            try { Files.copy(backup, test); }
            catch (IOException restore) { failure.addSuppressed(restore); }
            throw new IOException("Could not regenerate the test. Original content is preserved at " + backup, failure);
        }
    }

    /** Preflight the whole selection, preserve existing tests, then create all new files in one transaction. */
    public static List<Path> writeAll(Path root, String originalPom, List<FlowTestScaffold.Scaffold> scaffolds) throws IOException {
        root = root.toAbsolutePath().normalize();
        Set<Path> seen = new HashSet<>();
        List<Path> tests = new ArrayList<>();
        Map<String, String> files = new LinkedHashMap<>();
        String updatedPom = null;
        for (var scaffold : scaffolds) {
            Path test = safe(root, scaffold.testPath());
            if (!seen.add(test)) throw new IOException("Selected flows produce the same test class: " + test);
            if (Files.exists(test)) {
                if (!Files.isRegularFile(test)) throw new IOException("Expected a test file: " + test);
                continue;
            }
            if (updatedPom != null && !updatedPom.equals(scaffold.rootPom())) throw new IOException("Conflicting test module configuration.");
            updatedPom = scaffold.rootPom();
            tests.add(test);
            for (var entry : scaffold.files().entrySet()) {
                String previous = files.putIfAbsent(entry.getKey(), entry.getValue());
                if (previous != null && !previous.equals(entry.getValue())) throw new IOException("Conflicting flow test file: " + entry.getKey());
            }
        }
        if (tests.isEmpty()) return List.of();
        String first = root.relativize(tests.get(0)).toString().replace('\\', '/');
        write(root, originalPom, new FlowTestScaffold.Scaffold(updatedPom, first, files), Set.copyOf(tests));
        return List.copyOf(tests);
    }
    public static Path write(Path root, String originalPom, FlowTestScaffold.Scaffold scaffold) throws IOException {
        return write(root, originalPom, scaffold, Set.of(safe(root.toAbsolutePath().normalize(), scaffold.testPath())));
    }
    private static Path write(Path root, String originalPom, FlowTestScaffold.Scaffold scaffold, Set<Path> requiredTests) throws IOException {
        root = root.toAbsolutePath().normalize();
        Path pom = safe(root, "pom.xml");
        Path test = safe(root, scaffold.testPath());
        for (Path required : requiredTests) {
            if (Files.exists(required)) throw new ExistingTestException(required);
            String relative = root.relativize(required).toString().replace('\\', '/');
            if (!scaffold.files().containsKey(relative)) throw new IOException("Missing test content: " + relative);
        }
        if (!Files.readString(pom).equals(originalPom)) throw new IOException("pom.xml changed. Generate the test again.");
        Map<Path, String> pending = new LinkedHashMap<>();
        for (var entry : scaffold.files().entrySet()) {
            Path path = safe(root, entry.getKey());
            if (Files.exists(path) && !Files.isRegularFile(path)) throw new IOException("Expected a file: " + path);
            if (requiredTests.contains(path) || !Files.exists(path)) pending.put(path, entry.getValue());
        }
        List<Path> created = new ArrayList<>();
        try {
            for (var entry : pending.entrySet()) {
                Files.createDirectories(entry.getKey().getParent());
                Files.writeString(entry.getKey(), entry.getValue(), StandardOpenOption.CREATE_NEW);
                created.add(entry.getKey());
            }
            if (!Files.readString(pom).equals(originalPom)) throw new IOException("pom.xml changed. Generate the test again.");
            if (!originalPom.equals(scaffold.rootPom())) {
                Path temp = Files.createTempFile(root, ".flow-test-pom-", ".tmp");
                try {
                    Files.writeString(temp, scaffold.rootPom());
                    Files.move(temp, pom, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } finally { Files.deleteIfExists(temp); }
            }
            return test;
        } catch (IOException failure) {
            for (Path path : created) {
                try {
                    if (Files.readString(path).equals(pending.get(path))) Files.delete(path);
                } catch (IOException cleanup) { failure.addSuppressed(cleanup); }
            }
            throw failure;
        }
    }
    private static Path safe(Path root, String relative) throws IOException {
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root) || Path.of(relative).isAbsolute()
                || !(relative.equals("pom.xml") || relative.startsWith("user-flow-tests/"))) {
            throw new IOException("Invalid flow test path: " + relative);
        }
        for (Path part = path; part != null && part.startsWith(root); part = part.getParent()) {
            if (Files.isSymbolicLink(part)) throw new IOException("Flow test generation does not follow symbolic links: " + part);
        }
        return path;
    }
}
