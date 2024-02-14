package org.ikasan.studio.model.ikasan.instance.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.FlowElement;

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

        // The properties for the flow itself
        ikasanElementSerializer.serializePayload(flow, jsonGenerator);

        if (flow.getConsumer() != null) {
            jsonGenerator.writeFieldName(Flow.CONSUMER);
            jsonGenerator.writeStartObject();
            ikasanElementSerializer.serializePayload(flow.getConsumer(), jsonGenerator);
            jsonGenerator.writeEndObject();
        }

        if (!flow.getTransitions().isEmpty()) {
            // Since transitions are simple pojos, we can use the default serialiser
            serializerProvider.defaultSerializeField(Flow.TRANSITIONS, flow.getTransitions(), jsonGenerator);
        }

        if (!flow.getFlowElements().isEmpty()) {
            jsonGenerator.writeArrayFieldStart(Flow.FLOW_ELEMENTS);
            for (FlowElement flowElement : flow.getFlowElements()) {
                jsonGenerator.writeStartObject();
                ikasanElementSerializer.serializePayload(flowElement, jsonGenerator);
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
        }

        ikasanElementSerializer.serializePayload(flow.getExceptionResolver(), jsonGenerator);
    }
}
