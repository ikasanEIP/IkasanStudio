package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Map;

public class IkasanElementSerializer extends StdSerializer<IkasanElement> {

    public IkasanElementSerializer() {
        super(IkasanElement.class);
    }

    protected IkasanElementSerializer(Class<IkasanElement> t) {
        super(t);
    }

    protected IkasanElementSerializer(JavaType type) {
        super(type);
    }

    protected IkasanElementSerializer(Class<?> t, boolean dummy) {
        super(t, dummy);
    }

    protected IkasanElementSerializer(StdSerializer<?> src) {
        super(src);
    }

    @Override
    public void serialize(IkasanElement ikasanElement, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        Map<String, IkasanComponentPropertyInstance> properties = ikasanElement.getConfiguredProperties();
        if (!properties.isEmpty()) {
            for (IkasanComponentPropertyInstance ikasanComponentPropertyInstance : properties.values()) {
                jsonGenerator.writeStringField(ikasanComponentPropertyInstance.getMeta().getPropertyName(),
                        ikasanComponentPropertyInstance.getValue() == null ? "null" : ikasanComponentPropertyInstance.getValue().toString());
            }
        }
        jsonGenerator.writeEndObject();
    }
}
