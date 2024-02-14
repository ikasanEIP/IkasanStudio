package org.ikasan.studio.model.ikasan.instance.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import java.util.Map;

public class SerializerUtils {
    public static Object getTypedValue(JsonNodeType type, Map.Entry<String, JsonNode> field) {
        Object value;
        if (JsonNodeType.BOOLEAN == type) {
            value = field.getValue().asBoolean();
        } else if (JsonNodeType.NUMBER == type) {
            value = field.getValue().asLong();
        } else {
            value = field.getValue().asText();
        }
        return value;
    }
}
