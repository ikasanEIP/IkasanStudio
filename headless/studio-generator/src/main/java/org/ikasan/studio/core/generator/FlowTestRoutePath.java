package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.model.ikasan.instance.FlowRoute;
import java.util.*;

/** Structural route examples; never infers or executes business routing decisions. */
final class FlowTestRoutePath {
    private FlowTestRoutePath() { }

    static Map<String, Object> describe(FlowRoute route, int[] sequence) {
        List<Map<String, String>> steps = new ArrayList<>();
        String routerMethod = null;
        for (var element : route.getFlowElements()) {
            if ((element.getComponentMeta().isDebug() || element.getComponentMeta().isInternalEndpoint())) continue;
            if (routerMethod != null) return null; // Routers must terminate their owning route.
            String type = element.getComponentMeta().getComponentType();
            String method = switch (type == null ? "" : type.substring(type.lastIndexOf('.') + 1)) {
                case "Converter" -> "converter";
                case "Translator" -> "translator";
                case "Broker" -> "broker";
                case "Producer" -> "producer";
                case "SingleRecipientRouter" -> "router";
                case "MultiRecipientRouter" -> "multiRecipientRouter";
                case "Filter" -> element.getComponentMeta().isFlowTestPassThroughFilter() ? "filter" : null;
                default -> null;
            };
            if (method == null) return null;
            steps.add(Map.of("method", method, "name", element.getIdentity()));
            if (method.equals("router") || method.equals("multiRecipientRouter")) routerMethod = method;
        }
        List<Map<String, Object>> children = new ArrayList<>();
        int id = ++sequence[0];
        if (routerMethod != null) {
            if (route.getChildRoutes().isEmpty()) return null;
            for (var child : route.getChildRoutes()) {
                var description = describe(child, sequence);
                if (description == null) return null;
                children.add(description);
            }
        } else if (!route.getChildRoutes().isEmpty() || steps.isEmpty()
                || !"producer".equals(steps.get(steps.size() - 1).get("method"))) return null;
        return Map.of("name", route.getRouteName(), "steps", steps, "children", children,
                "multi", "multiRecipientRouter".equals(routerMethod), "id", id);
    }
}
