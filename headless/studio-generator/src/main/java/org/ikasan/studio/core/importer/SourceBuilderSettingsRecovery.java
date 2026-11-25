package org.ikasan.studio.core.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.importer.SourceCheckoutIdentity.Candidate;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.persistence.json.StudioJson;

/** Reviewed recovery of pack-declared class-valued builder arguments; never executes original code. */
public final class SourceBuilderSettingsRecovery {
    private SourceBuilderSettingsRecovery() { }
    public record Setting(String flow, String component, String property, String value, String evidence) { }
    public record Plan(List<Setting> settings, Map<String, String> sourceInputs,
                       Map<String, String> destinationInputs, Map<String, String> outputs) { }

    public static Plan preview(SourceModuleImport.Analysis analysis) throws Exception {
        String model = read(analysis.destinationRoot(), ProjectArtifactRenderer.MODEL);
        if (model == null) return null;
        // Source import also supports non-Studio projects/tests without a complete saved design.
        JsonNode tree = StudioJson.newObjectMapper().readTree(model);
        if (!tree.has("modelFormat")) return null;
        Module module = ComponentIO.validatePersistedModuleJson(model, "builder settings source review", false);
        JavaParser parser = new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));
        Map<String, String> sources = new TreeMap<>(), inputs = new TreeMap<>(), outputs = new TreeMap<>();
        inputs.put(ProjectArtifactRenderer.MODEL, model);
        List<Setting> settings = new ArrayList<>();
        for (Flow flow : module.getFlows()) {
            String pkg = Generator.STUDIO_FLOW_PACKAGE + "." + flow.getJavaPackageName();
            String relative = "src/main/java/" + pkg.replace('.', '/') + "/ComponentFactory" + flow.getJavaClassName() + ".java";
            Set<String> paths = new TreeSet<>();
            Candidate candidate = analysis.identity().candidate();
            if (candidate == null) continue;
            for (String owner : candidate.ownedPomDirectories()) paths.add((owner.isBlank() ? "" : owner + "/") + relative);
            paths.add((candidate.directory().isBlank() ? "" : candidate.directory() + "/") + "generated/" + relative);
            Map<String, CompilationUnit> units = new TreeMap<>();
            for (String path : paths) {
                String text = read(analysis.sourceRoot(), path);
                if (text == null) continue;
                sources.put(path, text);
                ParseResult<CompilationUnit> parsed = parser.parse(text);
                if (parsed.isSuccessful() && parsed.getResult().isPresent()) {
                    CompilationUnit unit = parsed.getResult().get();
                    if (unit.getPackageDeclaration().map(p -> p.getNameAsString().equals(pkg)).orElse(false)
                            && unit.getTypes().stream().anyMatch(t -> t.getNameAsString().equals("ComponentFactory" + flow.getJavaClassName())))
                        units.put(path, unit);
                }
            }
            // Multiple factories for the same flow are ambiguous, even if they happen to agree today.
            if (units.size() != 1) continue;
            Entry<String,CompilationUnit> entry = units.entrySet().iterator().next(); CompilationUnit unit = entry.getValue();
            boolean changed = false;
            for (FlowElement component : flow.getFlowElementsNoExternalEndPoints()) {
                ComponentMeta meta = component.getComponentMeta();
                if (!meta.isUsesBuilderInFactory()) continue;
                List<MethodDeclaration> methods = unit.findAll(MethodDeclaration.class).stream()
                        .filter(m -> m.getNameAsString().equals("get" + component.getJavaClassName()) && m.getParameters().isEmpty()).toList();
                if (methods.size() != 1) continue;
                List<ReturnStmt> returns = methods.get(0).findAll(ReturnStmt.class);
                if (returns.size() != 1 || returns.get(0).getExpression().isEmpty()) continue;
                Expression expression = returns.get(0).getExpression().get();
                // A single fluent builder expression only; no variables, branches, reflection or helper execution.
                List<MethodCallExpr> chain = new ArrayList<>();
                while (expression.isMethodCallExpr()) {
                    MethodCallExpr call = expression.asMethodCallExpr(); chain.add(call);
                    if (call.getScope().isEmpty()) break;
                    expression = call.getScope().get();
                }
                if (chain.stream().filter(c -> c.getNameAsString().equals(meta.getIkasanComponentFactoryMethod())).count() != 1) continue;
                for (ComponentPropertyMeta property : meta.getAllowableProperties().values()) {
                    String key = property.getPropertyName();
                    if (!property.isSetterProperty() || !ComponentPropertyMeta.CLASS_LITERAL.equals(property.getUsageDataType())
                            || !component.getPropertyValueAsString(key).isBlank()) continue;
                    String setter = property.getSetterMethod();
                    if (setter == null || setter.isBlank()) setter = "set" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
                    String expected = setter;
                    List<MethodCallExpr> calls = chain.stream().filter(c -> c.getNameAsString().equals(expected)).toList();
                    if (calls.size() != 1 || calls.get(0).getArguments().size() != 1 || !calls.get(0).getArgument(0).isClassExpr()) continue;
                    String type = calls.get(0).getArgument(0).asClassExpr().getType().asString();
                    String qualified = qualify(type, unit);
                    if (qualified == null || qualified.isBlank()) continue;
                    if (property.getValidation() != null && !property.getValidation().isBlank() && !qualified.matches(property.getValidation())) continue;
                    component.setPropertyValue(key, qualified);
                    settings.add(new Setting(flow.getIdentity(), component.getComponentName(), key, qualified,
                            entry.getKey() + ":" + calls.get(0).getBegin().map(p -> p.line).orElse(1)));
                    changed = true;
                }
            }
            if (changed) {
                String path = "generated/" + relative;
                String before = read(analysis.destinationRoot(), path);
                if (before == null) throw new IOException("Generate the destination factory before recovering builder settings: " + path);
                inputs.put(path, before);
                outputs.put(path, FlowsComponentFactoryTemplate.create(pkg, module, flow));
            }
        }
        if (settings.isEmpty()) return null;
        outputs.put(ProjectArtifactRenderer.MODEL, ModelTemplate.create(module));
        return new Plan(List.copyOf(settings), Map.copyOf(sources), Map.copyOf(inputs), Map.copyOf(outputs));
    }

    private static String qualify(String type, CompilationUnit unit) {
        if (type.contains(".") && Character.isLowerCase(type.charAt(0))) return type;
        String first = type.split("\\.")[0];
        List<ImportDeclaration> imports = unit.getImports().stream().filter(i -> !i.isAsterisk() && !i.isStatic()
                && i.getName().getIdentifier().equals(first)).toList();
        if (imports.size() == 1) return imports.get(0).getNameAsString() + type.substring(first.length());
        // Wildcard imports and implicit same-package resolution need type resolution; do not guess.
        return null;
    }

    static String read(Path root, String path) throws IOException {
        Path file = SourceModuleImport.safe(root, path);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) return null;
        if (Files.size(file) > 2 * 1024 * 1024) throw new IOException("Source review file exceeds 2 MiB: " + path);
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
