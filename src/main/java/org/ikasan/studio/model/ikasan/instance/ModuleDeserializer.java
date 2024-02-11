package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ModuleDeserializer extends StdDeserializer<IkasanElement> {


    public ModuleDeserializer() {
        super(IkasanElement.class);
    }

    public List<Flow> getFlows(JsonNode root) {
        List<Flow> flows = new ArrayList<>();
        if (root.isArray()) {
            ArrayNode arrayNode = (ArrayNode) root;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                flows.add(getFlow(arrayElement));
            }
        }
        return flows;
    }

    public Flow getFlow(JsonNode jsonNode) {
        Flow flow = new Flow();

        if(jsonNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String   fieldName  = field.getKey();
                JsonNodeType type = field.getValue().getNodeType();
                Object value = getTypedValue(type, field);
                flow.setPropertyValue(fieldName, value);
            }
        }
        return flow;
    }
    public Flow getFlowElements(JsonNode jsonNode, Flow flow) {
        FlowElement flowElement;

        if(jsonNode.isObject()) {
            String componentType = jsonNode.get("componentType").asText();
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String   fieldName  = field.getKey();
                JsonNodeType type = field.getValue().getNodeType();
                Object value = getTypedValue(type, field);
                flow.setPropertyValue(fieldName, value);
            }
        }
        return flow;
    }

    @Override
    public Module deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        Module module = new Module();

        JsonNode jsonNode = jp.getCodec().readTree(jp);
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
        while(fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String   fieldName  = field.getKey();
            if ("flows".equals(fieldName)) {
                module.setFlows(getFlows(field.getValue()));
            } else {
                JsonNodeType type = field.getValue().getNodeType();
                Object value = getTypedValue(type, field);
                module.setPropertyValue(fieldName, value);
            }
        }
        return module;
    }

    public Object getTypedValue(JsonNodeType type, Map.Entry<String, JsonNode> field) {
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
