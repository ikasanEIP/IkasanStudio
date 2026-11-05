package org.ikasan.studio.core.importer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.printer.configuration.DefaultConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration.ConfigOption;
import org.ikasan.studio.core.generator.DebugTransitionComponentTemplate;
import org.ikasan.studio.core.generator.DeepCopyUtilTemplate;
import org.ikasan.studio.core.generator.ProjectArtifactRenderer;
import org.ikasan.studio.core.io.ComponentIO;
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
    public enum SupportStatus { SUPPLIED, ORIGINAL_DIFFERS, DESTINATION_DIFFERS, DESTINATION_MISSING, UNVERIFIED }
    /** Studio-provided support classes; only explicit reviewed copies become developer customisations. */
    public record ManagedFile(String source, String destination, String bytes, String current, SupportStatus status) { }
    public static final String SUPPORT_CUSTOMISATIONS = "user/import-reviews/support-customisations.txt";
    private static final Map<String, String> SUPPORT_CLASSES = Map.of(
            DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_PACKAGE + "." + DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_CLASS_NAME,
            "user/src/main/java/" + DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_PACKAGE.replace('.', '/') + "/" + DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_CLASS_NAME + ".java",
            DeepCopyUtilTemplate.DEEP_COPY_UTIL_PACKAGE + "." + DeepCopyUtilTemplate.DEEP_COPY_UTIL_CLASS_NAME,
            "user/src/main/java/" + DeepCopyUtilTemplate.DEEP_COPY_UTIL_PACKAGE.replace('.', '/') + "/" + DeepCopyUtilTemplate.DEEP_COPY_UTIL_CLASS_NAME + ".java");
    public record BeanRequirement(String name, String type, String generatedFile, int line,
                                  List<String> sourceDefinitions) { }
    public record PropertyFile(String source, String text, String environment) { }
    public record DependencyChoice(String id, Dependency dependency, boolean importable, String reason) { }
    public record Analysis(Path sourceRoot, Path destinationRoot, List<FileChoice> files,
                           List<BeanRequirement> beans, List<PropertyFile> properties,
                           List<DependencyChoice> dependencies, List<String> notes, String originalPom, Map<String, String> generatedInputs,
                           List<ManagedFile> managedFiles, Map<String, String> supportInputs, SourceCheckoutIdentity.Check identity) {
    }

    /** Normal and explicitly customisable support sources share the same protected import path. */
    public static List<FileChoice> importableFiles(Analysis analysis) {
        List<FileChoice> result = new ArrayList<>(analysis.files());
        analysis.managedFiles().stream().filter(file -> SUPPORT_CLASSES.containsValue(file.destination()))
                .map(file -> new FileChoice(file.source(), file.destination(), file.bytes(), file.current(), false, false,
                        supportDescription(file.status()))).forEach(result::add);
        return List.copyOf(result);
    }

    public static boolean preserveSupportCustomisation(Path root, String packageName, String className) throws IOException {
        String destination = "user/src/main/java/" + packageName.replace('.', '/') + "/" + className + ".java";
        return SUPPORT_CLASSES.containsValue(destination) && Files.exists(safe(root, destination), LinkOption.NOFOLLOW_LINKS)
                && supportCustomisations(existing(safe(root, SUPPORT_CUSTOMISATIONS))).contains(destination);
    }

    private static Set<String> supportCustomisations(String encoded) throws IOException {
        Set<String> paths = new TreeSet<>();
        if (encoded != null) for (String path : decode(encoded).lines().toList()) {
            if (path.isBlank() || path.startsWith("#")) continue;
            if (!SUPPORT_CLASSES.containsValue(path)) throw new IOException("Invalid support customisation path: " + path);
            paths.add(path);
        }
        return paths;
    }

    private record JavaSource(Path path, String relative, String bytes, CompilationUnit unit, String pkg, boolean wiring) { }

    public static Analysis analyse(Path sourceDirectory, Path destinationDirectory, Runnable checkCancelled) throws Exception {
        return analyse(sourceDirectory, destinationDirectory, null, checkCancelled);
    }

    public static Analysis analyse(Path sourceDirectory, Path destinationDirectory, String sourceModule,
                                   Runnable checkCancelled) throws Exception {
        var inspection = SourceCheckoutIdentity.inspect(sourceDirectory, destinationDirectory, checkCancelled);
        var candidate = sourceModule == null ? inspection.automaticCandidate()
                : inspection.candidates().stream().filter(c -> c.directory().equals(sourceModule)).findFirst().orElse(null);
        if (candidate == null) throw new IOException("Multiple source modules found. Choose the source module before analysing.");
        Path source = inspection.sourceRoot(), destination = inspection.destinationRoot();
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
            if (!inspection.includes(candidate, file)) continue;
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
        List<ManagedFile> managed = new ArrayList<>();
        Map<String, String> supportInputs = new TreeMap<>();
        supportInputs.put(SUPPORT_CUSTOMISATIONS, Objects.toString(existing(safe(destination, SUPPORT_CUSTOMISATIONS)), ""));
        Map<String, String> expectedSupport = expectedSupport(destination, units, supportInputs, notes);
        for (Iterator<JavaSource> iterator = units.iterator(); iterator.hasNext();) {
            JavaSource java = iterator.next();
            String support = supportPath(java);
            if (support == null) continue;
            String current = existing(safe(destination, support));
            String expected = expectedSupport.get(support);
            SupportStatus status = current == null ? SupportStatus.DESTINATION_MISSING
                    : expected == null ? SupportStatus.UNVERIFIED
                    : !sameJava(parser, decode(current), expected) ? SupportStatus.DESTINATION_DIFFERS
                    : sameJava(parser, decode(java.bytes()), decode(current)) ? SupportStatus.SUPPLIED : SupportStatus.ORIGINAL_DIFFERS;
            managed.add(new ManagedFile(java.relative(), support, java.bytes(), current, status));
            iterator.remove();
            classes.entrySet().removeIf(entry -> entry.getValue() == java);
        }
        managed.sort(Comparator.comparing(ManagedFile::source));
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
            // Factories can construct an implementation directly rather than inject it through @Resource.
            for (String reference : references(unit, unit, classes.keySet())) {
                JavaSource found = classes.get(reference);
                if (found != null && !found.wiring()) recommended.add(found);
            }
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
            boolean referenced = recommended.contains(java);
            boolean duplicate = java.unit().getTypes().stream().anyMatch(type -> ambiguous.contains((java.pkg().isEmpty() ? "" : java.pkg() + ".") + type.getNameAsString()));
            boolean debugSubclass = java.unit().getTypes().stream().filter(com.github.javaparser.ast.body.TypeDeclaration::isClassOrInterfaceDeclaration)
                    .map(com.github.javaparser.ast.body.TypeDeclaration::asClassOrInterfaceDeclaration)
                    .flatMap(type -> type.getExtendedTypes().stream()).anyMatch(type -> resolve(type.getNameWithScope(), java.unit(), SUPPORT_CLASSES.keySet())
                            .equals(DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_PACKAGE + "." + DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_CLASS_NAME));
            boolean newImplementation = previous == null && !java.wiring() && !duplicate && !debugSubclass;
            boolean suggested = !java.wiring() && !duplicate && !debugSubclass && (referenced || newImplementation);
            choices.add(new FileChoice(java.relative(), target, java.bytes(), previous, suggested, java.wiring(),
                    java.wiring() ? "Original Spring configuration: review duplicate beans and component scanning before selecting."
                            : duplicate ? "Ambiguous class in multiple source modules; select the correct implementation explicitly."
                            : debugSubclass ? "Flow debug subclass: Studio may already generate it; compare and select explicitly if needed."
                            : referenced ? "Referenced by generated code, a bean contract or a matching source implementation."
                            : newImplementation ? "New implementation/helper source with no destination file; selected for review, necessity is not proven by static analysis."
                            : "Existing destination source; compare and select explicitly if needed."));
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
                dependencies, List.copyOf(notes), originalPom, Map.copyOf(generatedInputs), List.copyOf(managed), Map.copyOf(supportInputs),
                new SourceCheckoutIdentity.Check(inspection, candidate, recommended.stream().map(java -> java.pkg() + "." + java.path().getFileName().toString().replace(".java", "")).sorted().toList()));
    }

    private static String supportPath(JavaSource java) {
        for (var type : java.unit().getTypes()) {
            String path = SUPPORT_CLASSES.get(java.pkg() + "." + type.getNameAsString());
            if (path != null && java.path().getFileName().toString().equals(type.getNameAsString() + ".java")) return path;
        }
        return null;
    }

    private static Map<String, String> expectedSupport(Path destination, List<JavaSource> units,
                                                      Map<String, String> inputs, List<String> notes) throws IOException {
        if (units.stream().noneMatch(java -> supportPath(java) != null)) return Map.of();
        String model = existing(safe(destination, ProjectArtifactRenderer.MODEL));
        if (model == null) {
            notes.add("Studio support templates could not be verified: save/generate the destination model, then analyse again.");
            return Map.of();
        }
        inputs.put(ProjectArtifactRenderer.MODEL, model);
        try {
            var module = ComponentIO.deserializeModuleInstanceString(decode(model), ProjectArtifactRenderer.MODEL);
            return Map.of(
                    SUPPORT_CLASSES.get(DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_PACKAGE + "." + DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_CLASS_NAME), DebugTransitionComponentTemplate.create(module),
                    SUPPORT_CLASSES.get(DeepCopyUtilTemplate.DEEP_COPY_UTIL_PACKAGE + "." + DeepCopyUtilTemplate.DEEP_COPY_UTIL_CLASS_NAME), DeepCopyUtilTemplate.create(module));
        } catch (Exception problem) {
            notes.add("Studio support templates could not be verified against the destination model and installed meta-pack. Regenerate and analyse again; inspect Studio support files separately.");
            return Map.of();
        }
    }

    /** Ignore formatting and comments for classification; comparison retains the exact original bytes. */
    private static boolean sameJava(JavaParser parser, String left, String right) {
        var first = parser.parse(left); var second = parser.parse(right);
        if (!first.isSuccessful() || !second.isSuccessful() || first.getResult().isEmpty() || second.getResult().isEmpty()) return false;
        var printing = new DefaultPrinterConfiguration()
                .removeOption(new DefaultConfigurationOption(
                        ConfigOption.PRINT_COMMENTS))
                .removeOption(new DefaultConfigurationOption(
                        ConfigOption.PRINT_JAVADOC))
                .addOption(new DefaultConfigurationOption(
                        ConfigOption.END_OF_LINE_CHARACTER, "\n"));
        return first.getResult().get().toString(printing).equals(second.getResult().get().toString(printing));
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
        if (poms.contains(rootPom) && Files.isRegularFile(rootPom)) shared.putAll(pom(new String(read(rootPom), StandardCharsets.UTF_8)).getProperties());
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
                boolean importable = !platform && (dependency.getVersion() == null || !dependency.getVersion().contains("${")) && !id.contains("${") && dependency.getSystemPath() == null
                        && dependency.getExclusions().stream().noneMatch(e -> Objects.toString(e.getGroupId(), "").contains("${") || Objects.toString(e.getArtifactId(), "").contains("${"))
                        && (dependency.getScope() == null || Set.of("compile", "runtime", "provided").contains(dependency.getScope()));
                var candidate = new DependencyChoice(id, dependency, importable, relative(source, path)
                        + (platform ? ": platform dependency; retain the selected meta-pack baseline."
                        : importable ? ": original Maven declaration included by default; versions, exclusions, scopes and optional flags are retained. An omitted version uses destination dependency management."
                        : ": unresolved expression, non-portable declaration or unsupported scope; resolve manually."));
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

    public record DependencyComparison(String original, String destination) { }

    /** Compare the selected declaration, including review-only dependencies, without approving an import. */
    public static DependencyComparison compareDependency(Analysis analysis, String id) throws Exception {
        var choice = analysis.dependencies().stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow();
        var target = pom(decode(analysis.originalPom())).getDependencies().stream()
                .filter(d -> coordinate(d).equals(id)).findFirst().orElse(null);
        return new DependencyComparison(dependencyXml(choice.dependency()), dependencyXml(target));
    }

    private static String dependencyXml(Dependency dependency) throws IOException {
        if (dependency == null) return "";
        var model = new Model(); model.setModelVersion("4.0.0"); model.addDependency(dependency.clone());
        var text = new StringWriter(); new MavenXpp3Writer().write(text, model); return text.toString();
    }

    public static String report(Analysis analysis, Set<String> files, Set<String> dependencies) {
        StringBuilder text = new StringBuilder("# Source-assisted module import review\n\n");
        text.append("Original checkout: ").append(analysis.sourceRoot()).append("\n\n");
        text.append("## Module identity\n\nImported module: ").append(analysis.identity().expected())
                .append("\nSource module names: ").append(analysis.identity().names()).append("\nStatus: ").append(analysis.identity().status()).append('\n');
        if (analysis.identity().candidate() != null) {
            text.append("Selected source module: ").append(analysis.identity().candidate().directory()).append('\n');
            analysis.identity().candidate().declarations().forEach(d -> text.append("- ").append(d.file()).append(": module.name = ").append(d.value()).append('\n'));
        }
        text.append("Referenced implementations matched: ").append(analysis.identity().matchedImplementations().size()).append("\n\n");
        text.append("This is a transfer and review record, not evidence that the module compiles or runs. The original checkout is unchanged.\n\n## Selected files\n\n");
        analysis.files().stream().filter(f -> files.contains(f.source())).forEach(f -> text.append("- ").append(f.source()).append(" → ").append(f.destination()).append(f.wiring() ? " — REVIEW ORIGINAL SPRING WIRING" : "").append('\n'));
        text.append("\n## Studio support files\n\n");
        text.append("Studio supplies these exact support classes for the destination meta-pack. Differing originals may contain version changes or customisations. Explicit copies are retained as developer customisations during regeneration.\n");
        analysis.managedFiles().forEach(file -> text.append("- ").append(file.source()).append(" → ").append(file.destination())
                .append(" — ").append(supportDescription(file.status())).append(files.contains(file.source()) ? " — CUSTOMISATION SELECTED" : " — unchanged").append('\n'));
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

    private static String supportDescription(SupportStatus status) {
        return switch (status) {
            case SUPPLIED -> "Supplied by Studio; matches the destination template and original implementation.";
            case ORIGINAL_DIFFERS -> "Supplied by Studio; original differs. Compare for version changes or customisations.";
            case DESTINATION_DIFFERS -> "Destination differs from its template. Review local edits before regenerating.";
            case DESTINATION_MISSING -> "Destination support is missing. Generate the project and analyse again.";
            case UNVERIFIED -> "Destination template could not be verified. Review the saved model and meta-pack, then analyse again.";
        };
    }

    public static Path apply(Analysis analysis, Set<String> files, Set<String> dependencies, boolean replaceExisting) throws Exception {
        return apply(analysis, files, dependencies, replaceExisting, false);
    }

    public static Path apply(Analysis analysis, Set<String> files, Set<String> dependencies, boolean replaceExisting,
                             boolean acceptIdentity) throws Exception {
        return apply(analysis, files, dependencies, replaceExisting, acceptIdentity, Map.of());
    }

    /** Reviewed Java text is staged separately; original bytes remain the stale-source guard. */
    public static Path apply(Analysis analysis, Set<String> files, Set<String> dependencies, boolean replaceExisting,
                             boolean acceptIdentity, Map<String, String> reviewedJava) throws Exception {
        for (var entry : reviewedJava.entrySet()) {
            if (!files.contains(entry.getKey()) || !entry.getKey().endsWith(".java")
                    || importableFiles(analysis).stream().noneMatch(file -> file.source().equals(entry.getKey())))
                throw new IOException("Staged merges must belong to selected Java source files.");
            if (entry.getValue().getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
                throw new IOException("Staged Java file exceeds 2 MiB: " + entry.getKey());
        }
        if (!analysis.sourceRoot().equals(analysis.identity().inspection().sourceRoot())
                || !analysis.destinationRoot().equals(analysis.identity().inspection().destinationRoot()))
            throw new IOException("Module identity review does not belong to this source/destination checkout.");
        SourceCheckoutIdentity.verify(analysis.identity());
        if (analysis.identity().requiresApproval() && !acceptIdentity)
            throw new IOException("Source module identity is mismatched or unverified. Explicitly approve this checkout before importing.");
        Map<String, String> generatedNow = new TreeMap<>();
        for (Path file : inventory(safe(analysis.destinationRoot(), "generated/src/main/java"), null, () -> { }))
            if (file.toString().endsWith(".java")) generatedNow.put(relative(analysis.destinationRoot(), file), encode(read(file)));
        if (!generatedNow.equals(analysis.generatedInputs())) throw new IOException("Generated code changed since review. Analyse again.");
        for (var input : analysis.supportInputs().entrySet())
            if (!Objects.equals(input.getKey().equals(SUPPORT_CUSTOMISATIONS)
                    ? Objects.toString(existing(safe(analysis.destinationRoot(), input.getKey())), "")
                    : existing(safe(analysis.destinationRoot(), input.getKey())), input.getValue()))
                throw new IOException("Destination model changed since support review. Analyse again.");
        for (var support : analysis.managedFiles()) {
            if (!Objects.equals(existing(safe(analysis.sourceRoot(), support.source())), support.bytes())
                    || !Objects.equals(existing(safe(analysis.destinationRoot(), support.destination())), support.current()))
                throw new IOException("Studio support file changed since review. Analyse again: " + support.source());
        }
        Map<Path, String> originals = new LinkedHashMap<>(), writes = new LinkedHashMap<>();
        for (String source : files) {
            var file = importableFiles(analysis).stream().filter(f -> f.source().equals(source)).findFirst().orElseThrow();
            if (!Objects.equals(existing(safe(analysis.sourceRoot(), source)), file.bytes())) throw new IOException("Original source changed since review: " + source);
            Path target = safe(analysis.destinationRoot(), file.destination());
            String output = reviewedJava.containsKey(source) ? encode(reviewedJava.get(source).getBytes(StandardCharsets.UTF_8)) : file.bytes();
            if (writes.putIfAbsent(target, output) != null) throw new IOException("Multiple sources target " + file.destination());
            if (!Objects.equals(file.previous(), existing(target))) throw new IOException("Destination changed since review: " + file.destination());
            if (file.previous() != null && !file.previous().equals(output) && !replaceExisting) throw new IOException("Approve replacement before importing " + file.destination());
            originals.put(target, file.previous());
        }
        var customSupport = analysis.managedFiles().stream().filter(file -> files.contains(file.source())
                && !Objects.equals(writes.get(analysis.destinationRoot().resolve(file.destination())), file.current())).toList();
        if (!customSupport.isEmpty()) {
            Path marker = safe(analysis.destinationRoot(), SUPPORT_CUSTOMISATIONS);
            String previous = existing(marker);
            if (!Objects.equals(Objects.toString(previous, ""), analysis.supportInputs().get(SUPPORT_CUSTOMISATIONS)))
                throw new IOException("Support customisations changed since review. Analyse again.");
            var preserved = supportCustomisations(previous);
            customSupport.forEach(file -> preserved.add(file.destination()));
            originals.put(marker, previous);
            writes.put(marker, encode(("# Explicitly customised support files; retained during regeneration.\n"
                    + String.join("\n", preserved) + "\n").getBytes(StandardCharsets.UTF_8)));
        }
        if (!dependencies.isEmpty()) {
            Path target = safe(analysis.destinationRoot(), "pom.xml");
            if (!Objects.equals(existing(target), analysis.originalPom())) throw new IOException("pom.xml changed since review. Analyse again.");
            originals.put(target, analysis.originalPom()); writes.put(target, encode(pomAfter(analysis, dependencies).getBytes(StandardCharsets.UTF_8)));
        }
        Path report = safe(analysis.destinationRoot(), "user/import-reviews/" + UUID.randomUUID() + ".md");
        originals.put(report, null); writes.put(report, encode((report(analysis, files, dependencies)
                + (analysis.identity().requiresApproval() && acceptIdentity ? "\nDeveloper explicitly approved the mismatched/unverified checkout identity.\n" : "")
                + (reviewedJava.isEmpty() ? "" : "\nStaged Java merges applied: " + new TreeSet<>(reviewedJava.keySet()) + "\n"))
                .getBytes(StandardCharsets.UTF_8)));
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
    static List<Path> inventory(Path root, Path excluded, Runnable checkCancelled) throws IOException {
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
    static String relative(Path root, Path file) { return root.relativize(file).toString().replace('\\', '/'); }
    static byte[] read(Path file) throws IOException {
        if (Files.size(file) > MAX_BYTES) throw new IOException("Source file exceeds 2 MiB: " + file.getFileName());
        return Files.readAllBytes(file);
    }
    private static String existing(Path file) throws IOException { return Files.exists(file, LinkOption.NOFOLLOW_LINKS) ? encode(read(file)) : null; }
    private static String encode(byte[] bytes) { return Base64.getEncoder().encodeToString(bytes); }
    private static String decode(String bytes) { return new String(Base64.getDecoder().decode(bytes), StandardCharsets.UTF_8); }
    static Path safe(Path root, String relative) throws IOException {
        Path base = root.toAbsolutePath().normalize(), path = base.resolve(relative).normalize();
        if (!path.startsWith(base) || Path.of(relative).isAbsolute()) throw new IOException("Import path escapes its project.");
        for (Path part = path; part != null && part.startsWith(base); part = part.getParent())
            if (Files.isSymbolicLink(part)) throw new IOException("Source import does not follow symbolic links: " + part);
        return path;
    }
}
