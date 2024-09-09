package org.ikasan.studio.core.model.ikasan.instance.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;

import java.io.IOException;

public class FlowElementSerializer extends StdSerializer<FlowElement> {

    public FlowElementSerializer() {
        super(FlowElement.class);
    }

    @Override
    public void serialize(FlowElement flowElement, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializePayload(flowElement, jsonGenerator, serializerProvider);
        jsonGenerator.writeEndObject();
    }

    protected void serializePayload(FlowElement flowElement, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        BasicElementSerializer basicElementSerializer = new BasicElementSerializer();
        basicElementSerializer.serializePayload(flowElement, jsonGenerator);

        // This is metadata but used to identify the element
        jsonGenerator.writeStringField(ComponentMeta.COMPONENT_TYPE_KEY, flowElement.getComponentMeta().getComponentType());
        jsonGenerator.writeStringField(ComponentMeta.IMPLEMENTING_CLASS_KEY, flowElement.getComponentMeta().getImplementingClass());
        if (flowElement.getComponentMeta().getAdditionalKey() != null) {
            jsonGenerator.writeStringField(ComponentMeta.ADDITIONAL_KEY, flowElement.getComponentMeta().getAdditionalKey());
        }

        if (flowElement.getDecorators() != null && !flowElement.getDecorators().isEmpty()) {
            // Since transitions are simple pojos, we can use the default serialiser
            serializerProvider.defaultSerializeField(FlowElement.DECORATORS_JSON_TAG, flowElement.getDecorators(), jsonGenerator);
        }
    }
}
