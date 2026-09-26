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
        Path testPom = safe(root, "user-flow-tests/pom.xml");
        String oldTestPom = Files.isRegularFile(testPom) ? Files.readString(testPom) : null;
        String newTestPom = oldTestPom == null ? null : withFtpTestDependencies(oldTestPom, scaffold.files());
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
            if (newTestPom != null && !newTestPom.equals(oldTestPom)) {
                if (!Files.readString(testPom).equals(oldTestPom)) throw new IOException("Flow-test pom.xml changed; retry generation.");
                Path backup = testPom.resolveSibling("pom.xml.bak" + java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")) + "-" + UUID.randomUUID());
                Files.copy(testPom, backup);
                Path temp = Files.createTempFile(testPom.getParent(), ".test-pom-", ".tmp");
                try {
                    Files.writeString(temp, newTestPom);
                    Files.move(temp, testPom, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } finally { Files.deleteIfExists(temp); }
            }
            addMissingFixtureInputDefaults(root, scaffold.files().get(FlowTestScaffold.TEST_PROPERTIES_PATH));
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
    /** Adds missing sample-input defaults only; explicit developer values (including false) win. */
    static void addMissingFixtureInputDefaults(Path root, String generated) throws IOException {
        if (generated == null) return;
        java.util.Properties defaults = new java.util.Properties();
        defaults.load(new java.io.StringReader(generated));
        Path file = safe(root.toAbsolutePath().normalize(), FlowTestScaffold.TEST_PROPERTIES_PATH);
        String original = Files.readString(file);
        java.util.Properties current = new java.util.Properties();
        current.load(new java.io.StringReader(original));
        StringBuilder added = new StringBuilder();
        for (String key : new java.util.TreeSet<>(defaults.stringPropertyNames())) {
            if (key.startsWith("studio.sample-consumer.") && key.endsWith(".fixture-input-enabled")
                    && "true".equals(defaults.getProperty(key)) && !current.containsKey(key)) {
                added.append(key).append("=true\n");
            }
        }
        if (added.isEmpty()) return;
        Path backup = file.resolveSibling(file.getFileName() + ".bak" + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")) + "-" + UUID.randomUUID());
        Path temporary = Files.createTempFile(file.getParent(), ".fixture-input-", ".tmp");
        try {
            Files.writeString(temporary, original + "\n\n# Deterministic sample input: remove if unused; set false to retain polling across regeneration.\n" + added);
            if (!Files.readString(file).equals(original)) throw new IOException("Test properties changed; retry generation.");
            Files.copy(file, backup);
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }

    /** Explicit dialog choice: preserve existing settings and archive the file before enabling the fixture. */
    static void enableLocalFtp(Path projectRoot) throws IOException {
        Path properties = safe(projectRoot.toAbsolutePath().normalize(), FlowTestScaffold.TEST_PROPERTIES_PATH);
        String original = Files.readString(properties);
        String updated = localFtpProperties(original);
        if (updated.equals(original)) return;
        Path backup = properties.resolveSibling(properties.getFileName() + ".bak"
                + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"))
                + "-" + UUID.randomUUID());
        Path temporary = Files.createTempFile(properties.getParent(), ".test-properties-", ".tmp");
        try {
            Files.writeString(temporary, updated);
            if (!Files.readString(properties).equals(original)) throw new IOException("Test properties changed; retry generation.");
            Files.copy(properties, backup);
            Files.move(temporary, properties, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }

    /** Last-key precedence preserves comments and unrelated settings, including escaped/multiline values. */
    static String localFtpProperties(String original) throws IOException {
        java.util.Properties properties = new java.util.Properties();
        properties.load(new java.io.StringReader(original));
        if ("true".equalsIgnoreCase(properties.getProperty("test.ftp.enabled"))) return original;
        StringBuilder result = new StringBuilder(original).append("\n\n# Enabled by Generate Flow Test: disposable loopback FTP, allocated port, temporary home.\n")
                .append("# Applies to all FTP endpoints in this test application; original settings above are retained.\n")
                .append("test.ftp.enabled=true\n");
        if (!properties.containsKey("test.ftp.username")) result.append("test.ftp.username=ikasan\n");
        if (!properties.containsKey("test.ftp.password")) {
            result.append(properties.containsKey("test.ftp.username")
                    ? "# test.ftp.password defaults to test.ftp.username when absent.\n"
                    : "test.ftp.password=ikasan\n");
        }
        return result.toString();
    }

    /** Adds only missing fixture dependencies; preserves developer XML and archives changes at the write boundary. */
    static String withFtpTestDependencies(String existing, Map<String, String> generated) throws IOException {
        if (!generated.containsKey("user-flow-tests/src/test/java/org/ikasan/studio/flowtests/LocalFtpTestServer.java")) return existing;
        try {
            var reader = new org.apache.maven.model.io.xpp3.MavenXpp3Reader();
            var model = reader.read(new java.io.StringReader(existing));
            StringBuilder additions = new StringBuilder();
            for (String[] dependency : List.of(new String[]{"org.apache.ftpserver", "ftpserver-core", "1.2.1"},
                    new String[]{"org.apache.mina", "mina-core", "2.2.9"})) {
                if (model.getDependencies().stream().noneMatch(d -> dependency[0].equals(d.getGroupId()) && dependency[1].equals(d.getArtifactId()))) {
                    additions.append("\n    <dependency><groupId>").append(dependency[0]).append("</groupId><artifactId>")
                            .append(dependency[1]).append("</artifactId><version>").append(dependency[2])
                            .append("</version><scope>test</scope></dependency>\n");
                }
            }
            if (additions.isEmpty()) return existing;
            return insertProjectDependencies(existing, additions.toString());
        } catch (org.codehaus.plexus.util.xml.pull.XmlPullParserException failure) {
            throw new IOException("Cannot update user-flow-tests/pom.xml", failure);
        }
    }

    /**
     * Finds direct project children in already validated XML. Only inserts text at that boundary;
     * profiles, plugin dependencies, dependency management, comments and formatting remain untouched.
     */
    private static String insertProjectDependencies(String xml, String additions) throws IOException {
        var tokens = java.util.regex.Pattern.compile(
                "<!--.*?-->|<!\\[CDATA\\[.*?]]>|<\\?.*?\\?>|<(?:\"[^\"]*\"|'[^']*'|[^'\">])*>",
                java.util.regex.Pattern.DOTALL).matcher(xml);
        int depth = 0;
        String projectPrefix = "";
        while (tokens.find()) {
            String token = tokens.group();
            if (token.startsWith("<!--") || token.startsWith("<![CDATA[") || token.startsWith("<?")) continue;
            if (token.startsWith("<!")) throw new IOException("DOCTYPE declarations are not supported when updating the flow-test POM.");
            boolean closing = token.startsWith("</");
            boolean empty = token.endsWith("/>");
            String name = token.substring(closing ? 2 : 1).split("[\\s/>]", 2)[0];
            String localName = name.substring(name.indexOf(':') + 1);
            String prefix = name.contains(":") ? name.substring(0, name.indexOf(':') + 1) : "";
            if (!closing && depth == 0) projectPrefix = prefix;
            if (localName.equals("dependencies") && ((closing && depth == 2) || (!closing && empty && depth == 1))) {
                String entries = additions.replaceAll("<(/?)([A-Za-z])", "<$1" + prefix + "$2");
                if (empty) {
                    String expanded = token.substring(0, token.length() - 2) + ">" + entries + "  </" + name + ">";
                    return xml.substring(0, tokens.start()) + expanded + xml.substring(tokens.end());
                }
                return xml.substring(0, tokens.start()) + entries + "  " + xml.substring(tokens.start());
            }
            if (closing && depth == 1 && localName.equals("project")) {
                String entries = additions.replaceAll("<(/?)([A-Za-z])", "<$1" + projectPrefix + "$2");
                return xml.substring(0, tokens.start()) + "  <" + projectPrefix + "dependencies>" + entries
                        + "  </" + projectPrefix + "dependencies>\n" + xml.substring(tokens.start());
            }
            if (closing) depth--;
            else if (!empty) depth++;
        }
        throw new IOException("Cannot locate the project element in the flow-test POM.");
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
