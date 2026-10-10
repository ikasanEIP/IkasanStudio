package org.ikasan.studio.core.persistence.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentMeta;

import java.io.IOException;
import java.util.*;

/**
 * Persisted design contract: native Ikasan module metadata and configuration records, accompanied by
 * Studio-only generation settings. The three documents share one atomic file. Neither topology nor
 * mapped configuration values are duplicated in the Studio document. The legacy tree is an internal
 * adapter for existing version-neutral model/generator APIs, never an alternative persisted authority.
 */
public final class IkasanModelDocuments {
    public static final String FORMAT = "ikasan-studio-documents";
    public static final int VERSION = 1;
    // Round-trip carrier inside the in-memory adapter, removed when writing the document container.
    public static final String RETAINED = "_ikasanDocumentExtensions";
    private static final String FLOW_RETAINED = "_ikasanFlowExtensions";
    private static final String COMPONENT_RETAINED = "_ikasanComponentExtensions";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> MODULE_FIELDS = List.of("name", "description", "host", "port", "context", "protocol", "moduleType", "configuredResourceId");
    private static final List<String> FLOW_FIELDS = List.of("name", "description", "flowStartupType", "flowStartupComment", "configurationId", "moduleVersion", "transitions");
    private static final List<String> COMPONENT_FIELDS = List.of("componentName", "description", "configurationId", "invokerConfigurationId");

    private IkasanModelDocuments() { }

    public static boolean isDocumentContainer(JsonNode node) {
        return node != null && node.has("modelFormat");
    }

    /** Convert a legacy design, or normalize an existing container, without changing files. */
    public static ObjectNode fromLegacy(JsonNode input) throws IOException {
        if (isDocumentContainer(input)) input = toLegacy(input);
        ObjectNode legacy = object(input, "Studio design").deepCopy();
        String pack = text(legacy, "version");
        try {
            JsonNode retained = legacy.remove(RETAINED);
            JsonNode imported = legacy.get("ikasanRuntimeImport");
            JsonNode original = retained != null ? retained : imported;
            ObjectNode module = original != null && original.path("module").isObject()
                    ? ((ObjectNode) original.path("module")).deepCopy() : JSON.createObjectNode();
            ArrayNode configs = JSON.createArrayNode();
            if (original != null && original.hasNonNull("configuration")) {
                JsonNode configuration = original.get("configuration");
                if (configuration instanceof ArrayNode array) configs.addAll(array.deepCopy());
                else configs.add(configuration.deepCopy());
            }
            Map<String, ObjectNode> configIndex = index(configs, "configurationId", "configuration");
            Map<String, ObjectNode> writtenConfigurations = new HashMap<>();
            ObjectNode root = JSON.createObjectNode().put("modelFormat", FORMAT).put("formatVersion", VERSION);
            root.set("module", module);
            root.set("configuration", configs);
            ObjectNode studio = root.putObject("studio").put("metaPack", pack);
            ObjectNode settings = legacy.deepCopy();
            settings.remove(List.of("version", "flows"));
            if (retained != null) module.remove(MODULE_FIELDS);
            move(settings, module, MODULE_FIELDS);
            module.put("ikasanVersion", ComponentLibrary.getMetaPackManifest(pack).ikasanVersion());
            // Runtime 'version' is the application's version, never the meta-pack ID.
            studio.set("properties", settings);
            ObjectNode flowSettings = studio.putObject("flows");
            Map<String, ObjectNode> oldFlows = index(arrayOrEmpty(module.get("flows"), "module.flows"), "name", "module.flows");
            ArrayNode flows = module.putArray("flows");
            Set<String> flowNames = new HashSet<>();
            for (JsonNode node : arrayOrEmpty(legacy.get("flows"), "flows")) {
                ObjectNode source = object(node, "flow").deepCopy();
                String name = text(source, "name");
                if (!flowNames.add(name)) throw invalid("Duplicate flow name: " + name);
                ObjectNode flow = oldFlows.containsKey(name) ? oldFlows.get(name).deepCopy() : JSON.createObjectNode();
                Map<String, ObjectNode> oldComponents = new LinkedHashMap<>();
                for (JsonNode component : arrayOrEmpty(flow.get("flowElements"), "flowElements"))
                    oldComponents.put(text(component, "componentName"), object(component, "component"));
                if (flow.path("consumer").isObject()) oldComponents.put(text(flow.get("consumer"), "componentName"), (ObjectNode) flow.get("consumer"));
                JsonNode flowExtensions = source.remove(FLOW_RETAINED);
                if (flowExtensions != null) flow.setAll(object(flowExtensions, "flow extensions"));
                if (retained != null) flow.remove(FLOW_FIELDS);
                flow.remove("transitions");
                move(source, flow, FLOW_FIELDS);
                ObjectNode extras = flowSettings.putObject(name);
                ObjectNode components = extras.putObject("components");
                ArrayNode elements = flow.putArray("flowElements");
                JsonNode consumer = source.remove("consumer");
                flow.remove("consumer");
                if (consumer != null && !consumer.isNull()) {
                    ObjectNode converted = component(object(consumer, "consumer"), oldComponents, components,
                            module.path("name").asText(), name, pack, configIndex, configs, writtenConfigurations);
                    flow.set("consumer", converted);
                    elements.add(converted.deepCopy()); // Core exports include the consumer in both places.
                }
                for (JsonNode component : arrayOrEmpty(source.remove("flowElements"), "flowElements"))
                    elements.add(component(object(component, "component"), oldComponents, components,
                            module.path("name").asText(), name, pack, configIndex, configs, writtenConfigurations));
                extras.set("properties", source);
                flows.add(flow);
            }
            applyGeneratedImplementations(legacy, module, false);
            return root;
        } catch (IOException e) { throw e; }
        catch (Exception e) { throw new IOException("Cannot separate the Ikasan model documents; the selected meta-pack must be available.", e); }
    }

    private static ObjectNode component(ObjectNode input, Map<String, ObjectNode> originals, ObjectNode extras,
                                        String moduleName, String flowName, String pack,
                                        Map<String, ObjectNode> configs, ArrayNode records, Map<String, ObjectNode> written) throws Exception {
        ObjectNode properties = input.deepCopy();
        String name = text(properties, "componentName");
        if (extras.has(name)) throw invalid("Duplicate component: " + name);
        var entry = ComponentLibrary.getIkasanComponents(pack).entrySet().stream().filter(e ->
                Objects.equals(e.getValue().getComponentType(), input.path("componentType").asText(null))
                && Objects.equals(e.getValue().getImplementingClass(), input.path("implementingClass").asText(null))
                && Objects.equals(e.getValue().getAdditionalKey(), input.path("additionalKey").asText(null)))
                .findFirst().orElseThrow(() -> invalid("No component mapping for " + name));
        ComponentMeta meta = entry.getValue();
        ObjectNode nativeComponent = originals.containsKey(name) ? originals.get(name).deepCopy() : JSON.createObjectNode();
        JsonNode componentExtensions = properties.remove(COMPONENT_RETAINED);
        if (componentExtensions != null) nativeComponent.setAll(object(componentExtensions, "component extensions"));
        if (componentExtensions != null) nativeComponent.remove(COMPONENT_FIELDS);
        move(properties, nativeComponent, COMPONENT_FIELDS);
        nativeComponent.put("componentType", meta.getComponentType());
        nativeComponent.put("implementingClass", meta.isGeneratesUserImplementedClass()
                && !input.path("requiresStub").asBoolean(true) ? input.path("userImplementedClassName").asText(meta.getImplementingClass())
                : meta.getImplementingClass());
        properties.remove(List.of("componentType", "implementingClass", "additionalKey"));
        if (meta.isGeneratesUserImplementedClass() && !input.path("requiresStub").asBoolean(true))
            properties.remove("userImplementedClassName");
        ObjectNode settings = extras.putObject(name).put("component", entry.getKey());
        settings.set("properties", properties);
        String idProperty = input.has("configuredResourceId") ? "configuredResourceId" : "configurationId";
        if (input.has(idProperty)) {
            settings.put("configurationIdProperty", idProperty);
            properties.remove(idProperty);
            String expression = input.path(idProperty).asText("");
            if (expression.contains("__")) {
                settings.put("configurationIdTemplate", expression);
                nativeComponent.put("configurationId", resolveId(expression, moduleName, flowName, name));
            } else if (!expression.isBlank()) nativeComponent.put("configurationId", expression);
        }
        String configClass = configurationClass(meta);
        if (!meta.getRuntimeConfigurationProperties().isEmpty()) {
            String id = nativeComponent.path("configurationId").asText("");
            if (id.isBlank()) id = identifier(moduleName, flowName, name, configClass);
            nativeComponent.put("configurationId", id).put("configurable", true);
            ObjectNode valuesToWrite = JSON.createObjectNode().put("implementingClass", configClass);
            for (String key : meta.getRuntimeConfigurationProperties()) if (properties.has(key)) valuesToWrite.set(key, properties.get(key));
            ObjectNode previous = written.putIfAbsent(id, valuesToWrite);
            if (previous != null && !previous.equals(valuesToWrite))
                throw invalid("Components sharing configuration " + id + " have different settings; use distinct configuration IDs or matching values");
            ObjectNode record = configs.get(id);
            if (record == null) {
                record = JSON.createObjectNode().put("configurationId", id);
                record.putArray("parameters");
                records.add(record);
                configs.put(id, record);
            }
            if (record.hasNonNull("implementingClass") && !configClass.equals(record.path("implementingClass").asText()))
                throw invalid("Configuration class does not match component " + name);
            record.put("implementingClass", configClass);
            Map<String, ObjectNode> parameters = index(arrayOrEmpty(record.get("parameters"), "parameters"), "name", "parameters");
            for (String key : meta.getRuntimeConfigurationProperties()) {
                JsonNode value = properties.remove(key);
                if (value == null) { parameters.remove(key); continue; }
                try {
                    value = scalarValue(meta, key, value);
                    checkValue(meta, key, value);
                } catch (IOException incompleteSetting) {
                    // A designer must be able to save unfinished values without presenting them as valid runtime configuration.
                    settings.withObject("/pendingConfiguration").set(key, value);
                    parameters.remove(key);
                    continue;
                }
                ObjectNode parameter = parameters.computeIfAbsent(key, ignored -> JSON.createObjectNode().put("name", key));
                parameter.set("value", value);
                parameter.put("implementingClass", parameterClass(meta.getAllowableProperties().get(key).getPropertyDataType()));
            }
            ArrayNode values = record.putArray("parameters");
            parameters.values().forEach(values::add);
        }
        return nativeComponent;
    }

    /** Reconstruct the internal design exclusively from the three documents; accepts old designs unchanged. */
    public static JsonNode toLegacy(JsonNode input) throws IOException {
        if (!isDocumentContainer(input)) return input;
        if (!FORMAT.equals(input.path("modelFormat").asText()) || !input.path("formatVersion").isInt()
                || input.path("formatVersion").asInt() != VERSION) throw invalid("Unsupported Studio model document format");
        knownFields(object(input, "model"), Set.of("modelFormat", "formatVersion", "module", "configuration", "studio"));
        ObjectNode module = object(input.get("module"), "module");
        ObjectNode studio = object(input.get("studio"), "studio");
        knownFields(studio, Set.of("metaPack", "properties", "flows"));
        String pack = text(studio, "metaPack");
        try {
            if (!ComponentLibrary.getMetaPackManifest(pack).ikasanVersion().equals(text(module, "ikasanVersion")))
                throw invalid("module.ikasanVersion does not match studio.metaPack");
            Map<String, ObjectNode> configs = index(requiredArray(input.get("configuration"), "configuration"), "configurationId", "configuration");
            for (ObjectNode config : configs.values()) index(requiredArray(config.get("parameters"), "parameters"), "name", "parameters");
            ObjectNode result = object(studio.get("properties"), "studio.properties").deepCopy();
            rejectFields(result, MODULE_FIELDS, "studio.properties");
            rejectFields(result, List.of("flows", "version", RETAINED), "studio.properties");
            copy(module, result, MODULE_FIELDS);
            result.put("version", pack);
            ArrayNode flows = result.putArray("flows");
            ObjectNode flowSettings = object(studio.get("flows"), "studio.flows");
            Set<String> names = new HashSet<>();
            for (JsonNode node : requiredArray(module.get("flows"), "module.flows")) {
                ObjectNode raw = object(node, "flow");
                String name = text(raw, "name");
                if (!names.add(name)) throw invalid("Duplicate flow: " + name);
                ObjectNode extra = object(flowSettings.get(name), "Studio metadata for flow " + name);
                knownFields(extra, Set.of("properties", "components"));
                ObjectNode flow = object(extra.get("properties"), "flow properties").deepCopy();
                rejectFields(flow, FLOW_FIELDS, "flow properties");
                rejectFields(flow, List.of("consumer", "flowElements"), "flow properties");
                copy(raw, flow, FLOW_FIELDS);
                ObjectNode flowExtensions = raw.deepCopy();
                flowExtensions.remove(FLOW_FIELDS);
                flowExtensions.remove(List.of("consumer", "flowElements"));
                flow.set(FLOW_RETAINED, flowExtensions);
                ObjectNode componentSettings = object(extra.get("components"), "component metadata");
                Set<String> used = new HashSet<>();
                String consumerName = raw.path("consumer").isObject() ? text(raw.get("consumer"), "componentName") : null;
                if (consumerName != null) {
                    flow.set("consumer", restoreComponent(raw.get("consumer"), componentSettings, pack, configs, module.path("name").asText(), name));
                    used.add(consumerName);
                }
                ArrayNode elements = flow.putArray("flowElements");
                boolean consumerSeen = false;
                for (JsonNode c : requiredArray(raw.get("flowElements"), "flowElements")) {
                    String componentName = text(c, "componentName");
                    if (componentName.equals(consumerName)) {
                        if (consumerSeen || !c.equals(raw.get("consumer"))) throw invalid("Conflicting consumer entries in " + name);
                        consumerSeen = true;
                        continue;
                    }
                    if (!used.add(componentName)) throw invalid("Duplicate component in " + name);
                    elements.add(restoreComponent(c, componentSettings, pack, configs, module.path("name").asText(), name));
                }
                validateTransitions(raw, used);
                if (!used.equals(fieldNames(componentSettings))) throw invalid("Component metadata does not match topology in " + name);
                flows.add(flow);
            }
            if (!names.equals(fieldNames(flowSettings))) throw invalid("Flow metadata does not match module topology");
            applyGeneratedImplementations(result, module, true);
            ObjectNode retained = result.putObject(RETAINED);
            retained.set("module", module.deepCopy());
            retained.set("configuration", input.get("configuration").deepCopy());
            return result;
        } catch (IOException e) { throw e; }
        catch (Exception e) { throw new IOException("Cannot load the Ikasan model documents; check the selected meta-pack.", e); }
    }

    private static ObjectNode restoreComponent(JsonNode raw, ObjectNode settings, String pack,
                                               Map<String, ObjectNode> configs, String moduleName, String flowName) throws Exception {
        String name = text(raw, "componentName");
        ObjectNode extra = object(settings.get(name), "Studio metadata for component " + name);
        knownFields(extra, Set.of("component", "properties", "configurationIdProperty", "configurationIdTemplate", "pendingConfiguration"));
        ComponentMeta meta = ComponentLibrary.getIkasanComponents(pack).get(text(extra, "component"));
        if (meta == null) throw invalid("Unknown component variant for " + name);
        if (!meta.getComponentType().equals(text(raw, "componentType"))) throw invalid("Component type disagrees with variant for " + name);
        ObjectNode result = object(extra.get("properties"), "component properties").deepCopy();
        rejectFields(result, COMPONENT_FIELDS, "component properties");
        // Collection settings lived in studio.properties before native List/Map support.
        // Keep those legacy values for ComponentProperty to normalize, but never silently
        // choose between a legacy setting and a native configuration value below.
        for (String key : meta.getRuntimeConfigurationProperties()) {
            if (!org.ikasan.studio.core.model.StringCollectionValues.supports(
                    meta.getAllowableProperties().get(key).getPropertyDataType())) {
                rejectFields(result, List.of(key), "component properties");
            }
        }
        rejectFields(result, List.of("componentType", "implementingClass", "additionalKey"), "component properties");
        copy(raw, result, COMPONENT_FIELDS);
        ObjectNode componentExtensions = object(raw, "component").deepCopy();
        componentExtensions.remove(COMPONENT_FIELDS);
        componentExtensions.remove(List.of("componentType", "implementingClass"));
        result.set(COMPONENT_RETAINED, componentExtensions);
        result.put("componentType", meta.getComponentType()).put("implementingClass", meta.getImplementingClass());
        if (meta.getAdditionalKey() != null) result.put("additionalKey", meta.getAdditionalKey());
        String implementation = text(raw, "implementingClass");
        if (!meta.isGeneratesUserImplementedClass() && !meta.getImplementingClass().equals(implementation))
            throw invalid("Runtime implementation disagrees with variant for " + name);
        if (meta.isGeneratesUserImplementedClass() && !result.path("requiresStub").asBoolean(true)) {
            result.put("userImplementedClassName", implementation).put("requiresStub", false);
        }
        String id = raw.path("configurationId").asText("");
        if (extra.has("configurationIdProperty")) {
            String key = text(extra, "configurationIdProperty");
            if (!List.of("configurationId", "configuredResourceId").contains(key)) throw invalid("Invalid configuration ID property");
            result.remove("configurationId");
            String value = id;
            if (extra.has("configurationIdTemplate")) {
                value = text(extra, "configurationIdTemplate");
                if (!resolveId(value, moduleName, flowName, name).equals(id)) throw invalid("Configuration ID disagrees with its Studio naming template for " + name);
            }
            result.put(key, value);
        }
        // A generated native ID is not an explicitly configured Studio property. Turning it
        // into one on load adds configurationIdProperty on the next save and makes a freshly
        // saved live canvas compare unequal to the same model reloaded for test generation.
        if (!extra.has("configurationIdProperty") && !meta.getRuntimeConfigurationProperties().isEmpty()
                && id.equals(identifier(moduleName, flowName, name, configurationClass(meta)))) {
            result.remove("configurationId");
        }
        if (!meta.getRuntimeConfigurationProperties().isEmpty()) {
            ObjectNode config = configs.get(id);
            if (config == null) throw invalid("Missing configuration " + id + " for " + name);
            if (!configurationClass(meta).equals(text(config, "implementingClass"))) throw invalid("Configuration class does not match " + name);
            for (JsonNode p : requiredArray(config.get("parameters"), "parameters")) {
                String key = text(p, "name");
                if (meta.getRuntimeConfigurationProperties().contains(key)) {
                    if (result.has(key)) throw invalid("Conflicting Studio and runtime configuration field " + key + " for " + name);
                    if (!p.has("value")) throw invalid("Missing configuration value for " + key);
                    checkValue(meta, key, p.get("value"));
                    result.set(key, p.get("value").deepCopy());
                }
            }
        }
        if (extra.has("pendingConfiguration")) {
            ObjectNode pending = object(extra.get("pendingConfiguration"), "pendingConfiguration");
            for (var entry : pending.properties()) {
                if (!meta.getRuntimeConfigurationProperties().contains(entry.getKey()) || result.has(entry.getKey()))
                    throw invalid("Conflicting or unknown pending configuration field " + entry.getKey());
                result.set(entry.getKey(), entry.getValue().deepCopy());
            }
        }
        return result;
    }

    private static String configurationClass(ComponentMeta meta) {
        if (meta.getRuntimeConfigurationClass() != null) return meta.getRuntimeConfigurationClass();
        var property = meta.getAllowableProperties().get("configuration");
        return property == null ? "" : property.getUsageDataType();
    }

    private static JsonNode scalarValue(ComponentMeta meta, String key, JsonNode value) throws IOException {
        Class<?> type = meta.getAllowableProperties().get(key).getPropertyDataType();
        if (org.ikasan.studio.core.model.StringCollectionValues.supports(type)) {
            try { return JSON.valueToTree(org.ikasan.studio.core.model.StringCollectionValues.normalize(type, JSON.convertValue(value, Object.class))); }
            catch (IllegalArgumentException failure) { throw invalid("Invalid collection configuration for " + key); }
        }
        if (!value.isTextual() || type == String.class) return value;
        String text = value.asText();
        try {
            if ((type == Boolean.class || type == boolean.class) && ("true".equals(text) || "false".equals(text)))
                return JSON.getNodeFactory().booleanNode(Boolean.parseBoolean(text));
            if (type == Integer.class || type == int.class) return JSON.getNodeFactory().numberNode(Integer.parseInt(text));
            if (type == Long.class || type == long.class) return JSON.getNodeFactory().numberNode(Long.parseLong(text));
        } catch (NumberFormatException ignored) { /* Report the field without disclosing its contents. */ }
        throw invalid("Invalid configuration value type for " + key);
    }

    private static void checkValue(ComponentMeta meta, String key, JsonNode value) throws IOException {
        Class<?> type = meta.getAllowableProperties().get(key).getPropertyDataType();
        boolean valid = value.isNull() || org.ikasan.studio.core.model.StringCollectionValues.compatible(type, value) || type == String.class && value.isTextual()
                || (type == Boolean.class || type == boolean.class) && value.isBoolean()
                || (type == Integer.class || type == int.class) && value.isIntegralNumber() && value.canConvertToInt()
                || (type == Long.class || type == long.class) && value.isIntegralNumber() && value.canConvertToLong();
        if (!valid) throw invalid("Invalid configuration value type for " + key);
    }

    private static String parameterClass(Class<?> type) {
        String suffix = type == boolean.class ? "Boolean" : type == int.class ? "Integer" : type == long.class ? "Long" : type.getSimpleName();
        return "org.ikasan.configurationService.model.ConfigurationParameter" + suffix + "Impl";
    }

    private static void applyGeneratedImplementations(ObjectNode legacy, ObjectNode topology, boolean validate) throws Exception {
        var model = org.ikasan.studio.core.io.ComponentIO.deserializeModuleInstanceString(legacy.toString(), "implementation names");
        Map<String, ObjectNode> flows = index((ArrayNode) topology.get("flows"), "name", "flows");
        for (var flow : model.getFlows()) {
            ObjectNode nativeFlow = flows.get(flow.getIdentity());
            Map<String, ObjectNode> components = index((ArrayNode) nativeFlow.get("flowElements"), "componentName", "components");
            for (var component : flow.getFlowElementsNoExternalEndPoints()) {
                var meta = component.getComponentMeta();
                if (!meta.isGeneratesUserImplementedClass() || meta.isUseImplementingClassInFactory()
                        || Boolean.FALSE.equals(component.getPropertyValue("requiresStub"))) continue;
                String className = component.getPropertyValueAsString("userImplementedClassName");
                if (className == null || className.isBlank()) continue;
                String actual = model.getApplicationPackageName() + "." + flow.getJavaPackageName() + "."
                        + org.ikasan.studio.core.StudioBuildUtils.substitutePlaceholderInPascalCase(model, flow, component, className);
                ObjectNode nativeComponent = components.get(component.getComponentName());
                if (nativeComponent == null) continue;
                if (validate && !actual.equals(nativeComponent.path("implementingClass").asText()))
                    throw invalid("Generated implementation disagrees with Studio class settings for " + component.getComponentName());
                nativeComponent.put("implementingClass", actual);
                if (nativeFlow.path("consumer").path("componentName").asText().equals(component.getComponentName()))
                    ((ObjectNode) nativeFlow.get("consumer")).put("implementingClass", actual);
            }
        }
    }

    private static String resolveId(String expression, String module, String flow, String component) {
        String value = expression.replace("__module", org.ikasan.studio.core.generation.JavaSourceNames.toVariableName(module))
                .replace("__flow", org.ikasan.studio.core.generation.JavaSourceNames.toVariableName(flow))
                .replace("__component", org.ikasan.studio.core.generation.JavaSourceNames.toVariableName(component));
        return org.ikasan.studio.core.StudioBuildUtils.toJavaIdentifier(value.replace("-", " "));
    }

    /** Same default identity algorithm as Ikasan 3/4 FlowBuilder for component configurations. */
    private static String identifier(String module, String flow, String component, String config) {
        String id = module + "_" + flow + "_" + component + "_" + config.hashCode() + "_C";
        if (id.length() > 255) id = module + "_" + flow + "_" + component.hashCode() + "_" + config.hashCode() + "_C";
        if (id.length() > 255) id = module + "_" + flow.hashCode() + "_" + component.hashCode() + "_" + config.hashCode() + "_C";
        if (id.length() > 255) id = module.hashCode() + "_" + flow.hashCode() + "_" + component.hashCode() + "_" + config.hashCode() + "_C";
        return id;
    }

    private static void knownFields(ObjectNode node, Set<String> allowed) throws IOException {
        for (String key : fieldNames(node)) if (!allowed.contains(key))
            throw invalid("Unknown document metadata field " + key + "; preserve it and use a compatible Studio version");
    }

    private static void validateTransitions(ObjectNode flow, Set<String> components) throws IOException {
        Map<String, List<String>> edges = new HashMap<>();
        Set<String> labels = new HashSet<>(), incoming = new HashSet<>();
        for (JsonNode edge : arrayOrEmpty(flow.get("transitions"), "transitions")) {
            String from = text(edge, "from"), to = text(edge, "to"), name = text(edge, "name");
            if (!components.contains(from) || !components.contains(to)) throw invalid("Transition refers to a missing component");
            if (!labels.add(from + "\u0000" + name) || !incoming.add(to)) throw invalid("Duplicate transitions or joining routes cannot be represented by Studio");
            if (to.equals(flow.path("consumer").path("componentName").asText())) throw invalid("A transition cannot return to the consumer");
            edges.computeIfAbsent(from, ignored -> new ArrayList<>()).add(to);
        }
        // Kahn's algorithm detects cycles without recursion on untrusted input.
        Map<String, Integer> degrees = new HashMap<>();
        components.forEach(c -> degrees.put(c, 0));
        edges.values().forEach(list -> list.forEach(c -> degrees.merge(c, 1, Integer::sum)));
        Deque<String> ready = new ArrayDeque<>();
        degrees.forEach((c, count) -> { if (count == 0) ready.add(c); });
        int visited = 0;
        while (!ready.isEmpty()) {
            String from = ready.removeFirst(); visited++;
            for (String to : edges.getOrDefault(from, List.of())) if (degrees.merge(to, -1, Integer::sum) == 0) ready.add(to);
        }
        if (visited != components.size()) throw invalid("Cyclic flow topology is unsupported");
    }

    private static void copy(JsonNode from, ObjectNode to, Collection<String> keys) {
        for (String key : keys) if (from.has(key)) to.set(key, from.get(key).deepCopy());
    }
    private static void move(ObjectNode from, ObjectNode to, Collection<String> keys) {
        for (String key : keys) { JsonNode value = from.remove(key); if (value != null) to.set(key, value); }
    }
    private static void rejectFields(ObjectNode node, Collection<String> fields, String location) throws IOException {
        for (String field : fields) if (node.has(field)) throw invalid("Duplicated authoritative field " + field + " in " + location);
    }
    private static ObjectNode object(JsonNode node, String location) throws IOException {
        if (!(node instanceof ObjectNode object)) throw invalid(location + " must be an object");
        return object;
    }
    private static ArrayNode requiredArray(JsonNode node, String location) throws IOException {
        if (!(node instanceof ArrayNode array)) throw invalid(location + " must be an array");
        return array;
    }
    private static ArrayNode arrayOrEmpty(JsonNode node, String location) throws IOException {
        return node == null || node.isNull() ? JSON.createArrayNode() : requiredArray(node, location);
    }
    private static String text(JsonNode node, String field) throws IOException {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) throw invalid("Missing text field " + field);
        return value.asText();
    }
    private static Map<String, ObjectNode> index(ArrayNode nodes, String key, String location) throws IOException {
        Map<String, ObjectNode> index = new LinkedHashMap<>();
        for (JsonNode node : nodes) if (index.putIfAbsent(text(node, key), object(node, location)) != null)
            throw invalid("Duplicate " + key + " in " + location);
        return index;
    }
    private static Set<String> fieldNames(ObjectNode node) {
        Set<String> names = new HashSet<>(); node.fieldNames().forEachRemaining(names::add); return names;
    }
    private static IOException invalid(String message) { return new IOException(message + ". No model files have been changed."); }
}
