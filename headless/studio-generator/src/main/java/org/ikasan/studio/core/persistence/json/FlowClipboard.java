package org.ikasan.studio.core.persistence.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.io.IOException;
import java.util.*;

/** Detached clipboard data. Unlike saved transitions, the route tree also preserves unfinished flows. */
public final class FlowClipboard {
    public static final String PREFIX = "Ikasan Studio flow/1\n";
    public static final String SOURCE_PREFIX = "Ikasan Studio flow/2\n";
    private static final int MAX_LENGTH = 8 * 1024 * 1024;

    private FlowClipboard() { }

    public record Sources(String packageName, Map<String, String> files) {
        public Sources { files = Map.copyOf(files); }
        public static Sources empty() { return new Sources("", Map.of()); }

        public void validate() throws IOException {
            if (!files.isEmpty() && (packageName == null || !javax.lang.model.SourceVersion.isName(packageName))) {
                throw new IOException("Invalid source package");
            }
            for (String path : files.keySet()) {
                if (!path.endsWith(".java") || path.startsWith("/") || path.contains("\\")) {
                    throw new IOException("Invalid source path");
                }
                String[] parts = path.substring(0, path.length() - 5).split("/", -1);
                for (int i = 0; i < parts.length; i++) {
                    boolean packageInfo = i == parts.length - 1 && parts[i].equals("package-info");
                    if ((!packageInfo && !javax.lang.model.SourceVersion.isName(parts[i])) || parts[i].contains(".")) {
                        throw new IOException("Invalid source path");
                    }
                }
            }
        }
    }

    public record Transfer(Flow flow, Sources sources) { }

    public static boolean isFlow(String text) {
        return text != null && (text.startsWith(PREFIX) || text.startsWith(SOURCE_PREFIX));
    }

    public static String encode(Map<String, Object> snapshot, Sources sources) throws IOException {
        sources.validate();
        if (sources.files().isEmpty()) return encode(snapshot);
        String result = SOURCE_PREFIX + StudioJson.newObjectMapper().writeValueAsString(
                Map.of("flow", snapshot, "sources", sources));
        if (result.length() > MAX_LENGTH) throw new IOException("Flow exceeds clipboard size limit");
        return result;
    }

    public static Transfer decodeTransfer(String text, String version) throws IOException, StudioBuildException {
        if (text != null && text.startsWith(PREFIX)) return new Transfer(decode(text, version), Sources.empty());
        if (text == null || !text.startsWith(SOURCE_PREFIX) || text.length() > MAX_LENGTH) {
            throw new IOException("Invalid flow clipboard data");
        }
        ObjectMapper mapper = StudioJson.newObjectMapper();
        JsonNode root = mapper.readTree(text.substring(SOURCE_PREFIX.length()));
        if (root == null) throw new IOException("Missing flow clipboard data");
        requireObject(root.path("flow"));
        JsonNode source = root.path("sources");
        requireObject(source.path("files"));
        if (!source.path("packageName").isTextual()) throw new IOException("Missing source package");
        Map<String, String> files = new LinkedHashMap<>();
        for (var field : source.get("files").properties()) {
            if (!field.getValue().isTextual()) throw new IOException("Invalid source text");
            files.put(field.getKey(), field.getValue().textValue());
        }
        Sources sources = new Sources(source.get("packageName").textValue(), files);
        sources.validate();
        return new Transfer(decode(PREFIX + mapper.writeValueAsString(root.get("flow")), version), sources);
    }

    /** Capture on the model-owning thread; contains no model references, metadata lookups or JSON parsing. */
    public static Map<String, Object> capture(Flow flow, String version) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("version", version);
        snapshot.put("properties", properties(flow));
        snapshot.put("route", route(flow.getFlowRoute()));
        if (flow.getConsumer() != null) snapshot.put("consumer", element(flow.getConsumer()));
        if (flow.getExceptionResolver() != null) {
            Map<String, Object> resolutions = new LinkedHashMap<>();
            for (ExceptionResolution resolution : flow.getExceptionResolver().getExceptionResolutionList()) {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put(ComponentMeta.EXCEPTIONS_CAUGHT_KEY, resolution.getExceptionsCaught());
                value.put(ComponentMeta.ACTION_KEY, resolution.getTheAction());
                value.put(ComponentMeta.ACTION_PROPERTIES_KEY, properties(resolution));
                resolutions.put(resolution.getExceptionsCaught(), value);
            }
            snapshot.put("exceptionResolver", resolutions);
        }
        return snapshot;
    }

    public static String encode(Map<String, Object> snapshot) throws IOException {
        String result = PREFIX + StudioJson.newObjectMapper().writeValueAsString(snapshot);
        if (result.length() > MAX_LENGTH) throw new IOException("Flow exceeds clipboard size limit");
        return result;
    }

    public static Flow decode(String text, String destinationVersion) throws IOException, StudioBuildException {
        if (text == null || !text.startsWith(PREFIX) || text.length() > MAX_LENGTH) {
            throw new IOException("Invalid flow clipboard data");
        }
        ObjectMapper mapper = StudioJson.newObjectMapper();
        JsonNode root = mapper.readTree(text.substring(PREFIX.length()));
        if (root == null || !root.isObject()) throw new IOException("Missing flow clipboard data");
        String sourceVersion = root.path("version").asText();
        if (!Objects.equals(sourceVersion, destinationVersion)) throw new VersionMismatch(sourceVersion, destinationVersion);
        ModuleDeserializer reader = new ModuleDeserializer();
        requireObject(root.path("properties"));
        Flow flow = reader.getFlow(root.path("properties"), destinationVersion);
        if (flow == null || flow.getIdentity() == null || flow.getIdentity().isBlank()) throw new IOException("Missing flow name");
        flow.setFlowRoute(readRoute(root.path("route"), flow, destinationVersion, reader));
        if (root.has("consumer")) {
            FlowElement consumer = readElement(root.get("consumer"), flow, flow.getFlowRoute(), destinationVersion, reader);
            if (!consumer.getComponentMeta().isConsumer()) throw new IOException("Invalid flow consumer");
            flow.setConsumer(consumer);
        }
        if (root.has("exceptionResolver")) {
            requireObject(root.get("exceptionResolver"));
            flow.setExceptionResolver(reader.getExceptionResolver(flow, root.get("exceptionResolver"), destinationVersion));
        }
        // The normal model reader can recover missing components/properties. Clipboard paste must not silently lose them.
        if (cannotPreserve(mapper.valueToTree(capture(flow, destinationVersion)), root)) {
            throw new IOException("The destination cannot preserve all copied flow data");
        }
        return flow;
    }

    /** Includes Java class, variable and package collisions caused by punctuation or case differences. */
    public static boolean nameAvailable(Module module, String name) {
        return name != null && !name.isBlank() && module.getFlows().stream().noneMatch(flow ->
                Objects.equals(flow.getIdentity(), name)
                || Objects.equals(flow.getJavaClassName(), StudioBuildUtils.toJavaClassName(name))
                || Objects.equals(flow.getJavaVariableName(), StudioBuildUtils.toJavaIdentifier(name))
                || Objects.equals(flow.getJavaPackageName(), StudioBuildUtils.toJavaPackageName(name)));
    }

    public static String availableName(Module module, String original) {
        if (nameAvailable(module, original)) return original;
        String candidate = original + " Copy";
        for (int suffix = 2; !nameAvailable(module, candidate); suffix++) candidate = original + " Copy " + suffix;
        return candidate;
    }

    private static Map<String, Object> properties(BasicElement element) {
        Map<String, Object> values = new LinkedHashMap<>();
        element.getUnknownJsonProperties().forEach((key, value) -> values.put(key, value.deepCopy()));
        if (element.getComponentProperties() != null) element.getComponentProperties().forEach((key, property) -> {
            Object value = property.getValue();
            if (value instanceof List<?>) value = property.getValueString();
            if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) value = ((Number) value).longValue();
            if (value != null) values.put(key, value instanceof Boolean || value instanceof Number ? value : value.toString());
        });
        return values;
    }

    private static Map<String, Object> element(FlowElement element) {
        Map<String, Object> result = properties(element);
        var meta = element.getComponentMeta();
        result.put(ComponentMeta.COMPONENT_TYPE_KEY, meta.getComponentType());
        result.put(ComponentMeta.IMPLEMENTING_CLASS_KEY, meta.getImplementingClass());
        if (meta.getAdditionalKey() != null) result.put(ComponentMeta.ADDITIONAL_KEY, meta.getAdditionalKey());
        if (element.getDecorators() != null && !element.getDecorators().isEmpty()) {
            result.put(FlowElement.DECORATORS_JSON_TAG, element.getDecorators().stream().map(decorator -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("type", decorator.getType().name());
                value.put("name", decorator.getName());
                value.put("configurationId", decorator.getConfigurationId());
                value.put("configurable", decorator.isConfigurable());
                if (decorator.getTimeToLive() != null) value.put("timeToLive", decorator.getTimeToLive());
                return value;
            }).toList());
        }
        return result;
    }

    private static Map<String, Object> route(FlowRoute route) {
        return Map.of("name", route.getRouteName(),
                "elements", route.getFlowElements().stream().map(FlowClipboard::element).toList(),
                "children", route.getChildRoutes().stream().map(FlowClipboard::route).toList());
    }

    private static FlowRoute readRoute(JsonNode node, Flow flow, String version, ModuleDeserializer reader)
            throws IOException, StudioBuildException {
        requireObject(node);
        if (!node.path("name").isTextual() || !node.path("elements").isArray() || !node.path("children").isArray()) {
            throw new IOException("Invalid flow route");
        }
        FlowRoute route = FlowRoute.flowRouteBuilder().flow(flow).routeName(node.get("name").asText()).build();
        for (JsonNode element : node.get("elements")) {
            FlowElement restored = readElement(element, flow, route, version, reader);
            if (restored.getComponentMeta().isConsumer()) throw new IOException("Consumer inside route");
            route.getFlowElements().add(restored);
        }
        for (JsonNode child : node.get("children")) route.getChildRoutes().add(readRoute(child, flow, version, reader));
        return route;
    }

    private static FlowElement readElement(JsonNode node, Flow flow, FlowRoute route, String version, ModuleDeserializer reader)
            throws IOException, StudioBuildException {
        requireObject(node);
        FlowElement element = reader.getInitialFlowElement(node, flow, version);
        if (element == null) throw new IOException("Unsupported flow component");
        element.setContainingFlowRoute(route);
        return element;
    }

    private static void requireObject(JsonNode node) throws IOException {
        if (!node.isObject()) throw new IOException("Invalid flow clipboard object");
    }

    private static boolean cannotPreserve(JsonNode actual, JsonNode expected) {
        if (actual == null) return true;
        if (expected.isObject()) {
            for (var field : expected.properties()) {
                if (cannotPreserve(actual.get(field.getKey()), field.getValue())) return true;
            }
            return !actual.isObject();
        }
        if (expected.isArray()) {
            if (!actual.isArray() || actual.size() != expected.size()) return true;
            for (int i = 0; i < expected.size(); i++) if (cannotPreserve(actual.get(i), expected.get(i))) return true;
            return false;
        }
        if (actual.isNumber() && expected.isNumber()) return actual.decimalValue().compareTo(expected.decimalValue()) != 0;
        return !actual.equals(expected);
    }

    public static final class VersionMismatch extends IOException {
        public final String source;
        public final String destination;
        public VersionMismatch(String source, String destination) {
            super("Flow and destination versions differ");
            this.source = source;
            this.destination = destination;
        }
    }
}
