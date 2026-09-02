package org.ikasan.studio.intellij.project;

import com.fasterxml.jackson.core.JsonProcessingException;
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
            JsonNode value = OBJECT_MAPPER.readTree(json).get(attributeName);
            return value == null || value.isNull() ? null : value.asText();
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }
}
