<#-- JSON avoids lossy comma splitting and SpEL evaluation of configuration content. -->
@org.springframework.beans.factory.annotation.Autowired
private org.springframework.core.env.Environment studioEnvironment;

private com.fasterxml.jackson.databind.JsonNode studioCollection(String name) {
    try {
        return new com.fasterxml.jackson.databind.ObjectMapper()
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .readTree(studioEnvironment.getRequiredProperty(name));
    } catch (java.io.IOException failure) {
        throw new IllegalArgumentException("Invalid collection configuration: " + name);
    }
}

private java.util.List<String> studioStringList(String name) {
    String raw = studioEnvironment.getRequiredProperty(name);
    // Existing deployment overrides may still use comma-separated recipient lists.
    if (!raw.stripLeading().startsWith("[")) {
        return raw.isBlank() ? new java.util.ArrayList<>()
            : new java.util.ArrayList<>(java.util.Arrays.asList(raw.split("\\s*,\\s*", -1)));
    }
    com.fasterxml.jackson.databind.JsonNode value = studioCollection(name);
    if (!value.isArray()) throw new IllegalArgumentException("Expected string list: " + name);
    java.util.List<String> result = new java.util.ArrayList<>();
    for (com.fasterxml.jackson.databind.JsonNode entry : value) {
        if (!entry.isTextual()) throw new IllegalArgumentException("Expected string entries: " + name);
        result.add(entry.textValue());
    }
    return result;
}

private java.util.Map<String, String> studioStringMap(String name) {
    com.fasterxml.jackson.databind.JsonNode value = studioCollection(name);
    if (value == null || !value.isObject()) throw new IllegalArgumentException("Expected string map: " + name);
    java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
    value.fields().forEachRemaining(entry -> {
        if (!entry.getValue().isTextual()) throw new IllegalArgumentException("Expected string entries: " + name);
        result.put(entry.getKey(), entry.getValue().textValue());
    });
    return result;
}
