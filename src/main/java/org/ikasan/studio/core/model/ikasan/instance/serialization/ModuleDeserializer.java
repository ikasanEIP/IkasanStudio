package org.ikasan.studio.core.model.ikasan.instance.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.ikasan.studio.core.generator.Generator.FLOWS_TAG;
import static org.ikasan.studio.core.model.ikasan.instance.serialization.SerializerUtils.getTypedValue;

public class ModuleDeserializer extends StdDeserializer<Module> {
    private static final Logger LOG = LoggerFactory.getLogger(ModuleDeserializer.class);
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
            if (FLOWS_TAG.equals(fieldName)) {
                module.setFlows(getFlows(field.getValue()));
            } else {
                Object value = getTypedValue(field);
                module.setPropertyValue(fieldName, value);
            }
        }
        return module;
    }

    public List<Flow> getFlows(JsonNode root) throws IOException {
        List<Flow> flows = new ArrayList<>();
        if (root.isArray()) {
            ArrayNode arrayNode = (ArrayNode) root;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                Flow newFlow = getFlow(arrayElement);
                if (newFlow != null) {
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
            Map<String, FlowElement> flowElementsMap = null;

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String   fieldName  = field.getKey();
                if (Flow.CONSUMER_JSON_TAG.equals(fieldName)) {
                    flow.setConsumer(getFlowElement(field.getValue(), flow));
                } else if (Flow.TRANSITIONS_JSON_TAG.equals(fieldName)) {
                    flow.setTransitions(getTransitions(field.getValue()));
                } else if (Flow.FLOW_ELEMENTS_JSON_TAG.equals(fieldName)) {
                    flowElementsMap = getFlowElements(field.getValue(), flow);
                } else {
                    Object value = getTypedValue(field);
                    flow.setPropertyValue(fieldName, value);
                }
            }
            flow.setFlowElements(orderFlowElementsByTransitions(flow, flowElementsMap));
            // Now we need to sort the elements.
        }
        return flow;
    }

    /**
     * The transitions attribute is used to protect the order of flow element when the flow is serialised
     * This method will return the flow elements in the order dictated by the transitions attribute
     * @param flow to be updated
     * @param flowElementsMap containing componentName -> flowElement
     * @return A list of flow elements in the order dictated by the transitions attribute
     */
    protected List<FlowElement> orderFlowElementsByTransitions(Flow flow, Map<String, FlowElement> flowElementsMap) {
        List<FlowElement> sortedFlowElements = new ArrayList<>();
        if (!flow.getTransitions().isEmpty()) {
            Map<String, Transition> transitionsMap = flow.getTransitions().stream()
                    .collect(Collectors.toMap(Transition::getFrom, Function.identity()));
            Set<String> toKeys  = flow.getTransitions().stream()
                    .map(Transition::getTo)
                    .collect(Collectors.toSet());
            Set<String> fromKeys = new HashSet<>(transitionsMap.keySet());
            // this should leave 1 element, that has a from but never a to i.e. the start of the chain
            fromKeys.removeAll(toKeys);
            Optional<String> startKey = fromKeys.stream().findFirst();
            if (startKey.isEmpty()) {
                LOG.warn("ERROR: Could not find the start of the transition chain " + flow.getTransitions());
            } else {
                Transition transition = transitionsMap.get(startKey.get());
                // If we have a consumer, first flowElement is consumer, so skip
                if (flow.getConsumer() == null) {
                    sortedFlowElements.add(flowElementsMap.get(transition.getFrom()));
                }
                sortedFlowElements.add(flowElementsMap.get(transition.getTo()));

                Transition next = transitionsMap.get(transition.getTo());
                while (next != null) {
                    sortedFlowElements.add(flowElementsMap.get(next.getTo()));
                    next = transitionsMap.get(next.getTo());
                }
            }
        }
        return sortedFlowElements;
    }

    public List<Transition> getTransitions(JsonNode root) {
        List<Transition> transitions = new ArrayList<>();
        if (root.isArray()) {
            ArrayNode arrayNode = (ArrayNode) root;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                Transition newTransition = getTransition(arrayElement);
                if (newTransition != null) {
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
                if (ComponentPropertyMeta.FROM.equals(fieldName)) {
                    transition.setFrom((String) getTypedValue(field));
                } else if (ComponentPropertyMeta.TO.equals(fieldName)) {
                    transition.setTo((String) getTypedValue(field));
                } else if (ComponentPropertyMeta.NAME.equals(fieldName)) {
                    transition.setName((String) getTypedValue(field));
                }
            }
        }
        return transition;
    }
    public Map<String, FlowElement> getFlowElements(JsonNode root, Flow containingFlow) throws IOException {
        Map<String, FlowElement> flowElementsMap = new TreeMap<>();
        if (root.isArray()) {
            ArrayNode arrayNode = (ArrayNode) root;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                FlowElement newFlowElement = getFlowElement(arrayElement, containingFlow);
                if (newFlowElement != null) {
                    flowElementsMap.put(newFlowElement.getComponentName(), newFlowElement);
                }
            }
        }
        return flowElementsMap;
    }

    public FlowElement getFlowElement(JsonNode jsonNode, Flow containingFlow) throws IOException {
        FlowElement flowElement = null;
        // Possibly just open/close brackets
        if(jsonNode.isObject() && !jsonNode.isEmpty()) {
            String implementingClass = jsonNode.get(ComponentMeta.IMPLEMENTING_CLASS) != null ? jsonNode.get(ComponentMeta.IMPLEMENTING_CLASS).asText() : null;
            String componentType = jsonNode.get(ComponentMeta.COMPONENT_TYPE) != null ? jsonNode.get(ComponentMeta.COMPONENT_TYPE).asText() : null;
            String additionalKey = jsonNode.get(ComponentMeta.ADDITIONAL_KEY) != null ? jsonNode.get(ComponentMeta.COMPONENT_TYPE).asText() : null;

            ComponentMeta componentMeta = IkasanComponentLibrary.getIkasanComponentByDeserialisationKey(
                    IkasanComponentLibrary.STD_IKASAN_PACK, implementingClass, componentType, additionalKey);
            if (componentMeta == null) {
                throw new IOException("Could not create a flow element using implementingClass" + implementingClass + " or componentType " + componentType);
            }
            if (componentMeta.isGeneratesUserImplementedClass()) {
                flowElement = FlowUserImplementedElement.flowElementBuilder().componentMeta(componentMeta).containingFlow(containingFlow).build();
            } else {
                flowElement = FlowElement.flowElementBuilder().componentMeta(componentMeta).containingFlow(containingFlow).build();
            }


            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                if (ComponentMeta.IMPLEMENTING_CLASS.equals(fieldName) ||
                        ComponentMeta.COMPONENT_TYPE.equals(fieldName)) {
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
