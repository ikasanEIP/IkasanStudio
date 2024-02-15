package org.ikasan.studio.model.ikasan.instance.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.ikasan.studio.model.ikasan.instance.IkasanComponentProperty;
import org.ikasan.studio.model.ikasan.instance.IkasanElement;

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
        serializePayload(ikasanElement, jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    protected void serializePayload(IkasanElement ikasanElement, JsonGenerator jsonGenerator) throws IOException {
        // because we are serializing many nested elements, its possible the element is null, in which case we do nothing.
        if (ikasanElement != null) {
            Map<String, IkasanComponentProperty> properties = ikasanElement.getConfiguredProperties();

            if (!properties.isEmpty()) {
                for (IkasanComponentProperty ikasanComponentProperty : properties.values()) {
                    jsonGenerator.writeStringField(
                        ikasanComponentProperty.getMeta().getPropertyName(),
                        ikasanComponentProperty.getValue() == null ? "null" : ikasanComponentProperty.getValue().toString());
                }
            }
        }
    }
}
