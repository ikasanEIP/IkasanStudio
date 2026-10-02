package org.ikasan.studio.core.importer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.*;

/**
 * Adapts Ikasan's runtime topology and separate ConfigurationMetaData JSON to a Studio design.
 * Never loads classes named by the input. Original documents are retained as inert JSON;
 * only configuration fields explicitly declared by the selected meta-pack are applied.
 */
public final class IkasanRuntimeImport {
    public static final String SOURCE_FIELD = "ikasanRuntimeImport";
    private static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private static final int MAX_JSON_LENGTH = 8 * 1024 * 1024;

    private IkasanRuntimeImport() { }

    /** Result is a validated Studio document plus review messages, without configuration values. */
    public record Result(String studioJson, List<String> review) {
        public Result { review = List.copyOf(review); }
    }

    /** A runtime implementation can correspond to several design-time variants (for example scheduled/file consumers). */
    public record ComponentRef(String flow, String component) { }
    public record MappingChoice(ComponentRef component, List<String> variants) { }

    /** Returns variants requiring an explicit choice; configuration classes alone cannot identify message providers. */
    public static List<MappingChoice> mappingChoices(String moduleJson, String pack) throws StudioBuildException {
        try {
            JsonNode root = read(moduleJson);
            List<MappingChoice> choices = new ArrayList<>();
            Set<ComponentRef> seen = new HashSet<>();
            for (JsonNode flow : root.path("flows")) {
                List<JsonNode> elements = new ArrayList<>();
                elements.add(flow.path("consumer"));
                flow.path("flowElements").forEach(elements::add);
                for (JsonNode element : elements) {
                    ComponentRef ref = new ComponentRef(required(flow, "name"), required(element, "componentName"));
                    if (!seen.add(ref)) continue;
                    var candidates = exactCandidates(element, pack);
                    if (candidates.size() > 1) choices.add(new MappingChoice(ref, candidates.stream().map(ComponentMeta::getName).sorted().toList()));
                }
            }
            return List.copyOf(choices);
        } catch (StudioBuildException e) { throw e; }
        catch (Exception e) { throw new StudioBuildException("Cannot inspect Ikasan module metadata JSON.", e); }
    }

    public static Result convert(String moduleJson, String configurationJson, String pack, String packageName)
            throws StudioBuildException {
        return convert(moduleJson, configurationJson, pack, packageName, Map.of());
    }

    /**
     * Imports a module metadata object and an optional configuration object/array. The caller supplies
     * the design-time package and meta-pack because runtime exports do not reliably identify them.
     * Unsupported or ambiguous components and graphs fail before any project files are changed.
     */
    public static Result convert(String moduleJson, String configurationJson, String pack, String packageName,
                                 Map<ComponentRef, String> componentMappings) throws StudioBuildException {
        try {
            JsonNode raw = read(moduleJson);
            if (!(raw instanceof ObjectNode topology) || !topology.path("flows").isArray())
                throw problem("Expected an Ikasan module metadata object with a flows array.");
            if (topology.has("applicationPackageName"))
                throw problem("This is a Studio model. Use the Studio model import option.");
            if (packageName == null || !javax.lang.model.SourceVersion.isName(packageName))
                throw problem("Supply a valid application Java package.");
            var manifest = ComponentLibrary.getMetaPackManifest(pack);
            String runtimeVersion = topology.path("ikasanVersion").asText("");
            if (!runtimeVersion.isBlank() && !runtimeVersion.equals(manifest.ikasanVersion()))
                throw problem("The export uses Ikasan " + runtimeVersion + "; select its matching meta-pack. Import before migrating.");
            JsonNode configuration = configurationJson == null || configurationJson.isBlank()
                    ? JSON.createArrayNode() : read(configurationJson);
            Map<String, JsonNode> configurations = configurations(configuration);
            List<String> review = new ArrayList<>();
            review.add("Runtime JSON does not include Java implementations, dependencies, Spring wiring or all builder settings. Review these before building or running.");
            review.add("Project ports and build settings use Studio defaults. Module, flow and invoker configuration is retained for reference, not automatically applied.");
            review.add("Flows are imported MANUAL. Original topology and configuration are retained in model.json under " + SOURCE_FIELD + ". They may contain credentials; review before sharing.");
            if (runtimeVersion.isBlank()) review.add("The export has no ikasanVersion; verify the selected meta-pack against the original application.");
            Module base = new Module(pack);
            base.setName(required(topology, "name"));
            base.setApplicationPackageName(packageName);
            base.defaultUnsetMandatoryProperties();
            ObjectNode design = (ObjectNode) JSON.readTree(ComponentIO.toJson(base));
            design.put("flowStartupType", "MANUAL");
            if (topology.hasNonNull("description")) design.set("description", topology.get("description"));
            // Runtime module 'version' is the application's version, not Studio's meta-pack ID.
            ObjectNode source = design.putObject(SOURCE_FIELD);
            source.set("module", topology.deepCopy());
            source.set("configuration", configuration.deepCopy());
            ArrayNode flows = design.putArray("flows");
            if (topology.path("flows").size() > 1_000) throw problem("Import at most 1,000 flows in one module.");
            Set<String> names = new HashSet<>();
            Set<String> usedConfigurations = new HashSet<>();
            for (JsonNode flow : topology.path("flows")) {
                String name = required(flow, "name");
                if (!names.add(name)) throw problem("Duplicate flow name: " + name);
                flows.add(flow(flow, pack, configurations, usedConfigurations, review, componentMappings));
            }
            for (String id : configurations.keySet()) if (!usedConfigurations.contains(id))
                review.add("Configuration " + id + ": retained only; no supported component mapping applied (module, flow, invoker or unmatched resource).");
            source.set("review", JSON.valueToTree(review));
            String result = JSON.writerWithDefaultPrettyPrinter().writeValueAsString(design);
            Module validated = ComponentIO.validatePersistedModuleJson(result, "Ikasan runtime import", false);
            // Prove that Studio's tree representation has not lost or rewired runtime transitions.
            JsonNode roundTrip = JSON.readTree(ComponentIO.toValidatedModuleJson(validated));
            for (int i = 0; i < flows.size(); i++) {
                if (!edges(flows.get(i)).equals(edges(roundTrip.path("flows").get(i))))
                    throw problem("Flow " + flows.get(i).path("name").asText() + ": graph cannot be represented without changing transitions.");
            }
            return new Result(result, review);
        } catch (StudioBuildException e) {
            throw e;
        } catch (Exception e) {
            // Do not expose JSON excerpts or configuration values through parser exception text.
            throw new StudioBuildException("Cannot import Ikasan JSON. Check the document structure and selected meta-pack. No files were changed.", e);
        }
    }

    private static ObjectNode flow(JsonNode input, String pack, Map<String, JsonNode> configs,
                                   Set<String> used, List<String> review, Map<ComponentRef, String> mappings) throws Exception {
        String flowName = required(input, "name");
        if (!input.path("consumer").isObject() || !input.path("flowElements").isArray()
                || !input.path("transitions").isArray()) throw problem("Flow " + flowName + ": consumer, flowElements and transitions are required.");
        LinkedHashMap<String, JsonNode> components = new LinkedHashMap<>();
        String consumer = required(input.get("consumer"), "componentName");
        components.put(consumer, input.get("consumer"));
        for (JsonNode element : input.path("flowElements")) {
            String name = required(element, "componentName");
            if (name.equals(consumer) && element.equals(input.get("consumer"))) continue;
            if (components.putIfAbsent(name, element) != null) throw problem("Flow " + flowName + ": duplicate component " + name);
        }
        if (components.size() > 500) throw problem("Flow " + flowName + ": import at most 500 components per flow.");
        Map<String, List<JsonNode>> outgoing = new LinkedHashMap<>();
        Map<String, Integer> incoming = new HashMap<>();
        Set<String> edgeKeys = new HashSet<>();
        for (JsonNode edge : input.path("transitions")) {
            String from = required(edge, "from"), to = required(edge, "to"), label = required(edge, "name");
            if (!components.containsKey(from) || !components.containsKey(to)) throw problem("Flow " + flowName + ": transition refers to a missing component.");
            if (!edgeKeys.add(from + "\u0000" + label) || incoming.merge(to, 1, Integer::sum) > 1 || to.equals(consumer))
                throw problem("Flow " + flowName + ": duplicate routes, joins or cycles require an explicit Studio mapping.");
            outgoing.computeIfAbsent(from, ignored -> new ArrayList<>()).add(edge);
        }
        Set<String> reached = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        pending.add(consumer);
        while (!pending.isEmpty()) {
            String node = pending.removeFirst();
            if (!reached.add(node)) throw problem("Flow " + flowName + ": cyclic topology is unsupported.");
            for (JsonNode edge : outgoing.getOrDefault(node, List.of())) pending.add(required(edge, "to"));
        }
        if (reached.size() != components.size()) throw problem("Flow " + flowName + ": disconnected components are not imported silently.");
        ObjectNode result = JSON.createObjectNode().put("name", flowName).put("flowStartupType", "MANUAL");
        if (input.hasNonNull("configurationId")) result.set("configurationId", input.get("configurationId"));
        result.set("transitions", input.get("transitions").deepCopy());
        ArrayNode elements = result.putArray("flowElements");
        for (var entry : components.entrySet()) {
            JsonNode raw = entry.getValue();
            ComponentRef ref = new ComponentRef(flowName, entry.getKey());
            ComponentMeta meta = resolve(raw, pack, mappings.get(ref));
            if (mappings.containsKey(ref)) review.add(flowName + " → " + entry.getKey() + ": selected variant " + meta.getName() + ".");
            if (entry.getKey().equals(consumer) != meta.isConsumer()) throw problem("Flow " + flowName + ": invalid consumer placement.");
            List<JsonNode> routes = outgoing.getOrDefault(entry.getKey(), List.of());
            if (routes.size() > 1 && !meta.isRouter()) throw problem("Flow " + flowName + ": branching component is not a supported router.");
            ObjectNode target = JSON.createObjectNode();
            target.put("componentName", entry.getKey());
            target.put("componentType", meta.getComponentType());
            target.put("implementingClass", meta.getImplementingClass());
            if (meta.getAdditionalKey() != null) target.put("additionalKey", meta.getAdditionalKey());
            for (String key : List.of("description", "configurationId"))
                if (raw.hasNonNull(key)) target.set(key, raw.get(key));
            if (meta.isGeneratesUserImplementedClass()) {
                target.put("userImplementedClassName", required(raw, "implementingClass"));
                if (!meta.getAllowableProperties().containsKey("requiresStub"))
                    throw problem("Component " + entry.getKey() + ": the pack cannot preserve this existing implementation without generating a replacement.");
                target.put("requiresStub", false);
                for (String typeProperty : List.of("fromType", "toType")) {
                    if (meta.getAllowableProperties().containsKey(typeProperty)) target.put(typeProperty, "java.lang.Object");
                }
                review.add(flowName + " → " + entry.getKey() + ": supply existing implementation " + required(raw, "implementingClass") + " and its Spring bean/wiring; no replacement stub requested.");
            }
            if (meta.isRouter()) {
                if (routes.stream().anyMatch(e -> e.path("name").asText().contains(",")))
                    throw problem("Flow " + flowName + ": route names containing commas cannot be represented by this pack.");
                target.put("routeNames", String.join(",", routes.stream().map(e -> e.path("name").asText()).toList()));
            }
            if (raw.path("decorators").isArray() && !raw.path("decorators").isEmpty())
                review.add(flowName + " → " + entry.getKey() + ": decorators retained in original metadata; configure equivalent Studio decorators manually.");
            String id = raw.path("configurationId").asText("");
            if (!id.isBlank()) {
                JsonNode config = configs.get(id);
                if (config == null) review.add(flowName + " → " + entry.getKey() + ": no configuration supplied for " + id + ".");
                else {
                    used.add(id);
                    applyConfiguration(target, config, meta, flowName, review);
                }
            }
            if (entry.getKey().equals(consumer)) result.set("consumer", target); else elements.add(target);
        }
        return result;
    }

    private static List<ComponentMeta> exactCandidates(JsonNode raw, String pack) throws StudioBuildException {
        String type = required(raw, "componentType"), implementation = required(raw, "implementingClass");
        return ComponentLibrary.getPaletteComponentList(pack).stream()
                .filter(m -> type.equals(m.getComponentType()) && implementation.equals(m.getImplementingClass())).toList();
    }

    private static ComponentMeta resolve(JsonNode raw, String pack, String selectedVariant) throws StudioBuildException {
        String type = required(raw, "componentType"), implementation = required(raw, "implementingClass");
        if (!javax.lang.model.SourceVersion.isName(implementation))
            throw problem("Component " + required(raw, "componentName") + ": implementingClass must be a Java class name.");
        var candidates = exactCandidates(raw, pack);
        if (selectedVariant != null) {
            return candidates.stream().filter(m -> selectedVariant.equals(m.getName())).findFirst()
                    .orElseThrow(() -> problem("Selected variant does not match runtime component " + raw.path("componentName").asText()));
        }
        if (candidates.size() == 1) return candidates.get(0);
        if (candidates.size() > 1) throw problem("Component " + required(raw, "componentName") + ": select a variant explicitly: "
                + String.join(", ", candidates.stream().map(ComponentMeta::getName).toList())
                + ". Runtime JSON does not identify all message-provider wiring.");
        candidates = ComponentLibrary.getPaletteComponentList(pack).stream().filter(m -> type.equals(m.getComponentType())
                && m.isGeneratesUserImplementedClass() && (m.isGeneric() || m.getAdditionalKey() == null)).toList();
        if (candidates.size() > 1) {
            var genericContracts = candidates.stream().filter(m -> m.isGeneric()
                    || (type + ".Custom").equals(m.getImplementingClass())).toList();
            if (genericContracts.size() == 1) return genericContracts.get(0);
        }
        if (candidates.size() != 1) throw problem("Component " + required(raw, "componentName") + ": no unambiguous mapping for " + implementation + " in " + pack + ".");
        return candidates.get(0);
    }

    private static void applyConfiguration(ObjectNode target, JsonNode config, ComponentMeta meta,
                                           String flowName, List<String> review) throws StudioBuildException {
        var configurationProperty = meta.getAllowableProperties().get("configuration");
        String configurationClass = meta.getRuntimeConfigurationClass();
        if (configurationClass == null && configurationProperty != null) configurationClass = configurationProperty.getUsageDataType();
        boolean matchingClass = config.path("implementingClass").asText().equals(configurationClass);
        for (JsonNode parameter : config.path("parameters")) {
            String name = required(parameter, "name");
            var property = meta.getAllowableProperties().get(name);
            JsonNode value = parameter.get("value");
            if (matchingClass && meta.getRuntimeConfigurationProperties().contains(name) && property != null
                    && value != null && !value.isNull() && compatible(value, property.getPropertyDataType())) {
                target.set(name, value.deepCopy());
                review.add(flowName + " → " + target.path("componentName").asText() + ": applied configuration field " + name + ".");
            } else review.add(flowName + " → " + target.path("componentName").asText() + ": retained configuration field " + name + " without applying it; review original configuration.");
        }
    }

    private static boolean compatible(JsonNode value, Class<?> type) {
        if (org.ikasan.studio.core.model.StringCollectionValues.supports(type))
            return org.ikasan.studio.core.model.StringCollectionValues.compatible(type, value);
        if (type == String.class) return value.isTextual();
        if (type == Boolean.class || type == boolean.class) return value.isBoolean();
        if (type == Integer.class || type == int.class) return value.isIntegralNumber() && value.canConvertToInt();
        if (type == Long.class || type == long.class) return value.isIntegralNumber() && value.canConvertToLong();
        return false;
    }

    private static Map<String, JsonNode> configurations(JsonNode json) throws StudioBuildException {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        Iterable<JsonNode> entries = json.isArray() ? json : List.of(json);
        for (JsonNode entry : entries) {
            String id = required(entry, "configurationId");
            if (!entry.path("parameters").isArray()) throw problem("Configuration " + id + ": parameters must be an array.");
            Set<String> names = new HashSet<>();
            for (JsonNode parameter : entry.get("parameters")) {
                if (!names.add(required(parameter, "name")) || !parameter.has("value"))
                    throw problem("Configuration " + id + ": duplicate parameter or missing value.");
            }
            if (result.putIfAbsent(id, entry) != null) throw problem("Duplicate configurationId: " + id);
        }
        return result;
    }

    private static Set<JsonNode> edges(JsonNode flow) {
        Set<JsonNode> result = new HashSet<>();
        for (JsonNode edge : flow.path("transitions")) result.add(edge);
        return result;
    }

    private static JsonNode read(String input) throws Exception {
        if (input == null || input.length() > MAX_JSON_LENGTH) throw problem("Each import document must be at most 8 MiB of text.");
        if (input.startsWith("\uFEFF")) input = input.substring(1);
        try (JsonParser parser = JSON.createParser(input)) {
            JsonNode value = JSON.readTree(parser);
            if (value == null || parser.nextToken() != null) throw problem("Supply one JSON document per input.");
            return value;
        }
    }

    private static String required(JsonNode node, String field) throws StudioBuildException {
        if (node == null || !node.path(field).isTextual() || node.path(field).asText().isBlank())
            throw problem("Missing or invalid text field: " + field);
        return node.path(field).asText();
    }

    private static StudioBuildException problem(String message) { return new StudioBuildException(message); }
}
