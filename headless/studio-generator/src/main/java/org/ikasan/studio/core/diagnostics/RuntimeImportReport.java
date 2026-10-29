package org.ikasan.studio.core.diagnostics;

import com.fasterxml.jackson.databind.JsonNode;
import org.ikasan.studio.core.persistence.json.StudioJson;
import java.util.*;

/** Bounded, value-free report. Never stores input documents, application names, URLs or exception messages. */
public final class RuntimeImportReport {
    public enum Stage { FETCH, PARSE, MATCHING, CONVERSION, VALIDATION, GENERATION }
    private final List<String> issues = new ArrayList<>();
    private final Map<String, String> publicContext = new HashMap<>();
    private final List<String> failures = new ArrayList<>();
    private Stage stage = Stage.FETCH;
    private String outcome = "Not started";
    private String version = "unavailable", pack = "unavailable", revision = "unavailable";
    private boolean inspected;
    public synchronized void revision(String value) { revision = safeVersion(value); }
    public enum Endpoint { METADATA, CONFIGURATION }
    private Endpoint endpoint;
    private int httpStatus;
    private final String attempt = UUID.randomUUID().toString().substring(0, 8);
    public synchronized void request(Endpoint value, int status) { endpoint = value; httpStatus = status >= 100 && status <= 599 ? status : 0; }
    private int flows, components, transitions, configurations, unset, omitted, exactMatches;
    public synchronized void stage(Stage value) { stage = value; outcome = "In progress"; }
    public synchronized void pack(String value) { pack = safeVersion(value); }
    /** Call only with an exactly matched bundled metadata descriptor, never a raw REST class/name. */
    public synchronized void publicContext(String location, String standardClass, String property) {
        if (!Set.of("V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT", "V5.1.0").contains(pack)) return;
        if (publicContext.size() >= 1000 || !location.matches("F[0-9]+/C[0-9]+(?:/P[0-9]+)?")) return;
        if (standardClass == null || !standardClass.matches("org[.]ikasan[.]component[.][a-zA-Z0-9_.$]+")) return;
        if (property != null && !property.matches("[a-zA-Z][a-zA-Z0-9_]{0,79}")) return;
        publicContext.put(location, property == null ? standardClass : "Studio property: " + property);
    }
    public synchronized void cancelled() { outcome = "Cancelled"; }
    public synchronized void matched() { exactMatches++; }
    public synchronized void complete() { outcome = "Completed"; }
    public synchronized void issue(String location, String code) {
        // Only program-owned codes and positional identifiers may cross this boundary.
        if (!location.matches("F[0-9]+(?:/C[0-9]+(?:/P[0-9]+)?)?|CONFIG[0-9]+|DOCUMENT")
                || !code.matches("[A-Z_][A-Z_0-9]*")) throw new IllegalArgumentException("Invalid diagnostic code");
        if (issues.size() < 200) issues.add(location + " — " + code); else omitted++;
    }
    public synchronized void failure(Throwable failure) {
        outcome = "Failed";
        failures.clear();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable t = failure; t != null && seen.add(t) && seen.size() <= 4; t = t.getCause()) {
            // Private exception class names can themselves disclose internal implementations.
            failures.add(t instanceof org.ikasan.studio.core.StudioBuildException
                    ? org.ikasan.studio.core.StudioBuildException.class.getName()
                    : t.getClass().getName().startsWith("java.") ? t.getClass().getName() : "External exception (type omitted)");
            if (t instanceof com.fasterxml.jackson.core.JsonProcessingException parse && parse.getLocation() != null)
                failures.add("  JSON line " + Math.max(0, parse.getLocation().getLineNr()) + ", column " + Math.max(0, parse.getLocation().getColumnNr()));
            int count = 0;
            for (var frame : t.getStackTrace()) if (isStudioFrame(frame.getClassName()) && count++ < 4)
                failures.add("  " + frame.getClassName().replaceAll("[^a-zA-Z0-9_.$]", "_") + "." + frame.getMethodName().replaceAll("[^a-zA-Z0-9_$]", "_") + ":" + Math.max(0, frame.getLineNumber()));
        }
    }
    private static boolean isStudioFrame(String name) {
        return name.startsWith("org.ikasan.studio.core.") || name.startsWith("org.ikasan.studio.intellij.")
                || name.startsWith("org.ikasan.studio.integration.") || name.startsWith("org.ikasan.studio.ui.");
    }
    public synchronized void inspect(String module, String configuration) {
        if (inspected) return;
        inspected = true;
        stage(Stage.PARSE);
        try {
            if (module.length() > 8 * 1024 * 1024 || configuration.length() > 8 * 1024 * 1024) {
                issue("DOCUMENT", "DOCUMENT_TOO_LARGE"); return;
            }
            var mapper = StudioJson.newObjectMapper()
                    .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                    .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
            JsonNode root = mapper.readTree(module);
            if (root == null || !root.path("flows").isArray()) { issue("DOCUMENT", "RUNTIME_FLOWS_ARRAY_MISSING"); return; }
            version = safeVersion(root.path("ikasanVersion").asText());
            Set<String> flowNames = new HashSet<>();
            for (var flow : root.path("flows")) {
                if (++flows > 1000) { issue("DOCUMENT", "FLOW_LIMIT_EXCEEDED"); break; }
                String location = "F" + flows;
                if (!flow.path("name").isTextual() || flow.path("name").asText().isBlank()) issue(location, "FLOW_NAME_MISSING");
                if (!flowNames.add(flow.path("name").asText())) issue(location, "DUPLICATE_FLOW_NAME");
                inspectFlow(flow, location);
            }
            if (!configuration.isBlank()) {
                JsonNode configs = mapper.readTree(configuration);
                if (configs == null) { issue("DOCUMENT", "CONFIGURATION_NULL"); return; }
                Iterable<JsonNode> entries = configs.isArray() ? configs : List.of(configs);
                Set<String> configIds = new HashSet<>();
                for (var entry : entries) {
                    if (++configurations > 10000) { issue("DOCUMENT", "CONFIGURATION_LIMIT_EXCEEDED"); break; }
                    String location = "CONFIG" + configurations;
                    if (!entry.path("configurationId").isTextual() || entry.path("configurationId").asText().isBlank()) issue(location, "CONFIGURATION_ID_MISSING");
                    else if (!configIds.add(entry.path("configurationId").asText())) issue(location, "DUPLICATE_CONFIGURATION_ID");
                    Set<String> names = new HashSet<>();
                    for (var parameter : entry.path("parameters")) {
                        if (!parameter.path("name").isTextual() || parameter.path("name").asText().isBlank()) issue(location, "PARAMETER_NAME_MISSING");
                        else if (!names.add(parameter.path("name").asText())) issue(location, "DUPLICATE_PARAMETER_NAME");
                    }
                    if (!entry.path("parameters").isArray()) issue("CONFIG" + configurations, "PARAMETERS_ARRAY_MISSING");
                    for (var parameter : entry.path("parameters")) if (!parameter.hasNonNull("value")) unset++;
                }
            }
        } catch (Exception failure) { issue("DOCUMENT", "JSON_PARSE_FAILED"); failure(failure); }
    }
    private void inspectFlow(JsonNode flow, String location) {
        if (!flow.path("consumer").isObject() || !flow.path("flowElements").isArray() || !flow.path("transitions").isArray())
            issue(location, "FLOW_STRUCTURE_INCOMPLETE");
        Map<String, JsonNode> nodes = new LinkedHashMap<>();
        var consumer = flow.path("consumer");
        if (consumer.isObject()) nodes.put(consumer.path("componentName").asText(), consumer);
        for (var node : flow.path("flowElements")) {
            String name = node.path("componentName").asText();
            if (nodes.size() >= 500 && !nodes.containsKey(name)) { issue(location, "COMPONENT_LIMIT_EXCEEDED"); break; }
            var previous = nodes.putIfAbsent(name, node);
            // Runtime metadata routinely repeats the consumer in flowElements.
            if (previous != null && !(node.equals(consumer) && name.equals(consumer.path("componentName").asText())))
                issue(location, "DUPLICATE_COMPONENT_NAME");
        }
        int index = 0;
        for (var node : nodes.values()) {
            components++; index++;
            if (!node.path("componentName").isTextual() || node.path("componentName").asText().isBlank()) issue(location + "/C" + index, "COMPONENT_NAME_MISSING");
            if (!node.path("implementingClass").isTextual()) issue(location + "/C" + index, "IMPLEMENTING_CLASS_MISSING");
        }
        Map<String, List<String>> edges = new HashMap<>();
        Set<String> routes = new HashSet<>();
        Map<String, Integer> incoming = new HashMap<>();
        for (var edge : flow.path("transitions")) {
            if (++transitions > 500000) { issue("DOCUMENT", "TRANSITION_LIMIT_EXCEEDED"); break; }
            if (!edge.path("from").isTextual() || !edge.path("to").isTextual() || !edge.path("name").isTextual()
                    || edge.path("from").asText().isBlank() || edge.path("to").asText().isBlank() || edge.path("name").asText().isBlank())
                issue(location, "TRANSITION_FIELDS_MISSING");
            String from = edge.path("from").asText(), to = edge.path("to").asText();
            if (!nodes.containsKey(from) || !nodes.containsKey(to)) issue(location, "TRANSITION_ENDPOINT_MISSING");
            if (!routes.add(from + "\u0000" + edge.path("name").asText())) issue(location, "DUPLICATE_ROUTE");
            if (incoming.merge(to, 1, Integer::sum) > 1) issue(location, "JOIN_REQUIRES_MAPPING");
            edges.computeIfAbsent(from, ignored -> new ArrayList<>()).add(to);
        }
        Set<String> reached = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        if (consumer.isObject()) pending.add(consumer.path("componentName").asText());
        while (!pending.isEmpty()) {
            String node = pending.removeFirst();
            if (!reached.add(node)) continue;
            pending.addAll(edges.getOrDefault(node, List.of()));
        }
        if (!reached.containsAll(nodes.keySet())) issue(location, "DISCONNECTED_COMPONENTS");
        var degrees = new HashMap<String, Integer>();
        nodes.keySet().forEach(node -> degrees.put(node, 0));
        edges.forEach((from, targets) -> { if (nodes.containsKey(from)) targets.forEach(to -> { if (nodes.containsKey(to)) degrees.merge(to, 1, Integer::sum); }); });
        var ready = new ArrayDeque<String>();
        degrees.forEach((node, degree) -> { if (degree == 0) ready.add(node); });
        int visited = 0;
        while (!ready.isEmpty()) {
            String node = ready.removeFirst(); visited++;
            for (String to : edges.getOrDefault(node, List.of())) if (degrees.containsKey(to) && degrees.merge(to, -1, Integer::sum) == 0) ready.add(to);
        }
        if (visited < nodes.size()) issue(location, "CYCLIC_TOPOLOGY");
        if (incoming.containsKey(consumer.path("componentName").asText())) issue(location, "CONSUMER_HAS_INCOMING_TRANSITION");
    }
    private static String safeVersion(String value) {
        return value != null && value.matches("[Vv]?[0-9]+(?:[.][0-9]+){1,3}(?:-(?:SNAPSHOT|beta[.][0-9]+|rc[.][0-9]+))?") ? value : "unavailable/nonstandard";
    }
    public synchronized String render(String pluginVersion, String ideBuild) { return render(pluginVersion, ideBuild, null); }
    public synchronized String render(String pluginVersion, String ideBuild, String buildIdentity) {
        StringBuilder text = new StringBuilder("PAGE 1 — SUMMARY (screenshot this first)\nIKASAN STUDIO IMPORT REPORT v1\n")
                .append("Plugin: ").append(safeVersion(pluginVersion)).append("; IDE build: ").append(safeVersion(ideBuild))
                .append("\nRuntime: ").append(version).append("; pack: ").append(pack).append("; revision: ").append(revision)
                .append("\nSource build: ").append(buildIdentity != null && buildIdentity.matches("[a-f0-9]{64}") ? buildIdentity.substring(0, 16) : "unavailable")
                .append("\nAttempt: ").append(attempt)
                .append("\nLast REST request: ").append(endpoint == null ? "not recorded" : endpoint).append("; HTTP: ").append(httpStatus == 0 ? "not received" : httpStatus)
                .append("\nStage: ").append(stage).append("; outcome: ").append(outcome)
                .append("\nFlows: ").append(flows).append("; components: ").append(components).append("; transitions: ").append(transitions)
                .append("\nExact/inferred pack matches: ").append(exactMatches)
                .append("\nConfiguration records: ").append(configurations).append("; unset parameters: ").append(unset)
                .append("\nIssues: ").append(issues.size()).append("; omitted: ").append(omitted)
                .append("\nF/C/P references follow REST document order; application names and values are excluded.\n")
                .append("This is a structural diagnostic, not proof of runtime compatibility.\n");
        var representatives = new LinkedHashMap<String, Integer>();
        var occurrences = new HashMap<String, Integer>();
        for (int i = 0; i < issues.size(); i++) {
            String code = issues.get(i).split(" — ", 2)[1];
            representatives.putIfAbsent(code, i); occurrences.merge(code, 1, Integer::sum);
        }
        int shown = 0;
        for (var entry : representatives.entrySet()) {
            if (shown++ >= 8) break;
            appendIssue(text, entry.getValue(), false);
            if (occurrences.get(entry.getKey()) > 1) text.append("  Occurrences: ").append(occurrences.get(entry.getKey())).append('\n');
        }
        if (!failures.isEmpty()) text.append("Failure: ").append(failures.get(0)).append(" (locations on detail pages)\n");
        if (representatives.size() > 8) text.append("Additional issue categories follow on detail pages.\n");
        text.append("No automatic upload. Review before sharing.\n");
        var details = new ArrayList<String>();
        for (int i = 0; i < issues.size(); i++) {
            var line = new StringBuilder(); appendIssue(line, i, true);
            details.addAll(Arrays.asList(line.toString().split("\n")));
        }
        details.addAll(failures);
        for (int i = 0; i < details.size(); i++) {
            if (i % 16 == 0) text.append("\nPAGE ").append(2 + i / 16).append(" — DETAILS\n");
            text.append(details.get(i)).append('\n');
        }
        return text.toString();
    }
    private void appendIssue(StringBuilder text, int index, boolean detail) {
        text.append(String.format(java.util.Locale.ROOT, "IMPORT-%03d: ", index + 1)).append(issues.get(index)).append('\n');
        if (detail) {
            String location = issues.get(index).split(" — ")[0];
            String component = location.replaceFirst("/P[0-9]+$", "");
            if (publicContext.containsKey(component)) text.append("  ").append(publicContext.get(component)).append('\n');
            if (!component.equals(location) && publicContext.containsKey(location)) text.append("  ").append(publicContext.get(location)).append('\n');
        }
    }
}
