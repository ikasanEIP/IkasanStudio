package org.ikasan.studio.core.model.ikasan.instance.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;

import java.io.IOException;
import java.util.Map;

public class BasicElementSerializer extends StdSerializer<BasicElement> {

    public BasicElementSerializer() {
        super(BasicElement.class);
    }

    protected BasicElementSerializer(Class<BasicElement> t) {
        super(t);
    }

    protected BasicElementSerializer(JavaType type) {
        super(type);
    }

    protected BasicElementSerializer(Class<?> t, boolean dummy) {
        super(t, dummy);
    }

    protected BasicElementSerializer(StdSerializer<?> src) {
        super(src);
    }

    @Override
    public void serialize(BasicElement basicElement, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializePayload(basicElement, jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    protected void serializePayload(BasicElement basicElement, JsonGenerator jsonGenerator) throws IOException {
        // because we are serializing many nested elements, its possible the element is null, in which case we do nothing.
        if (basicElement != null) {
            Map<String, ComponentProperty> properties = basicElement.getConfiguredProperties();

            if (!properties.isEmpty()) {
                for (ComponentProperty componentProperty : properties.values()) {
                    jsonGenerator.writeStringField(
                        componentProperty.getMeta().getPropertyName(),
                        componentProperty.getValue() == null ? "null" : componentProperty.getValue().toString());
                }
            }
        }
    }
}
