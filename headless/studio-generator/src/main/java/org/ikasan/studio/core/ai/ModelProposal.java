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
                switch (type) {
                    case "setFlowProperty" -> {
                        fields(op, "type", "flow", "property", "value");
                        if (!op.has("value")) fail("setFlowProperty requires value (null clears the value).");
                        String property = text(op, "property");
                        var meta = flow.getComponentMeta().getMetadata(property);
                        if (meta != null && (meta.isAffectsUserImplementedClass() || meta.isUserSuppliedClass()))
                            fail("Edit implementation class properties in Studio: " + property);
                        setProperty(flow, property, op.get("value"));
                        summary.add("Set flow " + flowName + " / " + property);
                    }
                    case "addComponent" -> {
                        fields(op, "type", "flow", "key", "name", "properties", "route");
                        FlowRoute route = findRoute(flow, op.path("route"));
                        addComponent(draft, flow, route, op, route.getFlowElements().size());
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
                        var replacementMeta = componentMeta(draft, text(op, "key"));
                        if (previous.getComponentMeta().isConsumer() != replacementMeta.isConsumer())
                            fail("Replace a consumer with another consumer; replace body components with body components.");
                        FlowRoute route = previous.getContainingFlowRoute();
                        if (route == null) route = flow.getFlowRoute();
                        int position = route.getFlowElements().indexOf(previous);
                        removeComponent(flow, previous);
                        addComponent(draft, flow, route, op, Math.max(0, position));
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
                    case "configureRoutes" -> {
                        fields(op, "type", "flow", "component", "names");
                        FlowElement router = findElement(flow, text(op, "component"));
                        if (!router.getComponentMeta().isRouter()) fail("configureRoutes requires a router.");
                        JsonNode names = op.path("names");
                        if (!names.isArray() || names.size() < 2 || names.size() > 32) fail("Provide 2 to 32 route names.");
                        List<String> desired = new ArrayList<>();
                        for (JsonNode name : names) {
                            if (!name.isTextual()) fail("Route names must be strings.");
                            if (!name.asText().matches("[A-Za-z][A-Za-z0-9]*")) fail("Route names must start with a letter and contain only letters and digits.");
                            if (desired.stream().anyMatch(n -> sameGeneratedName(n, name.asText()))) fail("Duplicate route name.");
                            desired.add(name.asText());
                        }
                        FlowRoute parent = router.getContainingFlowRoute();
                        for (FlowRoute child : parent.getChildRoutes()) {
                            if (!desired.contains(child.getRouteName()) && (!child.getChildRoutes().isEmpty()
                                    || child.getFlowElements().stream().anyMatch(e -> !e.getComponentMeta().isEndpoint())))
                                fail("Cannot remove or rename populated route: " + child.getRouteName());
                        }
                        router.setPropertyValue("routeNames", desired);
                        parent.syncChildRoutesForRouter(draft.getVersion(), router);
                        summary.add("Configure routes for " + flowName + " / " + router.getIdentity() + ": " + desired);
                    }
                    case "setExceptionResolution" -> {
                        fields(op, "type", "flow", "exception", "action", "properties");
                        setExceptionResolution(draft, flow, op);
                        summary.add("Resolve " + text(op, "exception") + " in " + flowName + " with " + text(op, "action"));
                    }
                    case "connect" -> {
                        fields(op, "type", "flow", "order", "route");
                        FlowRoute route = findRoute(flow, op.path("route"));
                        List<FlowElement> all = new ArrayList<>(route.getFlowElements().stream()
                                .filter(e -> !e.getComponentMeta().isEndpoint()).toList());
                        boolean root = route == flow.getFlowRoute();
                        if (root && flow.getConsumer() != null) all.add(0, flow.getConsumer());
                        JsonNode order = op.path("order");
                        if (!order.isArray() || order.size() != all.size() || (root && flow.getConsumer() == null))
                            fail("connect.order must list every component in the selected route exactly once, consumer first for the root.");
                        List<FlowElement> ordered = new ArrayList<>();
                        Set<String> names = new HashSet<>();
                        for (JsonNode name : order) {
                            if (!name.isTextual() || !names.add(name.asText())) fail("Duplicate or invalid component in connect.order.");
                            FlowElement element = findElement(flow, name.asText());
                            if (!all.contains(element)) fail("Component is not in the selected route: " + name.asText());
                            ordered.add(element);
                        }
                        if (root && ordered.get(0) != flow.getConsumer()) fail("The consumer must be first.");
                        route.getFlowElements().removeIf(e -> !e.getComponentMeta().isEndpoint());
                        route.getFlowElements().addAll(root ? ordered.subList(1, ordered.size()) : ordered);
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
            validateRoute(flow.getFlowRoute());
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

    /** Resolve a component key, or fail with a message the AI can act on (the raw library error names nothing). */
    private static org.ikasan.studio.core.metapack.model.ComponentMeta componentMeta(Module draft, String key) throws Exception {
        var meta = ComponentLibrary.getIkasanComponentByKey(draft.getVersion(), key);
        if (meta != null) return meta;
        var all = ComponentLibrary.getIkasanComponents(draft.getVersion());
        String sameIgnoringCase = all.keySet().stream().filter(k -> k.equalsIgnoreCase(key)).findFirst().orElse(null);
        String valid = all.entrySet().stream()
                .filter(e -> !(e.getValue().isModule() || e.getValue().isFlow() || e.getValue().isEndpoint() || e.getValue().isExceptionResolver()))
                .map(Map.Entry::getKey).sorted().collect(java.util.stream.Collectors.joining(", "));
        throw new IllegalArgumentException("Unknown component key '" + key + "'. Keys are case-sensitive and exact."
                + (sameIgnoringCase == null ? "" : " Did you mean '" + sameIgnoringCase + "'?")
                + " Valid keys: " + valid + ". See studio_catalogue for details.");
    }

    private static void removeComponent(Flow flow, FlowElement element) {
        if (flow.getConsumer() == element) flow.setConsumer(null);
        else {
            FlowRoute route = element.getContainingFlowRoute();
            if (element.getComponentMeta().isRouter() && route.getChildRoutes().stream().anyMatch(r ->
                    !r.getChildRoutes().isEmpty() || r.getFlowElements().stream().anyMatch(e -> !e.getComponentMeta().isEndpoint())))
                fail("Remove branch components before deleting or replacing their router.");
            route.removeFlowElement(element);
        }
    }

    private static void addComponent(Module draft, Flow flow, FlowRoute route, JsonNode op, int position) throws Exception {
        String name = text(op, "name");
        checkName(name);
        if (elements(flow).stream().anyMatch(e -> sameGeneratedName(e.getIdentity(), name))) fail("Component already exists: " + name);
        var meta = componentMeta(draft, text(op, "key"));
        if (meta.isModule() || meta.isFlow() || meta.isEndpoint() || meta.isExceptionResolver())
            fail("Choose a consumer, processor or router. Use setExceptionResolution for exception policies; endpoints are not standalone components.");
        if (meta.isConsumer() && route != flow.getFlowRoute()) fail("A consumer belongs only in the root route.");
        if (meta.isRouter() && route.hasProducer()) fail("Remove or move the terminal producer before adding a router.");
        String issue = flow.issueCausedByAdding(meta, route) + route.issueCausedByAdding(meta);
        if (!issue.isBlank()) fail(issue);
        FlowElement element = FlowElementFactory.createFlowElement(draft.getVersion(), meta, flow, route, name);
        if (op.has("properties")) {
            if (!op.get("properties").isObject()) fail("properties must be an object");
            for (var property : op.get("properties").properties()) setProperty(element, property.getKey(), property.getValue());
        }
        element.defaultUnsetMandatoryProperties();
        StudioBuildUtils.substituteAllPlaceholderInPascalCase(draft, flow, element);
        if (meta.isConsumer()) flow.setConsumer(element);
        else {
            if (!meta.isRouter() && !meta.isProducer()) {
                for (int i = 0; i < route.getFlowElements().size(); i++)
                    if (route.getFlowElements().get(i).getComponentMeta().isRouter()) position = Math.min(position, i);
            }
            route.insertFlowElement(position, element);
            if (meta.isRouter()) route.syncChildRoutesForRouter(draft.getVersion(), element);
        }
    }

    private static void setProperty(BasicElement element, String key, JsonNode value) {
        var meta = element.getComponentMeta().getMetadata(key);
        if (meta == null || Set.of("componentName", "name", "version", "testHarnessOwner", "routeNames").contains(key)
                || meta.isIgnoreProperty()) fail("Unknown or structural property: " + key);
        element.setPropertyValue(key, propertyValue(meta, key, value));
    }

    private static Object propertyValue(org.ikasan.studio.core.metapack.model.ComponentPropertyMeta meta, String key, JsonNode value) {
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
        return converted;
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
            draftFlow.getComponentProperties().forEach((key, property) -> {
                var previous = original.getComponentProperties().get(key);
                Object before = previous == null ? null : previous.getValue();
                Object after = property.getValue();
                if (!Objects.equals(before, after)) {
                    forward.add(() -> original.setPropertyValue(key, after));
                    backward.add(() -> { if (previous == null) original.removeProperty(key); else original.setPropertyValue(key, before); });
                }
            });
            Map<String, FlowElement> originals = new HashMap<>();
            elements(original).forEach(e -> originals.put(e.getIdentity(), e));
            Map<String, FlowElement> targets = new HashMap<>();
            for (FlowElement candidate : elements(draftFlow)) {
                FlowElement target = originals.get(prepared.originalNames().get(candidate));
                if (target == null) {
                    target = candidate;
                    target.setContainingFlow(original);

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
            reconcileRoute(original.getFlowRoute(), draftFlow.getFlowRoute(), original, targets, forward, backward);
            forward.add(() -> original.setConsumer(newConsumer));
            backward.add(() -> original.setConsumer(oldConsumer));
            var resolver = original.getExceptionResolver();
            var proposedResolver = draftFlow.getExceptionResolver();
            if (resolver == null) {
                if (proposedResolver != null) {
                    proposedResolver.setContainingFlow(original);
                    forward.add(() -> original.setExceptionResolver(proposedResolver));
                    backward.add(() -> original.setExceptionResolver(null));
                }
            } else if (proposedResolver != null) {
                var oldRules = resolver.getIkasanExceptionResolutionMap();
                var newRules = new LinkedHashMap<>(proposedResolver.getIkasanExceptionResolutionMap());
                forward.add(() -> resolver.setIkasanExceptionResolutionMap(newRules));
                backward.add(() -> resolver.setIkasanExceptionResolutionMap(oldRules));
            }

        }
        forward.add(() -> { live.getFlows().clear(); live.getFlows().addAll(newFlows); });
        backward.add(() -> { live.getFlows().clear(); live.getFlows().addAll(oldFlows); });
        return new ChangeSet(forward, backward);
    }

    private static void reconcileRoute(FlowRoute target, FlowRoute candidate, Flow flow,
                                       Map<String, FlowElement> targets, List<Runnable> forward, List<Runnable> backward) {
        var oldBody = new ArrayList<>(target.getFlowElements());
        var oldChildren = new ArrayList<>(target.getChildRoutes());
        List<FlowElement> newBody = new ArrayList<>();
        for (FlowElement element : candidate.getFlowElements()) {
            FlowElement mapped = element.getComponentMeta().isEndpoint()
                    ? oldBody.stream().filter(e -> e.getComponentMeta().isEndpoint() && e.getIdentity().equals(element.getIdentity()))
                        .findFirst().orElse(element)
                    : targets.get(element.getIdentity());
            newBody.add(mapped);
        }
        List<FlowRoute> newChildren = new ArrayList<>();
        for (FlowRoute child : candidate.getChildRoutes()) {
            FlowRoute existing = oldChildren.stream().filter(r -> r.getRouteName().equals(child.getRouteName())).findFirst().orElse(null);
            if (existing == null) {
                try { existing = FlowRoute.flowRouteBuilder().flow(flow).routeName(child.getRouteName()).build(); }
                catch (Exception failure) { throw new IllegalArgumentException(failure); }
            }
            reconcileRoute(existing, child, flow, targets, forward, backward);
            newChildren.add(existing);
        }
        forward.add(() -> {
            target.getFlowElements().clear(); target.getFlowElements().addAll(newBody);
            newBody.forEach(e -> { e.setContainingFlow(flow); e.setContainingFlowRoute(target); });
            target.getChildRoutes().clear(); target.getChildRoutes().addAll(newChildren);
        });
        backward.add(() -> {
            target.getFlowElements().clear(); target.getFlowElements().addAll(oldBody);
            oldBody.forEach(e -> { e.setContainingFlow(flow); e.setContainingFlowRoute(target); });
            target.getChildRoutes().clear(); target.getChildRoutes().addAll(oldChildren);
        });
    }

    private static FlowRoute findRoute(Flow flow, JsonNode path) {
        FlowRoute route = flow.getFlowRoute();
        if (path.isMissingNode()) return route;
        if (!path.isArray() || path.size() > 16) { fail("route must be an array of branch names (maximum depth 16)."); }
        for (JsonNode name : path) {
            if (!name.isTextual()) fail("route must contain branch names.");
            route = route.getChildRoutes().stream().filter(r -> r.getRouteName().equals(name.asText())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown route: " + name.asText()));
        }
        return route;
    }

    private static void validateRoute(FlowRoute route) {
        List<FlowElement> body = route.getFlowElements().stream().filter(e -> !e.getComponentMeta().isEndpoint()).toList();
        for (int i = 0; i < body.size() - 1; i++)
            if (body.get(i).getComponentMeta().isProducer() || body.get(i).getComponentMeta().isRouter())
                fail("A producer or router must be last in its route.");
        if (!route.getChildRoutes().isEmpty() && (body.isEmpty() || !body.get(body.size()-1).getComponentMeta().isRouter()))
            fail("Child routes require a terminal router.");
        route.getChildRoutes().forEach(ModelProposal::validateRoute);
    }

    private static void setExceptionResolution(Module draft, Flow flow, JsonNode op) throws Exception {
        String exception = text(op, "exception");
        if (exception.endsWith(".class")) exception = exception.substring(0, exception.length()-6);
        if (!javax.lang.model.SourceVersion.isName(exception) || !exception.contains(".")) fail("Use a fully qualified exception class name.");
        exception += ".class";
        var meta = ComponentLibrary.getExceptionResolverMetaMandatory(draft.getVersion());
        var action = meta.getExceptionActionWithName(text(op, "action"));
        if (action == null) fail("Unknown exception action: " + text(op, "action"));
        JsonNode values = op.path("properties");
        if (!values.isMissingNode() && !values.isObject()) fail("Exception properties must be an object.");
        var properties = new LinkedHashMap<String, ComponentProperty>();
        var allowed = action.getActionProperties();
        for (var entry : values.properties()) if (!allowed.containsKey(entry.getKey())) fail("Unknown exception action property: " + entry.getKey());
        for (var entry : allowed.entrySet()) {
            var propertyMeta = entry.getValue();
            JsonNode value = values.get(entry.getKey());
            if (value == null && propertyMeta.getDefaultValue() != null) value = StudioJson.newObjectMapper().valueToTree(propertyMeta.getDefaultValue());
            if (value == null || value.isNull()) {
                if (propertyMeta.isMandatory()) fail("Missing exception action property: " + entry.getKey());
                continue;
            }
            Object converted = propertyValue(propertyMeta, entry.getKey(), value);
            if (converted instanceof Number number && number.longValue() < 0) fail("Exception retry values cannot be negative.");
            if (entry.getKey().equals("cronExpression") && !String.valueOf(converted).matches("[A-Za-z0-9*?,/#LW-]+(?: [A-Za-z0-9*?,/#LW-]+){5,6}"))
                fail("Invalid exception retry cron expression.");
            properties.put(entry.getKey(), new ComponentProperty(propertyMeta, converted));
        }
        var resolver = flow.getExceptionResolver();
        if (resolver == null) { resolver = new ExceptionResolver(draft.getVersion(), flow); flow.setExceptionResolver(resolver); }
        var rules = new LinkedHashMap<>(resolver.getIkasanExceptionResolutionMap());
        rules.put(exception, ExceptionResolution.exceptionResolutionBuilder().metapackVersion(draft.getVersion())
                .exceptionsCaught(exception).theAction(action.getActionName()).componentProperties(properties).build());
        resolver.setIkasanExceptionResolutionMap(rules);
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
