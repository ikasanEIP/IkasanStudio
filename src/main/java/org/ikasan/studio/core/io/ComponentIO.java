package org.ikasan.studio.core.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.studio.StudioRuntimeException;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.StudioBuildRuntimeException;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.metapack.model.ComponentTypeMeta;
import org.ikasan.studio.core.metapack.model.IkasanMeta;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ComponentIO {
    private static final ObjectMapper MAPPER = StudioJson.newObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(ComponentIO.class);


    /*
     * Deserialize the ComponentTypeMeta at the given path.
     * @param path to the file containing the JSON to deserializes
     * @return an ComponentTypeMeta object representing the deserialized class
     * @throws StudioBuildException to wrap JsonProcessingException
     */
    public static ComponentTypeMeta deserializeComponentTypeMeta(final String path) throws StudioBuildException {
        final String jsonString = getJsonFromFile(path);

        ComponentTypeMeta componentTypeMeta;
        try {
            componentTypeMeta = MAPPER.readValue(jsonString, ComponentTypeMeta.class);
        } catch (JsonProcessingException e) {
            throw new StudioBuildException("The serialised data in [" + path + "] could not be read due to [" + e.getMessage() + "] trace: " + Arrays.toString(e.getStackTrace()));
        }
        return componentTypeMeta;
    }

    /*
     * Deserialize the component at the given path.
     * The component could be any of the child classes of IkasanMeta
     * @param path to the file containing the JSON to deserializes
     * @return an IkasanMeta object representing the deserialized class
     * @throws StudioBuildException  to wrap JsonProcessingException
     */
    public static IkasanMeta deserializeMetaComponent(final String path) throws StudioBuildException {
        final String jsonString = getJsonFromFile(path);

        IkasanMeta ikasanMeta;
        try {
            ikasanMeta = MAPPER.readValue(jsonString, IkasanMeta.class);
        } catch (JsonProcessingException e) {
            throw new StudioBuildException("The serialised data in [" + path + "] could not be read due to [" + e.getMessage() + "] trace: " + Arrays.toString(e.getStackTrace()));
        }
        return ikasanMeta;
    }

    /*
     * Deserialize a module
     * @param path to the file containing the JSON to deserializes
     * @return the deserialized Module
     * @throws StudioBuildException  to wrap JsonProcessingException
     */
    public static Module deserializeModuleInstance(final String path) throws StudioBuildException {
        return deserializeModuleInstanceString(getJsonFromFile(path), path);
    }
    // This interface is easier to test generically
    public static Module deserializeModuleInstanceString(final String jsonString, final String source) throws StudioBuildException {
        Module moduleInstance;
        try {
            moduleInstance = MAPPER.readValue(jsonString, Module.class);
        } catch (JsonProcessingException e) {
            throw new StudioBuildException("The serialised data in [" + source + "] could not be read due to " + e.getMessage() + " trace: " + Arrays.toString(e.getStackTrace()));
        } catch (StudioBuildRuntimeException e) {
            throw new StudioBuildException("The serialised data in [" + source + "] is not safe to load: " + e.getMessage(), e);
        }
        return moduleInstance;
    }

    private static String getJsonFromFile(final String path) throws StudioBuildException {
        final InputStream inputStream = ComponentIO.class.getClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            throw new StudioBuildException("The serialised data in [" + path + "] could not be loaded, check the path is correct");
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
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            LOG.warn("STUDIO: Could not generate JSON for type [" +
                    (value == null ? "null" : value.getClass().getName()) + "]", e);
            throw new StudioRuntimeException(
                    "The Studio model could not be converted to JSON. The existing model.json has not been changed.", e);
        }
    }

    /** Serializes a module and proves that the result can be read back before it is eligible for persistence. */
    public static String toValidatedModuleJson(Module module) {
        if (module == null) {
            throw new StudioRuntimeException("The Studio model is not available. The existing model.json has not been changed.");
        }
        String json = toJson(module);
        try {
            validatePersistedModuleJson(json, "in-memory model validation", false);
            return json;
        } catch (StudioBuildException e) {
            throw new StudioRuntimeException("The generated JSON failed validation. The existing model.json has not been changed.", e);
        }
    }
    /** Validates JSON shape and the minimum identity required for a configured Studio module. */
    public static Module validatePersistedModuleJson(String json, String source, boolean allowEmptyBootstrap)
            throws StudioBuildException {
        final JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new StudioBuildException("The serialised data in [" + source + "] is not valid JSON: " + e.getMessage(), e);
        }
        if (root == null || !root.isObject()) {
            throw new StudioBuildException("The serialised data in [" + source + "] must be a JSON object");
        }
        if (root.isEmpty()) {
            if (allowEmptyBootstrap) {
                return Module.getDumbModuleVersion();
            }
            throw new StudioBuildException("The serialised data in [" + source + "] is an empty bootstrap model, not a configured module");
        }
        Module module = deserializeModuleInstanceString(json, source);
        if (!module.isInitialised()) {
            throw new StudioBuildException("The serialised data in [" + source + "] is missing required configured-module fields");
        }
        return module;
    }
}
