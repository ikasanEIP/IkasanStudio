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
        List<Transition> transitions = new ArrayList<>();
        Flow flow = null;
        if(jsonNode.isObject() && !jsonNode.isEmpty()) {
            flow = new Flow(metapackVersion);
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
            Map<String, FlowElement> flowElementsMap = new TreeMap<>();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String   fieldName  = field.getKey();
                if (Flow.CONSUMER_JSON_TAG.equals(fieldName)) {
                    flow.setConsumer(getFlowElement(field.getValue(), flow, metapackVersion));
                } else if (Flow.TRANSITIONS_JSON_TAG.equals(fieldName)) {
                    transitions = getTransitions(field.getValue());
                } else if (Flow.FLOW_ELEMENTS_JSON_TAG.equals(fieldName)) {
                    flowElementsMap = getFlowElements(flowElementsMap, field.getValue(), flow, metapackVersion);
                } else if (Flow.EXCEPTION_RESOLVER_JSON_TAG.equals(fieldName)) {
                    flow.setExceptionResolver(getExceptionResolver(flow, field.getValue(), metapackVersion));
                } else {
                    Object value = getTypedValue(field);
                    flow.setPropertyValue(fieldName, value);
                }
            }
            if (flow.getConsumer() != null) {

                flowElementsMap.put(flow.getConsumer().getComponentName(), flow.getConsumer());
            }
            flow.setFlowRoute(orderFlowElementsByTransitions(transitions, flow, flowElementsMap));
        }
        return flow;
    }

    /**
     * Due to Multi-Recipient Routers, the flow of a route is more complex.
     * If there are no MRR, there is 1 FlowRoute, the flow elements go from Consumer -> Producer, there are no childFlowRoutes
     * If there are MRR
     * The FlowRouteLists represent the branches of the MRR
     * default route : Consumer -> 0..n elments -> MRR  route1 : 0..n elements -> producer
     *                                                  route2 : 0..n elements -> producer
     * @param flow to be updated
     * @param flowElementsMap containing componentName -> flowElement
     * @return A list of flow elements in the order dictated by the transitions attribute
     */
    protected FlowRoute orderFlowElementsByTransitions(List<Transition> transitions, Flow flow, Map<String, FlowElement> flowElementsMap) throws StudioBuildException {
        List<FlowElement> firstFlowElements = new ArrayList<>();
        FlowRoute returnFlowRoute = FlowRoute.flowBuilder().flow(flow).routeName(Transition.DEFAULT_TRANSITION_NAME).flowElements(firstFlowElements).build();

        if (!transitions.isEmpty()) {
            String startKey = getStartKey(transitions);
            if (startKey.isEmpty() || startKey.isBlank()) {
                ILOG.warn("ERROR: Could not find the start of the transition chain " + transitions);
                LOG.warn("ERROR: Could not find the start of the transition chain " + transitions);
            } else {
                // MRR have multiple 'from' with the same key, only the 'name' distinguishes them (used to hold the route name)
                // so the key is the element name, the value is a single transition for everything except MRR, for MRR it could
                // be one or many transitions represeting the branches coming from the MRR
                Map<String, List<Transition>> transitionsMap = new HashMap<>();
                for (Transition transition : transitions) {
                    List<Transition> value = transitionsMap.get(transition.getFrom());
                    if (value == null) {
                        List<Transition> transitionList = new ArrayList<>();
                        transitionList.add(transition);
                        transitionsMap.put(transition.getFrom(), transitionList);
                    } else {
                        value.add(transition);
                    }
                }

                List<Transition> transition = transitionsMap.get(startKey);
                if (flow.getConsumer() == null) {
                    // if the transition is not MRR, there is only 1 element, if it is MRR, any of them will do to add to flow elements,
                    addIfNotNull(returnFlowRoute.getFlowElements(), flowElementsMap, transition.get(0).getFrom());
                }
                buildRouteTree(returnFlowRoute, flow, transitionsMap, flowElementsMap, transition);
            }
        }
        return returnFlowRoute;
    }

    /**
     * Determine which transition key is the first in the chain. It will be the only one that is in a from but has no previous to.
     * @param transitions to be checked
     * @return the string representing the 'from' of the first transition.
     */
    private String getStartKey(List<Transition> transitions) {
        Set<String> fromKeys  = transitions.stream()
                .map(Transition::getFrom)
                .collect(Collectors.toSet());

        Set<String> toKeys  = transitions.stream()
                .map(Transition::getTo)
                .collect(Collectors.toSet());
        // this should leave 1 element, that has a from but never a to i.e. the start of the chain
        fromKeys.removeAll(toKeys);
        return fromKeys.stream().findFirst().orElse("");
    }

    /**
     * Update returnFlowRoute, build out the flowElement and nested flowRoute
     * @param returnFlowRoute to be updated
     * @param flow holding the routes
     * @param transitionsMap transition.from -> transition
     * @param flowElementsMap element.nam -> element
     * @param transitions to process. A normal element appears in 1 From and 1 To
     *                    A Multi Recipient Router appears in 1 .. n 'From'
     *                    From: My MRR
     *                    To: xx component
     *                    Name: route1
     *                    From: My MRR
     *                    To: yy component
     *                    Name: route2
     * @throws StudioBuildException if there were problems identifying the correct metapack to use.
     */
    protected void buildRouteTree(FlowRoute returnFlowRoute, Flow flow, Map<String, List<Transition>> transitionsMap, Map<String, FlowElement> flowElementsMap, List<Transition> transitions) throws StudioBuildException {
        if (transitions != null) {
            // Only MRR have lists greater than 1
            if (elementIsRouter(flowElementsMap, transitions.get(0).getFrom()) || transitions.size() > 1) {
                // for each transition, start new brances and threads
                for (Transition transition : transitions) {
                    // If the last element in a flow route is an MRR, it will have the name of the route in it
                    if (elementIsRouter(flowElementsMap, transition.getFrom())) {
                        FlowRoute newChild = FlowRoute.flowBuilder().flow(flow).routeName(transition.getName()).build();
                        returnFlowRoute.getChildRoutes().add(newChild);
                        addIfNotNull(newChild.getFlowElements(), flowElementsMap, transition.getFrom());
                        // Add the rest of this branch to the new child
                        List<Transition> nextTransitions = transitionsMap.get(transition.getTo());
                        buildRouteTree(newChild, flow, transitionsMap, flowElementsMap, nextTransitions);
                    } else {
                        LOG.error("A router was expected, but the element was " + flowElementsMap.get(transition.getFrom()));
                    }
                }

            } else {  // Not a router, add it to this level
                addIfNotNull(returnFlowRoute.getFlowElements(), flowElementsMap, transitions.get(0).getFrom());
                List<Transition> nextTransitions = transitionsMap.get(transitions.get(0).getTo());
                // The last link in the chain
                if (nextTransitions == null) {
                    addIfNotNull(returnFlowRoute.getFlowElements(), flowElementsMap, transitions.get(0).getTo());
                } else {
                    buildRouteTree(returnFlowRoute, flow, transitionsMap, flowElementsMap, nextTransitions);
                }
            }
        }
    }

    /**
     * In user testing, devs removed components but forgot to update the transitions
     * We can be robust here, still use the name of the removed component to daisychain onto the next component,
     * just don't add the missing component into the flowElement list
     * @param sortedFlowElements to be updated if the found flow element is not null
     * @param flowElementsMap containing the components known to this flow
     * @param componentName to be added
     */
    private void addIfNotNull(List<FlowElement> sortedFlowElements, Map<String, FlowElement> flowElementsMap, String componentName) {
        FlowElement flowElement = flowElementsMap.get(componentName);
        if (flowElement != null && !flowElement.getComponentMeta().isConsumer()) {
            sortedFlowElements.add(flowElement);
        } else {
            LOG.warn("While trying to add the component, the component named " + componentName + " was present in a transition but was not defined in the flow, known components are [" + flowElementsMap.keySet() + "], assuming it was removed without updating the transition");
        }
    }

    private boolean elementIsRouter(Map<String, FlowElement> flowElementsMap, String componentName) {
        boolean isRouter = false;
        FlowElement flowElement = flowElementsMap.get(componentName);
        if (flowElement != null) {
            isRouter = flowElement.getComponentMeta().isRouter();
        } else {
            LOG.warn("While checking the type, the component named " + componentName + " was present in a transition but was not defined in the flow, known components are [" + flowElementsMap.keySet() + "], assuming it was removed without updating the transition");
        }
        return isRouter;
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
     * @param metapackVersion to buildRouteTree
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
     * @param metapackVersion to buildRouteTree
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
    public Map<String, FlowElement> getFlowElements(Map<String, FlowElement> flowElementsMap, JsonNode root, Flow containingFlow, String metapackVersion) throws IOException, StudioBuildException {
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
