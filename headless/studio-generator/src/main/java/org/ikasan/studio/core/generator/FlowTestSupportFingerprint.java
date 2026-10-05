package org.ikasan.studio.core.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Semantic fingerprint of shared test wiring; no source code, layout or version-only changes. */
public final class FlowTestSupportFingerprint {
    private FlowTestSupportFingerprint() { }

    public static String fingerprint(String model, Set<String> fields) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = wiringModel(mapper.readTree(model), mapper);
        // Legacy serialization omits an empty flow collection; empty modules still have valid wiring.
        if (root != null && root.isObject() && root.hasNonNull("name") && !root.has("flows"))
            ((ObjectNode) root).putArray("flows");
        if (root == null || !root.isObject() || !root.path("flows").isArray())
            throw new IOException("Expected a Studio module model with flows");
        ObjectNode selected = mapper.createObjectNode();
        for (String key : List.of("name", "applicationPackageName"))
            if (root.has(key)) selected.set(key, root.get(key));
        List<String> flows = new ArrayList<>();
        for (JsonNode flow : root.path("flows")) {
            ObjectNode entry = mapper.createObjectNode();
            entry.set("name", flow.path("name"));
            entry.set("consumer", component(flow.path("consumer"), fields, mapper));
            List<String> components = new ArrayList<>();
            for (JsonNode element : flow.path("flowElements"))
                components.add(component(element, fields, mapper).toString());
            Collections.sort(components);
            entry.set("components", mapper.valueToTree(components));
            flows.add(entry.toString());
        }
        Collections.sort(flows);
        selected.set("flows", mapper.valueToTree(flows));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(selected.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : digest) hex.append(String.format("%02x", value & 255));
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    /** Read shared test wiring from the native documents without a Studio runtime dependency. */
    private static JsonNode wiringModel(JsonNode root, ObjectMapper mapper) throws IOException {
        if (root == null || !root.has("modelFormat")) return root;
        if (!"ikasan-studio-documents".equals(root.path("modelFormat").asText()) || root.path("formatVersion").asInt() != 1)
            throw new IOException("Unsupported Studio model format; refresh the flow-test support utilities.");
        JsonNode module = root.path("module"), studio = root.path("studio");
        if (!module.path("flows").isArray() || !studio.path("properties").isObject() || !root.path("configuration").isArray())
            throw new IOException("Incomplete Ikasan model documents");
        ObjectNode result = studio.path("properties").deepCopy();
        result.set("name", module.path("name"));
        var flows = result.putArray("flows");
        for (JsonNode flow : module.path("flows")) {
            ObjectNode output = flows.addObject();
            output.set("name", flow.path("name"));
            JsonNode settings = studio.path("flows").path(flow.path("name").asText()).path("components");
            if (flow.path("consumer").isObject()) output.set("consumer", wiringComponent(flow.path("consumer"), settings, root, mapper));
            var elements = output.putArray("flowElements");
            for (JsonNode c : flow.path("flowElements")) {
                if (!c.path("componentName").equals(flow.path("consumer").path("componentName")))
                    elements.add(wiringComponent(c, settings, root, mapper));
            }
        }
        return result;
    }

    private static JsonNode wiringComponent(JsonNode component, JsonNode settings, JsonNode documents, ObjectMapper mapper) throws IOException {
        JsonNode extra = settings.path(component.path("componentName").asText());
        if (!extra.path("properties").isObject()) throw new IOException("Missing Studio component metadata");
        ObjectNode result = extra.path("properties").deepCopy();
        if (extra.path("pendingConfiguration").isObject()) result.setAll((ObjectNode) extra.path("pendingConfiguration"));
        for (JsonNode record : documents.path("configuration")) {
            if (component.hasNonNull("configurationId") && component.path("configurationId").equals(record.path("configurationId")))
                for (JsonNode parameter : record.path("parameters")) result.set(parameter.path("name").asText(), parameter.path("value"));
        }
        for (String field : List.of("componentName", "componentType", "implementingClass")) result.set(field, component.path(field));
        JsonNode identity = variant(documents.path("studio").path("metaPack").asText(), extra.path("component").asText(), mapper);
        result.set("implementingClass", identity.path("implementingClass"));
        if (identity.hasNonNull("additionalKey")) result.set("additionalKey", identity.get("additionalKey"));
        if (extra.path("properties").has("requiresStub") && !extra.path("properties").path("requiresStub").asBoolean(true))
            result.set("userImplementedClassName", component.path("implementingClass"));
        return result;
    }

    private static JsonNode component(JsonNode component, Set<String> fields, ObjectMapper mapper) {
        ObjectNode result = mapper.createObjectNode();
        Set<String> keys = new TreeSet<>(fields);
        keys.addAll(List.of("componentName", "componentType", "implementingClass", "additionalKey", "userImplementedClassName"));
        for (String key : keys) {
            JsonNode value = component.get(key);
            if (value != null && !value.isNull() && !(value.isTextual() && value.asText().isBlank()))
                result.set(key, value.isValueNode() ? mapper.getNodeFactory().textNode(value.asText()) : canonical(value, mapper));
        }
        return result;
    }

    private static JsonNode variant(String pack, String key, ObjectMapper mapper) throws IOException {
        try {
            var meta = org.ikasan.studio.core.metapack.ComponentLibrary.getIkasanComponents(pack).get(key);
            if (meta == null) throw new IOException("Unknown component variant " + key);
            return mapper.createObjectNode().put("implementingClass", meta.getImplementingClass()).put("additionalKey", meta.getAdditionalKey());
        } catch (org.ikasan.studio.core.StudioBuildException e) { throw new IOException("Cannot load component identities", e); }
    }

    private static JsonNode canonical(JsonNode value, ObjectMapper mapper) {
        if (value.isObject()) {
            ObjectNode sorted = mapper.createObjectNode();
            Set<String> names = new TreeSet<>();
            value.fieldNames().forEachRemaining(names::add);
            for (String name : names) sorted.set(name, canonical(value.get(name), mapper));
            return sorted;
        }
        if (value.isArray()) {
            var array = mapper.createArrayNode();
            for (JsonNode item : value) array.add(canonical(item, mapper));
            return array;
        }
        return value;
    }
}
