package org.ikasan.studio.model.ikasan.instance.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.ikasan.instance.Transition;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentMeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ModuleDeserializer extends StdDeserializer<Module> {
    public ModuleDeserializer() {
        super(Module.class);
    }

    @Override
    public Module deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        Module module = new Module();

        JsonNode jsonNode = jp.getCodec().readTree(jp);
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
        while(fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String   fieldName  = field.getKey();
            if ("flows".equals(fieldName)) {
                module.setFlows(getFlows(field.getValue(), ctxt));
            } else {
                JsonNodeType type = field.getValue().getNodeType();
                Object value = SerializerUtils.getTypedValue(type, field);
                module.setPropertyValue(fieldName, value);
            }
        }
        return module;
    }

    public List<Flow> getFlows(JsonNode root, DeserializationContext ctxt) throws IOException {
        List<Flow> flows = new ArrayList<>();
        if (root.isArray()) {
            ArrayNode arrayNode = (ArrayNode) root;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                flows.add(getFlow(arrayElement, ctxt));
            }
        }
        return flows;
    }

    public Flow getFlow(JsonNode jsonNode, DeserializationContext ctxt) throws IOException {
        Flow flow = new Flow();

        if(jsonNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String   fieldName  = field.getKey();
                if (Flow.CONSUMER.equals(fieldName)) {
                    flow.setConsumer(getFlowElement(field.getValue()));
                } else if (Flow.TRANSITIONS.equals(fieldName)) {
                    flow.setTransitions(getTransitions(field.getValue(), ctxt));
                } else if (Flow.FLOW_ELEMENTS.equals(fieldName)) {
                    flow.setFlowElements(getFlowElements(field.getValue()));
                } else {

                    JsonNodeType type = field.getValue().getNodeType();
                    Object value = SerializerUtils.getTypedValue(type, field);
                    flow.setPropertyValue(fieldName, value);
                }
            }
        }
        return flow;
    }

    public List<Transition> getTransitions(JsonNode root, DeserializationContext ctxt) throws IOException {
        List<Transition> transitions = new ArrayList<>();
        if (root.isArray()) {
            ArrayNode arrayNode = (ArrayNode) root;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                transitions.add(getTransition(arrayElement, ctxt));
            }
        }
        return transitions;
    }

    public Transition getTransition(JsonNode jsonNode, DeserializationContext ctxt) throws IOException {
        return ctxt.readValue(jsonNode.traverse(), Transition.class);
    }
    public List<FlowElement> getFlowElements(JsonNode root) throws IOException {
        List<FlowElement> flowElements = new ArrayList<>();
        if (root.isArray()) {
            ArrayNode arrayNode = (ArrayNode) root;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                flowElements.add(getFlowElement(arrayElement));
            }
        }
        return flowElements;
    }

    public FlowElement getFlowElement(JsonNode jsonNode) throws IOException {
        FlowElement flowElement = null;

        if(jsonNode.isObject()) {
            String implementingClass = jsonNode.get(IkasanComponentMeta.IMPLEMENTING_CLASS) != null ? jsonNode.get(IkasanComponentMeta.IMPLEMENTING_CLASS).asText() : null;
            String componentType = jsonNode.get(IkasanComponentMeta.COMPONENT_TYPE) != null ? jsonNode.get(IkasanComponentMeta.COMPONENT_TYPE).asText() : null;
            IkasanComponentMeta ikasanComponentMeta = IkasanComponentLibrary.getIkasanComponentByClassOrType(
                    IkasanComponentLibrary.STD_IKASAN_PACK, implementingClass, componentType);
            if (ikasanComponentMeta == null) {
                throw new IOException("Could not create a flow element using implementingClass" + implementingClass + " or componentType " + componentType);
            }
            flowElement = FlowElement.flowElementBuilder().componentMeta(ikasanComponentMeta).build();
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String   fieldName  = field.getKey();
                JsonNodeType type = field.getValue().getNodeType();
                Object value = SerializerUtils.getTypedValue(type, field);
                flowElement.setPropertyValue(fieldName, value);
            }
        }
        return flowElement;
    }




}
