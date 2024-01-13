package org.ikasan.studio.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

/**
 * In order to support a generics based JSON object reader, we need to compromise by wrapping
 * all pojos in a payload element
 */
public class PojoDeserialisation {
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public static <T> T deserializePojo(String path, TypeReference<GenericPojo<T>> typeReference) throws JsonProcessingException {
        InputStream inputStream = PojoDeserialisation.class.getClassLoader().getResourceAsStream(path);
        String jsonString = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining());
        GenericPojo<T> pojo = mapper.readValue(jsonString, typeReference);
        return pojo.getPayload();
    }
}
