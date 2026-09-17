package org.ikasan.studio.core.ai;

import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.*;

/** A detached value graph. Capture on the model's owning thread; encode/validate off that thread. */
public final class LiveModelSnapshot {
    private LiveModelSnapshot() { }

    public static Map<String, Object> capture(Module module) {
        Map<String, Object> result = properties(module);
        if (module.isWiretapManagementEnabled()) result.put("wiretapManagementEnabled", true);
        result.put("flows", module.getFlows().stream().map(LiveModelSnapshot::flow).toList());
        return result;
    }

    private static Map<String, Object> flow(Flow flow) {
        Map<String, Object> result = properties(flow);
        if (flow.getConsumer() != null) result.put("consumer", element(flow.getConsumer()));
        List<Map<String, Object>> elements = new ArrayList<>();
        List<Map<String, String>> transitions = new ArrayList<>();
        route(flow.getFlowRoute(), flow.getConsumer(), elements, transitions);
        result.put("flowElements", elements);
        result.put("transitions", transitions);
        if (flow.getExceptionResolver() != null) {
            var resolver = flow.getExceptionResolver();
            Map<String, Object> resolutions = properties(resolver);
            resolver.getIkasanExceptionResolutionMap().forEach((key, resolution) -> resolutions.put(key,
                    Map.of("exceptionsCaught", resolution.getExceptionsCaught(), "action", resolution.getTheAction(),
                            "actionProperties", properties(resolution))));
            result.put("exceptionResolver", resolutions);
        }
        return result;
    }

    private static void route(FlowRoute route, FlowElement previous, List<Map<String, Object>> elements,
                              List<Map<String, String>> transitions) {
        if (route == null) return;
        boolean first = true;
        for (var element : route.getFlowElements()) {
            if (element.getComponentMeta().isEndpoint() && !element.getComponentMeta().isInternalEndpoint()) continue;
            elements.add(element(element));
            if (previous != null) transitions.add(Map.of("from", previous.getIdentity(), "to", element.getIdentity(),
                    "name", first ? route.getRouteName() : "default"));
            previous = element;
            first = false;
        }
        for (var child : route.getChildRoutes()) route(child, previous, elements, transitions);
    }

    private static Map<String, Object> element(FlowElement element) {
        Map<String, Object> result = properties(element);
        var meta = element.getComponentMeta();
        result.put("componentType", meta.getComponentType());
        result.put("implementingClass", meta.getImplementingClass());
        if (meta.getAdditionalKey() != null) result.put("additionalKey", meta.getAdditionalKey());
        if (element.getDecorators() != null) result.put("decorators", element.getDecorators().stream().map(d -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("type", d.getType().name());
            value.put("name", d.getName());
            value.put("configurationId", d.getConfigurationId());
            value.put("configurable", d.isConfigurable());
            if (d.getTimeToLive() != null) value.put("timeToLive", d.getTimeToLive());
            return value;
        }).toList());
        return result;
    }

    private static Map<String, Object> properties(BasicElement element) {
        Map<String, Object> result = new TreeMap<>();
        element.getUnknownJsonProperties().forEach((key, value) -> result.put(key, value.deepCopy()));
        if (element.getComponentProperties() == null) return result;
        element.getComponentProperties().forEach((key, property) -> {
            Object value = property.getValue();
            if (value != null) result.put(key, value instanceof Number || value instanceof Boolean || value instanceof String
                    ? value : value.toString());
        });
        return result;
    }
}
