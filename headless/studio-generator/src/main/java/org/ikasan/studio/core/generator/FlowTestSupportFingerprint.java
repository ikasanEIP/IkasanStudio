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
        JsonNode root = mapper.readTree(model);
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

    private static JsonNode component(JsonNode component, Set<String> fields, ObjectMapper mapper) {
        ObjectNode result = mapper.createObjectNode();
        Set<String> keys = new TreeSet<>(fields);
        keys.addAll(List.of("componentName", "componentType", "implementingClass", "additionalKey", "userImplementedClassName"));
        for (String key : keys) {
            JsonNode value = component.get(key);
            if (value != null && !value.isNull() && !(value.isTextual() && value.asText().isBlank()))
                result.set(key, canonical(value, mapper));
        }
        return result;
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
