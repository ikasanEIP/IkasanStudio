package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class ModuleDeserializer extends StdDeserializer<IkasanElement> {


    public ModuleDeserializer() {
        super(IkasanElement.class);
    }

    @Override
    public Module deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        // The list of name value pairs need to be assigned to the properties of the module
        JsonNode jsonNode = jp.getCodec().readTree(jp);
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
        Module module = new Module();
        fields.forEachRemaining(field -> module.setPropertyValue(field.getKey(),field.getValue().asText()));

//        if (jsonNode.isObject()) {
//            Iterator<Entry<String, JsonNode>> fields = jsonNode.fields();
//            fields.forEachRemaining(field -> {
//                keys.add(field.getKey());
//                getAllKeysUsingJsonNodeFieldNames((JsonNode) field.getValue(), keys);
//            });
//        } else if (jsonNode.isArray()) {
//            ArrayNode arrayField = (ArrayNode) jsonNode;
//            arrayField.forEach(node -> {
//                getAllKeysUsingJsonNodeFieldNames(node, keys);
//            });
//        }

        return module;
    }
}
