package org.ikasan.studio.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.persistence.json.StudioJson;

import java.util.*;

/** Validates an AI edit on an isolated model and prepares reversible edits retaining live object identity. */
public final class ModelProposal {
    private ModelProposal() { }
    public record Prepared(Module draft, Set<String> affectedFlows, List<String> summary) { }

    public static Prepared prepare(Map<String, Object> snapshot, JsonNode operations) throws Exception {
        if (!operations.isArray() || operations.isEmpty() || operations.size() > 100)
            throw new IllegalArgumentException("Provide between 1 and 100 operations.");
        Module draft = ComponentIO.validatePersistedModuleJson(StudioJson.newObjectMapper().writeValueAsString(snapshot), "AI snapshot", false);
        Set<String> affected = new LinkedHashSet<>();
        List<String> summary = new ArrayList<>();
        for (JsonNode op : operations) {
            String type = text(op, "type");
            String flowName = text(op, "flow");
            checkName(flowName);
            Flow flow;
            if (type.equals("addFlow")) {
                fields(op, "type", "flow");
                if (draft.getFlows().stream().anyMatch(f -> sameGeneratedName(f.getIdentity(), flowName))) fail("Flow already exists: " + flowName);
                flow = new Flow(draft.getVersion());
                flow.setName(flowName);
                draft.getFlows().add(flow);
                summary.add("Add flow: " + flowName);
            } else {
                flow = findFlow(draft, flowName);
                if (!flow.getPropertyValueAsString("testHarnessOwner").isBlank()) fail("Test harness flows cannot be edited by proposals.");
                if (!flow.getFlowRoute().getChildRoutes().isEmpty()) fail("Editing branched flows is not yet supported: " + flowName);
                switch (type) {
                    case "addComponent" -> {
                        fields(op, "type", "flow", "key", "name", "properties");
                        String name = text(op, "name");
                        checkName(name);
                        if (elements(flow).stream().anyMatch(e -> sameGeneratedName(e.getIdentity(), name))) fail("Component already exists: " + name);
                        var meta = ComponentLibrary.getIkasanComponentByKeyMandatory(draft.getVersion(), text(op, "key"));
                        if (meta.isModule() || meta.isFlow() || meta.isEndpoint() || meta.isRouter() || meta.isExceptionResolver())
                            fail("Choose a consumer or a linear flow component. Routers, endpoints and resolvers are not supported by this operation.");
                        String issue = flow.issueCausedByAdding(meta, flow.getFlowRoute()) + flow.getFlowRoute().issueCausedByAdding(meta);
                        if (!issue.isBlank()) fail(issue);
                        FlowElement element = FlowElementFactory.createFlowElement(draft.getVersion(), meta, flow, flow.getFlowRoute(), name);
                        if (op.has("properties")) {
                            if (!op.get("properties").isObject()) fail("properties must be an object");
                            for (var property : op.get("properties").properties()) {
                                setProperty(element, property.getKey(), property.getValue());
                            }
                        }
                        element.defaultUnsetMandatoryProperties();
                        StudioBuildUtils.substituteAllPlaceholderInPascalCase(draft, flow, element);
                        if (meta.isConsumer()) flow.setConsumer(element);
                        else flow.getFlowRoute().insertFlowElement(flow.getFlowRoute().getFlowElements().size(), element);
                        summary.add("Add " + text(op, "key") + ": " + flowName + " / " + name);
                    }
                    case "setProperty" -> {
                        fields(op, "type", "flow", "component", "property", "value");
                        if (!op.has("value")) fail("setProperty requires value (null clears the value).");
                        FlowElement element = findElement(flow, text(op, "component"));
                        String property = text(op, "property");
                        var meta = element.getComponentMeta().getMetadata(property);
                        if (meta != null && meta.isAffectsUserImplementedClass()) fail("Edit implementation class properties in Studio: " + property);
                        setProperty(element, property, op.get("value"));
                        summary.add("Set " + flowName + " / " + element.getIdentity() + " / " + property);
                    }
                    case "connect" -> {
                        fields(op, "type", "flow", "order");
                        JsonNode order = op.path("order");
                        List<FlowElement> all = elements(flow);
                        if (!order.isArray() || order.size() != all.size() || flow.getConsumer() == null)
                            fail("connect.order must list every component exactly once, starting with the consumer.");
                        List<FlowElement> ordered = new ArrayList<>();
                        Set<String> names = new HashSet<>();
                        for (JsonNode name : order) {
                            if (!name.isTextual() || !names.add(name.asText())) fail("Duplicate or invalid component in connect.order.");
                            ordered.add(findElement(flow, name.asText()));
                        }
                        if (ordered.get(0) != flow.getConsumer()) fail("The consumer must be first.");
                        flow.getFlowRoute().getFlowElements().clear();
                        flow.getFlowRoute().getFlowElements().addAll(ordered.subList(1, ordered.size()));
                        summary.add("Connect " + flowName + ": " + String.join(" → ", ordered.stream().map(FlowElement::getIdentity).toList()));
                    }
                    default -> fail("Unknown operation: " + type);
                }
            }
            affected.add(flowName);
        }
        for (String name : affected) {
            Flow flow = findFlow(draft, name);
            String integrity = flow.getFlowIntegrityStatus();
            if (!integrity.isBlank()) fail(name + ": " + integrity);
            List<FlowElement> body = flow.getFlowRoute().getFlowElements();
            for (int i = 0; i < body.size() - 1; i++) if (body.get(i).getComponentMeta().isProducer()) fail("A producer must be last in its flow.");
            for (FlowElement element : elements(flow)) {
                if (element.hasUnsetMandatoryProperties()) fail(name + " / " + element.getIdentity() + ": missing " + element.listUnsetMandatoryProperties());
                String recipeId = element.getPropertyValueAsString("conversionRecipeId");
                if (!recipeId.isBlank()) {
                    var recipe = element.getComponentMeta().getConversionRecipes().stream()
                            .filter(r -> recipeId.equals(r.getId())).findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Unknown conversion recipe: " + recipeId));
                    if (!recipe.matches(element.getPropertyValueAsString("fromType"), element.getPropertyValueAsString("toType")))
                        fail("Conversion recipe does not match fromType/toType: " + recipeId);
                }
                String warning = element.getUpstreamTypeMismatchWarning();
                if (warning != null && !warning.isBlank()) fail(warning);
            }
        }
        ComponentIO.toValidatedModuleJson(draft);
        return new Prepared(draft, Set.copyOf(affected), List.copyOf(summary));
    }

    private static void setProperty(FlowElement element, String key, JsonNode value) {
        var meta = element.getComponentMeta().getMetadata(key);
        if (meta == null || Set.of("componentName", "name", "version", "testHarnessOwner", "routeNames").contains(key)
                || meta.isIgnoreProperty()) fail("Unknown or structural property: " + key);
        if (value.isContainerNode()) fail("Property values must be scalar: " + key);
        Object converted = null;
        if (!value.isNull()) {
            Class<?> type = meta.getPropertyDataType();
            try {
                if (type == Boolean.class || type == boolean.class) {
                    if (!value.isBoolean()) fail("Expected a boolean for " + key);
                    converted = value.booleanValue();
                } else if (type != null && Number.class.isAssignableFrom(type)) {
                    if (Set.of(Integer.class, Long.class, Short.class, Byte.class).contains(type) && !value.asText().matches("[-+]?\\d+"))
                        fail("Expected an integer for " + key);
                    converted = StudioJson.newObjectMapper().convertValue(value, type);
                    if (!value.asText().matches("[-+]?\\d+(\\.\\d+)?")) fail("Expected a number for " + key);
                } else {
                    if (!value.isTextual()) fail("Expected a string for " + key);
                    converted = value.textValue();
                }
            } catch (IllegalArgumentException failure) { throw new IllegalArgumentException("Invalid value for " + key + ": " + failure.getMessage()); }
            if (meta.getChoices() != null && !meta.getChoices().isEmpty() && !meta.isChoicesEditable()
                    && !meta.getChoices().contains(String.valueOf(converted))) fail("Value is not an allowed choice for " + key);
            if (meta.getValidationPattern() != null && !meta.getValidationPattern().matcher(String.valueOf(converted)).matches())
                fail("Value does not match the validation rule for " + key);
        }
        element.setPropertyValue(key, converted);
    }

    /** Run on the model-owning thread after checking the snapshot. Does not change anything until apply(). */
    public static ChangeSet changes(Module live, Prepared prepared) {
        List<Runnable> forward = new ArrayList<>();
        List<Runnable> backward = new ArrayList<>();
        for (Flow draftFlow : prepared.draft().getFlows()) {
            if (!prepared.affectedFlows().contains(draftFlow.getIdentity())) continue;
            Flow original = live.getFlows().stream().filter(f -> f.getIdentity().equals(draftFlow.getIdentity())).findFirst().orElse(null);
            if (original == null) {
                forward.add(() -> live.getFlows().add(draftFlow));
                backward.add(() -> live.getFlows().removeIf(f -> f == draftFlow));
                continue;
            }
            Map<String, FlowElement> originals = new HashMap<>();
            elements(original).forEach(e -> originals.put(e.getIdentity(), e));
            Map<String, FlowElement> targets = new HashMap<>();
            for (FlowElement candidate : elements(draftFlow)) {
                FlowElement target = originals.get(candidate.getIdentity());
                if (target == null) {
                    target = candidate;
                    target.setContainingFlow(original);
                    target.setContainingFlowRoute(original.getFlowRoute());
                } else {
                    FlowElement existing = target;
                    candidate.getComponentProperties().forEach((key, property) -> {
                        var previous = existing.getComponentProperties().get(key);
                        Object before = previous == null ? null : previous.getValue();
                        Object after = property.getValue();
                        if (!Objects.equals(before, after)) {
                            forward.add(() -> existing.setPropertyValue(key, after));
                            backward.add(() -> { if (previous == null) existing.removeProperty(key); else existing.setPropertyValue(key, before); });
                        }
                    });
                }
                targets.put(candidate.getIdentity(), target);
            }
            var oldConsumer = original.getConsumer();
            var newConsumer = targets.get(draftFlow.getConsumer().getIdentity());
            var oldBody = new ArrayList<>(original.getFlowRoute().getFlowElements());
            var newBody = draftFlow.getFlowRoute().getFlowElements().stream().map(e -> targets.get(e.getIdentity())).toList();
            forward.add(() -> { original.setConsumer(newConsumer); original.getFlowRoute().getFlowElements().clear(); original.getFlowRoute().getFlowElements().addAll(newBody); });
            backward.add(() -> { original.setConsumer(oldConsumer); original.getFlowRoute().getFlowElements().clear(); original.getFlowRoute().getFlowElements().addAll(oldBody); });
        }
        return new ChangeSet(forward, backward);
    }

    public record ChangeSet(List<Runnable> forward, List<Runnable> backward) {
        public void apply() { forward.forEach(Runnable::run); }
        public void undo() { for (int i = backward.size() - 1; i >= 0; i--) backward.get(i).run(); }
    }

    private static List<FlowElement> elements(Flow flow) { return flow.ftlGetConsumerAndFlowElements().stream()
            .filter(e -> !e.getComponentMeta().isEndpoint()).toList(); }
    private static Flow findFlow(Module module, String name) { return module.getFlows().stream().filter(f -> f.getIdentity().equals(name))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown flow: " + name)); }
    private static FlowElement findElement(Flow flow, String name) { return elements(flow).stream().filter(e -> e.getIdentity().equals(name))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown component: " + name)); }
    private static String text(JsonNode object, String field) {
        if (!object.path(field).isTextual() || object.path(field).asText().isBlank()) fail("Required text field: " + field);
        return object.get(field).asText();
    }
    private static void fields(JsonNode node, String... allowed) {
        Set<String> keys = Set.of(allowed);
        node.fieldNames().forEachRemaining(key -> { if (!keys.contains(key)) fail("Unknown operation field: " + key); });
    }
    private static void checkName(String name) {
        if (!name.matches("[A-Za-z][A-Za-z0-9_ ]{0,79}")) fail("Names must start with a letter and contain only letters, digits, spaces or underscores (maximum 80 characters).");
    }
    private static boolean sameGeneratedName(String left, String right) {
        return StudioBuildUtils.toPascalCase(left).equals(StudioBuildUtils.toPascalCase(right));
    }
    private static void fail(String message) { throw new IllegalArgumentException(message); }
}
