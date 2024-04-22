package org.ikasan.studio.core.model.ikasan.instance.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.ikasan.studio.core.model.ikasan.instance.Decorator;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;

import java.io.IOException;

public class DecoratorSerializer extends StdSerializer<Decorator> {

    public DecoratorSerializer() {
        super(Decorator.class);
    }

    @Override
    public void serialize(Decorator decorator, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializePayload(decorator, jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    protected void serializePayload(Decorator decorator, JsonGenerator jsonGenerator) throws IOException {
        // This is metadata but used to identify the element
        jsonGenerator.writeStringField(ComponentMeta.TYPE_KEY, decorator.getType());
        jsonGenerator.writeStringField(ComponentMeta.NAME_KEY, decorator.getName());
        jsonGenerator.writeStringField(ComponentMeta.CONFIGURATION_ID_KEY, decorator.getConfigurationId());
        jsonGenerator.writeStringField(ComponentMeta.CONFIGURABLE_KEY, Boolean.toString(decorator.isConfigurable()));
    }
}
