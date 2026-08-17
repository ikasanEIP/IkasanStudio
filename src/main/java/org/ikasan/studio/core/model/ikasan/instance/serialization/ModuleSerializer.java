package org.ikasan.studio.core.model.ikasan.instance.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.Flow;

import java.io.IOException;

public class ModuleSerializer extends StdSerializer<Module> {

    public ModuleSerializer() {
        super(Module.class);
    }

    /**
     * Serializes the properties of a Module into JSON fields.
     *
     * @param module the Module to serialize
     * @param jsonGenerator the JsonGenerator used for writing JSON
     * @param serializerProvider the SerializerProvider used for serialization
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void serialize(Module module, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        BasicElementSerializer basicElementSerializer = new BasicElementSerializer();

        jsonGenerator.writeStartObject();
        // First, the module fields
        basicElementSerializer.serializePayload(module, jsonGenerator);
        if (module.isWiretapManagementEnabled()) {
            jsonGenerator.writeBooleanField(Module.WIRETAP_MANAGEMENT_ENABLED_JSON_TAG, true);
        }

        // Now Flows
        if (module.getFlows() != null && !module.getFlows().isEmpty()) {
            jsonGenerator.writeArrayFieldStart("flows");
            FlowSerializer flowSerializer = new FlowSerializer();
            for (Flow flow : module.getFlows()) {
                jsonGenerator.writeStartObject();
                flowSerializer.serializePayload(flow, jsonGenerator, serializerProvider);
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
        }
        jsonGenerator.writeEndObject();
    }
}
