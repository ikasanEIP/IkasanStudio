package org.ikasan.studio.core.model.ikasan.instance.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.meta.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.ikasan.studio.core.generator.Generator.FLOWS_TAG;
import static org.ikasan.studio.core.model.ikasan.instance.serialization.SerializerUtils.getTypedValue;
import static org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta.VERSION;
import static org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary.DEFAULT_IKASAN_PACK;

public class ModuleDeserializer extends StdDeserializer<Module> {
    private static final Logger LOG = LoggerFactory.getLogger(ModuleDeserializer.class);
    private static final com.intellij.openapi.diagnostic.Logger ILOG = com.intellij.openapi.diagnostic.Logger.getInstance("#ModelRebuildAction");
    public ModuleDeserializer() {
        super(Module.class);
    }

    @Override
    public Module deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode jsonNode = jp.getCodec().readTree(jp);
        String metapackVersion = DEFAULT_IKASAN_PACK;
        JsonNode versionNode = jsonNode.get(VERSION);
        if (versionNode != null) {
            metapackVersion = versionNode.asText();
        } else {
            LOG.warn("The metapackVersion of the module was not stated, using default metapackVersion");
        }

        Module module;
        try {
            module = new Module(metapackVersion);
        } catch (StudioBuildException se) {
            String message = "A StudioBuildException was raised that will compromise functionality, please investigate " + se.getMessage() + Arrays.asList(se.getStackTrace());
            LOG.error(message);
            throw new IOException(message, se);
        }
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
        while(fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName  = field.getKey();
            if (FLOWS_TAG.equals(fieldName)) {
                try {
                    module.setFlows(getFlows(field.getValue(), metapackVersion));
                } catch (StudioBuildException se) {
                    String message = "A StudioBuildException was raised that will compromise functionality, please investigate " + se.getMessage() + Arrays.asList(se.getStackTrace());
                    LOG.error(message);
                    throw new IOException(message, se);
                }
            } else {
                if (fieldName != null && fieldName.equals(VERSION)) {
                    module.setPropertyValue(fieldName, metapackVersion);
                } else {
                    Object value = getTypedValue(field);
                    module.setPropertyValue(fieldName, value);
                }
            }
        }
        return module;
    }

    public List<Flow> getFlows(JsonNode root, String metapackVersion) throws IOException, StudioBuildException {
        List<Flow> flows = new ArrayList<>();
        if (root.isArray()) {
            ArrayNode arrayNode = (ArrayNode) root;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                Flow newFlow = getFlow(arrayElement, metapackVersion);
                if (newFlow != null) {
                    flows.add(newFlow);
                }
            }
        }
        return flows;
    }

    public Flow getFlow(JsonNode jsonNode, String metapackVersion) throws IOException, StudioBuildException {
        Flow flow = null;
        if(jsonNode.isObject() && !jsonNode.isEmpty()) {
            flow = new Flow(metapackVersion);
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
            Map<String, FlowElement> flowElementsMap = null;

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String   fieldName  = field.getKey();
                if (Flow.CONSUMER_JSON_TAG.equals(fieldName)) {
                    flow.setConsumer(getFlowElement(field.getValue(), flow, metapackVersion));
                } else if (Flow.TRANSITIONS_JSON_TAG.equals(fieldName)) {
                    flow.setTransitions(getTransitions(field.getValue()));
                } else if (Flow.FLOW_ELEMENTS_JSON_TAG.equals(fieldName)) {
                    flowElementsMap = getFlowElements(field.getValue(), flow, metapackVersion);
                } else if (Flow.EXCEPTION_RESOLVER_JSON_TAG.equals(fieldName)) {
                    flow.setExceptionResolver(getExceptionResolver(flow, field.getValue(), metapackVersion));
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
                ILOG.warn("ERROR: Could not find the start of the transition chain " + flow.getTransitions());
                LOG.warn("ERROR: Could not find the start of the transition chain " + flow.getTransitions());
            } else {
                Transition transition = transitionsMap.get(startKey.get());
                // If no consumer, first from should be added (remeber, flowElements list excludes the consumer)
                if (flow.getConsumer() == null) {
                    addIfNotNull(sortedFlowElements, flowElementsMap, transition.getFrom());
                }
                addIfNotNull(sortedFlowElements, flowElementsMap, transition.getTo());
                Transition next = transitionsMap.get(transition.getTo());
                while (next != null) {
                    addIfNotNull(sortedFlowElements, flowElementsMap, next.getTo());
                    next = transitionsMap.get(next.getTo());
                }
            }
        }
        return sortedFlowElements;
    }

    /**
     * In user testing, devs removed components but forgot to update the transitions
     * We can be robust here, still use the name of the removed component to daisychain onto the next component,
     * just don't add the missing component into the flowElement list
     * @param sortedFlowElements to be updateed if the found flow element is not null
     * @param flowElementsMap containing the components known to this flow
     * @param componentName to be added
     */
    private void addIfNotNull(List<FlowElement> sortedFlowElements, Map<String, FlowElement> flowElementsMap, String componentName) {
        FlowElement flowElement = flowElementsMap.get(componentName);
        if (flowElement != null) {
            sortedFlowElements.add(flowElement);
        } else {
            LOG.warn("The component named " + componentName + " was present in a transition but was not defined in the flow, known components are [" + flowElementsMap.keySet() + "], assuming it was removed without updating the transition");
        }
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

    /**
     * Deserialize the exception resolver e.g.
     *             "exceptionResolver": {
     *                 "javax.resource.ResourceException.class": {
     *                     "exceptionsCaught": "javax.resource.ResourceException.class",
     *                     "theAction": "ignore"
     *                 },
     *                 "javax.jms.JMSException.class": {
     *                     "exceptionsCaught": "javax.jms.JMSException.class",
     *                     "theAction": "retry",
     *                     "properties": {
     *                         "delay": "1",
     *                         "interval": "2"
     *                     }
     *                 }
     *             },
     * @param jsonNode - the root, i.e. "exceptionResolver"
     * @param metapackVersion to build
     * @return an ExceptionResolver instance
     * @throws StudioBuildException if there were issues with the metapack.
     */
    public ExceptionResolver getExceptionResolver(Flow containingFlow, JsonNode jsonNode, String metapackVersion) throws StudioBuildException {
        ExceptionResolver.ExceptionResolverBuilder exceptionResolverBuilder = ExceptionResolver.exceptionResolverBuilder()
                .containingFlow(containingFlow)
                .metapackVersion(metapackVersion);
        Map<String, ExceptionResolution> ikasanExceptionResolutionMap = new HashMap<>();

        if(jsonNode.isObject() && !jsonNode.isEmpty()) {
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                ikasanExceptionResolutionMap.put(fieldName, getExceptionResolution(field.getValue(), metapackVersion));
            }
        }
        if (!ikasanExceptionResolutionMap.isEmpty()) {
            exceptionResolverBuilder.ikasanExceptionResolutionMap(ikasanExceptionResolutionMap);
        }
        return exceptionResolverBuilder.build();
    }


    /**
     * Deserialize the exception resolver e.g.
     *                 {
     *                     "exceptionsCaught": "javax.jms.JMSException.class",
     *                     "theAction": "retry",
     *                     "properties": {
     *                         "delay": "1",
     *                         "interval": "2"
     *                     }
     *                 }
     *             },
     * @param jsonNode - the root, i.e. "exceptionResolver"
     * @param metapackVersion to build
     * @return an ExceptionResolver instance
     * @throws StudioBuildException if there were issues with the metapack.
     */
    public ExceptionResolution getExceptionResolution(JsonNode jsonNode, String metapackVersion) throws StudioBuildException {
        ExceptionResolution exceptionResolution = null;
        ExceptionResolution.ExceptionResolutionBuilder exceptionResolutionBuilder = ExceptionResolution.exceptionResolutionBuilder()
                .metapackVersion(metapackVersion);

        if(jsonNode.isObject() && !jsonNode.isEmpty()) {
            String exceptionCaught = jsonNode.get(ComponentMeta.EXCEPTIONS_CAUGHT_KEY) != null ? jsonNode.get(ComponentMeta.EXCEPTIONS_CAUGHT_KEY).asText() : null;
            String action = jsonNode.get(ComponentMeta.ACTION_KEY) != null ? jsonNode.get(ComponentMeta.ACTION_KEY).asText() : null;
            if (action == null || exceptionCaught == null) {
                LOG.error("DATA CORRUPTION : Attempting to create an Exception Resolution but mandatory aproperty was not set action=[" + action + "] excepptionCaught [" + exceptionCaught + "] skipping");
            } else {
                exceptionResolutionBuilder.exceptionsCaught(exceptionCaught);
                exceptionResolutionBuilder.theAction(action);

                JsonNode actionProperties = jsonNode.get(ComponentMeta.ACTION_PROPERTIES_KEY);
                if (actionProperties != null) {
                    ExceptionResolverMeta exceptionResolverMeta = (ExceptionResolverMeta) IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metapackVersion, "Exception Resolver");
                    ExceptionActionMeta exceptionActionMeta = exceptionResolverMeta.getExceptionActionWithName(action);
                    if (exceptionActionMeta == null) {
                        LOG.error("DATA CORRUPTION : Attempting to set properties for an unknown action=[" + action + "], skipping");
                    } else {
                        exceptionResolutionBuilder.componentProperties((new TreeMap<>()));
                        exceptionResolution = exceptionResolutionBuilder.build();

                        Iterator<Map.Entry<String, JsonNode>> fields = actionProperties.fields();
                        while (fields.hasNext()) {
                            Map.Entry<String, JsonNode> field = fields.next();
                            String fieldName = field.getKey();
                            Object value = getTypedValue(field);

                            ComponentPropertyMeta componentPropertyMeta = exceptionActionMeta.getMetaProperty(fieldName);
                            if (componentPropertyMeta == null) {
                                LOG.error("DATA CORRUPTION : Attempting to set properties field that has no recorded meta, fieldName=[" + fieldName + "], skipping");
                            } else {
                                exceptionResolution.setPropertyValue(componentPropertyMeta, fieldName, value);
                            }
                        }
                    }
                } else {
                    exceptionResolution = exceptionResolutionBuilder.build();
                }
            }
        }
        return exceptionResolution;
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
    public Map<String, FlowElement> getFlowElements(JsonNode root, Flow containingFlow, String metapackVersion) throws IOException, StudioBuildException {
        Map<String, FlowElement> flowElementsMap = new TreeMap<>();
        if (root.isArray()) {
            ArrayNode arrayNode = (ArrayNode) root;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                FlowElement newFlowElement = getFlowElement(arrayElement, containingFlow, metapackVersion);
                if (newFlowElement != null) {
                    flowElementsMap.put(newFlowElement.getComponentName(), newFlowElement);
                }
            }
        }
        return flowElementsMap;
    }

    public FlowElement getFlowElement(JsonNode jsonNode, Flow containingFlow, String metapackVersion) throws IOException, StudioBuildException {
        FlowElement flowElement = null;
        // Possibly just open/close brackets
        if(jsonNode.isObject() && !jsonNode.isEmpty()) {
            String implementingClass = jsonNode.get(ComponentMeta.IMPLEMENTING_CLASS_KEY) != null ? jsonNode.get(ComponentMeta.IMPLEMENTING_CLASS_KEY).asText() : null;
            String componentType = jsonNode.get(ComponentMeta.COMPONENT_TYPE_KEY) != null ? jsonNode.get(ComponentMeta.COMPONENT_TYPE_KEY).asText() : null;
            String additionalKey = jsonNode.get(ComponentMeta.ADDITIONAL_KEY) != null ? jsonNode.get(ComponentMeta.ADDITIONAL_KEY).asText() : null;

            ComponentMeta componentMeta = IkasanComponentLibrary.getIkasanComponentByDeserialisationKey(
                    metapackVersion, implementingClass, componentType, additionalKey);
            if (componentMeta == null) {
                throw new IOException("Could not create a flow element using implementingClass" + implementingClass + " componentType " + componentType + " additionalKey " +additionalKey);
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
                if (ComponentMeta.IMPLEMENTING_CLASS_KEY.equals(fieldName) ||
                    ComponentMeta.COMPONENT_TYPE_KEY.equals(fieldName) ||
                    ComponentMeta.ADDITIONAL_KEY.equals(fieldName)) {
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
