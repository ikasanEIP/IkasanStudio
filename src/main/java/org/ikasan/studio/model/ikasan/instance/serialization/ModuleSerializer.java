package org.ikasan.studio.model.ikasan.instance.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.Module;

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
        BasicElementSerializer basicElementSerializer = new BasicElementSerializer();

        jsonGenerator.writeStartObject();
        // First, the module fields
        basicElementSerializer.serializePayload(module, jsonGenerator);

        // Now Flows
        if (module.getFlows() != null && !module.getFlows().isEmpty()) {
            jsonGenerator.writeArrayFieldStart("flows");
            FlowSerializer flowSerializer = new FlowSerializer();
            for (Flow flow : module.getFlows()) {
                jsonGenerator.writeStartObject();
                flowSerializer.serializePayload(flow, jsonGenerator, serializerProvider);
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
        }
        jsonGenerator.writeEndObject();
    }
}
