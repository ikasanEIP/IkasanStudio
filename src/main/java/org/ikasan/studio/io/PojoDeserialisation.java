package org.ikasan.studio.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.ikasan.studio.StudioException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

/**
 * In order to support a generics based JSON object reader, we need to compromise by wrapping
 * all pojos in a payload element
 * This approach will be sufficient for Internal json but external, we can't pollute the json.
 */
public class PojoDeserialisation {
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * Deserialize the object that has been wrapped in the GenericPojo wrapper.
     * The json will typically be { payload : { real object in json form }}
     * A typical invocation would be
     * MyClass myClass = PojoDeserialisation.deserializePojo(
     *      "myClassPojo.json",
     *      new TypeReference<GenericPojo<MyClass>>() {});
     * @param path to the object
     * @param typeReference of the object to create
     * @return the deserialized object
     * @param <T> type of object to deserialize
     * @throws StudioException if there were issues in the deserialization
     */
    public static <T> T deserializePojo(String path, TypeReference<GenericPojo<T>> typeReference) throws StudioException {
        InputStream inputStream = PojoDeserialisation.class.getClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            throw new StudioException("The serialised data in [" + path + "] could not be loaded, check the path is correct");
        }
        String jsonString = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining());

        String header = jsonString.substring(0, Math.min(jsonString.length(), 20));
        if (!header.contains("payload")) {
            throw new StudioException("The serialised data in [" + path + "] did not contain the required 'payload' top level element");
        }
        GenericPojo<T> pojo = null;
        try {
            pojo = mapper.readValue(jsonString, typeReference);
        } catch (JsonProcessingException e) {
            throw new StudioException("The serialised data in [" + path + "] could not be read due to" + e.getMessage(), e);
        }
        return pojo.getPayload();
    }
}