package org.ikasan.studio.intellij.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Small, PSI-independent JSON query used while locating Studio project metadata. */
final class JsonAttributes {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonAttributes() {
    }

    static String get(String json, String attributeName) {
        if (json == null || attributeName == null) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            JsonNode value;
            if (root != null && root.path("modelFormat").asText().equals("ikasan-studio-documents")) {
                value = "version".equals(attributeName) ? root.path("studio").get("metaPack")
                        : root.path("module").has(attributeName) ? root.path("module").get(attributeName)
                        : root.path("studio").path("properties").get(attributeName);
            } else value = root == null ? null : root.get(attributeName);
            return value == null || value.isNull() ? null : value.asText();
        } catch (java.io.IOException ignored) {
            return null;
        }
    }
}
