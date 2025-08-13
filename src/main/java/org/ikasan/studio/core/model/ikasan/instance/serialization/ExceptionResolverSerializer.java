package org.ikasan.studio.core.model.ikasan.instance.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.ikasan.studio.core.model.ikasan.instance.ExceptionResolution;
import org.ikasan.studio.core.model.ikasan.instance.ExceptionResolver;

import java.io.IOException;
import java.util.Map;

import static org.ikasan.studio.core.model.ikasan.meta.ComponentMeta.*;

public class ExceptionResolverSerializer extends StdSerializer<ExceptionResolver> {

    public ExceptionResolverSerializer() {
        super(ExceptionResolver.class);
    }

    @Override
    public void serialize(ExceptionResolver exceptionResolver, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializePayload(exceptionResolver, jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    /**
     * Serializes the properties of an ExceptionResolver into JSON fields.
     *
     * @param exceptionResolver the ExceptionResolver to serialize
     * @param jsonGenerator the JsonGenerator used for writing JSON
     * @throws IOException if an I/O error occurs
     */
    protected void serializePayload(ExceptionResolver exceptionResolver, JsonGenerator jsonGenerator) throws IOException {
        // First serialise any BasicElement i.e. configured properties (for ExceptionResolver there are currently non be keep this here for extensibility)
        BasicElementSerializer basicElementSerializer = new BasicElementSerializer();
        basicElementSerializer.serializePayload(exceptionResolver, jsonGenerator);

        Map<String, ExceptionResolution> ikasanExceptionResolutionMap = exceptionResolver.getIkasanExceptionResolutionMap();
        if (ikasanExceptionResolutionMap != null && !exceptionResolver.getIkasanExceptionResolutionMap().isEmpty()) {
            for (ExceptionResolution exceptionResolution : ikasanExceptionResolutionMap.values()) {
                jsonGenerator.writeFieldName(exceptionResolution.getExceptionsCaught());
                jsonGenerator.writeStartObject();
                    jsonGenerator.writeStringField(EXCEPTIONS_CAUGHT_KEY, exceptionResolution.getExceptionsCaught());
                    jsonGenerator.writeStringField(ACTION_KEY, exceptionResolution.getTheAction());

                        if (exceptionResolution.getComponentProperties() != null && !exceptionResolution.getComponentProperties().isEmpty()) {
                            // Standard BasicElement properties
                            jsonGenerator.writeFieldName(ACTION_PROPERTIES_KEY);
                            jsonGenerator.writeStartObject();
                            basicElementSerializer.serializePayload(exceptionResolution, jsonGenerator);
                            jsonGenerator.writeEndObject();
                        }

                jsonGenerator.writeEndObject();
            }
        }
    }
}