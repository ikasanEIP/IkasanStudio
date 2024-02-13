package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class ModuleSerializer extends StdSerializer<Module> {

    public ModuleSerializer() {
        super(Module.class);
    }

    protected ModuleSerializer(Class<Module> t) {
        super(t);
    }

    protected ModuleSerializer(JavaType type) {
        super(type);
    }

    protected ModuleSerializer(Class<?> t, boolean dummy) {
        super(t, dummy);
    }

    protected ModuleSerializer(StdSerializer<?> src) {
        super(src);
    }

    @Override
    public void serialize(Module module, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        IkasanElementSerializer ikasanElementSerializer = new IkasanElementSerializer();
        FlowSerializer flowSerializer = new FlowSerializer();

        jsonGenerator.writeStartObject();
        // First, the module fields
        ikasanElementSerializer.serializePayload(module, jsonGenerator);

        // Now Flows
        jsonGenerator.writeArrayFieldStart("flows");
        for (Flow flow : module.getFlows()) {
            jsonGenerator.writeStartObject();
            flowSerializer.serializePayload(flow, jsonGenerator, serializerProvider);
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }

}
