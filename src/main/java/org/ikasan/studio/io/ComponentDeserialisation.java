package org.ikasan.studio.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.studio.StudioException;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentMetaIfc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class ComponentDeserialisation {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
    }

    public static IkasanComponentMetaIfc deserializeComponent(String path) throws StudioException {

        InputStream inputStream = ComponentDeserialisation.class.getClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            throw new StudioException("The serialised data in [" + path + "] could not be loaded, check the path is correct");
        }
        String jsonString = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining());

        IkasanComponentMetaIfc ikasanComponentMeta;
        try {
            ikasanComponentMeta = MAPPER.readValue(jsonString, IkasanComponentMetaIfc.class);
        } catch (JsonProcessingException e) {
            throw new StudioException("The serialised data in [" + path + "] could not be read due to" + e.getMessage(), e);
        }
        return ikasanComponentMeta;
    }
}
