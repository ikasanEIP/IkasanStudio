package org.ikasan.studio.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ReturnStmt;
import java.nio.file.*;
import java.util.*;

/** Parse only: never compile or execute developer tests. Unknown expressions are left to runtime checks. */
final class FlowTestPathValidation {
    private static final Map<String, String> ROLES = Map.ofEntries(
            Map.entry("consumer", "Consumer"), Map.entry("scheduledConsumer", "Consumer"),
            Map.entry("producer", "Producer"), Map.entry("converter", "Converter"),
            Map.entry("translator", "Translator"), Map.entry("broker", "Broker"),
            Map.entry("filter", "Filter"), Map.entry("splitter", "Splitter"),
            Map.entry("sequencer", "Sequencer"), Map.entry("router", "SingleRecipientRouter"),
            Map.entry("multiRecipientRouter", "MultiRecipientRouter"));

    static List<ImplementationReadiness.Finding> scan(Path root, JsonNode model) {
        List<ImplementationReadiness.Finding> result = new ArrayList<>();
        Path directory = root.resolve("user-flow-tests/src/test/java");
        if (!Files.isDirectory(directory)) return result;
        var parser = new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17));
        try (var paths = Files.walk(directory)) {
            for (Path file : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (Files.size(file) > 1_048_576) continue;
                var parsed = parser.parse(file);
                if (!parsed.isSuccessful() || parsed.getResult().isEmpty()) continue;
                for (var declaration : parsed.getResult().get().getTypes()) {
                    if (!(declaration instanceof ClassOrInterfaceDeclaration clazz)) continue;
                    Map<String, String> constants = new HashMap<>();
                    for (FieldDeclaration field : clazz.getFields())
                        if (field.isFinal()) for (var variable : field.getVariables())
                            variable.getInitializer().filter(Expression::isStringLiteralExpr).ifPresent(value ->
                                    constants.put(variable.getNameAsString(), value.asStringLiteralExpr().asString()));
                    String flowName = null;
                    for (var method : clazz.getMethodsByName("getFlowName"))
                        if (method.getBody().isPresent() && method.getBody().get().getStatements().size() == 1
                                && method.getBody().get().getStatement(0) instanceof ReturnStmt returned)
                            flowName = returned.getExpression().map(e -> text(e, constants)).orElse(null);
                    JsonNode flow = null;
                    for (JsonNode candidate : model.path("flows"))
                        if (Objects.equals(flowName, candidate.path("name").asText())) flow = candidate;
                    if (flow == null) continue;
                    Map<String, String> components = new LinkedHashMap<>();
                    List<JsonNode> elements = new ArrayList<>();
                    elements.add(flow.path("consumer"));
                    flow.path("flowElements").forEach(elements::add);
                    for (var element : elements) {
                        if (!element.has("componentName")) continue;
                        String type = element.path("componentType").asText();
                        components.put(element.path("componentName").asText(), type.substring(type.lastIndexOf('.') + 1));
                    }
                    for (var method : clazz.getMethodsByName("defineExpectedPath")) {
                        if (method.getParameters().size() != 1 || method.getBody().isEmpty()) continue;
                        String harness = method.getParameter(0).getNameAsString();
                        Map<String, String> usableConstants = new HashMap<>(constants);
                        // Avoid resolving a field constant when a local variable shadows it.
                        method.findAll(VariableDeclarator.class).forEach(v -> usableConstants.remove(v.getNameAsString()));
                        for (var call : method.getBody().get().findAll(MethodCallExpr.class)) {
                            String role = call.getNameAsString();
                            if (!ROLES.containsKey(role) || call.getArguments().size() != 1
                                    || call.getScope().filter(e -> receiver(e, harness)).isEmpty()) continue;
                            String name = text(call.getArgument(0), usableConstants);
                            if (name == null) continue;
                            String actual = components.get(name);
                            String problem = actual == null ? "Component '" + name + "' does not exist in this flow."
                                    : !actual.isEmpty() && !actual.equals(ROLES.get(role))
                                    ? "Component '" + name + "' is a " + actual + ", but the expected path uses " + role + "." : null;
                            if (problem == null) continue;
                            String suggestion = components.keySet().stream().filter(n -> !n.equals(name)
                                            && n.replaceAll("\\s", "").equalsIgnoreCase(name.replaceAll("\\s", "")))
                                    .findFirst().map(n -> " Did you mean '" + n + "'?").orElse("");
                            result.add(new ImplementationReadiness.Finding(flowName, name,
                                    root.relativize(file).toString().replace('\\', '/'), call.getBegin().orElseThrow().line,
                                    "INVALID_EXPECTED_PATH", problem + suggestion + " Update defineExpectedPath."));
                        }
                    }
                }
            }
        } catch (java.io.IOException failure) {
            result.add(new ImplementationReadiness.Finding("", "Flow tests", "user-flow-tests/src/test/java", 1,
                    "NOT_CHECKED", "Could not inspect saved flow-test paths."));
        }
        return result;
    }

    private static boolean receiver(Expression expression, String harness) {
        if (expression instanceof NameExpr name) return name.getNameAsString().equals(harness);
        return expression instanceof MethodCallExpr call
                && (ROLES.containsKey(call.getNameAsString()) || Set.of("repeat", "blockStart", "blockEnd").contains(call.getNameAsString()))
                && call.getScope().filter(e -> receiver(e, harness)).isPresent();
    }

    private static String text(Expression expression, Map<String, String> constants) {
        if (expression instanceof StringLiteralExpr literal) return literal.asString();
        return expression instanceof NameExpr name ? constants.get(name.getNameAsString()) : null;
    }
}
