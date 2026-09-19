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
    public record Prepared(Module draft, Set<String> affectedFlows, List<String> summary,
                           Map<FlowElement, String> originalNames, Set<Flow> originalFlows, boolean deletesContent) { }

    public static Prepared prepare(Map<String, Object> snapshot, JsonNode operations) throws Exception {
        if (!operations.isArray() || operations.isEmpty() || operations.size() > 100)
            throw new IllegalArgumentException("Provide between 1 and 100 operations.");
        Module draft = ComponentIO.validatePersistedModuleJson(StudioJson.newObjectMapper().writeValueAsString(snapshot), "AI snapshot", false);
        Map<FlowElement, String> originalNames = new IdentityHashMap<>();
        Set<Flow> originalFlows = Collections.newSetFromMap(new IdentityHashMap<>());
        originalFlows.addAll(draft.getFlows());
        boolean deletesContent = false;
        draft.getFlows().forEach(flow -> elements(flow).forEach(element -> originalNames.put(element, element.getIdentity())));
        Set<String> affected = new LinkedHashSet<>();
        List<String> summary = new ArrayList<>();
        for (JsonNode op : operations) {
            String type = text(op, "type");
            String flowName = text(op, "flow");
            deletesContent |= Set.of("deleteFlow", "deleteComponent", "replaceComponent").contains(type);
            Flow flow;
            if (type.equals("deleteFlow")) {
                fields(op, "type", "flow");
                flow = findFlow(draft, flowName);
                var harnesses = draft.getFlows().stream()
                        .filter(f -> f.getPropertyValueAsString("testHarnessOwner").startsWith(flowName + "/")).toList();
                draft.getFlows().remove(flow);
                draft.getFlows().removeAll(harnesses);
                harnesses.forEach(f -> summary.add("Delete associated test harness: " + f.getIdentity()));
                summary.add("Delete entire flow: " + flowName + " and all its components (developer-owned source files are retained)");
            } else if (type.equals("addFlow")) {
                fields(op, "type", "flow");
                // Only names this proposal introduces are constrained; existing flows are validated by lookup
                // and may legitimately carry names Studio's own UI allows (e.g. "Order-Flow").
                checkName(flowName);
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
                        addComponent(draft, flow, op, flow.getFlowRoute().getFlowElements().size());
                        summary.add("Add " + text(op, "key") + ": " + flowName + " / " + text(op, "name"));
                    }
                    case "deleteComponent" -> {
                        fields(op, "type", "flow", "component");
                        FlowElement element = findElement(flow, text(op, "component"));
                        removeComponent(flow, element);
                        summary.add("Delete component: " + flowName + " / " + element.getIdentity()
                                + " (developer-owned source files are retained)");
                    }
                    case "replaceComponent" -> {
                        fields(op, "type", "flow", "component", "key", "name", "properties");
                        FlowElement previous = findElement(flow, text(op, "component"));
                        var replacementMeta = ComponentLibrary.getIkasanComponentByKeyMandatory(draft.getVersion(), text(op, "key"));
                        if (previous.getComponentMeta().isConsumer() != replacementMeta.isConsumer())
                            fail("Replace a consumer with another consumer; replace body components with body components.");
                        int position = flow.getFlowRoute().getFlowElements().indexOf(previous);
                        removeComponent(flow, previous);
                        addComponent(draft, flow, op, Math.max(0, position));
                        summary.add("Replace " + flowName + " / " + previous.getIdentity() + " with "
                                + text(op, "name") + " (" + text(op, "key") + "; developer-owned source files are retained)");
                    }
                    case "setProperty" -> {
                        fields(op, "type", "flow", "component", "property", "value");
                        if (!op.has("value")) fail("setProperty requires value (null clears the value).");
                        FlowElement element = findElement(flow, text(op, "component"));
                        String property = text(op, "property");
                        var meta = element.getComponentMeta().getMetadata(property);
                        // A protected user-supplied class is a bean reference, not a request to regenerate its code.
                        boolean repairsUnresolvedClass = property.equals("userImplementedClassName")
                                && element.getPropertyValueAsString(property).startsWith("__fieldName:")
                                && op.path("value").isTextual()
                                && javax.lang.model.SourceVersion.isIdentifier(op.path("value").asText())
                                && !javax.lang.model.SourceVersion.isKeyword(op.path("value").asText());
                        if (meta != null && meta.isAffectsUserImplementedClass() && !repairsUnresolvedClass
                                && !(meta.isUserSuppliedClass() && meta.isProtectFromOverwrite()))
                            fail("Edit implementation class properties in Studio: " + property);
                        setProperty(element, property, op.get("value"));
                        summary.add("Set " + flowName + " / " + element.getIdentity() + " / " + property);
                    }
                    case "renameComponent" -> {
                        fields(op, "type", "flow", "component", "name");
                        FlowElement element = findElement(flow, text(op, "component"));
                        String name = text(op, "name");
                        checkName(name);
                        if (elements(flow).stream().anyMatch(other -> other != element && sameGeneratedName(other.getIdentity(), name)))
                            fail("Component already exists: " + name);
                        String previous = element.getIdentity();
                        element.setName(name);
                        summary.add("Rename " + flowName + " / " + previous + " to " + name);
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
            Flow flow = draft.getFlows().stream().filter(f -> f.getIdentity().equals(name)).findFirst().orElse(null);
            if (flow == null) continue;
            // Like manual Studio editing, proposals may leave a design in progress.
            // Completeness is a review warning; structural and property checks still apply below.
            String integrity = flow.getFlowIntegrityStatus();
            if (!integrity.isBlank()) summary.add("Flow " + name + " is incomplete: " + integrity
                    + " Complete the flow before running it.");
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
        return new Prepared(draft, Set.copyOf(affected), List.copyOf(summary), Collections.unmodifiableMap(originalNames),
                Collections.unmodifiableSet(originalFlows), deletesContent);
    }

    private static void removeComponent(Flow flow, FlowElement element) {
        if (flow.getConsumer() == element) flow.setConsumer(null);
        else flow.getFlowRoute().getFlowElements().remove(element);
    }

    private static void addComponent(Module draft, Flow flow, JsonNode op, int position) throws Exception {
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
            for (var property : op.get("properties").properties()) setProperty(element, property.getKey(), property.getValue());
        }
        element.defaultUnsetMandatoryProperties();
        StudioBuildUtils.substituteAllPlaceholderInPascalCase(draft, flow, element);
        if (meta.isConsumer()) flow.setConsumer(element);
        else flow.getFlowRoute().insertFlowElement(position, element);
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
        var oldFlows = new ArrayList<>(live.getFlows());
        List<Flow> newFlows = new ArrayList<>();
        for (Flow draftFlow : prepared.draft().getFlows()) {
            Flow original = prepared.originalFlows().contains(draftFlow)
                    ? live.getFlows().stream().filter(f -> f.getIdentity().equals(draftFlow.getIdentity())).findFirst().orElse(null) : null;
            newFlows.add(original == null ? draftFlow : original);
            if (!prepared.affectedFlows().contains(draftFlow.getIdentity())) continue;
            if (original == null) {
                continue;
            }
            Map<String, FlowElement> originals = new HashMap<>();
            elements(original).forEach(e -> originals.put(e.getIdentity(), e));
            Map<String, FlowElement> targets = new HashMap<>();
            for (FlowElement candidate : elements(draftFlow)) {
                FlowElement target = originals.get(prepared.originalNames().get(candidate));
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
                            if (previous != null && previous.getMeta().isUserSuppliedClass()
                                    && previous.getMeta().isProtectFromOverwrite() && previous.isOverwriteEnabled())
                                fail("Turn off source overwrite in Studio before changing the bean reference: " + key);
                            forward.add(() -> existing.setPropertyValue(key, after));
                            backward.add(() -> { if (previous == null) existing.removeProperty(key); else existing.setPropertyValue(key, before); });
                        }
                    });
                }
                targets.put(candidate.getIdentity(), target);
            }
            var oldConsumer = original.getConsumer();
            var newConsumer = draftFlow.getConsumer() == null ? null : targets.get(draftFlow.getConsumer().getIdentity());
            var oldBody = new ArrayList<>(original.getFlowRoute().getFlowElements());
            var newBody = draftFlow.getFlowRoute().getFlowElements().stream().map(e -> targets.get(e.getIdentity())).toList();
            forward.add(() -> { original.setConsumer(newConsumer); original.getFlowRoute().getFlowElements().clear(); original.getFlowRoute().getFlowElements().addAll(newBody); });
            backward.add(() -> { original.setConsumer(oldConsumer); original.getFlowRoute().getFlowElements().clear(); original.getFlowRoute().getFlowElements().addAll(oldBody); });
        }
        forward.add(() -> { live.getFlows().clear(); live.getFlows().addAll(newFlows); });
        backward.add(() -> { live.getFlows().clear(); live.getFlows().addAll(oldFlows); });
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
        if (!name.matches("[A-Za-z][A-Za-z0-9_ ]{0,79}")) fail("Invalid new name \"" + name + "\". Names must start with a letter and contain only letters, digits, spaces or underscores (maximum 80 characters). For numbered flows, use Flow01 rather than 01.");
    }
    private static boolean sameGeneratedName(String left, String right) {
        return StudioBuildUtils.toPascalCase(left).equals(StudioBuildUtils.toPascalCase(right));
    }
    private static void fail(String message) { throw new IllegalArgumentException(message); }
}
