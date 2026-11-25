package org.ikasan.studio.core.importer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.printer.configuration.DefaultConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration.ConfigOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.PrinterConfiguration;
import java.io.*;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.ikasan.studio.core.generator.DebugTransitionComponentTemplate;
import org.ikasan.studio.core.generator.DeepCopyUtilTemplate;
import org.ikasan.studio.core.generator.ProjectArtifactRenderer;
import org.ikasan.studio.core.importer.SourceCheckoutIdentity.Candidate;
import org.ikasan.studio.core.importer.SourceCheckoutIdentity.Inspection;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Module;

/** Offline, source-assisted import. Never loads application classes, runs Maven, or changes the source checkout. */
public final class SourceModuleImport {
    private SourceModuleImport() { }
    private static final Set<String> SKIP = Set.of(".git", ".idea", "target", "build", ".gradle", "node_modules", "generated");
    private static final int LIMIT = 10_000;
    private static final long MAX_BYTES = 2 * 1024 * 1024;
    private static final String UNSCANNED_PROVIDER = "Copied Spring provider is outside destination component scanning: ";

    public record FileChoice(String source, String destination, String bytes, String previous,
                             boolean recommended, boolean wiring, String reason, boolean identicalToDestination, String originalBytes) {
        public FileChoice(String source, String destination, String bytes, String previous,
                          boolean recommended, boolean wiring, String reason, boolean identicalToDestination) {
            this(source, destination, bytes, previous, recommended, wiring, reason, identicalToDestination, bytes);
        }
        public FileChoice(String source, String destination, String bytes, String previous,
                          boolean recommended, boolean wiring, String reason) {
            this(source, destination, bytes, previous, recommended, wiring, reason,
                    previous != null && Objects.equals(bytes, previous));
        }
    }
    public enum SupportStatus { SUPPLIED, ORIGINAL_DIFFERS, DESTINATION_DIFFERS, DESTINATION_MISSING, UNVERIFIED }
    /** Studio-provided support classes; only explicit reviewed copies become developer customisations. */
    public record ManagedFile(String source, String destination, String bytes, String current, SupportStatus status,
                              boolean identicalToDestination) {
        /** Cached during background analysis; encoded-byte equality retains comments, whitespace and line endings. */
        public ManagedFile(String source, String destination, String bytes, String current, SupportStatus status) {
            this(source, destination, bytes, current, status, current != null && Objects.equals(bytes, current));
        }
    }
    public static final String SUPPORT_CUSTOMISATIONS = "user/import-reviews/support-customisations.txt";
    private static final Map<String, String> SUPPORT_CLASSES = Map.of(
            DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_PACKAGE + "." + DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_CLASS_NAME,
            "user/src/main/java/" + DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_PACKAGE.replace('.', '/') + "/" + DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_CLASS_NAME + ".java",
            DeepCopyUtilTemplate.DEEP_COPY_UTIL_PACKAGE + "." + DeepCopyUtilTemplate.DEEP_COPY_UTIL_CLASS_NAME,
            "user/src/main/java/" + DeepCopyUtilTemplate.DEEP_COPY_UTIL_PACKAGE.replace('.', '/') + "/" + DeepCopyUtilTemplate.DEEP_COPY_UTIL_CLASS_NAME + ".java");
    public record BeanRequirement(String name, String type, String generatedFile, int line,
                                  List<String> sourceDefinitions, boolean generatedProvider) {
        public BeanRequirement(String name, String type, String generatedFile, int line, List<String> sourceDefinitions) {
            this(name, type, generatedFile, line, sourceDefinitions, false);
        }
    }
    public enum ReviewKind { BEAN, SOURCE, RESOURCE, WIRING, DEPENDENCY, SUPPORT_COMPARE, SUPPORT_REPAIR, SCAN }
    public record ReviewAction(ReviewKind kind, String subject, List<String> evidence) { }
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
        Inspection inspection = SourceCheckoutIdentity.inspect(sourceDirectory, destinationDirectory, checkCancelled);
        Candidate candidate = sourceModule == null ? inspection.automaticCandidate()
                : inspection.candidates().stream().filter(c -> c.directory().equals(sourceModule)).findFirst().orElse(null);
        if (candidate == null) throw new IOException("Multiple source modules found. Choose the source module before analysing.");
        Path source = inspection.sourceRoot(), destination = inspection.destinationRoot();
        List<String> notes = new ArrayList<>();
        JavaParser parser = new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));
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
                ParseResult<CompilationUnit> result = parser.parse(new String(bytes, StandardCharsets.UTF_8));
                if (!result.isSuccessful() || result.getResult().isEmpty()) {
                    notes.add("Cannot parse source; review manually: " + relative); continue;
                }
                CompilationUnit unit = result.getResult().get();
                String pkg = unit.getPackageDeclaration().map(PackageDeclaration::getNameAsString).orElse("");
                boolean wiring = unit.getTypes().stream().anyMatch(t -> t.getAnnotations().stream()
                        .anyMatch(a -> Set.of("Configuration", "SpringBootApplication", "SpringBootConfiguration").contains(a.getName().getIdentifier())));
                wiring |= unit.findAll(MethodDeclaration.class).stream().anyMatch(m -> m.getAnnotations().stream()
                        .anyMatch(a -> a.getName().getIdentifier().equals("Bean")));
                JavaSource java = new JavaSource(file, relative, encode(bytes), unit, pkg, wiring);
                units.add(java);
                for (TypeDeclaration<?> type : unit.getTypes()) {
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
        // Include existing explicit user-owned registrations in the review and its stale-input guard.
        Path destinationSources = safe(destination, "user/src/main/java");
        for (Path file : Files.isDirectory(destinationSources, LinkOption.NOFOLLOW_LINKS)
                ? inventory(destinationSources, null, checkCancelled) : List.<Path>of()) {
            checkCancelled.run();
            if (!file.toString().endsWith(".java")) continue;
            byte[] content = read(file);
            ParseResult<CompilationUnit> parsed = parser.parse(new String(content, StandardCharsets.UTF_8));
            if (parsed.isSuccessful() && parsed.getResult().isPresent()
                    && parsed.getResult().get().getTypes().stream().anyMatch(type -> type.getAnnotations().stream()
                            .anyMatch(annotation -> annotation.getName().getIdentifier().equals("Import"))))
                supportInputs.put(relative(destination, file), encode(content));
        }
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
        Map<String, CompilationUnit> generatedUnits = new LinkedHashMap<>();
        for (Path file : inventory(generated, null, checkCancelled)) {
            if (!file.toString().endsWith(".java")) continue;
            byte[] generatedBytes = read(file);
            generatedInputs.put(relative(destination, file), encode(generatedBytes));
            ParseResult<CompilationUnit> result = parser.parse(new String(generatedBytes, StandardCharsets.UTF_8));
            if (!result.isSuccessful() || result.getResult().isEmpty()) throw new IOException("Cannot inspect generated Java: " + relative(destination, file));
            CompilationUnit unit = result.getResult().get();
            generatedUnits.put(relative(destination, file), unit);
            // Factories can construct an implementation directly rather than inject it through @Resource.
            for (String reference : references(unit, unit, classes.keySet())) {
                JavaSource found = classes.get(reference);
                if (found != null && !found.wiring()) recommended.add(found);
            }
            for (FieldDeclaration field : unit.findAll(FieldDeclaration.class)) {
                Optional<AnnotationExpr> resource = field.getAnnotations().stream().filter(a -> a.getName().getIdentifier().equals("Resource")).findFirst();
                if (resource.isEmpty()) continue;
                for (VariableDeclarator variable : field.getVariables()) {
                    String type = resolve(variable.getType().asString(), unit, classes.keySet());
                    if (type.startsWith("org.ikasan.builder.")) continue;
                    String name = variable.getNameAsString();
                    if (resource.get().isNormalAnnotationExpr()) {
                        for (MemberValuePair pair : resource.get().asNormalAnnotationExpr().getPairs())
                            if (pair.getNameAsString().equals("name") && pair.getValue().isStringLiteralExpr()) name = pair.getValue().asStringLiteralExpr().asString();
                    }
                    JavaSource found = classes.get(type);
                    if (found != null && !found.wiring()) recommended.add(found);
                    List<String> evidence = new ArrayList<>();
                    for (JavaSource original : units) {
                        checkCancelled.run();
                        for (MethodDeclaration method : original.unit().findAll(MethodDeclaration.class)) {
                            Optional<AnnotationExpr> bean = method.getAnnotations().stream().filter(a -> a.getName().getIdentifier().equals("Bean")).findFirst();
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
        Set<String> knownTypes = new HashSet<>(classes.keySet());
        generatedUnits.values().forEach(unit -> unit.getTypes().forEach(type -> knownTypes.add(
                unit.getPackageDeclaration().map(pkg -> pkg.getNameAsString() + ".").orElse("") + type.getNameAsString())));
        List<GeneratedProvider> providers = declaredProviders(generatedUnits, knownTypes);
        beans = beans.stream().map(bean -> {
            String type = resolve(bean.type(), generatedUnits.get(bean.generatedFile()), knownTypes);
            boolean supplied = provided(providers, new BeanRequirement(bean.name(), type, bean.generatedFile(), bean.line(), bean.sourceDefinitions()));
            if (!supplied && !classes.containsKey(type)) {
                String contract = type;
                List<String> compatible = classes.keySet().stream().filter(name -> concreteSourceType(name, classes)
                        && sourceAssignableTo(name, contract, classes, new HashSet<>())).toList();
                // Never choose among multiple implementations of the same injected contract.
                if (compatible.size() == 1) {
                    type = compatible.get(0); recommended.add(classes.get(type));
                }
            }
            return new BeanRequirement(bean.name(), type, bean.generatedFile(), bean.line(), bean.sourceDefinitions(), supplied);
        }).toList();
        // Conservative source closure: explicit imports, wildcard imports used in the AST, and same-package types.
        Deque<JavaSource> pending = new ArrayDeque<>(recommended);
        while (!pending.isEmpty()) {
            checkCancelled.run();
            JavaSource original = pending.removeFirst();
            for (String reference : references(original.unit(), original.unit(), classes.keySet())) {
                JavaSource dependency = classes.get(reference);
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
            boolean debugSubclass = java.unit().getTypes().stream().filter(TypeDeclaration::isClassOrInterfaceDeclaration)
                    .map(TypeDeclaration::asClassOrInterfaceDeclaration)
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
        for (JavaSource java : recommended) for (AnnotationExpr annotation : java.unit().findAll(AnnotationExpr.class)) {
            if (!annotation.getName().getIdentifier().equals("PropertySource")) continue;
            for (StringLiteralExpr literal : annotation.findAll(StringLiteralExpr.class)) {
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
        Path runtimeProperties = safe(destination, "generated/src/main/resources/application.properties");
        if (Files.isRegularFile(runtimeProperties, LinkOption.NOFOLLOW_LINKS))
            supportInputs.put(relative(destination, runtimeProperties), encode(read(runtimeProperties)));
        addTrustedContextFactories(source, destination, inspection, candidate, parser, choices, generatedInputs, notes);
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

    /** Keep the reflected binary name without duplicating its enclosing Spring configuration. */
    private static void addTrustedContextFactories(Path source, Path destination,
            SourceCheckoutIdentity.Inspection inspection, SourceCheckoutIdentity.Candidate candidate,
            JavaParser parser, List<FileChoice> choices, Map<String, String> generatedInputs, List<String> notes) throws IOException {
        Path properties = safe(destination, "generated/src/main/resources/application.properties");
        if (!Files.isRegularFile(properties, LinkOption.NOFOLLOW_LINKS)) return;
        Matcher names = Pattern.compile("org\\.ikasan\\.studio\\.boot\\.flow\\.[A-Za-z0-9_]+\\.ComponentFactory[A-Za-z0-9_]+\\$Trusted[A-Za-z0-9_]+ContextFactory")
                .matcher(new String(read(properties), StandardCharsets.ISO_8859_1));
        Set<String> seen = new HashSet<>();
        while (names.find()) {
            String binary = names.group();
            if (!seen.add(binary)) continue;
            int dollar = binary.indexOf('$'), dot = binary.lastIndexOf('.', dollar);
            String outer = binary.substring(0, dollar), nested = binary.substring(dollar + 1);
            String outerPath = "generated/src/main/java/" + outer.replace('.', '/') + ".java";
            String destinationJava = generatedInputs.get(outerPath);
            if (destinationJava != null && parser.parse(decode(destinationJava)).getResult().stream()
                    .flatMap(u -> u.findAll(ClassOrInterfaceDeclaration.class).stream())
                    .anyMatch(t -> t.getNameAsString().equals(nested))) continue;
            String target = "user/src/main/java/" + binary.replace('.', '/') + ".java";
            ArrayList<Path> matches = new ArrayList<>();
            for (String owner : candidate.ownedPomDirectories()) {
                Path file = safe(source, (owner.isBlank() ? "" : owner + "/") + "src/main/java/" + outer.replace('.', '/') + ".java");
                if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) && inspection.includes(candidate, file)) matches.add(file);
            }
            // Generated Maven modules are deliberately excluded from ordinary source transfer.
            // Inspect only the selected application's exact factory path for this narrow exception.
            Path generatedOriginal = safe(source, (candidate.directory().isBlank() ? "" : candidate.directory() + "/") + outerPath);
            if (Files.isRegularFile(generatedOriginal, LinkOption.NOFOLLOW_LINKS) && !matches.contains(generatedOriginal))
                matches.add(generatedOriginal);
            if (matches.size() == 1) {
                Path file = matches.get(0);
                byte[] original = read(file);
                String sourcePath = relative(source, file);
                CompilationUnit unit = parser.parse(new String(original, StandardCharsets.UTF_8)).getResult().orElse(null);
                if (unit != null) {
                    ClassOrInterfaceDeclaration helper = unit.findAll(ClassOrInterfaceDeclaration.class).stream()
                            .filter(t -> t.getNameAsString().equals(nested) && t.isPublic() && t.isStatic()
                                    && t.getParentNode().filter(n -> n instanceof TypeDeclaration<?>).isPresent()
                                    && t.getExtendedTypes().size() == 1
                                    && resolve(t.getExtendedTypes().get(0).getNameWithScope(), unit, Set.of())
                                            .equals("org.apache.activemq.jndi.ActiveMQInitialContextFactory"))
                            .findFirst().orElse(null);
                    if (helper != null && helper.getConstructors().isEmpty() && helper.getFields().isEmpty()
                            && helper.getMethods().size() == 3
                            && unit.findAll(ClassOrInterfaceDeclaration.class).stream()
                                    .filter(t -> t.getNameAsString().startsWith("Trusted") && t.getNameAsString().endsWith("ContextFactory")).count() == 1
                            && choices.stream().noneMatch(choice -> choice.source().equals(sourcePath))
                            && helper.findAll(NameExpr.class).stream().noneMatch(n -> n.getNameAsString().equals(outer.substring(dot + 1)))
                            && helper.getMethods().stream().map(MethodDeclaration::getNameAsString).collect(Collectors.toSet())
                                    .equals(Set.of("initialFactoryName", "configure", "createConnectionFactory"))) {
                        ClassOrInterfaceDeclaration copy = helper.clone();
                        String simple = binary.substring(dot + 1);
                        copy.setStatic(false); copy.setName(simple);
                        copy.findAll(SimpleName.class).stream()
                                .filter(n -> n.asString().equals(nested)).forEach(n -> n.setIdentifier(simple));
                        CompilationUnit output = new CompilationUnit(); output.setPackageDeclaration(binary.substring(0, dot));
                        Set<String> used = copy.findAll(SimpleName.class).stream()
                                .map(SimpleName::asString).collect(Collectors.toSet());
                        unit.getImports().stream().filter(i -> i.isAsterisk() || used.contains(i.getName().getIdentifier()))
                                .forEach(i -> output.addImport(i.clone()));
                        output.addType(copy);
                        String bytes = encode(output.toString().getBytes(StandardCharsets.UTF_8));
                        String previous = existing(safe(destination, target));
                        choices.add(new FileChoice(relative(source, file), target, bytes, previous, true, false,
                                "Extracted JMS trusted-package context factory. The $ in the Java class name preserves the runtime binary name; "
                                        + "only this helper is copied, not its enclosing Spring configuration.",
                                Objects.equals(bytes, previous), encode(original)));
                        continue;
                    }
                }
            }
            notes.add("Missing runtime context factory: " + binary
                    + ". Its trusted-package configuration cannot be recovered automatically; review the original JMS provider source before running.");
        }
    }

    private static String supportPath(JavaSource java) {
        for (TypeDeclaration<?> type : java.unit().getTypes()) {
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
            Module module = ComponentIO.deserializeModuleInstanceString(decode(model), ProjectArtifactRenderer.MODEL);
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
        ParseResult<CompilationUnit> first = parser.parse(left); ParseResult<CompilationUnit> second = parser.parse(right);
        if (!first.isSuccessful() || !second.isSuccessful() || first.getResult().isEmpty() || second.getResult().isEmpty()) return false;
        PrinterConfiguration printing = new DefaultPrinterConfiguration()
                .removeOption(new DefaultConfigurationOption(
                        ConfigOption.PRINT_COMMENTS))
                .removeOption(new DefaultConfigurationOption(
                        ConfigOption.PRINT_JAVADOC))
                .addOption(new DefaultConfigurationOption(
                        ConfigOption.END_OF_LINE_CHARACTER, "\n"));
        return first.getResult().get().toString(printing).equals(second.getResult().get().toString(printing));
    }

    private static boolean concreteSourceType(String name, Map<String, JavaSource> classes) {
        JavaSource source = classes.get(name);
        if (source == null) return false;
        return source.unit().getTypes().stream().filter(type -> (source.pkg().isEmpty() ? "" : source.pkg() + ".")
                        .concat(type.getNameAsString()).equals(name))
                .filter(TypeDeclaration::isClassOrInterfaceDeclaration)
                .map(TypeDeclaration::asClassOrInterfaceDeclaration)
                .anyMatch(type -> !type.isInterface() && !type.isAbstract() && type.isPublic());
    }

    private static boolean sourceAssignableTo(String name, String contract, Map<String, JavaSource> classes, Set<String> seen) {
        if (name.equals(contract)) return true;
        if (!seen.add(name) || !classes.containsKey(name)) return false;
        JavaSource source = classes.get(name);
        for (TypeDeclaration<?> type : source.unit().getTypes()) {
            if (!((source.pkg().isEmpty() ? "" : source.pkg() + ".") + type.getNameAsString()).equals(name)
                    || !type.isClassOrInterfaceDeclaration()) continue;
            ClassOrInterfaceDeclaration declaration = type.asClassOrInterfaceDeclaration();
            for (ClassOrInterfaceType parent : Stream.concat(declaration.getImplementedTypes().stream(), declaration.getExtendedTypes().stream()).toList())
                if (sourceAssignableTo(resolve(parent.getNameWithScope(), source.unit(), classes.keySet()), contract, classes, seen)) return true;
        }
        return false;
    }

    private static Set<String> references(Node node, CompilationUnit unit, Set<String> names) {
        Set<String> result = new TreeSet<>();
        for (ClassOrInterfaceType type : node.findAll(ClassOrInterfaceType.class)) result.add(resolve(type.getNameWithScope(), unit, names));
        for (NameExpr name : node.findAll(NameExpr.class)) result.add(resolve(name.getNameAsString(), unit, names));
        // Static imports may reference local helper classes without a type expression.
        unit.getImports().stream().filter(ImportDeclaration::isStatic).forEach(i -> {
            String name = i.getNameAsString();
            while (name.contains(".")) { if (names.contains(name)) result.add(name); name = name.substring(0, name.lastIndexOf('.')); }
        });
        return result;
    }

    private static String resolve(String type, CompilationUnit unit, Set<String> names) {
        type = type.replaceAll("<.*>", "").replace("[]", "");
        if (names.contains(type) || type.contains(".") && Character.isLowerCase(type.charAt(0))) return type;
        for (ImportDeclaration imported : unit.getImports()) {
            if (imported.isStatic()) continue;
            String name = imported.getNameAsString();
            if (!imported.isAsterisk() && name.endsWith("." + type)) return name;
        }
        String local = unit.getPackageDeclaration().map(p -> p.getNameAsString() + ".").orElse("") + type;
        if (names.contains(local)) return local;
        List<String> wildcard = new ArrayList<>();
        for (ImportDeclaration imported : unit.getImports()) if (imported.isAsterisk() && !imported.isStatic()) {
            String candidate = imported.getNameAsString() + "." + type;
            if (names.contains(candidate)) wildcard.add(candidate);
        }
        return wildcard.size() == 1 ? wildcard.get(0) : type;
    }

    private static List<DependencyChoice> dependencies(Path source, List<Path> poms, String destinationPom, List<String> notes) throws Exception {
        Model target = pom(decode(destinationPom));
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
                for (Exclusion exclusion : dependency.getExclusions()) {
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
                DependencyChoice candidate = new DependencyChoice(id, dependency, importable, relative(source, path)
                        + (platform ? ": platform dependency; retain the selected meta-pack baseline."
                        : importable ? ": original Maven declaration included by default; versions, exclusions, scopes and optional flags are retained. An omitted version uses destination dependency management."
                        : ": unresolved expression, non-portable declaration or unsupported scope; resolve manually."));
                DependencyChoice previous = candidates.putIfAbsent(id, candidate);
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
            Matcher matcher = Pattern.compile("\\$\\{([^}]+)}").matcher(value);
            String expanded = matcher.replaceAll(m -> Matcher.quoteReplacement(values.getProperty(m.group(1), m.group())));
            if (expanded.equals(value)) break;
            value = expanded;
        }
        return value;
    }
    private static String coordinate(Dependency d) { return d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getType() + ":" + Objects.toString(d.getClassifier(), ""); }
    private static Model pom(String text) throws Exception { return new MavenXpp3Reader().read(new StringReader(text)); }

    public static String pomAfter(Analysis analysis, Set<String> dependencies) throws Exception {
        if (dependencies.isEmpty()) return decode(analysis.originalPom());
        Model model = pom(decode(analysis.originalPom()));
        for (String id : new TreeSet<>(dependencies)) {
            DependencyChoice choice = analysis.dependencies().stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow();
            if (!choice.importable()) throw new IOException("Resolve dependency manually: " + id);
            model.addDependency(choice.dependency().clone());
        }
        StringWriter writer = new StringWriter(); new MavenXpp3Writer().write(writer, model); return writer.toString();
    }

    public record DependencyComparison(String original, String destination) { }

    /** Compare the selected declaration, including review-only dependencies, without approving an import. */
    public static DependencyComparison compareDependency(Analysis analysis, String id) throws Exception {
        DependencyChoice choice = analysis.dependencies().stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow();
        Dependency target = pom(decode(analysis.originalPom())).getDependencies().stream()
                .filter(d -> coordinate(d).equals(id)).findFirst().orElse(null);
        return new DependencyComparison(dependencyXml(choice.dependency()), dependencyXml(target));
    }

    private static String dependencyXml(Dependency dependency) throws IOException {
        if (dependency == null) return "";
        Model model = new Model(); model.setModelVersion("4.0.0"); model.addDependency(dependency.clone());
        StringWriter text = new StringWriter(); new MavenXpp3Writer().write(text, model); return text.toString();
    }

    private record GeneratedProvider(String name, String type, boolean directImport, boolean explicitName) { }

    private static List<GeneratedProvider> declaredProviders(Map<String, CompilationUnit> units, Set<String> knownTypes) {
        List<GeneratedProvider> providers = new ArrayList<>();
        for (CompilationUnit unit : units.values()) for (TypeDeclaration<?> type : unit.getTypes()) {
            // Conditional providers require review rather than an assumption that their profile is active.
            if (type.getAnnotations().stream().anyMatch(a -> (a.getName().getIdentifier().equals("Profile") || a.getName().getIdentifier().startsWith("Conditional")))) continue;
            Optional<AnnotationExpr> component = type.getAnnotations().stream().filter(a -> Set.of("Component", "Configuration", "Service", "Repository", "Controller", "RestController")
                    .contains(a.getName().getIdentifier())).findFirst();
            if (component.isEmpty()) continue;
            String qualified = unit.getPackageDeclaration().map(pkg -> pkg.getNameAsString() + ".").orElse("") + type.getNameAsString();
            String simpleName = type.getNameAsString();
            String defaultName = simpleName.length() > 1 && Character.isUpperCase(simpleName.charAt(0)) && Character.isUpperCase(simpleName.charAt(1))
                    ? simpleName : Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
            boolean directImport = type.isClassOrInterfaceDeclaration()
                    && type.asClassOrInterfaceDeclaration().isPublic()
                    && !type.asClassOrInterfaceDeclaration().isInterface()
                    && !type.asClassOrInterfaceDeclaration().isAbstract()
                    && !component.get().getName().getIdentifier().equals("Configuration");
            for (String name : beanNames(component.get(), defaultName)) providers.add(new GeneratedProvider(name, qualified, directImport, !name.isBlank() && beanNames(component.get(), "").contains(name)));
            for (MethodDeclaration method : type.getMethods()) {
                if (method.getAnnotations().stream().anyMatch(a -> (a.getName().getIdentifier().equals("Profile") || a.getName().getIdentifier().startsWith("Conditional")))) continue;
                Optional<AnnotationExpr> bean = method.getAnnotations().stream().filter(a -> a.getName().getIdentifier().equals("Bean")).findFirst();
                if (bean.isPresent()) for (String name : beanNames(bean.get(), method.getNameAsString()))
                    providers.add(new GeneratedProvider(name, resolve(method.getType().asString(), unit, knownTypes), false, false));
            }
        }
        return providers;
    }

    private static List<String> beanNames(AnnotationExpr annotation, String fallback) {
        List<Expression> values = new ArrayList<>();
        if (annotation.isSingleMemberAnnotationExpr()) values.add(annotation.asSingleMemberAnnotationExpr().getMemberValue());
        if (annotation.isNormalAnnotationExpr()) annotation.asNormalAnnotationExpr().getPairs().stream()
                .filter(pair -> Set.of("value", "name").contains(pair.getNameAsString())).forEach(pair -> values.add(pair.getValue()));
        if (values.isEmpty()) return List.of(fallback);
        List<String> names = new ArrayList<>();
        for (Expression value : values) {
            if (value.isStringLiteralExpr() && !value.asStringLiteralExpr().asString().isBlank()) names.add(value.asStringLiteralExpr().asString());
            else if (value.isArrayInitializerExpr()) for (Expression item : value.asArrayInitializerExpr().getValues())
                if (item.isStringLiteralExpr() && !item.asStringLiteralExpr().asString().isBlank()) names.add(item.asStringLiteralExpr().asString());
        }
        return names;
    }

    private static boolean provided(List<GeneratedProvider> providers, BeanRequirement bean) {
        List<GeneratedProvider> named = providers.stream().filter(provider -> provider.name().equals(bean.name())).toList();
        if (!named.isEmpty()) return named.size() == 1 && named.get(0).type().equals(bean.type());
        return providers.stream().filter(provider -> provider.type().equals(bean.type())).count() == 1;
    }

    private static List<GeneratedProvider> reviewedProviders(Analysis analysis, Set<String> files, Map<String, String> reviewedJava) {
        return reviewedProviders(analysis, files, reviewedJava, true);
    }

    private static List<GeneratedProvider> reviewedProviders(Analysis analysis, Set<String> files,
                                                            Map<String, String> reviewedJava, boolean requireScannedPackage) {
        JavaParser parser = new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));
        Set<String> scanned = new HashSet<>();
        for (String encoded : analysis.generatedInputs().values()) {
            if (Thread.currentThread().isInterrupted()) throw new CancellationException();
            ParseResult<CompilationUnit> parsed = parser.parse(decode(encoded));
            if (!parsed.isSuccessful() || parsed.getResult().isEmpty()) continue;
            CompilationUnit unit = parsed.getResult().get();
            for (TypeDeclaration<?> type : unit.getTypes()) {
                if (type.getAnnotations().stream().anyMatch(a -> a.getName().getIdentifier().equals("Profile") || a.getName().getIdentifier().startsWith("Conditional"))) continue;
                boolean explicitScan = type.getAnnotations().stream().anyMatch(a -> a.getName().getIdentifier().equals("ComponentScan"));
                for (AnnotationExpr annotation : type.getAnnotations()) {
                    String name = annotation.getName().getIdentifier();
                    if (name.equals("ComponentScan")) {
                        if (annotation.isNormalAnnotationExpr() && annotation.asNormalAnnotationExpr().getPairs().stream()
                                .anyMatch(pair -> pair.getNameAsString().equals("excludeFilters"))) continue;
                        if (annotation.isSingleMemberAnnotationExpr()) collectLiteralPackages(annotation.asSingleMemberAnnotationExpr().getMemberValue(), scanned);
                        if (annotation.isNormalAnnotationExpr()) annotation.asNormalAnnotationExpr().getPairs().stream()
                                .filter(pair -> Set.of("value", "basePackages").contains(pair.getNameAsString()))
                                .forEach(pair -> collectLiteralPackages(pair.getValue(), scanned));
                    } else if (name.equals("SpringBootApplication")) {
                        boolean customScan = annotation.isNormalAnnotationExpr() && annotation.asNormalAnnotationExpr().getPairs().stream()
                                .anyMatch(pair -> Set.of("scanBasePackages", "scanBasePackageClasses", "exclude", "excludeName").contains(pair.getNameAsString()));
                        if (!explicitScan && !customScan) unit.getPackageDeclaration().ifPresent(pkg -> scanned.add(pkg.getNameAsString()));
                        if (annotation.isNormalAnnotationExpr()) annotation.asNormalAnnotationExpr().getPairs().stream()
                                .filter(pair -> pair.getNameAsString().equals("scanBasePackages"))
                                .forEach(pair -> collectLiteralPackages(pair.getValue(), scanned));
                    }
                }
            }
        }
        Set<String> explicitlyImported = new HashSet<>();
        List<String> registrationInputs = new ArrayList<>(analysis.generatedInputs().values());
        analysis.supportInputs().entrySet().stream().filter(entry -> entry.getKey().endsWith(".java"))
                .map(Map.Entry::getValue).forEach(registrationInputs::add);
        for (String encoded : registrationInputs) {
            ParseResult<CompilationUnit> parsed = parser.parse(decode(encoded));
            if (!parsed.isSuccessful() || parsed.getResult().isEmpty()) continue;
            CompilationUnit unit = parsed.getResult().get();
            String pkg = unit.getPackageDeclaration().map(PackageDeclaration::getNameAsString).orElse("");
            if (scanned.stream().noneMatch(base -> pkg.equals(base) || pkg.startsWith(base + "."))) continue;
            for (TypeDeclaration<?> type : unit.getTypes()) {
                if (type.getAnnotations().stream().noneMatch(annotation -> Set.of("Component", "Configuration", "Service",
                        "Repository", "Controller", "RestController", "SpringBootApplication")
                        .contains(annotation.getName().getIdentifier()))) continue;
                if (type.getAnnotations().stream().anyMatch(annotation -> annotation.getName().getIdentifier().equals("Profile")
                        || annotation.getName().getIdentifier().startsWith("Conditional"))) continue;
                type.getAnnotations().stream().filter(annotation -> annotation.getName().getIdentifier().equals("Import"))
                        .flatMap(annotation -> annotation.findAll(ClassExpr.class).stream())
                        .map(literal -> resolve(literal.getType().asString(), unit, Set.of())).forEach(explicitlyImported::add);
            }
        }
        Map<String, CompilationUnit> userUnits = new LinkedHashMap<>();
        for (FileChoice file : analysis.files()) {
            if (Thread.currentThread().isInterrupted()) throw new CancellationException();
            if (!file.source().endsWith(".java")) continue;
            String text = files.contains(file.source()) ? reviewedJava.getOrDefault(file.source(), decode(file.bytes()))
                    : file.previous() == null ? null : decode(file.previous());
            if (text == null) continue;
            ParseResult<CompilationUnit> parsed = parser.parse(text);
            if (!parsed.isSuccessful() || parsed.getResult().isEmpty()) continue;
            CompilationUnit unit = parsed.getResult().get();
            String pkg = unit.getPackageDeclaration().map(PackageDeclaration::getNameAsString).orElse("");
            if (!requireScannedPackage || scanned.stream().anyMatch(base -> pkg.equals(base) || pkg.startsWith(base + "."))) {
                userUnits.put(file.destination(), unit);
            } else {
                CompilationUnit importedUnit = unit.clone();
                importedUnit.getTypes().removeIf(type -> !explicitlyImported.contains(pkg + "." + type.getNameAsString()));
                if (!importedUnit.getTypes().isEmpty()) userUnits.put(file.destination(), importedUnit);
            }
        }
        Set<String> known = new HashSet<>();
        userUnits.values().forEach(unit -> unit.getTypes().forEach(type -> known.add(
                unit.getPackageDeclaration().map(pkg -> pkg.getNameAsString() + ".").orElse("") + type.getNameAsString())));
        return declaredProviders(userUnits, known);
    }

    private static void collectLiteralPackages(Expression value, Set<String> packages) {
        if (value.isStringLiteralExpr()) {
            String pkg = value.asStringLiteralExpr().asString();
            if (!pkg.isBlank() && !pkg.contains("${") && !pkg.contains("#{")) packages.add(pkg);
        } else if (value.isArrayInitializerExpr()) value.asArrayInitializerExpr().getValues().forEach(item -> collectLiteralPackages(item, packages));
    }

    /** Only concrete decisions for the live UI; the complete audit remains in report(). */
    public static List<ReviewAction> reviewActions(Analysis analysis, Set<String> files, Set<String> dependencies) {
        return reviewActions(analysis, files, dependencies, Map.of());
    }

    /** AST inspection belongs on a worker thread, including when reviewing staged diff contents. */
    public static List<ReviewAction> reviewActions(Analysis analysis, Set<String> files, Set<String> dependencies,
                                                  Map<String, String> reviewedJava) {
        List<GeneratedProvider> userProviders = reviewedProviders(analysis, files, reviewedJava);
        List<GeneratedProvider> availableProviders = reviewedProviders(analysis, files, reviewedJava, false);
        Set<String> scanWarnings = new HashSet<>();
        List<ReviewAction> actions = new ArrayList<>();
        for (BeanRequirement bean : analysis.beans()) if (!bean.generatedProvider() && !provided(userProviders, bean)) {
            List<String> evidence = new ArrayList<>();
            evidence.add(bean.generatedFile() + ":" + bean.line()); evidence.addAll(bean.sourceDefinitions());
            actions.add(new ReviewAction(ReviewKind.BEAN, bean.name() + " : " + bean.type(), List.copyOf(evidence)));
            if (provided(availableProviders, bean) && scanWarnings.add(bean.type())) {
                actions.add(new ReviewAction(ReviewKind.SCAN,
                        UNSCANNED_PROVIDER + bean.type(),
                        List.of("Copying this source is not sufficient to register the bean. Add a user-owned @Configuration in a scanned package with @Import for the copied component or its configuration, then rebuild.",
                                bean.generatedFile() + ":" + bean.line())));
            }
        }
        Set<String> referencedPaths = new HashSet<>();
        analysis.identity().matchedImplementations().forEach(type -> referencedPaths.add("user/src/main/java/" + type.replace('.', '/') + ".java"));
        for (FileChoice file : analysis.files()) {
            if (file.wiring() && files.contains(file.source()))
                actions.add(new ReviewAction(ReviewKind.WIRING, file.destination(), List.of()));
            else if (!files.contains(file.source()) && file.recommended() && !file.source().endsWith(".java")
                    && !Objects.equals(file.bytes(), file.previous()))
                actions.add(new ReviewAction(ReviewKind.RESOURCE, file.destination(), List.of()));
            else if (!files.contains(file.source()) && (referencedPaths.contains(file.destination()) || !file.originalBytes().equals(file.bytes()))
                    && !Objects.equals(file.bytes(), file.previous()))
                actions.add(new ReviewAction(ReviewKind.SOURCE, file.destination(), List.of()));
        }
        for (DependencyChoice dependency : analysis.dependencies()) {
            if (dependencies.contains(dependency.id())) continue;
            String group = Objects.toString(dependency.dependency().getGroupId(), "");
            if (group.startsWith("org.ikasan") || group.startsWith("org.springframework")) continue;
            actions.add(new ReviewAction(ReviewKind.DEPENDENCY, dependency.id(),
                    dependency.importable() ? List.of() : List.of(dependency.reason())));
        }
        for (ManagedFile support : analysis.managedFiles()) {
            if (support.status() == SupportStatus.UNVERIFIED || support.status() == SupportStatus.DESTINATION_MISSING)
                actions.add(new ReviewAction(ReviewKind.SUPPORT_REPAIR, support.destination(), List.of()));
            else if (!files.contains(support.source()) && support.status() == SupportStatus.ORIGINAL_DIFFERS)
                actions.add(new ReviewAction(ReviewKind.SUPPORT_COMPARE, support.destination(), List.of()));
        }
        for (String note : analysis.notes()) if (note.startsWith("Cannot parse source;") || note.startsWith("Ambiguous class in multiple source modules;")
                || note.startsWith("Cannot inspect Maven declarations:") || note.startsWith("Missing runtime context factory:"))
            actions.add(new ReviewAction(ReviewKind.SCAN, note, List.of()));
        return List.copyOf(actions);
    }

    public static String report(Analysis analysis, Set<String> files, Set<String> dependencies) {
        return report(analysis, files, dependencies, null);
    }

    private static String report(Analysis analysis, Set<String> files, Set<String> dependencies, SpringRegistration registration) {
        StringBuilder text = new StringBuilder("# Source-assisted module import review\n\n");
        text.append("Original checkout: ").append(analysis.sourceRoot()).append("\n\n");
        text.append("## Module identity\n\nImported module: ").append(analysis.identity().expected())
                .append("\nSource module names: ").append(analysis.identity().names()).append("\nStatus: ").append(analysis.identity().status()).append('\n');
        if (analysis.identity().candidate() != null) {
            text.append("Selected source module: ").append(analysis.identity().candidate().directory()).append('\n');
            analysis.identity().candidate().declarations().forEach(d -> text.append("- ").append(d.file()).append(": module.name = ").append(d.value()).append('\n'));
        }
        text.append("Referenced implementations matched: ").append(analysis.identity().matchedImplementations().size()).append("\n\n");
        text.append("## Selected files\n\n");
        analysis.files().stream().filter(f -> files.contains(f.source())).forEach(f -> text.append("- ").append(f.source()).append(" → ").append(f.destination()).append(f.wiring() ? " — REVIEW ORIGINAL SPRING WIRING" : "").append('\n'));
        text.append("\n## Studio support files\n\n");
        text.append("Studio supplies these exact support classes for the destination meta-pack. Differing originals may contain version changes or customisations. Explicit copies are retained as developer customisations during regeneration.\n");
        analysis.managedFiles().forEach(file -> text.append("- ").append(file.source()).append(" → ").append(file.destination())
                .append(" — ").append(supportDescription(file.status())).append(files.contains(file.source()) ? " — CUSTOMISATION SELECTED" : " — unchanged").append('\n'));
        text.append("\n## Generated bean contracts\n\n");
        for (BeanRequirement bean : analysis.beans()) {
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
        reviewActions(analysis, files, dependencies).stream().filter(action -> action.kind() == ReviewKind.SCAN)
                .filter(action -> requiresReviewAfterRegistration(registration, action))
                .forEach(action -> { text.append("- ").append(action.subject()).append('\n');
                    action.evidence().forEach(item -> text.append("  ").append(item).append('\n')); });
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

    public record SpringRegistration(String destination, String javaSource, List<String> components) { }

    private static boolean hasExplicitResourceName(Analysis analysis, BeanRequirement bean) {
        String generated = analysis.generatedInputs().get(bean.generatedFile());
        if (generated == null) return false;
        ParseResult<CompilationUnit> parsed = new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE))
                .parse(decode(generated));
        return parsed.getResult().isPresent() && parsed.getResult().get()
                .findAll(AnnotationExpr.class).stream()
                .filter(annotation -> annotation.getName().getIdentifier().equals("Resource") && annotation.isNormalAnnotationExpr())
                .flatMap(annotation -> annotation.asNormalAnnotationExpr().getPairs().stream())
                .anyMatch(pair -> pair.getNameAsString().equals("name") && pair.getValue().isStringLiteralExpr()
                        && pair.getValue().asStringLiteralExpr().asString().equals(bean.name()));
    }

    public static boolean requiresReviewAfterRegistration(SpringRegistration registration, ReviewAction action) {
        return registration == null || action.kind() != ReviewKind.SCAN
                || registration.components().stream().noneMatch(type -> action.subject().equals(UNSCANNED_PROVIDER + type));
    }

    /** A new, explicit registration for selected stereotypes; never invent factories for plain Java classes. */
    public static SpringRegistration previewSpringRegistration(Analysis analysis, Set<String> files,
                                                               Map<String, String> reviewedJava) throws IOException {
        List<GeneratedProvider> scanned = reviewedProviders(analysis, files, reviewedJava);
        List<GeneratedProvider> available = reviewedProviders(analysis, files, reviewedJava, false);
        Set<String> components = new TreeSet<>();
        for (BeanRequirement bean : analysis.beans()) {
            if (bean.generatedProvider() || provided(scanned, bean) || !provided(available, bean)) continue;
            // Distinct bean identities of the same class may represent different configured instances.
            // Do not collapse them into one imported singleton.
            if (analysis.beans().stream().filter(required -> !required.generatedProvider() && required.type().equals(bean.type()))
                    .map(BeanRequirement::name).distinct().count() > 1) continue;
            if (available.stream().noneMatch(provider -> provider.directImport() && provider.type().equals(bean.type()))) continue;
            // @Resource(name=...) does not fall back to lookup by type. For an unnamed
            // stereotype Spring's import name is the fully qualified class name, not its scan default.
            if (hasExplicitResourceName(analysis, bean) && available.stream().noneMatch(provider ->
                    provider.directImport() && provider.type().equals(bean.type())
                            && (provider.explicitName() ? provider.name() : provider.type()).equals(bean.name()))) continue;
            components.add(bean.type());
        }
        if (components.isEmpty()) return null;
        // The released Studio applications explicitly scan this namespace. Keep the new configuration
        // under user/ and import only reviewed component classes, rather than scanning the original application.
        String pkg = "org.ikasan.studio.boot.imported";
        JavaParser parser = new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));
        boolean discovered = false;
        for (String encoded : analysis.generatedInputs().values()) {
            ParseResult<CompilationUnit> parsed = parser.parse(decode(encoded));
            if (!parsed.isSuccessful() || parsed.getResult().isEmpty()) continue;
            CompilationUnit unit = parsed.getResult().get();
            for (AnnotationExpr annotation : unit.findAll(AnnotationExpr.class)) {
                Set<String> packages = new HashSet<>();
                if (annotation.getName().getIdentifier().equals("ComponentScan")) {
                    if (annotation.getParentNode().orElse(null) instanceof TypeDeclaration<?> owner
                            && owner.getAnnotations().stream().anyMatch(value -> value.getName().getIdentifier().equals("Profile")
                                    || value.getName().getIdentifier().startsWith("Conditional"))) continue;
                    if (annotation.isNormalAnnotationExpr() && annotation.asNormalAnnotationExpr().getPairs().stream()
                            .anyMatch(pair -> !Set.of("value", "basePackages").contains(pair.getNameAsString()))) continue;
                    if (annotation.isSingleMemberAnnotationExpr()) collectLiteralPackages(annotation.asSingleMemberAnnotationExpr().getMemberValue(), packages);
                    if (annotation.isNormalAnnotationExpr()) annotation.asNormalAnnotationExpr().getPairs().stream()
                            .filter(pair -> Set.of("value", "basePackages").contains(pair.getNameAsString()))
                            .forEach(pair -> collectLiteralPackages(pair.getValue(), packages));
                }
                if (packages.stream().anyMatch(base -> pkg.equals(base) || pkg.startsWith(base + "."))) discovered = true;
            }
        }
        if (!discovered) return null; // Custom startup/scanning needs explicit developer wiring.
        Set<String> reserved = new HashSet<>();
        analysis.files().stream().filter(file -> files.contains(file.source())).map(FileChoice::destination).forEach(reserved::add);
        String className = null, destination = null;
        for (int number = 1; number <= 1000; number++) {
            String candidate = "ImportedSourceComponents" + (number == 1 ? "" : number);
            String path = "user/src/main/java/" + pkg.replace('.', '/') + "/" + candidate + ".java";
            if (!reserved.contains(path) && !Files.exists(safe(analysis.destinationRoot(), path), LinkOption.NOFOLLOW_LINKS)) {
                className = candidate; destination = path; break;
            }
        }
        if (className == null) throw new IOException("No unused Spring registration filename is available.");
        String text = "package " + pkg + ";\n\n"
                + "/** User-owned registration for reviewed copied components. Preserved during Studio regeneration. */\n"
                + "@org.springframework.context.annotation.Configuration\n"
                + "@org.springframework.context.annotation.Import({\n"
                + components.stream().map(type -> "        " + type + ".class").collect(Collectors.joining(",\n"))
                + "\n})\npublic class " + className + " {\n}\n";
        return new SpringRegistration(destination, text, List.copyOf(components));
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
        return apply(analysis, files, dependencies, replaceExisting, acceptIdentity, reviewedJava, null);
    }

    public static Path apply(Analysis analysis, Set<String> files, Set<String> dependencies, boolean replaceExisting,
                             boolean acceptIdentity, Map<String, String> reviewedJava, SpringRegistration registration) throws Exception {
        return apply(analysis, files, dependencies, replaceExisting, acceptIdentity, reviewedJava, registration, null);
    }

    public static Path apply(Analysis analysis, Set<String> files, Set<String> dependencies, boolean replaceExisting,
                             boolean acceptIdentity, Map<String, String> reviewedJava, SpringRegistration registration,
                             SourceBuilderSettingsRecovery.Plan builderSettings) throws Exception {
        if (builderSettings != null && !builderSettings.equals(SourceBuilderSettingsRecovery.preview(analysis)))
            throw new IOException("Builder settings or source changed since review. Analyse and review again.");
        if (registration != null && !registration.equals(previewSpringRegistration(analysis, files, reviewedJava)))
            throw new IOException("Spring registration review changed. Review it again before importing.");
        if (registration != null) {
            for (FileChoice file : analysis.files()) {
                if (files.contains(file.source()) || !file.destination().endsWith(".java")) continue;
                if (!Objects.equals(existing(safe(analysis.destinationRoot(), file.destination())), file.previous()))
                    throw new IOException("Destination component changed since Spring registration review: " + file.destination());
            }
        }
        for (Entry<String,String> entry : reviewedJava.entrySet()) {
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
        for (Entry<String,String> input : analysis.supportInputs().entrySet())
            if (!Objects.equals(input.getKey().equals(SUPPORT_CUSTOMISATIONS)
                    ? Objects.toString(existing(safe(analysis.destinationRoot(), input.getKey())), "")
                    : existing(safe(analysis.destinationRoot(), input.getKey())), input.getValue()))
                throw new IOException(input.getKey().endsWith(".java")
                        ? "Destination Spring wiring changed since review. Analyse again: " + input.getKey()
                        : "Destination model changed since support review. Analyse again.");
        for (ManagedFile support : analysis.managedFiles()) {
            if (!Objects.equals(existing(safe(analysis.sourceRoot(), support.source())), support.bytes())
                    || !Objects.equals(existing(safe(analysis.destinationRoot(), support.destination())), support.current()))
                throw new IOException("Studio support file changed since review. Analyse again: " + support.source());
        }
        Map<Path, String> originals = new LinkedHashMap<>(), writes = new LinkedHashMap<>();
        for (String source : files) {
            FileChoice file = importableFiles(analysis).stream().filter(f -> f.source().equals(source)).findFirst().orElseThrow();
            if (!Objects.equals(existing(safe(analysis.sourceRoot(), source)), file.originalBytes())) throw new IOException("Original source changed since review: " + source);
            Path target = safe(analysis.destinationRoot(), file.destination());
            String output = reviewedJava.containsKey(source) ? encode(reviewedJava.get(source).getBytes(StandardCharsets.UTF_8)) : file.bytes();
            if (writes.putIfAbsent(target, output) != null) throw new IOException("Multiple sources target " + file.destination());
            if (!Objects.equals(file.previous(), existing(target))) throw new IOException("Destination changed since review: " + file.destination());
            if (file.previous() != null && !file.previous().equals(output) && !replaceExisting) throw new IOException("Approve replacement before importing " + file.destination());
            originals.put(target, file.previous());
        }
        if (registration != null) {
            Path target = safe(analysis.destinationRoot(), registration.destination());
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) || writes.containsKey(target))
                throw new IOException("Spring registration destination changed. Review it again before importing.");
            originals.put(target, null);
            writes.put(target, encode(registration.javaSource().getBytes(StandardCharsets.UTF_8)));
        }
        List<ManagedFile> customSupport = analysis.managedFiles().stream().filter(file -> files.contains(file.source())
                && !Objects.equals(writes.get(analysis.destinationRoot().resolve(file.destination())), file.current())).toList();
        if (!customSupport.isEmpty()) {
            Path marker = safe(analysis.destinationRoot(), SUPPORT_CUSTOMISATIONS);
            String previous = existing(marker);
            if (!Objects.equals(Objects.toString(previous, ""), analysis.supportInputs().get(SUPPORT_CUSTOMISATIONS)))
                throw new IOException("Support customisations changed since review. Analyse again.");
            Set<String> preserved = supportCustomisations(previous);
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
        if (builderSettings != null) {
            for (Entry<String,String> output : builderSettings.outputs().entrySet()) {
                Path target = safe(analysis.destinationRoot(), output.getKey());
                if (writes.containsKey(target)) throw new IOException("Conflicting builder recovery output: " + output.getKey());
                originals.put(target, encode(builderSettings.destinationInputs().get(output.getKey()).getBytes(StandardCharsets.UTF_8)));
                writes.put(target, encode(output.getValue().getBytes(StandardCharsets.UTF_8)));
            }
            Path backup = safe(analysis.destinationRoot(), "user/import-reviews/model-before-builder-recovery-" + UUID.randomUUID() + ".json");
            originals.put(backup, null);
            writes.put(backup, encode(builderSettings.destinationInputs().get(ProjectArtifactRenderer.MODEL).getBytes(StandardCharsets.UTF_8)));
        }
        Path report = safe(analysis.destinationRoot(), "user/import-reviews/" + UUID.randomUUID() + ".md");
        originals.put(report, null); writes.put(report, encode((report(analysis, files, dependencies, registration)
                + (analysis.identity().requiresApproval() && acceptIdentity ? "\nDeveloper explicitly approved the mismatched/unverified checkout identity.\n" : "")
                + (reviewedJava.isEmpty() ? "" : "\nStaged Java merges applied: " + new TreeSet<>(reviewedJava.keySet()) + "\n")
                + (builderSettings == null ? "" : "\n## Reviewed builder settings\n\n" + builderSettings.settings() + "\n")
                + (registration == null ? "" : "\n## Reviewed Spring registration\n\nCreated " + registration.destination()
                        + "\nRegistered components: " + registration.components() + "\n"))
                .getBytes(StandardCharsets.UTF_8)));
        List<Path> applied = new ArrayList<>();
        try {
            for (Entry<Path,String> entry : writes.entrySet()) {
                Path path = entry.getKey(); safe(analysis.destinationRoot(), relative(analysis.destinationRoot(), path));
                if (!Objects.equals(existing(path), originals.get(path))) throw new IOException("Destination changed during import: " + path.getFileName());
                if (Objects.equals(entry.getValue(), originals.get(path))) continue;
                Files.createDirectories(path.getParent());
                try (OutputStream output = Files.newOutputStream(path, LinkOption.NOFOLLOW_LINKS,
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
