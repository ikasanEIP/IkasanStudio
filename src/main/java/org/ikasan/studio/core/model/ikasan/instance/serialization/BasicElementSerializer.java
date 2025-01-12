package org.ikasan.studio.core.model.ikasan.instance.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;

import java.io.IOException;
import java.util.Map;

public class BasicElementSerializer extends StdSerializer<BasicElement> {

    public BasicElementSerializer() {
        super(BasicElement.class);
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
            Map<String, ComponentProperty> properties = basicElement.getComponentProperties();

            if (!properties.isEmpty()) {
                for (ComponentProperty componentProperty : properties.values()) {
                    if (componentProperty.getValue() != null) {
                        // Since we need to cast, have to identify type explicitly
                        Class clazz = componentProperty.getValue().getClass();
                        if (clazz == Boolean.class) {
                            jsonGenerator.writeBooleanField(
                                    componentProperty.getMeta().getPropertyName(),
                                    (Boolean) componentProperty.getValue());
                        } else if (clazz == Short.class) {
                            jsonGenerator.writeNumberField(
                                    componentProperty.getMeta().getPropertyName(),
                                    (Short)componentProperty.getValue());
                        } else if (clazz == Integer.class) {
                            jsonGenerator.writeNumberField(
                                    componentProperty.getMeta().getPropertyName(),
                                    (Integer)componentProperty.getValue());
                        } else if (clazz == Long.class) {
                            jsonGenerator.writeNumberField(
                                    componentProperty.getMeta().getPropertyName(),
                                    (Long)componentProperty.getValue());
                        } else if (clazz == Float.class) {
                            jsonGenerator.writeNumberField(
                                    componentProperty.getMeta().getPropertyName(),
                                    (Float)componentProperty.getValue());
                        } else if (clazz == Double.class) {
                            jsonGenerator.writeNumberField(
                                    componentProperty.getMeta().getPropertyName(),
                                    (Double)componentProperty.getValue());
                        } else {
                            jsonGenerator.writeStringField(
                                    componentProperty.getMeta().getPropertyName(),
                                    componentProperty.getValue().toString());
                        }
                    }
                }
            }
        }
    }
}
