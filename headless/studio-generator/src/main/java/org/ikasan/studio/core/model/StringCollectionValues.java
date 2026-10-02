package org.ikasan.studio.core.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/** Strict value handling for Ikasan's List<String> and Map<String, String> configuration parameters. */
public final class StringCollectionValues {
    private StringCollectionValues() {}
    public static boolean supports(Class<?> type) { return type == List.class || type == Map.class; }

    /** Null is unset; an empty collection is an explicitly configured empty value. List order and duplicates are retained. */
    public static Object normalize(Class<?> type, Object value) {
        if (value == null) return null;
        if (value instanceof String text) {
            if (text.isBlank()) return null;
            // Compatibility with Studio's older comma-separated list properties.
            if (type == List.class && !text.stripLeading().startsWith("["))
                value = Arrays.asList(text.split("\\s*,\\s*", -1));
            else {
                try {
                    var mapper = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
                    value = mapper.readValue(text, Object.class);
                } catch (Exception failure) {
                    // Old Studio versions wrote List.toString() text for recipient lists.
                    if (type == List.class && text.startsWith("[") && text.endsWith("]") && !text.contains("\"")) {
                        String contents = text.substring(1, text.length() - 1);
                        value = contents.isEmpty() ? List.of() : Arrays.asList(contents.split("\\s*,\\s*", -1));
                    } else throw new IllegalArgumentException("Expected a JSON string list or string map; map keys must be unique.");
                }
            }
        }
        if (type == List.class && value instanceof List<?> list && list.stream().allMatch(String.class::isInstance))
            return new ArrayList<>(list);
        if (type == Map.class && value instanceof Map<?, ?> map
                && map.entrySet().stream().allMatch(e -> e.getKey() instanceof String && e.getValue() instanceof String))
            return new LinkedHashMap<>(map);
        throw new IllegalArgumentException("Collection entries must be strings; null entries and nested objects are not supported.");
    }
    public static boolean compatible(Class<?> type, JsonNode value) {
        if (type == List.class && !value.isArray() || type == Map.class && !value.isObject() || !supports(type)) return false;
        for (JsonNode entry : value) if (!entry.isTextual()) return false;
        return true;
    }
    public static String json(Object value) {
        try { return new ObjectMapper().writeValueAsString(value); }
        catch (Exception failure) { throw new IllegalArgumentException("Cannot encode collection", failure); }
    }
    /** Mutable collections for builders that add to or modify the supplied configuration. */
    public static String javaLiteral(Class<?> type, Object value) {
        Object checked = normalize(type, value);
        if (checked == null) return "null";
        if (checked instanceof List<?> list)
            return "new java.util.ArrayList<String>(java.util.Arrays.asList(" +
                    String.join(", ", list.stream().map(StringCollectionValues::json).toList()) + "))";
        var map = (Map<?, ?>) checked;
        return "new java.util.LinkedHashMap<String, String>(java.util.Map.ofEntries(" +
                String.join(", ", map.entrySet().stream().sorted(java.util.Comparator.comparing(e -> (String) e.getKey())).map(e -> "java.util.Map.entry(" + json(e.getKey()) + ", " + json(e.getValue()) + ")").toList()) + "))";
    }
}
