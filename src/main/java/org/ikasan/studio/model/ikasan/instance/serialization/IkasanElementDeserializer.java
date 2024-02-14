package org.ikasan.studio.model.ikasan.instance.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.IntNode;
import org.ikasan.studio.model.ikasan.instance.IkasanElement;

import java.io.IOException;

public class IkasanElementDeserializer extends StdDeserializer<IkasanElement> {


    public  IkasanElementDeserializer () {
        super(IkasanElement.class);
    }
    protected IkasanElementDeserializer(Class<?> vc) {
        super(vc);
    }

    protected IkasanElementDeserializer(JavaType valueType) {
        super(valueType);
    }

    protected IkasanElementDeserializer(StdDeserializer<?> src) {
        super(src);
    }

    @Override
    public IkasanElement deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        int id = (Integer) ((IntNode) node.get("id")).numberValue();
        String itemName = node.get("itemName").asText();
        int userId = (Integer) ((IntNode) node.get("createdBy")).numberValue();

        return new IkasanElement();
    }
}
