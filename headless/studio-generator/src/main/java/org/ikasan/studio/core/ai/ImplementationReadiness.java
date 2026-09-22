package org.ikasan.studio.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.persistence.json.StudioJson;
import java.nio.file.*;
import java.util.*;

/** Conservative saved-source observations, never a proof of runtime readiness. */
public final class ImplementationReadiness {
    private ImplementationReadiness() { }
    public record Finding(String flow, String component, String file, int line, String code, String message) { }
    public record Report(String checkedAt, String scope, boolean runtimeVerified, List<Finding> findings) { }
    public static Report scan(Path root, JsonNode model) {
        List<Finding> findings = new ArrayList<>();
        for (JsonNode flow : model.path("flows")) {
            List<JsonNode> components = new ArrayList<>();
            if (flow.has("consumer")) components.add(flow.get("consumer"));
            flow.path("flowElements").forEach(components::add);
            for (JsonNode component : components) {
                String className = component.path("userImplementedClassName").asText("");
                if (className.isBlank()) continue;
                String path = "user/src/main/java/" + model.path("applicationPackageName").asText("").replace('.', '/')
                        + "/" + StudioBuildUtils.toJavaPackageName(flow.path("name").asText()) + "/" + className + ".java";
                Path file = root.resolve(path).normalize();
                if (!file.startsWith(root.toAbsolutePath().normalize()) || !className.matches("[A-Za-z_$][A-Za-z0-9_$]*")) continue;
                String flowName = flow.path("name").asText(), name = component.path("componentName").asText();
                try {
                    if (!Files.isRegularFile(file)) {
                        findings.add(new Finding(flowName, name, path, 1, "MISSING_SOURCE", "Expected implementation file is missing."));
                        continue;
                    }
                    if (Files.size(file) > 1_048_576) {
                        findings.add(new Finding(flowName, name, path, 1, "NOT_CHECKED", "Source exceeds the 1 MiB inspection limit."));
                        continue;
                    }
                    String source = Files.readString(file);
                    String[] lines = source.split("\\R", -1);
                    boolean block = false;
                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i].strip();
                        if (line.startsWith("/*")) block = true;
                        boolean comment = block || line.startsWith("//") || line.startsWith("*");
                        if (line.contains("*/")) block = false;
                        if (!comment && line.matches(".*throw\\s+new\\s+(?:java\\.lang\\.)?UnsupportedOperationException\\s*\\(\\s*\"Conversion has not been implemented\"\\s*\\).*")) {
                            findings.add(new Finding(flowName, name, path, i + 1, "THROWING_STUB", "Generated converter still throws: Conversion has not been implemented."));
                        } else if (line.matches("//\\s*@?TODO\\b.*")) {
                            // Any comment-only TODO, not a fixed wording: the generated stubs use several ("//@TODO implement your",
                            // "// TODO: Update the mutable payload", "// TODO review the default body"), so matching one or two
                            // phrases silently missed the Generic Consumer, Translator and Email Converter scaffolds.
                            findings.add(new Finding(flowName, name, path, i + 1, "REVIEW_SCAFFOLD", "A TODO marker remains in the implementation. Inspect the method; this warning alone does not prove it is unfinished."));
                        }
                    }
                    // The converter stub has a TODO comment directly above its throwing stub: report the stronger finding once.
                    for (int i = findings.size() - 1; i > 0; i--) {
                        var stub = findings.get(i);
                        var todo = findings.get(i - 1);
                        if ("THROWING_STUB".equals(stub.code()) && "REVIEW_SCAFFOLD".equals(todo.code()) && todo.file().equals(stub.file())
                                && todo.line() + 1 == stub.line()) findings.remove(i - 1);
                    }
                } catch (Exception failure) {
                    findings.add(new Finding(flowName, name, path, 1, "NOT_CHECKED", "Could not read implementation source."));
                }
            }
        }
        return new Report(java.time.Instant.now().toString(), "Saved primary user implementation files only; unsaved edits and provider/helper beans are not inspected. No findings is not proof of working behaviour. Exercise actual components and flow paths.", false, List.copyOf(findings));
    }
    public static Report scanProject(Path root) throws java.io.IOException {
        return scan(root.toAbsolutePath().normalize(), StudioJson.newObjectMapper().readTree(
                Files.readString(root.resolve("generated/src/main/model/model.json"))));
    }
}
