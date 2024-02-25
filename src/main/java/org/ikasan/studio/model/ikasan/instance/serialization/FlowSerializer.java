package org.ikasan.studio.model.ikasan.instance.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.model.ikasan.instance.Transition;

import java.io.IOException;

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
        IkasanElementSerializer ikasanElementSerializer = new IkasanElementSerializer();
        FlowElementSerializer flowElementSerializer = new FlowElementSerializer();

        // The properties for the flow itself
        ikasanElementSerializer.serializePayload(flow, jsonGenerator);

        if (flow.getConsumer() != null) {
            jsonGenerator.writeFieldName(Flow.CONSUMER);
            jsonGenerator.writeStartObject();
            flowElementSerializer.serializePayload(flow.getConsumer(), jsonGenerator);
            jsonGenerator.writeEndObject();
        }

        // Transitions exists to preserve the order of elements to/from JSON so only need to be generated during serialisation
        // Scenario1: Flow has consumer and at least 1 other element
        flow.getTransitions().clear();
        if (    flow.getConsumer() != null &&
                flow.getFlowElements() != null &&
                !flow.getFlowElements().isEmpty()) {
            Transition firstTransition = Transition.builder()
                .from(flow.getConsumer().getComponentName())
                .to(flow.getFlowElements().get(0).getComponentName())
                    .build();
            flow.getTransitions().add(firstTransition);
            populateTransitionsFromFlowElements(flow);
        // Scenario2: Flow has multiple flow elements but no consumer
        } else if ( flow.getFlowElements() != null &&
                    flow.getFlowElements().size() > 1) {
            populateTransitionsFromFlowElements(flow);
        }

        if (flow.getTransitions() != null && !flow.getTransitions().isEmpty()) {
            // Since transitions are simple pojos, we can use the default serialiser
            serializerProvider.defaultSerializeField(Flow.TRANSITIONS, flow.getTransitions(), jsonGenerator);
        }

        if (flow.getFlowElements() != null && !flow.getFlowElements().isEmpty()) {
            jsonGenerator.writeArrayFieldStart(Flow.FLOW_ELEMENTS);
            for (FlowElement flowElement : flow.getFlowElements()) {
                jsonGenerator.writeStartObject();
                flowElementSerializer.serializePayload(flowElement, jsonGenerator);
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
        }

        ikasanElementSerializer.serializePayload(flow.getExceptionResolver(), jsonGenerator);
    }

    /**
     * Update the transitions in the supplied flow
     * @param flow to be updated
     */
    private void populateTransitionsFromFlowElements(Flow flow) {
        if (flow.getFlowElements().size() > 1) {
            for(int index = 1; index < flow.getFlowElements().size(); index++) {
                flow.getTransitions().add(
                    Transition.builder()
                        .from(flow.getFlowElements().get(index-1).getComponentName())
                        .to(flow.getFlowElements().get(index).getComponentName())
                        .build()
                );
            }
        }
    }
}
