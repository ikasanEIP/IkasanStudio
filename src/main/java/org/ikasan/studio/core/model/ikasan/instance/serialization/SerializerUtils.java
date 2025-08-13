package org.ikasan.studio.core.model.ikasan.instance.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import java.util.Map;

public class SerializerUtils {
    /**
     * Utility method to extract a typed value from a JSON field.
     * It handles Boolean, Number, and String types.
     *
     * @param field the Map.Entry containing the field name and JsonNode value
     * @return the extracted value as types Java Object, or null if the JsonNode is null
     */
    public static Object getTypedValue(Map.Entry<String, JsonNode> field) {
        Object value = null;
        if (field.getValue() != null) {
            JsonNodeType type = field.getValue().getNodeType();
            if (JsonNodeType.BOOLEAN == type) {
                value = field.getValue().asBoolean();
            } else if (JsonNodeType.NUMBER == type) {
                value = field.getValue().asLong();
            } else {
                value = field.getValue().asText();
            }
        }
        return value;
    }
}
