package org.ikasan.studio.model.ikasan.instance.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.IntNode;
import org.ikasan.studio.model.ikasan.instance.BasicElement;

import java.io.IOException;

public class BasicElementDeserializer extends StdDeserializer<BasicElement> {


    public BasicElementDeserializer() {
        super(BasicElement.class);
    }
    protected BasicElementDeserializer(Class<?> vc) {
        super(vc);
    }

    protected BasicElementDeserializer(JavaType valueType) {
        super(valueType);
    }

    protected BasicElementDeserializer(StdDeserializer<?> src) {
        super(src);
    }

    @Override
    public BasicElement deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        int id = (Integer) ((IntNode) node.get("id")).numberValue();
        String itemName = node.get("itemName").asText();
        int userId = (Integer) ((IntNode) node.get("createdBy")).numberValue();

        return new BasicElement();
    }
}
