package org.ikasan.studio.integration.ikasan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Read-only module-local Ikasan 3/4 wiretap search. Stored events are text, not Base64. */
public final class WiretapEventsClient {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private WiretapEventsClient() { }
    public record Event(long identifier, String module, String flow, String component, String eventId,
                        String relatedEventId, long timestamp, boolean harvested, String payload) { }
    public record Page(List<Event> events, long total, boolean last) { }

    public static URI query(URI endpoint, int page, String flow, String component, String from, String until) {
        String query = ExcludedEventsClient.query(endpoint, page, flow, from, until).toASCIIString();
        if (!component.isBlank()) query += "&componentName=" + URLEncoder.encode(component.strip(), StandardCharsets.UTF_8);
        return URI.create(query);
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
            throw new IOException("Invalid wiretap page");
        List<Event> events = new ArrayList<>();
        for (JsonNode row : root.path("pagedResults")) {
            if (!row.isObject() || !row.path("identifier").isIntegralNumber() || !row.path("timestamp").isIntegralNumber()
                    || !row.path("flowName").isTextual() || !row.path("componentName").isTextual()
                    || (row.hasNonNull("event") && !row.path("event").isTextual()))
                throw new IOException("Invalid wiretap event");
            String payload = row.path("event").asText("");
            if (payload.length() > 32768) payload = payload.substring(0, 32768) + "\n…";
            events.add(new Event(row.path("identifier").asLong(), row.path("moduleName").asText(""),
                    row.path("flowName").asText(), row.path("componentName").asText(), row.path("eventId").asText(""),
                    row.path("relatedEventId").asText(""), row.path("timestamp").asLong(),
                    row.path("harvested").asBoolean(false), payload));
        }
        if (events.size() > ExcludedEventsClient.PAGE_SIZE) throw new IOException("Oversized wiretap page");
        return new Page(List.copyOf(events), root.path("resultSize").asLong(), root.path("lastPage").asBoolean());
    }
}
