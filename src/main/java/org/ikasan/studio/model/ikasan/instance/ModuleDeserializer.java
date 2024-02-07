package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class ModuleDeserializer extends StdDeserializer<IkasanElement> {


    public ModuleDeserializer() {
        super(IkasanElement.class);
    }
    protected ModuleDeserializer(Class<?> vc) {
        super(vc);
    }

    protected ModuleDeserializer(JavaType valueType) {
        super(valueType);
    }

    protected ModuleDeserializer(StdDeserializer<?> src) {
        super(src);
    }

    @Override
    public IkasanElement deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        // The list of name value pairs need to be assigned to the properties of the module
        JsonNode node = jp.getCodec().readTree(jp);

//        for (TextNode node : node.getC)
//
//
//        int id = (Integer) ((IntNode) node.get("id")).numberValue();
//        String itemName = node.get("itemName").asText();
//        int userId = (Integer) ((IntNode) node.get("createdBy")).numberValue();

        return new IkasanElement();
    }
}
