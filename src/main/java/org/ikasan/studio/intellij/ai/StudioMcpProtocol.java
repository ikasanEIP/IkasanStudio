package org.ikasan.studio.intellij.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/** MCP stdio messages are forwarded unchanged by the bundled bridge; no IDE dependency in the protocol. */
public final class StudioMcpProtocol {
    private static final ObjectMapper JSON = new ObjectMapper();
    @FunctionalInterface public interface Tools { Object call(String name, JsonNode arguments) throws Exception; }
    private final Tools tools;
    public StudioMcpProtocol(Tools tools) { this.tools = tools; }

    public Object handle(JsonNode request) {
        if (request != null && request.isArray()) {
            if (request.isEmpty()) return error(null, -32600, "Empty JSON-RPC batch");
            List<Object> replies = new ArrayList<>();
            request.forEach(item -> { Object reply = handle(item); if (reply != null) replies.add(reply); });
            return replies.isEmpty() ? null : replies;
        }
        if (request == null) return error(null, -32600, "Invalid JSON-RPC request");
        if (!request.isObject() || !"2.0".equals(request.path("jsonrpc").asText()) || !request.path("method").isTextual())
            return error(null, -32600, "Invalid JSON-RPC request");
        if (!request.has("id")) return null;
        Object id = JSON.convertValue(request.get("id"), Object.class);
        try {
            Object result;
            switch (request.path("method").asText()) {
                case "initialize" -> result = Map.of("protocolVersion", "2025-03-26", "capabilities", Map.of("tools", Map.of()),
                        "serverInfo", Map.of("name", "ikasan-studio", "version", "1.0"),
                        "instructions", "Read studio_snapshot and studio_catalogue. Submit studio_propose with the snapshot revision. Validated supported changes can apply automatically unless Confirm deletes requires review (enabled by default for deletions and replacements) or Always ask for approval is enabled; potential developer-owned code replacement always requires Apply in Studio; never edit model.json while Studio is open. Poll studio_proposal_status for the result.");
                case "ping" -> result = Map.of();
                case "tools/list" -> result = Map.of("tools", definitions());
                case "tools/call" -> {
                    String name = request.path("params").path("name").asText();
                    if (definitions().stream().noneMatch(tool -> tool.get("name").equals(name))) return error(id, -32602, "Unknown tool: " + name);
                    JsonNode arguments = request.path("params").path("arguments");
                    if (arguments.isMissingNode()) arguments = JSON.createObjectNode();
                    try {
                        Object value = tools.call(name, arguments);
                        result = Map.of("content", List.of(Map.of("type", "text", "text", JSON.writeValueAsString(value))), "isError", false);
                    } catch (Exception failure) {
                        result = Map.of("content", List.of(Map.of("type", "text", "text", failure.getMessage() == null ? "Studio could not process this request." : failure.getMessage())), "isError", true);
                    }
                }
                default -> { return error(id, -32601, "Method not found"); }
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("jsonrpc", "2.0"); response.put("id", id); response.put("result", result);
            return response;
        } catch (Exception failure) { return error(id, -32603, "Could not process MCP request"); }
    }

    private static Object error(Object id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0"); response.put("id", id); response.put("error", Map.of("code", code, "message", message));
        return response;
    }

    static List<Map<String, Object>> definitions() {
        Map<String, Object> string = Map.of("type", "string");
        Map<String, Object> operation = Map.of("type", "object", "description",
                "addFlow: {type,flow}. deleteFlow: {type,flow}; removes the entire flow and attached test harnesses, retaining developer-owned source files. addComponent: {type,flow,key,name,properties?}. setProperty: {type,flow,component,property,value}. renameComponent: {type,flow,component,name}. deleteComponent: {type,flow,component}. replaceComponent: {type,flow,component,key,name,properties?}; preserves position, replaces consumers only with consumers, preserves developer-owned source files. connect: {type,flow,order:[all component names, consumer first]}. New flow and component names must match [A-Za-z][A-Za-z0-9_ ]{0,79}: start with a letter, then letters, digits, spaces or underscores, maximum 80 characters. For numbered flows use Flow01 or Demo01, never a leading number. Existing flow/component references must use their exact saved names. Operations are applied in order. Empty flows and incremental linear-flow construction are supported; incomplete flows show review warnings and must be completed before running. Routers and branched routes are supported. addComponent and connect accept optional route:[branch names] (omit for root). connect lists every executable component in that route, consumer first only for root; endpoints are omitted. configureRoutes {type,flow,component,names:[branch names]} configures 2 to 32 alphanumeric names; populated branches cannot be removed or renamed. setExceptionResolution {type,flow,exception,action,properties?} adds or updates a flow-wide rule; read exceptionActions in studio_catalogue. Flow renaming and version changes require Studio.",
                "properties", Map.ofEntries(
                        Map.entry("type", Map.of("type", "string", "enum", List.of("addFlow", "deleteFlow", "addComponent", "setProperty", "renameComponent", "deleteComponent", "replaceComponent", "connect", "configureRoutes", "setExceptionResolution"))),
                        Map.entry("flow", string), Map.entry("key", string), Map.entry("name", string),
                        Map.entry("component", string), Map.entry("property", string), Map.entry("value", Map.of()),
                        Map.entry("properties", Map.of("type", "object")), Map.entry("order", Map.of("type", "array", "items", string)),
                        Map.entry("route", Map.of("type", "array", "items", string)), Map.entry("names", Map.of("type", "array", "items", string)),
                        Map.entry("exception", string), Map.entry("action", string)),
                "required", List.of("type", "flow"), "additionalProperties", false);
        return List.of(
                tool("studio_snapshot", "Read the current live module and revision. Known credential fields are redacted. Pending property edits must first be applied or cancelled in Studio.", Map.of(), List.of()),
                tool("studio_catalogue", "Read component keys, help, properties, payload contracts, recipe configuration examples, implementation/ownership flags and supported proposal operations for the selected meta-pack. Includes frameworkReference with version-specific Ikasan source navigation and offline research guidance. Generated stubs may still need implementation and tests.", Map.of(), List.of()),
                tool("studio_propose", "Validate edits. Validated supported changes apply automatically unless Confirm deletes requires review (enabled by default for deletions and replacements) or Always ask for approval is enabled; potential developer-owned code replacement always opens a review. Check the returned status and studio_proposal_status; ask for Apply only when awaiting_review. Only one review can be pending.",
                        Map.of("revision", string, "operations", Map.of("type", "array", "items", operation, "minItems", 1, "maxItems", 100)), List.of("revision", "operations")),
                tool("studio_proposal_status", "Read proposal review and generation status.", Map.of("proposalId", string), List.of("proposalId")));
    }
    private static Map<String, Object> tool(String name, String description, Map<String, Object> properties, List<String> required) {
        return Map.of("name", name, "description", description, "inputSchema",
                Map.of("type", "object", "properties", properties, "required", required, "additionalProperties", false));
    }
}
