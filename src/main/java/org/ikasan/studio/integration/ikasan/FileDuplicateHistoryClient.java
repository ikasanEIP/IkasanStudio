package org.ikasan.studio.integration.ikasan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** The file-transfer duplicate store is separate from failed-event exclusions. */
public final class FileDuplicateHistoryClient {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private FileDuplicateHistoryClient() { }
    public record Entry(String id, String clientId, String criteria, long size,
                        String modified, String accessed, String created) { }
    public record Page(List<Entry> entries, long total, boolean last) { }

    public static URI query(URI endpoint, int page, String clientId, String criteria) {
        if (page < 0) throw new IllegalArgumentException("page");
        String q = "pageNumber=" + page + "&pageSize=20";
        if (!clientId.isBlank()) q += "&clientId=" + URLEncoder.encode(clientId.strip(), StandardCharsets.UTF_8);
        if (!criteria.isBlank()) q += "&criteria=" + URLEncoder.encode(criteria.strip(), StandardCharsets.UTF_8);
        // Both filters are optional in the module's API, but with neither it builds an empty WHERE clause and answers
        // HTTP 500. The dialog promises that blank filters show everything, so ask for everything explicitly.
        if (clientId.isBlank() && criteria.isBlank()) q += "&criteria=" + URLEncoder.encode("%", StandardCharsets.UTF_8);
        return URI.create(endpoint.toASCIIString() + "?" + q);
    }

    public static Page fetch(URI uri) throws Exception {
        var response = ModuleControlClient.getRuntimeRecords(uri);
        if (response.statusCode() != 200) throw new ExcludedEventsClient.HttpFailure(response.statusCode());
        return parse(response.body());
    }

    public static Page parse(String json) throws IOException {
        JsonNode root = JSON.readTree(json);
        if (root == null || !root.path("pagedResults").isArray() || !root.path("resultSize").isIntegralNumber()
                || root.path("resultSize").asLong() < 0 || !root.path("lastPage").isBoolean())
            throw new IOException("Invalid file-filter page");
        List<Entry> entries = new ArrayList<>();
        for (var row : root.path("pagedResults")) {
            if (!row.isObject() || !row.path("clientId").isTextual() || !row.path("criteria").isTextual()
                    || !row.path("size").isIntegralNumber()) throw new IOException("Invalid file-filter entry");
            entries.add(new Entry(row.path("id").asText(""), row.path("clientId").asText(),
                    row.path("criteria").asText(), row.path("size").asLong(), date(row.get("lastModified")),
                    date(row.get("lastAccessed")), date(row.get("createdDateTime"))));
        }
        if (entries.size() > 20) throw new IOException("Oversized file-filter page");
        return new Page(List.copyOf(entries), root.path("resultSize").asLong(), root.path("lastPage").asBoolean());
    }

    // Jackson's Date representation depends on the running module's serialization configuration.
    private static String date(JsonNode value) throws IOException {
        if (value == null || value.isNull()) return "";
        if (value.isIntegralNumber()) return java.time.Instant.ofEpochMilli(value.asLong()).toString();
        if (value.isTextual()) return value.asText();
        throw new IOException("Invalid file-filter timestamp");
    }
}
