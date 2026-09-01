package org.ikasan.studio.ui.actions;

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
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared HTTP client for GETting a running module's own Ikasan REST interface - {@code /rest/moduleControl/...}
 * for each flow's current state (running/stopped/stoppedInError/...) and {@code /rest/error/...} for the detail
 * behind a "stoppedInError" flow - used by {@code org.ikasan.studio.ui.intellij.FlowErrorMonitorService} to flag
 * flows on the canvas that have stopped in error (see {@code DesignerCanvas#paintFlowErrorFlashes}). Public,
 * like {@code TestMailServerSupport}, since its caller lives in the ui.intellij package.
 * -
 * Mirrors {@link StudioInjectClient}'s own URL construction and default-credentials convention deliberately -
 * same context-path derivation (module identity -&gt; server.servlet.context-path), same seeded admin:admin
 * basic auth - rather than inventing a second way to reach the same running module.
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
        public String flowName;
        public String flowElementName;
        public String errorMessage;
        public String exceptionClass;
        public long timestamp;
    }

    static final class PagedErrorResponse {
        public List<ErrorOccurrenceEntry> pagedResults;
    }

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
    public static String fetchLatestErrorSummary(Module module, String flowName) {
        try {
            String query = "pageNumber=0&pageSize=1&orderBy=timestamp&orderAscending=false&flow=" + flowName;
            HttpResponse<String> response = get(module, "/rest/error/", query);
            if (response.statusCode() != 200) {
                return null;
            }
            return parseLatestErrorSummary(response.body());
        } catch (Exception e) {
            return null;
        }
    }

    public static String parseLatestErrorSummary(String json) throws Exception {
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
        return summary.toString();
    }

    private static String shortClassName(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? fullyQualifiedName.substring(lastDot + 1) : fullyQualifiedName;
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
