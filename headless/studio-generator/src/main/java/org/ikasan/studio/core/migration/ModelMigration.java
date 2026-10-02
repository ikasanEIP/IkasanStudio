package org.ikasan.studio.core.migration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.ikasan.studio.core.persistence.json.IkasanModelDocuments;

import java.util.*;

/** Offline, non-mutating migration analysis. Only explicitly supported version pairs are eligible. */
public final class ModelMigration {
    private static final ObjectMapper JSON = StudioJson.newObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    public static final List<String> SUPPORTED_VERSIONS = List.of("V3.3.9", "V4.1.6");
    private static final Set<String> TYPE_PROPERTIES = Set.of("fromType", "toType", "type", "objectClass");
    private static final Set<String> STRUCTURAL = Set.of("flows", "consumer", "flowElements", "transitions",
            "exceptionResolver", "decorators", "componentType", "implementingClass", "additionalKey", "wiretapManagementEnabled");

    static final String RESTORE_NOTE = "To revert this migration, use Tools → Ikasan Studio → Restore Previous Ikasan Migration…\n";
    static final String REPORT_NOTES = "\nNOTE:\n" + RESTORE_NOTE;

    private ModelMigration() { }

    public record Finding(boolean blocking, String path, String message) { }
    public record Plan(String sourceVersion, String targetVersion, String sourceJson, String targetJson,
                       List<Finding> findings) {
        public Plan { findings = List.copyOf(findings); }
        public boolean canApply() { return findings.stream().noneMatch(Finding::blocking); }
        public String report() {
            StringBuilder text = new StringBuilder("Ikasan migration: " + sourceVersion + " → " + targetVersion + "\n\n");
            JsonNode source;
            try { source = IkasanModelDocuments.toLegacy(JSON.readTree(sourceJson)); }
            catch (java.io.IOException ex) { source = JSON.missingNode(); }
            JsonNode reportSource = source;
            findings.stream().filter(Finding::blocking).forEach(f -> text.append("BLOCKED: ")
                    .append(reportLocation(reportSource, f.path())).append(" — ").append(f.message()).append('\n'));
            findings.stream().filter(f -> !f.blocking() && "/version".equals(f.path()))
                    .forEach(f -> text.append(f.message()).append('\n'));
            var reviews = findings.stream().filter(f -> !f.blocking() && !"/version".equals(f.path())).toList();
            if (!reviews.isEmpty()) {
                text.append("\nYou are advised to review the following after the upgrade completes:\n\n");
                reviews.forEach(f -> text.append("REVIEW: ").append(reportLocation(reportSource, f.path()))
                        .append(" — ").append(f.message()).append('\n'));
            }
            return text.append(REPORT_NOTES).toString();
        }
    }

    /** Resolve readable names; retain the pointer only when the location cannot be identified. */
    private static String reportLocation(JsonNode source, String path) {
        if (source == null || !path.startsWith("/flows/")) return path;
        String[] parts = path.split("/", -1);
        if (parts.length < 3 || !parts[2].matches("[0-9]+")) return path;
        JsonNode flow = source.at("/flows/" + parts[2]);
        String flowName = flow.path("name").asText();
        if (flowName.isBlank()) return path;
        String label = flowName;
        if (parts.length >= 4) {
            JsonNode component = JSON.missingNode();
            if ("consumer".equals(parts[3])) component = flow.path("consumer");
            else if ("flowElements".equals(parts[3]) && parts.length >= 5 && parts[4].matches("[0-9]+")) {
                component = source.at("/flows/" + parts[2] + "/flowElements/" + parts[4]);
            }
            String componentName = component.path("componentName").asText();
            if (!componentName.isBlank()) {
                label += " → " + componentName;
                int propertyStart = "consumer".equals(parts[3]) ? 4 : 5;
                if (parts.length > propertyStart) label += " → " + String.join(" → ", Arrays.copyOfRange(parts, propertyStart, parts.length));
            }
            else if ("exceptionResolver".equals(parts[3])) label += " → Exception Resolver";
            else if ("consumer".equals(parts[3]) || "flowElements".equals(parts[3])) return path;
            else label += " → " + String.join(" → ", Arrays.copyOfRange(parts, 3, parts.length));
        }
        return label.replace('\n', ' ').replace('\r', ' ');
    }

    public static Plan analyse(String sourceJson, String targetVersion) throws Exception {
        JsonNode parsed = IkasanModelDocuments.toLegacy(JSON.readTree(sourceJson));
        if (!(parsed instanceof ObjectNode source)) throw new StudioBuildException("The model must be a JSON object.");
        String sourceVersion = source.path("version").asText();
        List<Finding> findings = new ArrayList<>();
        ObjectNode target = source.deepCopy();
        if (!SUPPORTED_VERSIONS.contains(sourceVersion) || !SUPPORTED_VERSIONS.contains(targetVersion)
                || sourceVersion.equals(targetVersion)) {
            findings.add(new Finding(true, "/version", "Supported migration paths are V3.3.9 ↔ V4.1.6."));
            return plan(sourceVersion, targetVersion, sourceJson, target, findings);
        }
        MigrationRules rules = MigrationRules.load(sourceVersion, targetVersion);
        validateTopology(source, findings);
        if (findings.stream().anyMatch(Finding::blocking)) return plan(sourceVersion, targetVersion, sourceJson, target, findings);
        Module sourceModule = ComponentIO.validatePersistedModuleJson(sourceJson, "migration source", false);
        Map<String, ComponentMeta> from = ComponentLibrary.getIkasanComponents(sourceVersion);
        Map<String, ComponentMeta> to = ComponentLibrary.getIkasanComponents(targetVersion);
        migrateObject(target, from.get("Module"), to.get("Module"), "", rules, findings);
        int i = 0;
        for (JsonNode node : target.path("flows")) {
            ObjectNode flow = (ObjectNode) node;
            String path = "/flows/" + i++;
            migrateObject(flow, from.get("Flow"), to.get("Flow"), path, rules, findings);
            migrateComponent(flow.get("consumer"), path + "/consumer", from, to, rules, findings, sourceModule, flow.path("name").asText());
            int j = 0;
            for (JsonNode component : flow.path("flowElements")) {
                migrateComponent(component, path + "/flowElements/" + j++, from, to, rules, findings, sourceModule, flow.path("name").asText());
            }
            // Resolver keys and caught exception types carry the same identity and must move together.
            if (flow.path("exceptionResolver") instanceof ObjectNode resolver) {
                List<String> keys = new ArrayList<>();
                resolver.fieldNames().forEachRemaining(keys::add);
                for (String key : keys) {
                    JsonNode resolution = resolver.get(key);
                    if (resolution instanceof ObjectNode object && object.path("exceptionsCaught").isTextual()) {
                        rewriteType(object, "exceptionsCaught", path + "/exceptionResolver", rules, findings);
                        String newKey = object.path("exceptionsCaught").asText();
                        if (!key.equals(newKey)) {
                            if (resolver.has(newKey)) findings.add(new Finding(true, path + "/exceptionResolver", "Exception mappings collide after migration."));
                            else { resolver.remove(key); resolver.set(newKey, object); }
                        }
                    }
                }
            }
        }
        target.put("version", targetVersion);
        findings.add(new Finding(false, "/version", "Migration will update the Ikasan BOM and set the Java build baseline to "
                + ComponentLibrary.getMetaPackManifest(targetVersion).javaVersion() + ". Compatible model type references are updated automatically; Migration recommended replacements apply only when selected. "
                + "Use a compatible JDK to build and run the migrated module."));
        Plan result = plan(sourceVersion, targetVersion, sourceJson, target, findings);
        if (result.canApply()) {
            try {
                Module module = ComponentIO.validatePersistedModuleJson(result.targetJson(), "migration target", false);
                // Detect model structures which Studio would silently omit when it next saves.
                JsonNode roundTrip = JSON.readTree(ComponentIO.toJson(module));
                checkPreserved(target, roundTrip, "", findings);
            } catch (Exception ex) {
                findings.add(new Finding(true, "/", "Target validation failed: " + ex.getMessage()));
            }
        }
        return plan(sourceVersion, targetVersion, sourceJson, target, findings);
    }

    private static Plan plan(String from, String to, String original, ObjectNode target, List<Finding> findings) throws Exception {
        JsonNode output = target;
        if (IkasanModelDocuments.isDocumentContainer(JSON.readTree(original)) && findings.stream().noneMatch(Finding::blocking))
            output = IkasanModelDocuments.fromLegacy(target);
        return new Plan(from, to, original, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(output) + "\n", findings);
    }

    private static void migrateComponent(JsonNode node, String path, Map<String, ComponentMeta> from,
                                         Map<String, ComponentMeta> to, MigrationRules rules,
                                         List<Finding> findings, Module sourceModule, String flowName) {
        if (node == null || node.isNull() || node.isEmpty()) return;
        if (!(node instanceof ObjectNode component)) {
            findings.add(new Finding(true, path, "Component must be an object.")); return;
        }
        // Do not use the deserializer's generic fallback for an unrecognised built-in component.
        ComponentMeta sourceMeta = from.values().stream().filter(meta ->
                Objects.equals(meta.getComponentType(), component.path("componentType").asText(null))
                && Objects.equals(meta.getImplementingClass(), component.path("implementingClass").asText(null))
                && Objects.equals(meta.getAdditionalKey(), component.path("additionalKey").asText(null)))
                .findFirst().orElse(null);
        if (sourceMeta == null) {
            findings.add(new Finding(true, path, "No exact source component match. Configure the component in Studio before migrating.")); return;
        }
        String key = from.entrySet().stream().filter(e -> e.getValue() == sourceMeta).map(Map.Entry::getKey).findFirst().orElseThrow();
        String targetKey = rules.componentMappings().get(key);
        ComponentMeta targetMeta = targetKey == null ? null : to.get(targetKey);
        if (targetMeta == null || !Objects.equals(sourceMeta.getComponentType(), targetMeta.getComponentType())) {
            findings.add(new Finding(true, path, "No supported target mapping for " + key + ".")); return;
        }
        if (path.endsWith("/consumer") != targetMeta.isConsumer()) {
            findings.add(new Finding(true, path, "The consumer slot must contain a consumer; body components must not be consumers."));
        }
        migrateObject(component, sourceMeta, targetMeta, path, rules, findings);
        component.put("componentType", targetMeta.getComponentType());
        component.put("implementingClass", targetMeta.getImplementingClass());
        if (targetMeta.getAdditionalKey() == null) component.remove("additionalKey");
        else component.put("additionalKey", targetMeta.getAdditionalKey());
        if (sourceMeta.isGeneratesUserImplementedClass() || sourceMeta.isGeneric()) {
            String className = implementationClass(sourceModule, flowName, component.path("componentName").asText());
            findings.add(new Finding(false, path, (className == null ? key + " user implementation" : className)
                    + ": user-implemented class is preserved except for selected recommended replacements."));
        }
    }

    private static String implementationClass(Module module, String flowName, String componentName) {
        for (var flow : module.getFlows()) {
            if (!flowName.equals(flow.getIdentity())) continue;
            for (var component : flow.getFlowElementsNoExternalEndPoints()) {
                if (!componentName.equals(component.getIdentity())) continue;
                String name = component.getPropertyValueAsString("userImplementedClassName");
                if (name == null || name.isBlank() || name.startsWith("__")) return null;
                return name.contains(".") ? name
                        : org.ikasan.studio.core.generator.GeneratorUtils.getUserImplementedClassesPackageName(module, flow) + "." + name;
            }
        }
        return null;
    }

    private static void migrateObject(ObjectNode object, ComponentMeta from, ComponentMeta to, String path,
                                      MigrationRules rules, List<Finding> findings) {
        if (from == null || to == null) { findings.add(new Finding(true, path, "Missing component metadata.")); return; }
        object.fieldNames().forEachRemaining(name -> {
            if (STRUCTURAL.contains(name) || "version".equals(name) || IkasanModelDocuments.RETAINED.equals(name) || "_ikasanFlowExtensions".equals(name) || "_ikasanComponentExtensions".equals(name)) return;
            ComponentPropertyMeta old = from.getMetadata(name), next = to.getMetadata(name);
            if (old != null && next == null) {
                findings.add(new Finding(true, path + "/" + name, "Property has no target equivalent; no value has been discarded."));
            } else if (old == null) {
                findings.add(new Finding(false, path + "/" + name, "Unrecognised JSON field retained unchanged."));
            } else {
                // Older serializers wrote route-name lists using List.toString(). Use the same
                // parsing as Studio, preserving names and order while updating only their encoding.
                if ("routeNames".equals(name) && object.path(name).isTextual()
                        && ComponentPropertyMeta.STRING_LIST.equals(old.getUsageDataType())
                        && ComponentPropertyMeta.STRING_LIST.equals(next.getUsageDataType())) {
                    String original = object.path(name).asText();
                    String canonical = String.join(",", org.ikasan.studio.core.StudioBuildUtils.stringToList(original));
                    if (!original.equals(canonical)) {
                        object.put(name, canonical);
                        findings.add(new Finding(false, path + "/" + name,
                                "Update legacy route-name list formatting; route names and order are unchanged."));
                    }
                }
                if (!Objects.equals(old.getPropertyDataType(), next.getPropertyDataType())) {
                    findings.add(new Finding(true, path + "/" + name, "Property type changed; an explicit conversion rule is required."));
                }
                if (!Objects.equals(old.getUsageDataType(), next.getUsageDataType())) {
                    findings.add(new Finding(false, path + "/" + name, "Implementation API changes from " + old.getUsageDataType() + " to " + next.getUsageDataType() + "."));
                }
                if (TYPE_PROPERTIES.contains(name) && object.path(name).isTextual()) rewriteType(object, name, path, rules, findings);
                if (next.getChoices() != null && !next.getChoices().isEmpty() && !object.path(name).isNull()
                        && !next.getChoices().contains(object.path(name).asText())) {
                    findings.add(new Finding(true, path + "/" + name, "Value is not one of the target choices."));
                }
            }
        });
        to.getAllowableProperties().forEach((name, next) -> {
            if ("version".equals(name)) return;
            if (object.hasNonNull(name)) {
                if (next.isMandatory() && object.path(name).isTextual() && object.path(name).asText().isBlank()) {
                    findings.add(new Finding(true, path + "/" + name, "A required target property is blank."));
                }
                return;
            }
            ComponentPropertyMeta old = from.getMetadata(name);
            if (old != null && !Objects.equals(old.getDefaultValue(), next.getDefaultValue())) {
                if (old.getDefaultValue() != null) {
                    object.set(name, JSON.valueToTree(old.getDefaultValue()));
                    findings.add(new Finding(false, path + "/" + name, "Retained the source default explicitly."));
                } else findings.add(new Finding(true, path + "/" + name, "Target introduces a default; choose an explicit value before migrating."));
            }
            if (next.isMandatory() && next.getDefaultValue() == null && !object.hasNonNull(name)) {
                findings.add(new Finding(true, path + "/" + name, "A required target property has no value or default."));
            }
        });
    }

    private static void rewriteType(ObjectNode node, String field, String path, MigrationRules rules, List<Finding> findings) {
        String old = node.path(field).asText();
        String value = old;
        for (var mapping : rules.typePrefixes().entrySet()) {
            value = value.replaceAll("(?<![\\w.])" + java.util.regex.Pattern.quote(mapping.getKey()),
                    java.util.regex.Matcher.quoteReplacement(mapping.getValue()));
        }
        if (!old.equals(value)) {
            node.put(field, value);
            findings.add(new Finding(false, path + "/" + field, old + " → " + value + " (recommended replacement)"));
        }
    }

    private static void checkPreserved(JsonNode expected, JsonNode actual, String path, List<Finding> findings) {
        if (("/" + IkasanModelDocuments.RETAINED).equals(path)) return; // Native documents are re-rendered for the target pack.
        if (expected.isObject()) {
            expected.properties().forEach(e -> checkPreserved(e.getValue(), actual.path(e.getKey()), path + "/" + e.getKey(), findings));
        } else if (expected.isArray()) {
            if (expected.isEmpty() && actual.isMissingNode()) return;
            // Flow element order is derived from transitions; compare by identity rather than array position.
            if (path.endsWith("/flowElements") && actual.isArray()) {
                for (JsonNode item : expected) {
                    JsonNode match = null;
                    for (JsonNode candidate : actual) if (candidate.path("componentName").equals(item.path("componentName"))) match = candidate;
                    checkPreserved(item, match == null ? JSON.missingNode() : match, path + "/" + item.path("componentName").asText(), findings);
                }
            } else if (actual.isArray() && expected.size() == actual.size()) {
                for (int i = 0; i < expected.size(); i++) checkPreserved(expected.get(i), actual.get(i), path + "/" + i, findings);
            } else findings.add(new Finding(true, path, "Studio cannot retain this structure exactly; correct it before migration."));
        } else if (!expected.equals(actual) && !(expected.isNumber() && actual.isNumber() && expected.asText().equals(actual.asText()))) {
            findings.add(new Finding(true, path, "Studio cannot retain this value exactly; correct it before migration."));
        }
    }

    private static void validateTopology(ObjectNode root, List<Finding> findings) {
        if (root.has("flows") && !root.path("flows").isArray()) {
            findings.add(new Finding(true, "/flows", "Flows must be an array.")); return;
        }
        Set<String> flows = new HashSet<>();
        for (JsonNode flow : root.path("flows")) {
            if (!flow.isObject() || flow.path("name").asText().isBlank() || !flows.add(flow.path("name").asText())) {
                findings.add(new Finding(true, "/flows", "Flow names must be non-empty and unique.")); continue;
            }
            Set<String> names = new HashSet<>();
            List<JsonNode> components = new ArrayList<>();
            if (flow.hasNonNull("consumer") && !flow.path("consumer").isEmpty()) components.add(flow.get("consumer"));
            flow.path("flowElements").forEach(components::add);
            for (JsonNode component : components) {
                String name = component.path("componentName").asText();
                if (!component.isObject() || name.isBlank() || !names.add(name)) findings.add(new Finding(true, "/flows/" + flow.path("name").asText(), "Component names must be non-empty and unique."));
            }
            for (JsonNode edge : flow.path("transitions")) {
                if (!names.contains(edge.path("from").asText()) || !names.contains(edge.path("to").asText())) {
                    findings.add(new Finding(true, "/flows/" + flow.path("name").asText(), "A transition refers to a missing component."));
                }
            }
        }
    }
}
