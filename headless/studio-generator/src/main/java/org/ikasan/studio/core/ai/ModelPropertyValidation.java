package org.ikasan.studio.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.model.StringCollectionValues;
import org.ikasan.studio.core.persistence.json.StudioJson;

import java.util.*;
import static org.ikasan.studio.core.ai.ImplementationReadiness.Target;

/** Validates saved values without instantiating components, changing the model or revealing credentials. */
final class ModelPropertyValidation {
    private static final String MODEL = "generated/src/main/model/model.json";
    private ModelPropertyValidation() { }

    static List<ImplementationReadiness.Finding> scan(JsonNode model) {
        var findings = new ArrayList<ImplementationReadiness.Finding>();
        String pack = model.path("version").asText("");
        if (pack.isBlank()) return findings;
        try {
            validate(ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Module"), model, "", "Module", Target.MODULE, findings);
            for (JsonNode flow : model.path("flows")) {
                String name = flow.path("name").asText();
                validate(ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Flow"), flow, name, "Flow", Target.FLOW, findings);
                // The persisted model flattens nested branches into flowElements. A consumer can also appear there.
                Set<JsonNode> components = new LinkedHashSet<>();
                if (flow.path("consumer").isObject()) components.add(flow.get("consumer"));
                flow.path("flowElements").forEach(components::add);
                if (flow.path("exceptionResolver").isObject()) {
                    var resolver = (org.ikasan.studio.core.metapack.model.ExceptionResolverMeta)
                            ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Exception Resolver");
                    for (var rule : flow.get("exceptionResolver").properties()) {
                        var action = resolver.getExceptionActionWithName(rule.getValue().path("theAction").asText());
                        String location = "Exception Resolver: " + rule.getKey();
                        if (action == null) add(findings, name, location, Target.EXCEPTION_RESOLVER, rule.getValue(), "theAction", "Choose a supported exception action.");
                        else validate(action.getActionProperties(), rule.getValue().path("properties"), name, location, Target.EXCEPTION_RESOLVER, findings);
                    }
                }
                for (JsonNode component : components) {
                    String componentName = component.path("componentName").asText("Component");
                    ComponentMeta meta = ComponentLibrary.getIkasanComponentByDeserialisationKey(pack,
                            component.path("implementingClass").asText(null), component.path("componentType").asText(null),
                            component.path("additionalKey").asText(null));
                    if (meta == null) {
                        findings.add(new ImplementationReadiness.Finding(name, componentName, MODEL, 1,
                                "PROPERTIES_NOT_CHECKED", "Component metadata could not be resolved; check the selected meta-pack."));
                    } else validate(meta, component, name, componentName, Target.COMPONENT, findings);
                }
            }
        } catch (org.ikasan.studio.core.StudioBuildException failure) {
            findings.add(new ImplementationReadiness.Finding("", "Module", MODEL, 1,
                    "PROPERTIES_NOT_CHECKED", "Property metadata is unavailable; check the selected meta-pack."));
        }
        return findings;
    }

    private static void validate(ComponentMeta component, JsonNode node, String flow, String name, Target target,
                                 List<ImplementationReadiness.Finding> findings) {
        validate(component.getAllowableProperties(), node, flow, name, target, findings);
    }

    private static void validate(Map<String, ComponentPropertyMeta> properties, JsonNode node, String flow, String name, Target target,
                                 List<ImplementationReadiness.Finding> findings) {
        for (var meta : properties.values()) {
            if (meta.isIgnoreProperty()) continue;
            String key = meta.getPropertyName();
            Object value;
            try { value = value(meta, node.path(key)); }
            catch (IllegalArgumentException invalid) {
                add(findings, flow, name, target, node, key, "Value has an incompatible data type.");
                continue;
            }
            if (empty(value)) {
                if (meta.isMandatory()) add(findings, flow, name, target, node, key, "A value is required.");
                else if (meta.hasMandatoryUnlessAnyOf() && meta.getMandatoryUnlessAnyOf().stream()
                        .noneMatch(sibling -> populated(properties, node, sibling)))
                    add(findings, flow, name, target, node, key, "A value is required unless " + String.join(" or ", meta.getMandatoryUnlessAnyOf()) + " is supplied.");
                else if (meta.hasMandatoryIfTrue() && node.path(meta.getMandatoryIfTrue()).asBoolean(false))
                    add(findings, flow, name, target, node, key, "A value is required because " + meta.getMandatoryIfTrue() + " is enabled.");
                continue;
            }
            if (meta.getChoices() != null && !meta.getChoices().isEmpty() && !meta.isChoicesEditable()
                    && !meta.getChoices().contains(String.valueOf(value)))
                add(findings, flow, name, target, node, key, "Choose one of: " + String.join(", ", meta.getChoices()) + ".");
            if (meta.getValidationPattern() != null) {
                Object patternValue = meta.patternValidationValue(value);
                List<?> values = patternValue instanceof List<?> entries ? entries : List.of(patternValue);
                for (int i = 0; i < values.size(); i++) {
                    if (!meta.getValidationPattern().matcher(String.valueOf(values.get(i))).matches()) {
                        String hint = meta.getValidationMessage();
                        add(findings, flow, name, target, node, key, (patternValue instanceof List<?> ? "Entry " + (i + 1) + ": " : "")
                                + (hint == null || hint.isBlank() ? "Value does not match the validation rule." : hint));
                    }
                }
            }
        }
    }

    private static boolean populated(Map<String, ComponentPropertyMeta> properties, JsonNode node, String key) {
        var meta = properties.get(key);
        if (meta == null) return false;
        try { return !empty(value(meta, node.path(key))); }
        catch (IllegalArgumentException invalid) { return true; } // Report the sibling's invalid type separately.
    }

    private static Object value(ComponentPropertyMeta meta, JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            // Loading a model supplies concrete mandatory defaults; mirror that without modifying the model.
            Object defaultValue = meta.getDefaultValue();
            if (meta.isMandatory() && defaultValue != null && !ComponentPropertyMeta.isSubstitutionValue(defaultValue))
                return value(meta, StudioJson.newObjectMapper().valueToTree(defaultValue));
            return null;
        }
        Class<?> type = meta.getPropertyDataType();
        if (meta.isCommaSeparatedStringList())
            return StringCollectionValues.normalize(List.class, StudioJson.newObjectMapper().convertValue(node, Object.class));
        if (StringCollectionValues.supports(type))
            return StringCollectionValues.normalize(type, StudioJson.newObjectMapper().convertValue(node, Object.class));
        if (node.isContainerNode()) throw new IllegalArgumentException();
        String text = node.asText();
        if (text.isBlank()) return null;
        if (type == Integer.class) return Integer.valueOf(text);
        if (type == Long.class) return Long.valueOf(text);
        if (type == Boolean.class) {
            if (!text.equalsIgnoreCase("true") && !text.equalsIgnoreCase("false")) throw new IllegalArgumentException();
            return Boolean.valueOf(text);
        }
        if (type == String.class && !node.isTextual()) throw new IllegalArgumentException();
        return text;
    }

    private static boolean empty(Object value) {
        return value == null || value instanceof String text && text.isBlank()
                || value instanceof Collection<?> entries && entries.isEmpty()
                || value instanceof Map<?, ?> mapping && mapping.isEmpty();
    }

    static String safeDisplayValue(String key, JsonNode value) {
        if (value.isMissingNode() || value.isNull()) return "(not set)";
        if (key.toLowerCase(Locale.ROOT).matches(".*(password|secret|token|credential|key).*")) return "(hidden)";
        boolean address = Set.of("toRecipient", "toRecipients", "ccRecipient", "ccRecipients",
                "bccRecipient", "bccRecipients", "from").contains(key);
        boolean visible = value.isNumber() || value.isBoolean() || address
                || Set.of("cronExpression", "flowStartupType", "theAction").contains(key);
        if (!visible || value.isObject()) return "(hidden)";
        if (value.isArray()) {
            if (!address) return "(hidden)";
            for (JsonNode entry : value) if (!entry.isTextual()) return "(hidden)";
        }
        String text = value.toString(); // JSON escaping keeps newlines/control characters unambiguous.
        return text.length() <= 240 ? text : text.substring(0, 240) + "…";
    }

    private static void add(List<ImplementationReadiness.Finding> findings, String flow, String component, Target target, JsonNode node, String key, String message) {
        findings.add(new ImplementationReadiness.Finding(flow, component, MODEL, 1,
                "INVALID_PROPERTY", key + ": " + message + " Review this property in Studio.", target, key, safeDisplayValue(key, node.path(key))));
    }
}
