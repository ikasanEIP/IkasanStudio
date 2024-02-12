package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class FlowSerializer extends StdSerializer<Flow> {

    public FlowSerializer() {
        super(Flow.class);
    }

    protected FlowSerializer(Class<Flow> t) {
        super(t);
    }

    protected FlowSerializer(JavaType type) {
        super(type);
    }

    protected FlowSerializer(Class<?> t, boolean dummy) {
        super(t, dummy);
    }

    protected FlowSerializer(StdSerializer<?> src) {
        super(src);
    }

    @Override
    public void serialize(Flow flow, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializePayload(flow, jsonGenerator);
        //        ikasanElementSerializer.serializePayload(flow, jsonGenerator);


//        IkasanElementSerializer ikasanElementSerializer = new IkasanElementSerializer();
//        ikasanElementSerializer.serialize(flow, jsonGenerator, serializerProvider);
//        ikasanElementSerializer.serialize(flow.getConsumer(), jsonGenerator, serializerProvider);
//        for(FlowElement flowElement : flow.getFlowElements()) {
//            ikasanElementSerializer.serialize(flowElement, jsonGenerator, serializerProvider);
//        }
//        ikasanElementSerializer.serialize(flow.getIkasanExceptionResolver(), jsonGenerator, serializerProvider);


        jsonGenerator.writeEndObject();
    }

    protected void serializePayload(Flow flow, JsonGenerator jsonGenerator) throws IOException {
        IkasanElementSerializer ikasanElementSerializer = new IkasanElementSerializer();

        // The properties for the flow itself
        ikasanElementSerializer.serializePayload(flow, jsonGenerator);

        ikasanElementSerializer.serializePayload(flow.getConsumer(), jsonGenerator);

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
