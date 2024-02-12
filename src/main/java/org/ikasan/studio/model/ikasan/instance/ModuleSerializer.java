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
        jsonGenerator.writeFieldName("flows");
        jsonGenerator.writeStartArray();
        for (Flow flow : module.getFlows()) {
//            flowSerializer.serialize(flow, jsonGenerator,serializerProvider);
            flowSerializer.serializePayload(flow, jsonGenerator);
        }
        jsonGenerator.writeEndArray();

        ikasanElementSerializer.serializePayload(module, jsonGenerator);
//        ikasanElementSerializer.serialize(module, jsonGenerator, serializerProvider);
        jsonGenerator.writeEndObject();
    }

//    protected void serializePayload(IkasanElement ikasanElement, JsonGenerator jsonGenerator) throws IOException {
//        Map<String, IkasanComponentPropertyInstance> properties = ikasanElement.getConfiguredProperties();
//        if (!properties.isEmpty()) {
//            for (IkasanComponentPropertyInstance ikasanComponentPropertyInstance : properties.values()) {
//                jsonGenerator.writeStringField(ikasanComponentPropertyInstance.getMeta().getPropertyName(),
//                        ikasanComponentPropertyInstance.getValue() == null ? "null" : ikasanComponentPropertyInstance.getValue().toString());
//            }
//        }
//    }
}
