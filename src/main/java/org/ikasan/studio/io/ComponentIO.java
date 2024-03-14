package org.ikasan.studio.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import org.ikasan.studio.StudioException;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.ikasan.meta.ComponentTypeMeta;
import org.ikasan.studio.model.ikasan.meta.IkasanMeta;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class ComponentIO {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger LOG = Logger.getInstance("#ComponentIO");

    static {
        MAPPER
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /*
     * Deserialize the ComponentTypeMeta at the given path.
     * @param path to the file containing the JSON to deserializes
     * @return an ComponentTypeMeta object representing the deserialized class
     * @throws StudioException  to wrap JsonProcessingException
     */
    public static ComponentTypeMeta deserializeComponentTypeMeta(final String path) throws StudioException {
        final String jsonString = getJsonFromFile(path);

        ComponentTypeMeta componentTypeMeta;
        try {
            componentTypeMeta = MAPPER.readValue(jsonString, ComponentTypeMeta.class);
        } catch (JsonProcessingException e) {
            throw new StudioException("The serialised data in [" + path + "] could not be read due to [" + e.getMessage() + "]", e);
        }
        return componentTypeMeta;
    }

    /*
     * Deserialize the component at the given path.
     * The component could be any of the child classes of IkasanMeta
     * @param path to the file containing the JSON to deserializes
     * @return an IkasanMeta object representing the deserialized class
     * @throws StudioException  to wrap JsonProcessingException
     */
    public static IkasanMeta deserializeMetaComponent(final String path) throws StudioException {
        final String jsonString = getJsonFromFile(path);

        IkasanMeta ikasanMeta;
        try {
            ikasanMeta = MAPPER.readValue(jsonString, IkasanMeta.class);
        } catch (JsonProcessingException e) {
            throw new StudioException("The serialised data in [" + path + "] could not be read due to [" + e.getMessage() + "]", e);
        }
        return ikasanMeta;
    }

    /*
     * Deserialize a module
     * @param path to the file containing the JSON to deserializes
     * @return the deserialized Module
     * @throws StudioException  to wrap JsonProcessingException
     */
    public static Module deserializeModuleInstance(final String path) throws StudioException {
        return deserializeModuleInstanceString(getJsonFromFile(path), path);
    }
    // This interface is easier to test generically
    public static Module deserializeModuleInstanceString(final String jsonString, final String source) throws StudioException {
        Module moduleInstance;
        try {
            moduleInstance = MAPPER.readValue(jsonString, Module.class);
        } catch (JsonProcessingException e) {
            throw new StudioException("The serialised data in [" + source + "] could not be read due to " + e.getMessage(), e);
        }
        return moduleInstance;
    }

    private static String getJsonFromFile(final String path) throws StudioException {
        final InputStream inputStream = ComponentIO.class.getClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            throw new StudioException("The serialised data in [" + path + "] could not be loaded, check the path is correct");
        }
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining());
    }

    /**
     * Convert the supplied java Object into its JSON representation
     * @param value to be turned to JSON
     * @return the Object in JSON format.
     */
    public static String toJson(Object value) {
        String jsonString = "CouldNotConvert";

        try {
            jsonString = MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException jpe) {
            value = "";
            LOG.warn("Could not generate JSON from [" + value + "] message [" + jpe.getMessage() + "]", jpe);
        }
        return jsonString;
    }
}
