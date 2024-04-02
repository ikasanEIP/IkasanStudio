package org.ikasan.studio.core.model.ikasan.instance.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.ikasan.studio.core.model.ikasan.instance.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlowSerializer extends StdSerializer<Flow> {

    public FlowSerializer() {
        super(Flow.class);
    }

    @Override
    public void serialize(Flow flow, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializePayload(flow, jsonGenerator, serializerProvider);
        jsonGenerator.writeEndObject();
    }

    protected void serializePayload(Flow flow, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        List<Transition> transitions = new ArrayList<>();
        BasicElementSerializer basicElementSerializer = new BasicElementSerializer();
        FlowElementSerializer flowElementSerializer = new FlowElementSerializer();

        // The properties for the flow itself i.e. name: and description:
        basicElementSerializer.serializePayload(flow, jsonGenerator);

        if (flow.getConsumer() != null) {
            jsonGenerator.writeFieldName(Flow.CONSUMER_JSON_TAG);
            jsonGenerator.writeStartObject();
            flowElementSerializer.serializePayload(flow.getConsumer(), jsonGenerator);
            jsonGenerator.writeEndObject();
        }

        // Transitions exists to preserve the order of elements to/from JSON so only need to be generated during serialisation
        // Scenario1: Flow has consumer and at least 1 other element
        // Consumer -> E1, E1->E2 ....
        if (    flow.getConsumer() != null &&
                flow.getFlowRoute().getFlowElements() != null &&
                !flow.getFlowRoute().getFlowElements().isEmpty()) {
            processFlowRouteTransitions(flow.getConsumer().getComponentName(), transitions, flow.getFlowRoute());
        // Scenario2: Flow has multiple flow elements but no consumer
        // E1 -> E2
        } else if ( flow.getFlowRoute().getFlowElements() != null &&
                    flow.getFlowRoute().getFlowElements().size() > 1) {
            processFlowRouteTransitions(null, transitions, flow.getFlowRoute());
        }

        if (!transitions.isEmpty()) {
            // Since transitions are simple pojos, we can use the default serialiser
            serializerProvider.defaultSerializeField(Flow.TRANSITIONS_JSON_TAG, transitions, jsonGenerator);
        }

        if (    flow.getExceptionResolver() != null &&
                flow.getExceptionResolver().getIkasanExceptionResolutionMap() != null &&
                !flow.getExceptionResolver().getIkasanExceptionResolutionMap().isEmpty()) {
            // Since transitions are simple pojos, we can use the default serialiser
            serializerProvider.defaultSerializeField(Flow.EXCEPTION_RESOLVER_JSON_TAG, flow.getExceptionResolver(), jsonGenerator);
        }

        if (    flow.getFlowRoute().getFlowElements() != null &&
                !flow.getFlowRoute().getFlowElements().isEmpty()) {
            jsonGenerator.writeArrayFieldStart(Flow.FLOW_ELEMENTS_JSON_TAG);
            processFlowRouteFlowElements(Arrays.asList(flow.getFlowRoute()), jsonGenerator, flowElementSerializer);

            jsonGenerator.writeEndArray();
        }
    }


    /**
     * Flatten out all the flowRoutes so that they can be processed by populateTransitionsFromFlowElements
     * @param startElement if we are at a branch caused by a multi-recipient router (from recursive call), or we have a consumer
     * @param transitions the growing list of transitions to be updated
     * @param flowRoute to be interrogated
     */
    protected void processFlowRouteTransitions(String startElement, List<Transition> transitions, FlowRoute flowRoute) {
        if (flowRoute != null) {
            String routeName = flowRoute.getRouteName();
            // process the elements at this level
            List<FlowElement> flowElements = flowRoute.getFlowElements();
            String nextStartElement = null;
            if (flowElements != null && !flowElements.isEmpty()) {
                populateTransitionsFromFlowElements(startElement, routeName, transitions, flowElements);

                // Get the next start element, which might be the MRR
                FlowElement lastInFlow = flowElements.get(flowElements.size() - 1);
                nextStartElement = lastInFlow.getComponentName();
            }

            if (flowRoute.getChildRoutes() != null && !flowRoute.getChildRoutes().isEmpty()) {
                for (FlowRoute childFlowRoute : flowRoute.getChildRoutes()) {
                    processFlowRouteTransitions(nextStartElement, transitions, childFlowRoute);
                }
            }
        }
    }

    /**
     * Build up the transitions using the supplied properties
     * @param startElement if we are at a branch caused by a multi-recipient router, or we have a consumer
     * @param routeName this will be default of the name of the current MRR route
     * @param transitions the growing list of transitions to be updated
     * @param flowElements to be interrogated
     */
    private void populateTransitionsFromFlowElements(String startElement, String routeName, List<Transition> transitions, List<FlowElement> flowElements) {
        if (flowElements!=null && !flowElements.isEmpty()) {
            if (startElement != null) {
                transitions.add(Transition.builder()
                        .from(startElement)
                        .to(flowElements.get(0).getComponentName())
                        .name(routeName)
                        .build());
            }
            for(int index = 1; index < flowElements.size(); index++) {
                transitions.add(
                    Transition.builder()
                        .from(flowElements.get(index-1).getComponentName())
                        .to(flowElements.get(index).getComponentName())
                        .build()
                );
            }
        }
    }

    protected void processFlowRouteFlowElements(List<FlowRoute> flowRoutes, JsonGenerator jsonGenerator, FlowElementSerializer flowElementSerializer) throws IOException {
        if (flowRoutes != null && !flowRoutes.isEmpty()) {
            for (FlowRoute flowRoute : flowRoutes) {
                if (flowRoute.getFlowElements() != null && !flowRoute.getFlowElements().isEmpty()) {
                    for (FlowElement flowElement : flowRoute.getFlowElements()) {
                        jsonGenerator.writeStartObject();
                        flowElementSerializer.serializePayload(flowElement, jsonGenerator);
                        jsonGenerator.writeEndObject();
                    }
                }
                processFlowRouteFlowElements(flowRoute.getChildRoutes(), jsonGenerator, flowElementSerializer);
            }
        }
    }
}
