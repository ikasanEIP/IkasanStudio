package org.ikasan.studio.core.persistence.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import org.ikasan.studio.core.model.StringCollectionValues;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;

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

    /**
     * Serializes the properties of a BasicElement into JSON fields.
     * Handles null elements and supports multiple value types (Boolean, Short, Integer, Long, Float, Double, String).
     *
     * @param basicElement the BasicElement to serialize
     * @param jsonGenerator the JsonGenerator used for writing JSON
     * @throws IOException if an I/O error occurs
     */
    protected void serializePayload(BasicElement basicElement, JsonGenerator jsonGenerator) throws IOException {
        // because we are serializing many nested elements, its possible the element is null, in which case we do nothing.
        if (basicElement != null) {
            for (Entry<String,JsonNode> extension : basicElement.getUnknownJsonProperties().entrySet()) {
                jsonGenerator.writeFieldName(extension.getKey());
                jsonGenerator.writeTree(extension.getValue());
            }
            Map<String, ComponentProperty> properties = basicElement.getComponentProperties();

            if (!properties.isEmpty()) {
                for (ComponentProperty componentProperty : properties.values()) {
                    if (componentProperty.getValue() != null) {
                        // Since we need to cast, have to identify type explicitly
                        Class<?> clazz = componentProperty.getValue().getClass();
                        if (StringCollectionValues.supports(componentProperty.getMeta().getPropertyDataType())) {
                            jsonGenerator.writeObjectField(componentProperty.getMeta().getPropertyName(), componentProperty.getValue());
                        } else if (clazz == Boolean.class) {
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
                                    componentProperty.getValue() instanceof List<?>
                                            ? componentProperty.getValueString() : componentProperty.getValue().toString());
                        }
                    }
                }
            }
        }
    }
}
