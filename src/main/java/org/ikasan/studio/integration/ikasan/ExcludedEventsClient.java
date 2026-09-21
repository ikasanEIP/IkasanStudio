package org.ikasan.studio.integration.ikasan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Read-only adapter for the module-local Ikasan 3/4 exclusion search contract. */
public final class ExcludedEventsClient {
    public static final int PAGE_SIZE = 20;
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private ExcludedEventsClient() { }

    public record Event(String module, String flow, String identifier, long timestamp,
                        String errorUri, boolean harvested, String payload, boolean binary) { }
    public record Page(List<Event> events, long total, boolean last) { }

    /** Capture the destination on the EDT before starting background work. */
    public static URI endpoint(Module module) {
        return URI.create("http://localhost:" + Integer.parseInt(module.getPort() == null ? "8080" : module.getPort())
                + "/" + StudioBuildUtils.toUrlString(module.getIdentity()) + "/rest/exclusion/");
    }

    public static URI query(URI endpoint, int page, String flow, String from, String until) {
        if (page < 0) throw new IllegalArgumentException("page");
        DateTimeFormatter format = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss")
                .withResolverStyle(java.time.format.ResolverStyle.STRICT);
        LocalDateTime start = from.isBlank() ? null : LocalDateTime.parse(from.strip(), format);
        LocalDateTime end = until.isBlank() ? null : LocalDateTime.parse(until.strip(), format);
        if (start != null && end != null && start.isAfter(end)) throw new IllegalArgumentException("date order");
        StringBuilder q = new StringBuilder("pageNumber=" + page + "&pageSize=" + PAGE_SIZE
                + "&orderBy=timestamp&orderAscending=false");
        append(q, "flow", flow); append(q, "fromDateTime", from); append(q, "untilDateTime", until);
        return URI.create(endpoint.toASCIIString() + "?" + q);
    }

    private static void append(StringBuilder q, String name, String value) {
        if (!value.isBlank()) q.append('&').append(name).append('=')
                .append(URLEncoder.encode(value.strip(), StandardCharsets.UTF_8));
    }

    public static Page fetch(URI uri) throws Exception {
        var response = ModuleControlClient.getRuntimeRecords(uri);
        if (response.statusCode() != 200) throw new HttpFailure(response.statusCode());
        return parse(response.body());
    }

    public static final class HttpFailure extends IOException {
        public final int status;
        HttpFailure(int status) { super("HTTP " + status); this.status = status; }
    }

    public static Page parse(String json) throws IOException {
        JsonNode root = JSON.readTree(json);
        if (root == null || !root.path("pagedResults").isArray() || !root.path("resultSize").isIntegralNumber()
                || root.path("resultSize").asLong() < 0 || !root.path("lastPage").isBoolean())
            throw new IOException("Invalid exclusion page");
        List<Event> events = new ArrayList<>();
        for (JsonNode row : root.path("pagedResults")) {
            if (!row.isObject() || !row.path("timestamp").isIntegralNumber()
                    || !row.path("flowName").isTextual() || !row.path("identifier").isTextual()
                    || !row.path("harvested").isBoolean()) throw new IOException("Invalid exclusion event");
            String payload = "";
            boolean binary = false;
            JsonNode bytes = row.get("event");
            if (bytes != null && !bytes.isNull()) {
                if (!bytes.isTextual()) throw new IOException("Invalid exclusion payload");
                byte[] decoded;
                try { decoded = Base64.getDecoder().decode(bytes.asText()); }
                catch (IllegalArgumentException e) { throw new IOException("Invalid exclusion payload", e); }
                try {
                    payload = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(decoded)).toString();
                    binary = payload.codePoints().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t');
                } catch (java.nio.charset.CharacterCodingException e) { binary = true; }
                if (binary) payload = bytes.asText();
                if (payload.length() > 32768) payload = payload.substring(0, 32768) + "\n…";
            }
            events.add(new Event(row.path("moduleName").asText(""), row.path("flowName").asText(),
                    row.path("identifier").asText(), row.path("timestamp").asLong(), row.path("errorUri").asText(""),
                    row.path("harvested").asBoolean(), payload, binary));
        }
        if (events.size() > PAGE_SIZE) throw new IOException("Oversized exclusion page");
        return new Page(List.copyOf(events), root.path("resultSize").asLong(), root.path("lastPage").asBoolean());
    }
}
