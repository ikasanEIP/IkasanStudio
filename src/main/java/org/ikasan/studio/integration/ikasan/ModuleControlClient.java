package org.ikasan.studio.integration.ikasan;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared HTTP client for GETting a running module's own Ikasan REST interface - {@code /rest/moduleControl/...}
 * for each flow's current state (running/stopped/stoppedInError/...) and {@code /rest/error/...} for the detail
 * behind a "stoppedInError" flow - used by {@code org.ikasan.studio.intellij.runtime.FlowErrorMonitorService} to flag
 * flows on the canvas that have stopped in error (see {@code DesignerCanvas#paintFlowErrorFlashes}). Public,
 * like {@code TestMailServerSupport}, since its caller lives in the ui.intellij package.
 * -
 * Mirrors {@link StudioInjectClient}'s own URL construction and default-credentials convention deliberately -
 * same context-path derivation (module identity -&gt; server.servlet.context-path), same seeded admin:admin
 * basic auth - rather than inventing a second way to reach the same running module.
 * -
 * Also PUTs to the same endpoint to change a flow's state - see {@link #changeFlowState} - the click handler
 * behind the canvas's per-flow Start/Stop/Pause/Start-Paused buttons (see {@code FlowTransportControlAction}).
 * -
 * This REST interface comes from the {@code ikasan-rest-module} artifact, which every Studio-generated module
 * already depends on unconditionally and transitively (via {@code ikasan-eip-standalone}, itself always in
 * {@code Module}'s own jarDependencies) - so no pom.xml change was needed to enable this. A module that isn't
 * running yet (or isn't reachable for any other reason) simply fails the connection - callers treat that as
 * "nothing to report" rather than an error, since it's the overwhelmingly common case between debug sessions.
 */
public final class ModuleControlClient {
    // A status poll should stay snappy and fail fast - unlike StudioInjectClient's inject(), there's no
    // synchronous flow execution to wait out server-side, just a simple in-memory state lookup.
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(5);
    // Same seeded admin user StudioInjectClient uses - see its own comment for where this comes from.
    private static final String DEFAULT_CREDENTIALS = "admin:admin";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    // Deserializing into DTOs shaped after the server's own FlowDto/ModuleDto/ErrorOccurrence rather than a
    // Map - tolerate any extra fields the concrete ErrorOccurrence implementation adds beyond the interface.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ModuleControlClient() {
    }

    static final class FlowStateEntry {
        public String name;
        public String state;
    }

    static final class ModuleStateResponse {
        public String name;
        public List<FlowStateEntry> flows;
    }

    static final class ErrorOccurrenceEntry {
        public String moduleName;
        public String flowName;
        public String flowElementName;
        public String errorMessage;
        public String exceptionClass;
        public String errorDetail;
        public String uri;
        public String eventLifeIdentifier;
        public String eventRelatedIdentifier;
        public String action;
        public long timestamp;
    }

    static final class PagedErrorResponse {
        public List<ErrorOccurrenceEntry> pagedResults;
    }

    public record ErrorDetails(String summary, String report) {}

    private static final DateTimeFormatter ERROR_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    /** @return every flow's current state (e.g. "running", "stopped", "stoppedInError"), keyed by flow name. */
    public static Map<String, String> fetchFlowStates(Module module) throws Exception {
        HttpResponse<String> response = get(module, "/rest/moduleControl/" + module.getIdentity(), null);
        if (response.statusCode() != 200) {
            throw new IOException("moduleControl responded with HTTP " + response.statusCode());
        }
        return parseFlowStates(response.body());
    }

    public static Map<String, String> parseFlowStates(String json) throws Exception {
        ModuleStateResponse moduleState = OBJECT_MAPPER.readValue(json, ModuleStateResponse.class);
        Map<String, String> states = new LinkedHashMap<>();
        if (moduleState.flows != null) {
            for (FlowStateEntry flow : moduleState.flows) {
                if (flow.name != null) {
                    states.put(flow.name, flow.state);
                }
            }
        }
        return states;
    }

    /**
     * Best-effort only - a null return (network failure, unexpected response shape, nothing logged yet against
     * this flow) still leaves the flow correctly flagged by the caller, just without a human-readable detail.
     */
    public static ErrorDetails fetchLatestErrorDetails(Module module, String flowName) {
        try {
            String query = "pageNumber=0&pageSize=1&orderBy=timestamp&orderAscending=false&flow=" + flowName;
            HttpResponse<String> response = get(module, "/rest/error/", query);
            if (response.statusCode() != 200) {
                return null;
            }
            return parseLatestErrorDetails(response.body());
        } catch (Exception e) {
            return null;
        }
    }

//    public static String fetchLatestErrorSummary(Module module, String flowName) {
//        ErrorDetails details = fetchLatestErrorDetails(module, flowName);
//        return details != null ? details.summary() : null;
//    }

    public static ErrorDetails parseLatestErrorDetails(String json) throws Exception {
        PagedErrorResponse result = OBJECT_MAPPER.readValue(json, PagedErrorResponse.class);
        if (result.pagedResults == null || result.pagedResults.isEmpty()) {
            return null;
        }
        ErrorOccurrenceEntry entry = result.pagedResults.get(0);
        StringBuilder summary = new StringBuilder();
        if (entry.exceptionClass != null && !entry.exceptionClass.isBlank()) {
            summary.append(shortClassName(entry.exceptionClass)).append(": ");
        }
        summary.append(entry.errorMessage != null && !entry.errorMessage.isBlank() ? entry.errorMessage : "(no message)");
        if (entry.flowElementName != null && !entry.flowElementName.isBlank()) {
            summary.append(" at ").append(entry.flowElementName);
        }
        return new ErrorDetails(summary.toString(), formatErrorReport(entry));
    }

    public static String parseLatestErrorSummary(String json) throws Exception {
        ErrorDetails details = parseLatestErrorDetails(json);
        return details != null ? details.summary() : null;
    }

    private static String formatErrorReport(ErrorOccurrenceEntry entry) {
        StringBuilder report = new StringBuilder();
        appendReportField(report, "Timestamp", entry.timestamp > 0
                ? ERROR_TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault())) : null);
        appendReportField(report, "Module", entry.moduleName);
        appendReportField(report, "Flow", entry.flowName);
        appendReportField(report, "Component", entry.flowElementName);
        appendReportField(report, "Exception", entry.exceptionClass);
        appendReportField(report, "Message", entry.errorMessage);
        appendReportField(report, "Error URI", entry.uri);
        appendReportField(report, "Event identifier", entry.eventLifeIdentifier);
        appendReportField(report, "Related event identifier", entry.eventRelatedIdentifier);
        appendReportField(report, "Action", entry.action);
        if (entry.errorDetail != null && !entry.errorDetail.isBlank()) {
            report.append("\nStack trace:\n").append(entry.errorDetail.strip()).append("\n");
        }
        return report.toString().stripTrailing();
    }

    private static void appendReportField(StringBuilder report, String label, String value) {
        if (value != null && !value.isBlank()) {
            report.append(label).append(": ").append(value).append("\n");
        }
    }

    private static String shortClassName(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? fullyQualifiedName.substring(lastDot + 1) : fullyQualifiedName;
    }

    /**
     * Starts/stops/pauses/start-pauses/resumes a single flow via {@code PUT /rest/moduleControl} - the
     * non-deprecated form of the endpoint (ChangeFlowStateDto{moduleName,flowName,action}), confirmed identical
     * between the V3.3.8 and V4.0.x Ikasan cores (org.ikasan.rest.module.ModuleControlApplication#changeFlowState
     * in both). Throws on anything other than HTTP 200 - callers (the UI action) surfaces the
     * failure, they don't need the response body parsed since there's no state to read back from a successful
     * change.
     *
     * @param operation the exact UI-independent state transition to send to the module.
     */
    public static void changeFlowState(Module module, String flowName, FlowControlOperation operation) throws Exception {
        String requestBody = buildChangeFlowStateRequestBody(module.getIdentity(), flowName, operation);
        HttpResponse<String> response = put(module, "/rest/moduleControl", requestBody);
        if (response.statusCode() != 200) {
            throw new IOException("moduleControl PUT responded with HTTP " + response.statusCode()
                    + (response.body() != null && !response.body().isBlank() ? ": " + response.body() : ""));
        }
    }

    /** Split out from {@link #changeFlowState} purely so the request-body shape (ChangeFlowStateDto's field names) can be unit tested without a live server. */
    static String buildChangeFlowStateRequestBody(String moduleName, String flowName, FlowControlOperation operation) throws Exception {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("moduleName", moduleName);
        requestBody.put("flowName", flowName);
        requestBody.put("action", operation.getWireValue());
        return OBJECT_MAPPER.writeValueAsString(requestBody);
    }

    private static HttpResponse<String> put(Module module, String path, String jsonBody) throws Exception {
        String port = module.getPort() != null ? module.getPort() : "8080";
        String contextPath = "/" + StudioBuildUtils.toUrlString(module.getIdentity());
        URI uri = new URI("http", null, "localhost", Integer.parseInt(port), contextPath + path, null, null);

        String credentials = Base64.getEncoder().encodeToString(DEFAULT_CREDENTIALS.getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(RESPONSE_TIMEOUT)
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> get(Module module, String path, String query) throws Exception {
        String port = module.getPort() != null ? module.getPort() : "8080";
        // See StudioInjectClient's own comment - server.servlet.context-path is derived from the module's
        // identity the same way there, and must be reproduced identically here to reach the same running app.
        String contextPath = "/" + StudioBuildUtils.toUrlString(module.getIdentity());
        URI uri = new URI("http", null, "localhost", Integer.parseInt(port), contextPath + path, query, null);

        String credentials = Base64.getEncoder().encodeToString(DEFAULT_CREDENTIALS.getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(RESPONSE_TIMEOUT)
                .header("Authorization", "Basic " + credentials)
                .GET()
                .build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
