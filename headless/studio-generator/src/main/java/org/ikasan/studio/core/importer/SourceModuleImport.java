package org.ikasan.studio.core.importer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;

/** Offline, source-assisted import. Never loads application classes, runs Maven, or changes the source checkout. */
public final class SourceModuleImport {
    private SourceModuleImport() { }
    private static final Set<String> SKIP = Set.of(".git", ".idea", "target", "build", ".gradle", "node_modules", "generated");
    private static final int LIMIT = 10_000;
    private static final long MAX_BYTES = 2 * 1024 * 1024;

    public record FileChoice(String source, String destination, String bytes, String previous,
                             boolean recommended, boolean wiring, String reason) { }
    public record BeanRequirement(String name, String type, String generatedFile, int line,
                                  List<String> sourceDefinitions) { }
    public record PropertyFile(String source, String text, String environment) { }
    public record DependencyChoice(String id, Dependency dependency, boolean importable, String reason) { }
    public record Analysis(Path sourceRoot, Path destinationRoot, List<FileChoice> files,
                           List<BeanRequirement> beans, List<PropertyFile> properties,
                           List<DependencyChoice> dependencies, List<String> notes, String originalPom, Map<String, String> generatedInputs) { }
    private record JavaSource(Path path, String relative, String bytes, CompilationUnit unit, String pkg, boolean wiring) { }

    public static Analysis analyse(Path sourceDirectory, Path destinationDirectory, Runnable checkCancelled) throws Exception {
        Path source = sourceDirectory.toRealPath();
        Path destination = destinationDirectory.toRealPath();
        if (source.equals(destination)) throw new IOException("Select the original module checkout, not the Studio project.");
        List<String> notes = new ArrayList<>();
        var parser = new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));
        Map<String, JavaSource> classes = new TreeMap<>();
        Set<String> ambiguous = new TreeSet<>();
        List<JavaSource> units = new ArrayList<>();
        List<PropertyFile> properties = new ArrayList<>();
        List<Path> poms = new ArrayList<>();
        long sourceBytes = 0;
        for (Path file : inventory(source, destination, checkCancelled)) {
            checkCancelled.run();
            String relative = relative(source, file);
            if (relative.endsWith(".java") || relative.endsWith(".properties") || file.getFileName().toString().equals("pom.xml")) {
                sourceBytes += Files.size(file);
                if (sourceBytes > 64L * 1024 * 1024) throw new IOException("Source analysis exceeds 64 MiB; select a narrower module directory.");
            }
            if (relative.endsWith("/src/main/java/module-info.java") || relative.equals("src/main/java/module-info.java")) continue;
            if (relative.endsWith(".java") && (relative.startsWith("src/main/java/") || relative.contains("/src/main/java/"))) {
                byte[] bytes = read(file);
                var result = parser.parse(new String(bytes, StandardCharsets.UTF_8));
                if (!result.isSuccessful() || result.getResult().isEmpty()) {
                    notes.add("Cannot parse source; review manually: " + relative); continue;
                }
                var unit = result.getResult().get();
                String pkg = unit.getPackageDeclaration().map(com.github.javaparser.ast.PackageDeclaration::getNameAsString).orElse("");
                boolean wiring = unit.getTypes().stream().anyMatch(t -> t.getAnnotations().stream()
                        .anyMatch(a -> Set.of("Configuration", "SpringBootApplication", "SpringBootConfiguration").contains(a.getName().getIdentifier())));
                wiring |= unit.findAll(MethodDeclaration.class).stream().anyMatch(m -> m.getAnnotations().stream()
                        .anyMatch(a -> a.getName().getIdentifier().equals("Bean")));
                var java = new JavaSource(file, relative, encode(bytes), unit, pkg, wiring);
                units.add(java);
                for (var type : unit.getTypes()) {
                    String name = (pkg.isEmpty() ? "" : pkg + ".") + type.getNameAsString();
                    if (classes.putIfAbsent(name, java) != null) ambiguous.add(name);
                }
            } else if (relative.endsWith(".properties") && (relative.startsWith("config/") || relative.contains("/config/")
                    || relative.startsWith("src/main/resources/") || relative.contains("/src/main/resources/"))) {
                String name = file.getFileName().toString();
                properties.add(new PropertyFile(relative, new String(read(file), StandardCharsets.ISO_8859_1),
                        name.startsWith("application-") ? name.substring(12, name.length() - 11) : ""));
            } else if (file.getFileName().toString().equals("pom.xml")) poms.add(file);
        }
        ambiguous.forEach(name -> { classes.remove(name); notes.add("Ambiguous class in multiple source modules; no automatic match: " + name); });
        if (units.isEmpty()) throw new IOException("No Java sources found under src/main/java. Select the original Maven project or reactor directory.");
        List<BeanRequirement> beans = new ArrayList<>();
        Set<JavaSource> recommended = new LinkedHashSet<>();
        Path generated = safe(destination, "generated/src/main/java");
        if (!Files.isDirectory(generated)) throw new IOException("Generate/import the destination module before reviewing its source checkout.");
        Map<String, String> generatedInputs = new TreeMap<>();
        for (Path file : inventory(generated, null, checkCancelled)) {
            if (!file.toString().endsWith(".java")) continue;
            byte[] generatedBytes = read(file);
            generatedInputs.put(relative(destination, file), encode(generatedBytes));
            var result = parser.parse(new String(generatedBytes, StandardCharsets.UTF_8));
            if (!result.isSuccessful() || result.getResult().isEmpty()) throw new IOException("Cannot inspect generated Java: " + relative(destination, file));
            CompilationUnit unit = result.getResult().get();
            for (FieldDeclaration field : unit.findAll(FieldDeclaration.class)) {
                var resource = field.getAnnotations().stream().filter(a -> a.getName().getIdentifier().equals("Resource")).findFirst();
                if (resource.isEmpty()) continue;
                for (var variable : field.getVariables()) {
                    String type = resolve(variable.getType().asString(), unit, classes.keySet());
                    if (type.startsWith("org.ikasan.builder.")) continue;
                    String name = variable.getNameAsString();
                    if (resource.get().isNormalAnnotationExpr()) {
                        for (var pair : resource.get().asNormalAnnotationExpr().getPairs())
                            if (pair.getNameAsString().equals("name") && pair.getValue().isStringLiteralExpr()) name = pair.getValue().asStringLiteralExpr().asString();
                    }
                    JavaSource found = classes.get(type);
                    if (found != null && !found.wiring()) recommended.add(found);
                    List<String> evidence = new ArrayList<>();
                    for (JavaSource original : units) {
                        checkCancelled.run();
                        for (MethodDeclaration method : original.unit().findAll(MethodDeclaration.class)) {
                            var bean = method.getAnnotations().stream().filter(a -> a.getName().getIdentifier().equals("Bean")).findFirst();
                            if (bean.isEmpty()) continue;
                            String returns = resolve(method.getType().asString(), original.unit(), classes.keySet());
                            if (name.equals(method.getNameAsString()) || type.equals(returns)) {
                                evidence.add(original.relative() + ":" + method.getBegin().map(p -> p.line).orElse(1)
                                        + " — @Bean " + method.getNameAsString() + " returns " + returns);
                                // Method-local implementation types are useful even for interface-typed Resource fields.
                                for (String reference : references(method, original.unit(), classes.keySet())) {
                                    JavaSource referenced = classes.get(reference);
                                    if (referenced != null && !referenced.wiring()) recommended.add(referenced);
                                }
                            }
                        }
                    }
                    beans.add(new BeanRequirement(name, type, relative(destination, file),
                            variable.getBegin().map(p -> p.line).orElse(1), List.copyOf(evidence)));
                }
            }
        }
        // Conservative source closure: explicit imports, wildcard imports used in the AST, and same-package types.
        Deque<JavaSource> pending = new ArrayDeque<>(recommended);
        while (!pending.isEmpty()) {
            checkCancelled.run();
            var original = pending.removeFirst();
            for (String reference : references(original.unit(), original.unit(), classes.keySet())) {
                var dependency = classes.get(reference);
                if (dependency != null && !dependency.wiring() && recommended.add(dependency)) pending.add(dependency);
            }
        }
        List<FileChoice> choices = new ArrayList<>();
        for (JavaSource java : units) {
            if (java.unit().getTypes().stream().anyMatch(t -> t.getAnnotations().stream().anyMatch(a ->
                    Set.of("SpringBootApplication", "SpringBootConfiguration").contains(a.getName().getIdentifier())))) {
                notes.add("Original bootstrap is not copied; Studio owns application startup: " + java.relative()); continue;
            }
            String target = "user/src/main/java/" + (java.pkg().isEmpty() ? "" : java.pkg().replace('.', '/') + "/") + java.path().getFileName();
            String previous = existing(safe(destination, target));
            boolean suggested = recommended.contains(java);
            choices.add(new FileChoice(java.relative(), target, java.bytes(), previous, suggested, java.wiring(),
                    java.wiring() ? "Original Spring configuration: review duplicate beans and component scanning before selecting."
                            : suggested ? "Referenced by a generated bean contract or a matching source implementation."
                            : "Additional source; not proven necessary by static analysis."));
        }
        // Keep explicitly named @PropertySource classpath resources at their original classpath paths.
        // application*.properties remain external environment choices to avoid duplicate application resources.
        Set<String> resources = new TreeSet<>();
        for (JavaSource java : recommended) for (var annotation : java.unit().findAll(com.github.javaparser.ast.expr.AnnotationExpr.class)) {
            if (!annotation.getName().getIdentifier().equals("PropertySource")) continue;
            for (var literal : annotation.findAll(com.github.javaparser.ast.expr.StringLiteralExpr.class)) {
                String value = literal.asString();
                if (value.startsWith("classpath:") && value.endsWith(".properties") && !value.contains("${"))
                    resources.add(value.substring("classpath:".length()).replaceFirst("^/", ""));
            }
        }
        for (PropertyFile property : properties) {
            String marker = "src/main/resources/";
            int at = property.source().indexOf(marker);
            if (at < 0) continue;
            String resource = property.source().substring(at + marker.length());
            if (!resources.contains(resource) || Path.of(resource).getFileName().toString().startsWith("application")) continue;
            String target = "user/src/main/resources/" + resource;
            choices.add(new FileChoice(property.source(), target, encode(property.text().getBytes(StandardCharsets.ISO_8859_1)),
                    existing(safe(destination, target)), true, false, "Named @PropertySource resource of a recommended class; original classpath path is retained."));
        }
        choices.sort(Comparator.comparing(FileChoice::destination).thenComparing(FileChoice::source));
        String originalPom = existing(safe(destination, "pom.xml"));
        if (originalPom == null) throw new IOException("The destination project has no pom.xml.");
        List<DependencyChoice> dependencies = dependencies(source, poms, originalPom, notes);
        notes.add("The scan excludes generated/build output, tests and symbolic links. Non-properties resources require separate review.");
        notes.add("Static analysis cannot prove reflection, custom factories, external library internals, inherited Maven configuration or all resource usage. Review the original wiring and runtime-import report.");
        notes.add("Properties retain placeholders. Supply real environment settings; do not invent business defaults to suppress binding failures.");
        return new Analysis(source, destination, List.copyOf(choices), List.copyOf(beans), List.copyOf(properties),
                dependencies, List.copyOf(notes), originalPom, Map.copyOf(generatedInputs));
    }

    private static Set<String> references(Node node, CompilationUnit unit, Set<String> names) {
        Set<String> result = new TreeSet<>();
        for (var type : node.findAll(ClassOrInterfaceType.class)) result.add(resolve(type.getNameWithScope(), unit, names));
        for (var name : node.findAll(NameExpr.class)) result.add(resolve(name.getNameAsString(), unit, names));
        // Static imports may reference local helper classes without a type expression.
        unit.getImports().stream().filter(com.github.javaparser.ast.ImportDeclaration::isStatic).forEach(i -> {
            String name = i.getNameAsString();
            while (name.contains(".")) { if (names.contains(name)) result.add(name); name = name.substring(0, name.lastIndexOf('.')); }
        });
        return result;
    }

    private static String resolve(String type, CompilationUnit unit, Set<String> names) {
        type = type.replaceAll("<.*>", "").replace("[]", "");
        if (names.contains(type) || type.contains(".") && Character.isLowerCase(type.charAt(0))) return type;
        for (var imported : unit.getImports()) {
            if (imported.isStatic()) continue;
            String name = imported.getNameAsString();
            if (!imported.isAsterisk() && name.endsWith("." + type)) return name;
        }
        String local = unit.getPackageDeclaration().map(p -> p.getNameAsString() + ".").orElse("") + type;
        if (names.contains(local)) return local;
        List<String> wildcard = new ArrayList<>();
        for (var imported : unit.getImports()) if (imported.isAsterisk() && !imported.isStatic()) {
            String candidate = imported.getNameAsString() + "." + type;
            if (names.contains(candidate)) wildcard.add(candidate);
        }
        return wildcard.size() == 1 ? wildcard.get(0) : type;
    }

    private static List<DependencyChoice> dependencies(Path source, List<Path> poms, String destinationPom, List<String> notes) throws Exception {
        var target = pom(decode(destinationPom));
        Set<String> existing = new HashSet<>();
        target.getDependencies().forEach(d -> existing.add(coordinate(d)));
        Properties shared = new Properties();
        Path rootPom = safe(source, "pom.xml");
        if (Files.isRegularFile(rootPom)) shared.putAll(pom(new String(read(rootPom), StandardCharsets.UTF_8)).getProperties());
        Map<String, DependencyChoice> candidates = new TreeMap<>();
        for (Path path : poms) {
            Model model;
            try { model = pom(new String(read(path), StandardCharsets.UTF_8)); }
            catch (Exception failure) { notes.add("Cannot inspect Maven declarations: " + relative(source, path)); continue; }
            Properties values = new Properties(); values.putAll(shared); values.putAll(model.getProperties());
            for (Dependency declared : model.getDependencies()) {
                Dependency dependency = declared.clone();
                dependency.setGroupId(expand(dependency.getGroupId(), values));
                dependency.setArtifactId(expand(dependency.getArtifactId(), values));
                dependency.setVersion(expand(dependency.getVersion(), values));
                dependency.setClassifier(expand(dependency.getClassifier(), values));
                dependency.setType(expand(dependency.getType(), values));
                dependency.setScope(expand(dependency.getScope(), values));
                for (var exclusion : dependency.getExclusions()) {
                    exclusion.setGroupId(expand(exclusion.getGroupId(), values));
                    exclusion.setArtifactId(expand(exclusion.getArtifactId(), values));
                }
                String id = coordinate(dependency);
                if (existing.contains(id) || "test".equals(dependency.getScope())) continue;
                boolean platform = dependency.getGroupId() == null || dependency.getGroupId().startsWith("org.ikasan")
                        || dependency.getGroupId().startsWith("org.springframework");
                boolean importable = !platform && dependency.getVersion() != null
                        && !dependency.getVersion().contains("${") && !id.contains("${") && dependency.getSystemPath() == null
                        && dependency.getExclusions().stream().noneMatch(e -> Objects.toString(e.getGroupId(), "").contains("${") || Objects.toString(e.getArtifactId(), "").contains("${"))
                        && !dependency.isOptional() && (dependency.getScope() == null || Set.of("compile", "runtime", "provided").contains(dependency.getScope()));
                var candidate = new DependencyChoice(id, dependency, importable, relative(source, path)
                        + (platform ? ": platform dependency; retain the selected meta-pack baseline."
                        : importable ? ": direct declaration; confirm the selected classes require it (exclusions are retained)."
                        : ": unresolved/inherited version, optional or special dependency; review manually."));
                var previous = candidates.putIfAbsent(id, candidate);
                if (previous != null && !Objects.equals(previous.dependency().getVersion(), dependency.getVersion()))
                    candidates.put(id, new DependencyChoice(id, dependency, false, "Different source modules declare different versions; resolve manually."));
            }
            if (model.getParent() != null || !model.getProfiles().isEmpty() || !model.getRepositories().isEmpty())
                notes.add("Review parent, dependency management, repositories, profiles and plugins from " + relative(source, path) + "; these are not copied.");
        }
        return List.copyOf(candidates.values());
    }

    private static String expand(String value, Properties values) {
        if (value == null) return null;
        for (int n = 0; n < 10; n++) {
            var matcher = Pattern.compile("\\$\\{([^}]+)}").matcher(value);
            String expanded = matcher.replaceAll(m -> java.util.regex.Matcher.quoteReplacement(values.getProperty(m.group(1), m.group())));
            if (expanded.equals(value)) break;
            value = expanded;
        }
        return value;
    }
    private static String coordinate(Dependency d) { return d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getType() + ":" + Objects.toString(d.getClassifier(), ""); }
    private static Model pom(String text) throws Exception { return new MavenXpp3Reader().read(new StringReader(text)); }

    public static String pomAfter(Analysis analysis, Set<String> dependencies) throws Exception {
        if (dependencies.isEmpty()) return decode(analysis.originalPom());
        var model = pom(decode(analysis.originalPom()));
        for (String id : new TreeSet<>(dependencies)) {
            var choice = analysis.dependencies().stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow();
            if (!choice.importable()) throw new IOException("Resolve dependency manually: " + id);
            model.addDependency(choice.dependency().clone());
        }
        var writer = new StringWriter(); new MavenXpp3Writer().write(writer, model); return writer.toString();
    }

    public static String report(Analysis analysis, Set<String> files, Set<String> dependencies) {
        StringBuilder text = new StringBuilder("# Source-assisted module import review\n\n");
        text.append("Original checkout: ").append(analysis.sourceRoot()).append("\n\n");
        text.append("This is a transfer and review record, not evidence that the module compiles or runs. The original checkout is unchanged.\n\n## Selected files\n\n");
        analysis.files().stream().filter(f -> files.contains(f.source())).forEach(f -> text.append("- ").append(f.source()).append(" → ").append(f.destination()).append(f.wiring() ? " — REVIEW ORIGINAL SPRING WIRING" : "").append('\n'));
        text.append("\n## Generated bean contracts\n\n");
        for (var bean : analysis.beans()) {
            text.append("- Bean `").append(bean.name()).append("` of type `").append(bean.type()).append("`: ")
                    .append(bean.generatedFile()).append(':').append(bean.line()).append('\n');
            if (bean.sourceDefinitions().isEmpty()) text.append("  No matching original @Bean method was identified; check stereotypes, aliases and external libraries.\n");
            bean.sourceDefinitions().forEach(d -> text.append("  Original definition candidate: ").append(d).append('\n'));
        }
        text.append("\n## Selected dependency declarations\n\n"); dependencies.stream().sorted().forEach(d -> text.append("- ").append(d).append('\n'));
        text.append("\n## Other source and dependencies to review\n\n");
        analysis.files().stream().filter(f -> f.recommended() && !files.contains(f.source())).forEach(f ->
                text.append("- Recommended source not transferred: ").append(f.source()).append('\n'));
        analysis.dependencies().forEach(d -> text.append("- ").append(dependencies.contains(d.id()) ? "[selected] " : "[not transferred] ")
                .append(d.id()).append(" version ").append(Objects.toString(d.dependency().getVersion(), "inherited/managed"))
                .append(" — ").append(d.reason()).append('\n'));
        text.append("\n## Discovered properties (no values)\n\n");
        analysis.properties().forEach(p -> text.append("- ").append(p.source()).append('\n'));
        text.append("\n## Remaining review\n\n");
        analysis.notes().forEach(n -> text.append("- ").append(n).append('\n'));
        text.append("- Put new Spring wiring under user/ in a package scanned by the generated Application. Match generated @Resource bean names or provide explicit aliases/qualifiers. Do not blindly scan the original configuration; avoid duplicate beans and circular dependencies.\n")
                .append("- Verify real configuration prefixes at original factory call sites and Java field types in dependency sources. Properties files are offered separately; their values are not written to this report.\n")
                .append("- Restore non-properties resources, service-provider registrations and any reflection-only classes manually. Source recommendation is conservative.\n")
                .append("- Compile, package and run with the selected environment and matching JDK. Record bean/binding failures separately from unavailable infrastructure. Compilation or idle process existence does not prove delivery.\n")
                .append("- Before claiming a working flow, verify first delivery, continued idle readiness, and later delivery in the same application without restarting it.\n");
        return text.toString();
    }

    public static Path apply(Analysis analysis, Set<String> files, Set<String> dependencies, boolean replaceExisting) throws Exception {
        Map<String, String> generatedNow = new TreeMap<>();
        for (Path file : inventory(safe(analysis.destinationRoot(), "generated/src/main/java"), null, () -> { }))
            if (file.toString().endsWith(".java")) generatedNow.put(relative(analysis.destinationRoot(), file), encode(read(file)));
        if (!generatedNow.equals(analysis.generatedInputs())) throw new IOException("Generated code changed since review. Analyse again.");
        Map<Path, String> originals = new LinkedHashMap<>(), writes = new LinkedHashMap<>();
        for (String source : files) {
            var file = analysis.files().stream().filter(f -> f.source().equals(source)).findFirst().orElseThrow();
            if (!Objects.equals(existing(safe(analysis.sourceRoot(), source)), file.bytes())) throw new IOException("Original source changed since review: " + source);
            Path target = safe(analysis.destinationRoot(), file.destination());
            if (writes.putIfAbsent(target, file.bytes()) != null) throw new IOException("Multiple sources target " + file.destination());
            if (!Objects.equals(file.previous(), existing(target))) throw new IOException("Destination changed since review: " + file.destination());
            if (file.previous() != null && !file.previous().equals(file.bytes()) && !replaceExisting) throw new IOException("Approve replacement before importing " + file.destination());
            originals.put(target, file.previous());
        }
        if (!dependencies.isEmpty()) {
            Path target = safe(analysis.destinationRoot(), "pom.xml");
            if (!Objects.equals(existing(target), analysis.originalPom())) throw new IOException("pom.xml changed since review. Analyse again.");
            originals.put(target, analysis.originalPom()); writes.put(target, encode(pomAfter(analysis, dependencies).getBytes(StandardCharsets.UTF_8)));
        }
        Path report = safe(analysis.destinationRoot(), "user/import-reviews/" + UUID.randomUUID() + ".md");
        originals.put(report, null); writes.put(report, encode(report(analysis, files, dependencies).getBytes(StandardCharsets.UTF_8)));
        List<Path> applied = new ArrayList<>();
        try {
            for (var entry : writes.entrySet()) {
                Path path = entry.getKey(); safe(analysis.destinationRoot(), relative(analysis.destinationRoot(), path));
                if (!Objects.equals(existing(path), originals.get(path))) throw new IOException("Destination changed during import: " + path.getFileName());
                if (Objects.equals(entry.getValue(), originals.get(path))) continue;
                Files.createDirectories(path.getParent());
                try (var output = Files.newOutputStream(path, LinkOption.NOFOLLOW_LINKS,
                        originals.get(path) == null ? StandardOpenOption.CREATE_NEW : StandardOpenOption.TRUNCATE_EXISTING)) {
                    applied.add(path); output.write(Base64.getDecoder().decode(entry.getValue()));
                }
            }
        } catch (IOException | RuntimeException failure) {
            Collections.reverse(applied);
            for (Path path : applied) try {
                safe(analysis.destinationRoot(), relative(analysis.destinationRoot(), path));
                if (originals.get(path) == null) Files.deleteIfExists(path);
                else Files.write(path, Base64.getDecoder().decode(originals.get(path)), LinkOption.NOFOLLOW_LINKS, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (Exception rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
        return report;
    }

    // JDK file-visitor nullability contracts; keep the core free of IDE annotation dependencies.
    @SuppressWarnings("NullableProblems")
    private static List<Path> inventory(Path root, Path excluded, Runnable checkCancelled) throws IOException {
        List<Path> paths = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            private int visited;
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                checkCancelled.run();
                if (++visited > LIMIT) throw new IOException("Source scan exceeds 10,000 entries; select a narrower module directory.");
                String relativeDirectory = relative(root, dir);
                boolean testSources = relativeDirectory.equals("src/test") || relativeDirectory.endsWith("/src/test");
                if (!dir.equals(root) && (testSources || SKIP.contains(dir.getFileName().toString()) || dir.equals(excluded))) return FileVisitResult.SKIP_SUBTREE;
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                checkCancelled.run();
                if (++visited > LIMIT) throw new IOException("Source scan exceeds 10,000 entries; select a narrower module directory.");
                if (attrs.isRegularFile() && !Files.isSymbolicLink(file)) paths.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        paths.sort(Comparator.naturalOrder()); return paths;
    }
    private static String relative(Path root, Path file) { return root.relativize(file).toString().replace('\\', '/'); }
    private static byte[] read(Path file) throws IOException {
        if (Files.size(file) > MAX_BYTES) throw new IOException("Source file exceeds 2 MiB: " + file.getFileName());
        return Files.readAllBytes(file);
    }
    private static String existing(Path file) throws IOException { return Files.exists(file, LinkOption.NOFOLLOW_LINKS) ? encode(read(file)) : null; }
    private static String encode(byte[] bytes) { return Base64.getEncoder().encodeToString(bytes); }
    private static String decode(String bytes) { return new String(Base64.getDecoder().decode(bytes), StandardCharsets.UTF_8); }
    private static Path safe(Path root, String relative) throws IOException {
        Path base = root.toAbsolutePath().normalize(), path = base.resolve(relative).normalize();
        if (!path.startsWith(base) || Path.of(relative).isAbsolute()) throw new IOException("Import path escapes its project.");
        for (Path part = path; part != null && part.startsWith(base); part = part.getParent())
            if (Files.isSymbolicLink(part)) throw new IOException("Source import does not follow symbolic links: " + part);
        return path;
    }
}
