package org.ikasan.studio.model.ikasan.instance.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

import static org.ikasan.studio.model.ikasan.instance.serialization.SerializerUtils.getTypedValue;
import static org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta.*;

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
                module.setFlows(getFlows(field.getValue()));
            } else {
                Object value = getTypedValue(field);
                module.setPropertyValue(fieldName, value);
            }
        }
        return module;
    }

    public List<Flow> getFlows(JsonNode root) throws IOException {
        List<Flow> flows = null;
        if (root.isArray()) {
            ArrayNode arrayNode = (ArrayNode) root;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                Flow newFlow = getFlow(arrayElement);
                if (newFlow != null) {
                    if (flows == null) {
                        flows = new ArrayList<>();
                    }
                    flows.add(newFlow);
                }
            }
        }
        return flows;
    }

    public Flow getFlow(JsonNode jsonNode) throws IOException {
        Flow flow = null;

        if(jsonNode.isObject() && !jsonNode.isEmpty()) {
            flow = new Flow();
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String   fieldName  = field.getKey();
                if (Flow.CONSUMER.equals(fieldName)) {
                    flow.setConsumer(getFlowElement(field.getValue()));
                } else if (Flow.TRANSITIONS.equals(fieldName)) {
                    flow.setTransitions(getTransitions(field.getValue()));
                } else if (Flow.FLOW_ELEMENTS.equals(fieldName)) {
                    flow.setFlowElements(getFlowElements(field.getValue()));
                } else {
                    Object value = getTypedValue(field);
                    flow.setPropertyValue(fieldName, value);
                }
            }
        }
        return flow;
    }

    public List<Transition> getTransitions(JsonNode root) {
        List<Transition> transitions = null;
        if (root.isArray()) {
            ArrayNode arrayNode = (ArrayNode) root;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                Transition newTransition = getTransition(arrayElement);
                if (newTransition != null) {
                    if (transitions == null) {
                        transitions = new ArrayList<>();
                    }
                    transitions.add(newTransition);
                }
            }
        }
        return transitions;
    }

    public Transition getTransition(JsonNode jsonNode)  {
        Transition transition = null;
        if (!jsonNode.isEmpty()) {
            transition = new Transition();
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                if (FROM.equals(fieldName)) {
                    transition.setFrom((String) getTypedValue(field));
                } else if (TO.equals(fieldName)) {
                    transition.setTo((String) getTypedValue(field));
                } else if (NAME.equals(fieldName)) {
                    transition.setName((String) getTypedValue(field));
                }
            }
        }
        return transition;
    }
    public List<FlowElement> getFlowElements(JsonNode root) throws IOException {
        List<FlowElement> flowElements = null;
        if (root.isArray()) {
            ArrayNode arrayNode = (ArrayNode) root;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                FlowElement newFlowElement = getFlowElement(arrayElement);
                if (newFlowElement != null) {
                    if (flowElements == null) {
                        flowElements = new ArrayList<>();
                    }
                    flowElements.add(newFlowElement);
                }
            }
        }
        return flowElements;
    }

    public FlowElement getFlowElement(JsonNode jsonNode) throws IOException {
        FlowElement flowElement = null;
        // Possibly just open/close brackets
        if(jsonNode.isObject() && !jsonNode.isEmpty()) {
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
                String fieldName = field.getKey();
                if (IkasanComponentMeta.IMPLEMENTING_CLASS.equals(fieldName) ||
                        IkasanComponentMeta.COMPONENT_TYPE.equals(fieldName)) {
                    // these special components are actually meta and captured above, used to identify the component.
                    continue;
                }

                Object value = getTypedValue(field);
                flowElement.setPropertyValue(fieldName, value);
            }
        }
        return flowElement;
    }
}
