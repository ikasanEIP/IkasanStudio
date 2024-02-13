package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

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

        jsonGenerator.writeFieldName("consumer");
        jsonGenerator.writeStartObject();
        ikasanElementSerializer.serializePayload(flow.getConsumer(), jsonGenerator);
        jsonGenerator.writeEndObject();

        // Since transitions are simple pojos, we can use the default serialiser
        serializerProvider.defaultSerializeField("transitions", flow.getTransitions(), jsonGenerator);

        jsonGenerator.writeArrayFieldStart("flowElements");
        for(FlowElement flowElement : flow.getFlowElements()) {
            jsonGenerator.writeStartObject();
            ikasanElementSerializer.serializePayload(flowElement, jsonGenerator);
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();

        ikasanElementSerializer.serializePayload(flow.getIkasanExceptionResolver(), jsonGenerator);
    }
}
