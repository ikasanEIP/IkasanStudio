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
                        "instructions", "Read studio_snapshot and studio_catalogue. Submit studio_propose with the snapshot revision. Changes require Apply in Studio; never edit model.json while Studio is open. Poll studio_proposal_status for the result.");
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
                "addFlow: {type,flow}. addComponent: {type,flow,key,name,properties?}. setProperty: {type,flow,component,property,value}. connect: {type,flow,order:[all component names, consumer first]}. Operations are applied in order. Submit complete valid linear flows. No routers, deletion, renaming or version changes.",
                "properties", Map.of("type", Map.of("type", "string", "enum", List.of("addFlow", "addComponent", "setProperty", "connect")),
                        "flow", string, "key", string, "name", string, "component", string, "property", string,
                        "value", Map.of(), "properties", Map.of("type", "object"), "order", Map.of("type", "array", "items", string)),
                "required", List.of("type", "flow"), "additionalProperties", false);
        return List.of(
                tool("studio_snapshot", "Read the current live module and revision. Known credential fields are redacted. Pending property edits must first be applied or cancelled in Studio.", Map.of(), List.of()),
                tool("studio_catalogue", "Read the selected meta-pack's component keys, properties, defaults and payload contracts.", Map.of(), List.of()),
                tool("studio_propose", "Validate edits and open a preview in Studio. Does not apply edits. Only one review can be pending.",
                        Map.of("revision", string, "operations", Map.of("type", "array", "items", operation, "minItems", 1, "maxItems", 100)), List.of("revision", "operations")),
                tool("studio_proposal_status", "Read proposal review and generation status.", Map.of("proposalId", string), List.of("proposalId")));
    }
    private static Map<String, Object> tool(String name, String description, Map<String, Object> properties, List<String> required) {
        return Map.of("name", name, "description", description, "inputSchema",
                Map.of("type", "object", "properties", properties, "required", required, "additionalProperties", false));
    }
}
