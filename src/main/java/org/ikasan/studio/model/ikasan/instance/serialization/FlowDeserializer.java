package org.ikasan.studio.model.ikasan.instance.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.IntNode;
import org.ikasan.studio.model.ikasan.instance.Flow;

import java.io.IOException;

public class FlowDeserializer extends StdDeserializer<Flow> {


    public FlowDeserializer() {
        super(Flow.class);
    }
    protected FlowDeserializer(Class<?> vc) {
        super(vc);
    }

    protected FlowDeserializer(JavaType valueType) {
        super(valueType);
    }

    protected FlowDeserializer(StdDeserializer<?> src) {
        super(src);
    }

    @Override
    public Flow deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        int id = (Integer) ((IntNode) node.get("id")).numberValue();
        String itemName = node.get("itemName").asText();
        int userId = (Integer) ((IntNode) node.get("createdBy")).numberValue();

        return new Flow();
    }
}
