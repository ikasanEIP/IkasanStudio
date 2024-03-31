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
//        ExceptionResolverSerializer exceptionResolverSerializer = new ExceptionResolverSerializer();

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
            Transition firstTransition = Transition.builder()
                .from(flow.getConsumer().getComponentName())
                .to(flow.getFlowRoute().getFlowElements().get(0).getComponentName())
                    .build();
            transitions.add(firstTransition);
            processFlowRouteTransitions(flow.getConsumer().getComponentName(), transitions, Arrays.asList(flow.getFlowRoute()));

//            populateTransitionsFromFlowElements(flow, transitions);
        // Scenario2: Flow has multiple flow elements but no consumer
        // E1 -> E2
        } else if ( flow.getFlowRoute().getFlowElements() != null &&
                    flow.getFlowRoute().getFlowElements().size() > 1) {
            processFlowRouteTransitions(null, transitions, Arrays.asList(flow.getFlowRoute()));
//            populateTransitionsFromFlowElements(flow, transitions);
        }

        if (transitions != null && !transitions.isEmpty()) {
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

//            for (FlowElement flowElement : flow.getFlowElements()) {
//                jsonGenerator.writeStartObject();
//                flowElementSerializer.serializePayload(flowElement, jsonGenerator);
//                jsonGenerator.writeEndObject();
//            }

            jsonGenerator.writeEndArray();
        }

//        basicElementSerializer.serializePayload(flow.getExceptionResolver(), jsonGenerator);
//        if (flow.getFlowElements() != null && !flow.getFlowElements().isEmpty()) {
//            jsonGenerator.writeArrayFieldStart(Flow.FLOW_ELEMENTS_JSON_TAG);
//            for (FlowElement flowElement : flow.getFlowElements()) {
//                jsonGenerator.writeStartObject();
//                flowElementSerializer.serializePayload(flowElement, jsonGenerator);
//                jsonGenerator.writeEndObject();
//            }
//            jsonGenerator.writeEndArray();
//        }
//
//        basicElementSerializer.serializePayload(flow.getExceptionResolver(), jsonGenerator);
    }

    protected void processFlowRouteTransitions(String startElement, List<Transition> transitions, List<FlowRoute> flowRoutes) {

        if (flowRoutes != null && !flowRoutes.isEmpty()) {
            for (FlowRoute flowRoute : flowRoutes) {
                // process the elements at this level
                if (flowRoute.getFlowElements() != null && !flowRoute.getFlowElements().isEmpty()) {
                    List<FlowElement> flowElements = flowRoute.getFlowElements();
                    populateTransitionsFromFlowElements(startElement, transitions, flowElements);
                    FlowElement lastInFlow = flowElements.get(flowElements.size() - 1);
                    startElement = lastInFlow.getComponentName();
                }
                processFlowRouteTransitions(startElement, transitions, flowRoute.getChildRoutes());

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

////need to get froms
//    /**
//     * Update the transitions in the supplied flow
//     */
//    private void populateTransitionsFromFlowElements(List<Transition> transitions, List<FlowElement> flowElements) {
//        if (flowElements!=null && !flowElements.isEmpty()) {
//            for(int index = 1; index < flowElements.size(); index++) {
//                transitions.add(
//                    Transition.builder()
//                        .from(flowElements.get(index-1).getComponentName())
//                        .to(flowElements.get(index).getComponentName())
//                        .build()
//                );
//            }
//        }
//    }

    /**
     * Build up the transitions using the supplied properties
     * @param startElement if we are at a branch caused by a multi-recipient router, or we have a consumer
     * @param transitions the growing list of transitions to be updated
     * @param flowElements to be interrogated
     */
    private void populateTransitionsFromFlowElements(String startElement, List<Transition> transitions, List<FlowElement> flowElements) {

        if (flowElements!=null && !flowElements.isEmpty()) {
            if (startElement != null) {
                transitions.add(Transition.builder()
                        .from(startElement)
                        .to(flowElements.get(0).getComponentName())
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
}
