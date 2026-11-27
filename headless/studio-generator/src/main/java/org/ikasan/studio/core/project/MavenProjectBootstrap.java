package org.ikasan.studio.core.project;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.stream.Stream;
import javax.lang.model.SourceVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.maven.IkasanPomModel;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.MetaPackManifest;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.persistence.json.StudioJson;

/** Reviewed, offline setup of a simple Maven Java project. Never fetches or invokes an archetype. */
public final class MavenProjectBootstrap {
    private static final Set<String> RESERVED = Set.of("jar", "generated", "user", "user-flow-tests", "bin", "distribution");
    private static final Set<String> STANDARD_PLUGINS = Set.of("maven-compiler-plugin", "maven-surefire-plugin", "maven-resources-plugin");
    private MavenProjectBootstrap() { }
    public record Eligibility(boolean eligible, String detail, String artifactId, String packageName) { }
    public record Options(String pack, String packageName, int port) { }
    public record Change(String path, String before, String after) {
        public String action() { return before == null ? "CREATE" : after == null ? "MOVE SOURCE" : "UPDATE"; }
    }
    public record Plan(Options options, String packRevision, String modelJson, List<Change> changes, List<String> sources) {
        public Plan { changes = List.copyOf(changes); sources = List.copyOf(sources); }
    }

    public static boolean isStudioProject(Path root) {
        return Files.exists(root.resolve(".ikasan-studio-project"))
                || Files.exists(root.resolve("generated/src/main/model/model.json"))
                || Files.exists(root.resolve("jar/generated/src/main/model/model.json"))
                || (Files.exists(root.resolve("generated/pom.xml")) && Files.exists(root.resolve("user/pom.xml")));
    }

    public static Eligibility inspect(Path root) {
        try {
            if (isStudioProject(root)) return new Eligibility(false, "This is already a Studio project.", "", "");
            for (String path : RESERVED) if (Files.exists(root.resolve(path), LinkOption.NOFOLLOW_LINKS))
                return new Eligibility(false, "Setup will not replace the existing " + path + "/ directory. Use a fresh Maven project.", "", "");
            for (String name : List.of("application.properties", "application.yml", "application.yaml", "studio-harness.properties", "studio-flow-test-wiring.json"))
                if (Files.exists(root.resolve("src/main/resources").resolve(name)))
                    return new Eligibility(false, "Existing runtime application configuration needs a separate import review. Start with a fresh Maven project.", "", "");
            Model model = parse(read(root, "pom.xml"));
            if (!"jar".equals(model.getPackaging()) || !model.getModules().isEmpty() || !model.getProfiles().isEmpty())
                return new Eligibility(false, "Initial setup supports a single-module Maven JAR project without custom profiles.", "", "");
            if (model.getBuild() != null) {
                if (model.getBuild().getSourceDirectory() != null || model.getBuild().getTestSourceDirectory() != null
                        || model.getBuild().getDirectory() != null || !model.getBuild().getResources().isEmpty()
                        || !model.getBuild().getTestResources().isEmpty() || !model.getBuild().getExtensions().isEmpty())
                    return new Eligibility(false, "Custom source, resource, output or build-extension layouts need manual conversion.", "", "");
                for (Plugin plugin : model.getBuild().getPlugins()) {
                    if (!"org.apache.maven.plugins".equals(plugin.getGroupId()) || !STANDARD_PLUGINS.contains(plugin.getArtifactId()))
                        return new Eligibility(false, "Custom active build plugins need manual conversion: " + plugin.getArtifactId(), "", "");
                    if (plugin.getExecutions().stream().anyMatch(execution -> execution.getPhase() != null))
                        return new Eligibility(false, "Custom plugin lifecycle executions need manual conversion.", "", "");
                }
            }
            String group = coordinate(IkasanPomModel.groupId(model), model);
            coordinate(IkasanPomModel.version(model), model);
            String artifact = coordinate(model.getArtifactId(), model);
            if (!artifact.matches("[A-Za-z0-9_][A-Za-z0-9_.-]*")) throw new IOException("Use a normal Maven artifactId before setup.");
            String suggested = SourceVersion.isName(group) ? group : "com.example";
            return new Eligibility(true, "Sources and tests will be preserved under jar/user/.", artifact, suggested);
        } catch (Exception failure) {
            return new Eligibility(false, "Open a simple Maven project with a valid pom.xml. " + Objects.toString(failure.getMessage(), ""), "", "");
        }
    }

    public static Plan prepare(Path root, Options options) throws Exception {
        Eligibility eligibility = inspect(root);
        if (!eligibility.eligible()) throw new IOException(eligibility.detail());
        if (!SourceVersion.isName(options.packageName())) throw new IOException("Enter a valid Java package name.");
        if (options.port() < 1 || options.port() > 65533) throw new IOException("Choose an HTTP port from 1 to 65533, leaving room for both H2 ports.");
        MetaPackManifest pack = ComponentLibrary.getMetaPackManifest(options.pack());
        Module module = new Module(options.pack());
        module.setName(eligibility.artifactId());
        module.setApplicationPackageName(options.packageName());
        module.setPort(Integer.toString(options.port()));
        module.setH2DbPortNumber(Integer.toString(options.port() + 1));
        module.setH2WebPortNumber(Integer.toString(options.port() + 2));
        module.defaultUnsetMandatoryProperties();
        module.setPropertyValue("flowStartupType", "AUTOMATIC");
        Module normalised = ComponentIO.validatePersistedModuleJson(ModelTemplate.create(module), "initial project setup", false);
        return prepare(root, options, normalised, pack);
    }

    private static Plan prepare(Path root, Options options, Module module, MetaPackManifest pack) throws Exception {
        String originalPom = read(root, "pom.xml");
        Model outer = parse(originalPom);
        String group = coordinate(IkasanPomModel.groupId(outer), outer);
        String version = coordinate(IkasanPomModel.version(outer), outer);
        String artifact = outer.getArtifactId();
        List<Dependency> originalDependencies = new ArrayList<>(outer.getDependencies());
        outer.setDependencies(new ArrayList<>());
        outer.setPackaging("pom");
        outer.setModules(new ArrayList<>(List.of("jar", "distribution")));
        new IkasanPomModel(outer).applyBuildContract(pack);
        alignCompilerConfiguration(outer, pack.javaVersion());
        Model application = child(group, artifact, version, artifact + "-jar", "pom");
        application.setModules(new ArrayList<>(List.of("user", "generated")));
        IkasanPomModel appPom = new IkasanPomModel(application);
        appPom.applyBuildContract(pack);
        for (Dependency dependency : module.getAllUniqueSortedJarDependencies()) {
            Dependency managed = dependency.clone();
            if ("org.ikasan".equals(managed.getGroupId())) managed.setVersion(null);
            appPom.checkIfDependancyAlreadyExists(managed);
        }
        Model user = child(group, artifact + "-jar", version, artifact + "-user", "jar");
        user.setDependencies(originalDependencies);
        Model generated = child(group, artifact + "-jar", version, artifact + "-generated", "jar");
        Dependency implementations = new Dependency();
        implementations.setGroupId(group); implementations.setArtifactId(artifact + "-user"); implementations.setVersion(version);
        generated.addDependency(implementations);
        Model distribution = child(group, artifact, version, artifact + "-distribution", "pom");
        Map<String,String> output = new LinkedHashMap<>();
        output.put("pom.xml", xml(outer));
        output.put("jar/pom.xml", xml(application));
        output.put("jar/user/pom.xml", xml(user));
        output.put("jar/generated/pom.xml", xml(generated));
        output.put("distribution/pom.xml", xml(distribution));
        output.put("jar/" + StudioProjectLayout.APPLICATION_MARKER, "formatVersion=1\n");
        Map<String,String> rendered = ProjectArtifactRenderer.render(module, xml(application));
        rendered.forEach((path, text) -> output.put("jar/" + path, text));
        output.put("jar/generated/IKASAN_STUDIO.md", AiProjectContractGenerator.studioGuide(options.pack(), true));
        if (!Files.exists(root.resolve("AGENTS.md"))) output.put("AGENTS.md", AiProjectContractGenerator.agentsGuide(true));
        if (!Files.exists(root.resolve(LocalTestEnvironmentTemplate.FILE_NAME))) output.put(LocalTestEnvironmentTemplate.FILE_NAME, LocalTestEnvironmentTemplate.content());
        for (Map.Entry<String,String> entry : StudioAiSkillTemplates.files(true).entrySet())
            if (!Files.exists(root.resolve(entry.getKey()))) output.put(entry.getKey(), entry.getValue());
        // Reuse deployment generation in an isolated preview; the user's project stays untouched.
        Path staging = Files.createTempDirectory("studio-bootstrap-preview-");
        try {
            for (Map.Entry<String,String> entry : output.entrySet()) {
                Path target = staging.resolve(entry.getKey()); Files.createDirectories(target.getParent()); Files.writeString(target, entry.getValue());
            }
            for (DistributionScaffolding.FileChange change : DistributionScaffolding.prepare(staging, pack)) output.put(change.path(), change.content());
        } finally { removeTree(staging); }
        Map<String,String> existingSources = sourceInventory(root);
        List<Change> changes = new ArrayList<>();
        for (Map.Entry<String,String> entry : output.entrySet()) {
            String before = bytes(root, entry.getKey());
            if (before != null && !entry.getKey().equals("pom.xml")) throw new IOException("Setup will not replace existing file: " + entry.getKey());
            changes.add(new Change(entry.getKey(), before, encode(entry.getValue().getBytes(StandardCharsets.UTF_8))));
        }
        for (Map.Entry<String,String> entry : existingSources.entrySet()) {
            String relative = entry.getKey();
            if (relative.startsWith("src/main/java/org/ikasan/studio/boot/")) throw new IOException("Existing sources use Studio's reserved boot package.");
            changes.add(new Change("jar/user/" + relative, null, entry.getValue()));
            changes.add(new Change(relative, entry.getValue(), null));
        }
        String ignore = bytes(root, ".gitignore");
        String text = ignore == null ? "" : new String(Base64.getDecoder().decode(ignore), StandardCharsets.UTF_8);
        String addition = "\n# Ikasan Studio local data and setup recovery\n/jar/temporary-files/\n/jar/config/local/\n/.ikasan-studio/setup/\n";
        changes.add(new Change(".gitignore", ignore, encode((text + addition).getBytes(StandardCharsets.UTF_8))));
        // Publish the Maven reactor only after its sources and build files are complete.
        Change parentPom = changes.stream().filter(change -> change.path().equals("pom.xml")).findFirst().orElseThrow();
        Change savedModel = changes.stream().filter(change -> change.path().equals("jar/generated/src/main/model/model.json")).findFirst().orElseThrow();
        changes.remove(parentPom); changes.remove(savedModel);
        changes.add(savedModel); changes.add(parentPom);
        // Marker is last: project detection cannot observe a half-initialised project.
        changes.add(new Change(".ikasan-studio-project", null, encode("formatVersion=1\ncreatedBy=ikasan-studio-plugin\napplicationRoot=jar\n".getBytes(StandardCharsets.UTF_8))));
        String model = output.get("jar/generated/src/main/model/model.json");
        ComponentIO.validatePersistedModuleJson(model, "project setup preview", false);
        return new Plan(options, pack.packVersion(), model, changes, new ArrayList<>(existingSources.keySet()));
    }

    public static Path apply(Path root, Plan reviewed, Runnable checkCancelled) throws Exception {
        root = root.toAbsolutePath().normalize();
        Eligibility eligibility = inspect(root);
        if (!eligibility.eligible()) throw new IOException(eligibility.detail());
        MetaPackManifest pack = ComponentLibrary.getMetaPackManifest(reviewed.options().pack());
        if (!Objects.equals(pack.packVersion(), reviewed.packRevision())) throw new IOException("The selected meta-pack changed; review setup again.");
        Module module = ComponentIO.validatePersistedModuleJson(reviewed.modelJson(), "reviewed project setup", false);
        if (!module.getMetaVersion().equals(reviewed.options().pack()) || !module.getFlows().isEmpty()
                || !module.getApplicationPackageName().equals(reviewed.options().packageName())
                || !module.getPort().equals(Integer.toString(reviewed.options().port())))
            throw new IOException("The reviewed setup model does not match its options; review again.");
        Plan current = prepare(root, reviewed.options(), module, pack);
        if (!current.equals(reviewed)) throw new IOException("Project files changed since review; review setup again. No files were changed.");
        for (Change change : reviewed.changes()) verify(root, change);
        checkCancelled.run();
        Path backup = safe(root, ".ikasan-studio/setup/" + UUID.randomUUID());
        Files.createDirectories(backup);
        if (Files.getFileAttributeView(backup, PosixFileAttributeView.class) != null)
            Files.setPosixFilePermissions(backup, PosixFilePermissions.fromString("rwx------"));
        Files.writeString(backup.resolve("plan.json"), StudioJson.newObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(reviewed), StandardOpenOption.CREATE_NEW);
        for (Change change : reviewed.changes()) if (change.before() != null) {
            Path target = backup.resolve("original").resolve(change.path());
            verify(root, change);
            Files.createDirectories(target.getParent());
            Files.copy(safe(root, change.path()), target, StandardCopyOption.COPY_ATTRIBUTES);
            if (!encode(Files.readAllBytes(target)).equals(change.before())) throw new IOException("A source changed while its backup was being saved.");
        }
        List<Change> written = new ArrayList<>();
        try {
            for (Change change : reviewed.changes()) {
                checkCancelled.run(); verify(root, change);
                if (change.path().equals(".ikasan-studio-project") && !sourceInventory(root).isEmpty())
                    throw new IOException("New source files appeared during setup; setup was cancelled.");
                Path target = safe(root, change.path());
                if (change.path().startsWith("jar/user/src/")) {
                    Files.createDirectories(target.getParent());
                    Files.copy(safe(root, change.path().substring("jar/user/".length())), target, StandardCopyOption.COPY_ATTRIBUTES);
                } else write(target, change.after(), change.before() != null);
                written.add(change);
            }
            removeEmpty(root.resolve("src"));
            return backup;
        } catch (Exception failure) {
            IOException result = new IOException("Project setup did not complete. Original files are backed up in " + backup, failure);
            for (int i = written.size() - 1; i >= 0; i--) {
                Change change = written.get(i);
                try {
                    if (!Objects.equals(bytes(root, change.path()), change.after())) throw new IOException("A file changed during rollback: " + change.path());
                    if (change.before() == null) write(safe(root, change.path()), null, false);
                    else {
                        Path target = safe(root, change.path()); Files.createDirectories(target.getParent());
                        Files.copy(backup.resolve("original").resolve(change.path()), target,
                                StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception rollback) { result.addSuppressed(rollback); }
            }
            for (String directory : RESERVED) try { removeEmpty(root.resolve(directory)); }
            catch (IOException cleanup) { result.addSuppressed(cleanup); }
            throw result;
        }
    }

    private static void verify(Path root, Change change) throws IOException {
        if (!Objects.equals(bytes(root, change.path()), change.before())) throw new IOException("Project changed during setup: " + change.path());
    }
    private static Map<String,String> sourceInventory(Path root) throws IOException {
        Map<String,String> files = new TreeMap<>(); Path source = safe(root, "src");
        if (!Files.exists(source)) return files;
        long total = 0;
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.sorted().toList()) {
                if (Files.isSymbolicLink(path)) throw new IOException("Setup does not follow source symbolic links: " + path);
                if (Files.isRegularFile(path)) {
                    total += Files.size(path);
                    if (files.size() >= 5000 || total > 64L * 1024 * 1024) throw new IOException("This source tree is too large for initial setup; use a fresh Maven project.");
                    files.put(root.relativize(path).toString().replace('\\', '/'), encode(Files.readAllBytes(path)));
                }
            }
        }
        return files;
    }
    private static String coordinate(String value, Model model) throws IOException {
        if (value == null) throw new IOException("The POM must declare or inherit Maven coordinates.");
        if (value.startsWith("${") && value.endsWith("}")) value = model.getProperties().getProperty(value.substring(2, value.length() - 1), value);
        if (value.isBlank() || value.contains("${")) throw new IOException("Unresolved Maven coordinates need manual setup.");
        return value;
    }
    private static Model child(String group, String parentId, String version, String artifact, String packaging) {
        Model model = new Model(); model.setModelVersion("4.0.0"); model.setArtifactId(artifact); model.setPackaging(packaging);
        Parent parent = new Parent(); parent.setGroupId(group); parent.setArtifactId(parentId); parent.setVersion(version); model.setParent(parent);
        return model;
    }
    private static void alignCompilerConfiguration(Model model, String java) {
        if (model.getBuild() == null) return;
        List<Plugin> plugins = new ArrayList<>(model.getBuild().getPlugins());
        if (model.getBuild().getPluginManagement() != null) plugins.addAll(model.getBuild().getPluginManagement().getPlugins());
        for (Plugin plugin : plugins) if ("maven-compiler-plugin".equals(plugin.getArtifactId()) && plugin.getConfiguration() instanceof Xpp3Dom config)
            for (String key : List.of("source", "target", "release")) if (config.getChild(key) != null) config.getChild(key).setValue(java);
    }
    private static String xml(Model model) throws IOException { StringWriter out = new StringWriter(); new MavenXpp3Writer().write(out, model); return out.toString(); }
    private static Model parse(String text) throws Exception { return new MavenXpp3Reader().read(new StringReader(text)); }
    private static String read(Path root, String path) throws IOException { return new String(Base64.getDecoder().decode(Objects.requireNonNull(bytes(root, path), "Missing " + path)), StandardCharsets.UTF_8); }
    private static String encode(byte[] bytes) { return Base64.getEncoder().encodeToString(bytes); }
    private static String bytes(Path root, String path) throws IOException {
        Path file = safe(root, path);
        if (!Files.exists(file)) return null;
        if (!Files.isRegularFile(file) || Files.size(file) > 64L * 1024 * 1024) throw new IOException("Expected a regular project file: " + path);
        return encode(Files.readAllBytes(file));
    }
    private static Path safe(Path root, String relative) throws IOException {
        Path base = root.toAbsolutePath().normalize(), target = base.resolve(relative).normalize();
        if (Path.of(relative).isAbsolute() || !target.startsWith(base)) throw new IOException("Setup path escapes the project: " + relative);
        for (Path part = target; part != null && part.startsWith(base); part = part.getParent()) if (Files.isSymbolicLink(part)) throw new IOException("Setup does not follow symbolic links: " + part);
        return target;
    }
    private static void write(Path path, String encoded, boolean replace) throws IOException {
        if (encoded == null) { Files.deleteIfExists(path); return; }
        Files.createDirectories(path.getParent()); Path temp = Files.createTempFile(path.getParent(), ".studio-setup-", ".tmp");
        try {
            Files.write(temp, Base64.getDecoder().decode(encoded));
            if (Files.getFileAttributeView(temp, PosixFileAttributeView.class) != null)
                Files.setPosixFilePermissions(temp, replace ? Files.getPosixFilePermissions(path)
                        : PosixFilePermissions.fromString("rw-r--r--"));
            if (!replace) Files.move(temp, path);
            else {
                try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException unsupported) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
            }
        } finally { Files.deleteIfExists(temp); }
    }
    private static void removeEmpty(Path root) throws IOException {
        if (!Files.isDirectory(root) || Files.isSymbolicLink(root)) return;
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path directory : paths.filter(Files::isDirectory).sorted(Comparator.reverseOrder()).toList())
                try { Files.delete(directory); } catch (DirectoryNotEmptyException retained) { /* Preserve developer files. */ }
        }
    }
    private static void removeTree(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); }
    }
}
