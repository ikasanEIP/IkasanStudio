package org.ikasan.studio.core.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.ikasan.studio.core.configuration.EnvironmentProperties;
import org.ikasan.studio.core.generator.ProjectArtifactRenderer;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

/** Read-only identity evidence. Never resolves placeholders against the IDE environment or executes source code. */
public final class SourceCheckoutIdentity {
    private SourceCheckoutIdentity() { }
    public enum Status { MATCH, MISMATCH, UNVERIFIED }
    public record Declaration(String file, String value) { }
    public record Input(String file, String digest) { }
    public record Candidate(String directory, String artifactId, List<String> ownedPomDirectories,
                            List<Declaration> declarations, boolean uncertain) {
        public List<String> names() {
            return declarations.stream().map(Declaration::value).filter(SourceCheckoutIdentity::literal).distinct().sorted().toList();
        }
        public Status status(String expected) {
            if (!literal(expected) || uncertain || names().size() != 1) return Status.UNVERIFIED;
            return names().get(0).equals(expected) ? Status.MATCH : Status.MISMATCH;
        }
    }
    public record Inspection(Path sourceRoot, Path destinationRoot, String expected,
                             List<Candidate> candidates, List<String> pomDirectories,
                             List<Input> sourceInputs, List<Input> destinationInputs, Map<String, String> localParents) {
        public Inspection(Path sourceRoot, Path destinationRoot, String expected, List<Candidate> candidates,
                          List<String> pomDirectories, List<Input> sourceInputs, List<Input> destinationInputs) {
            this(sourceRoot, destinationRoot, expected, candidates, pomDirectories, sourceInputs, destinationInputs, Map.of());
        }
        public Candidate automaticCandidate() {
            var matching = candidates.stream().filter(c -> c.status(expected) == Status.MATCH).toList();
            if (matching.size() == 1) {
                Candidate match = matching.get(0);
                boolean anotherPlausible = candidates.stream().anyMatch(other -> other != match
                        && (other.names().contains(expected) || other.uncertain()));
                if (!anotherPlausible) return match;
            }
            return candidates.size() == 1 ? candidates.get(0) : null;
        }
        public boolean includes(Candidate candidate, Path path) {
            String owner = owner(sourceRoot, path, pomDirectories);
            if (candidate.ownedPomDirectories().contains(owner)) return true;
            if (!path.getFileName().toString().equals("pom.xml")) return false;
            for (String child : candidate.ownedPomDirectories()) {
                Set<String> seen = new HashSet<>();
                while (localParents.containsKey(child) && seen.add(child)) {
                    child = localParents.get(child);
                    if (child.equals(owner)) return true;
                }
            }
            return false;
        }
    }
    public record Check(Inspection inspection, Candidate candidate, List<String> matchedImplementations) {
        public Status status() { return candidate == null ? Status.UNVERIFIED : candidate.status(inspection.expected()); }
        public boolean requiresApproval() { return status() != Status.MATCH; }
        public String expected() { return inspection.expected(); }
        public List<String> names() { return candidate == null ? List.of() : candidate.names(); }
        public static Check unchecked(Path source, Path destination) {
            return new Check(new Inspection(source, destination, "", List.of(), List.of(), List.of(), List.of()), null, List.of());
        }
    }

    public static Inspection inspect(Path sourceDirectory, Path destinationDirectory, Runnable cancelled) throws Exception {
        Path source = sourceDirectory.toRealPath(), destination = destinationDirectory.toRealPath();
        if (source.equals(destination)) throw new IOException("Select the original module checkout, not the Studio project.");
        var files = SourceModuleImport.inventory(source, destination, cancelled);
        List<Path> poms = files.stream().filter(p -> p.getFileName().toString().equals("pom.xml")).toList();
        List<String> directories = poms.stream().map(p -> SourceModuleImport.relative(source, p.getParent())).distinct().sorted().toList();
        List<Input> inputs = new ArrayList<>();
        Map<String, String> artifacts = new TreeMap<>();
        Map<String, org.apache.maven.model.Model> models = new TreeMap<>();
        Map<String, List<Declaration>> declarations = new TreeMap<>();
        Set<String> uncertain = new HashSet<>(), studio = new HashSet<>();
        long bytes = 0;
        for (Path file : files) {
            cancelled.run();
            String name = file.getFileName().toString();
            if (name.endsWith(".java") || name.endsWith(".properties") || name.equals("pom.xml")) {
                bytes += Files.size(file);
                if (bytes > 64L * 1024 * 1024) throw new IOException("Source analysis exceeds 64 MiB; select a narrower module directory.");
            }
            String relative = SourceModuleImport.relative(source, file), directory = owner(source, file, directories);
            if (name.equals("pom.xml")) {
                byte[] content = snapshot(source, relative, inputs);
                try {
                    var model = new MavenXpp3Reader().read(new StringReader(new String(content, StandardCharsets.UTF_8)));
                    artifacts.put(directory, model.getArtifactId()); models.put(directory, model);
                }
                catch (Exception malformed) { /* Artifact IDs are hints, never identity proof. */ }
            } else if (name.endsWith(".properties") && propertiesLocation(relative)) {
                readProperties(source, relative, directory, inputs, declarations, uncertain);
            }
        }
        Set<String> roots = new TreeSet<>(directories); roots.add("");
        for (String directory : roots) {
            cancelled.run();
            String prefix = directory.isEmpty() ? "" : directory + "/";
            for (String model : List.of(ProjectArtifactRenderer.MODEL, "src/main/model/model.json")) {
                String path = prefix + model;
                byte[] content = snapshot(source, path, inputs);
                if (content != null) {
                    studio.add(directory);
                    addModel(content, path, directory, declarations, uncertain);
                }
            }
            String generatedProperties = prefix + "generated/src/main/resources/application.properties";
            if (Files.exists(SourceModuleImport.safe(source, generatedProperties))) {
                studio.add(directory);
                readProperties(source, generatedProperties, directory, inputs, declarations, uncertain);
            }
        }
        Map<String, Set<String>> groups = new TreeMap<>();
        for (Path file : files) {
            String relative = SourceModuleImport.relative(source, file);
            if (!relative.endsWith(".java") || !(relative.startsWith("src/main/java/") || relative.contains("/src/main/java/"))) continue;
            String directory = owner(source, file, directories), group = directory;
            for (String root : studio) {
                if (directory.equals(root) || directory.equals(root.isEmpty() ? "user" : root + "/user")) {
                    group = root; break;
                }
            }
            var owned = groups.computeIfAbsent(group, ignored -> new TreeSet<>()); owned.add(directory); owned.add(group);
        }
        if (groups.isEmpty()) groups.put("", new TreeSet<>(Set.of("")));
        List<Candidate> candidates = new ArrayList<>();
        for (var group : groups.entrySet()) {
            List<Declaration> evidence = new ArrayList<>();
            boolean unknown = false;
            for (String directory : group.getValue()) {
                evidence.addAll(declarations.getOrDefault(directory, List.of()));
                unknown |= uncertain.contains(directory);
            }
            candidates.add(new Candidate(group.getKey(), Objects.toString(artifacts.get(group.getKey()), ""),
                    List.copyOf(group.getValue()), List.copyOf(evidence), unknown));
        }
        Map<String, String> parents = new TreeMap<>();
        for (var entry : models.entrySet()) {
            var parent = entry.getValue().getParent();
            if (parent == null || parent.getRelativePath() == null || parent.getRelativePath().isBlank()) continue;
            Path parentFile = source.resolve(entry.getKey()).resolve(parent.getRelativePath()).normalize();
            if (!parentFile.startsWith(source) || !poms.contains(parentFile)) continue;
            String directory = SourceModuleImport.relative(source, parentFile.getParent());
            var model = models.get(directory);
            if (model == null) continue;
            String group = model.getGroupId() != null ? model.getGroupId() : model.getParent() == null ? null : model.getParent().getGroupId();
            String version = model.getVersion() != null ? model.getVersion() : model.getParent() == null ? null : model.getParent().getVersion();
            if (Objects.equals(parent.getArtifactId(), model.getArtifactId()) && Objects.equals(parent.getGroupId(), group)
                    && Objects.equals(parent.getVersion(), version)) parents.put(entry.getKey(), directory);
        }
        List<Input> targetInputs = new ArrayList<>();
        byte[] model = snapshot(destination, ProjectArtifactRenderer.MODEL, targetInputs);
        if (model == null) model = snapshot(destination, "src/main/model/model.json", targetInputs);
        String expected = modelName(model);
        return new Inspection(source, destination, expected, List.copyOf(candidates), List.copyOf(directories),
                sorted(inputs), sorted(targetInputs), Map.copyOf(parents));
    }

    static void verify(Check check) throws Exception {
        if (check.candidate() == null || !check.inspection().candidates().contains(check.candidate()))
            throw new IOException("Review the source checkout and verify its module identity before importing.");
        var current = inspect(check.inspection().sourceRoot(), check.inspection().destinationRoot(), () -> { });
        if (!check.inspection().equals(current)) throw new IOException("Module identity or checkout metadata changed since review. Analyse again.");
    }

    private static List<Input> sorted(List<Input> inputs) {
        return inputs.stream().distinct().sorted(Comparator.comparing(Input::file)).toList();
    }
    private static boolean propertiesLocation(String path) {
        return path.startsWith("config/") || path.contains("/config/")
                || path.startsWith("src/main/resources/") || path.contains("/src/main/resources/");
    }
    private static String owner(Path source, Path file, List<String> directories) {
        String path = SourceModuleImport.relative(source, file);
        return directories.stream().filter(d -> d.isEmpty() || path.startsWith(d + "/"))
                .max(Comparator.comparingInt(String::length)).orElse("");
    }
    private static boolean literal(String value) {
        if (value == null || value.isBlank() || value.contains("${") || value.contains("#{")) return false;
        int first = value.indexOf('@');
        return first < 0 || value.indexOf('@', first + 1) < 0;
    }
    private static void readProperties(Path source, String path, String directory, List<Input> inputs,
                                       Map<String, List<Declaration>> declarations, Set<String> uncertain) throws Exception {
        byte[] content = snapshot(source, path, inputs);
        if (content == null) return;
        try {
            String value = EnvironmentProperties.parse(new String(content, StandardCharsets.ISO_8859_1)).getProperty("module.name");
            if (value == null) return;
            declarations.computeIfAbsent(directory, ignored -> new ArrayList<>()).add(new Declaration(path, value));
            if (!literal(value)) uncertain.add(directory);
        } catch (IOException invalid) { uncertain.add(directory); }
    }
    private static void addModel(byte[] content, String path, String directory, Map<String, List<Declaration>> declarations, Set<String> uncertain) {
        String value = modelName(content);
        if (value.isBlank()) { uncertain.add(directory); return; }
        declarations.computeIfAbsent(directory, ignored -> new ArrayList<>()).add(new Declaration(path, value));
    }
    private static String modelName(byte[] content) {
        if (content == null) return "";
        try {
            var node = new ObjectMapper().enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                    .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(content);
            if (node == null) return "";
            var name = node.has("modelFormat") ? node.path("module").path("name") : node.path("name");
            return name.isTextual() ? name.textValue() : "";
        } catch (IOException invalid) { return ""; }
    }
    private static byte[] snapshot(Path root, String relative, List<Input> inputs) throws Exception {
        Path path = SourceModuleImport.safe(root, relative);
        byte[] bytes = Files.exists(path) ? SourceModuleImport.read(path) : null;
        String digest = bytes == null ? null : Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes));
        inputs.add(new Input(relative, digest)); return bytes;
    }
}
