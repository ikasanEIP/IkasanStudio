package org.ikasan.studio.core.conversion;

import org.ikasan.studio.core.metapack.model.ConversionRecipeMeta;
import java.util.*;

/** Offline matching of declared contracts; never loads project classes or guesses serialization. */
public final class ConversionRecipeMatcher {
    private ConversionRecipeMatcher() {}

    /** Compatibility with historic model values containing a display-only annotation. */
    public static String javaType(String value) {
        if (value == null) return "";
        return value.replace(" (auto-converted)", "").trim();
    }

    public static List<ConversionRecipeMeta> matching(List<ConversionRecipeMeta> recipes, String source, String targets) {
        if (recipes == null) return List.of();
        Set<String> accepted = new HashSet<>();
        if (targets != null) for (String target : targets.split(",")) accepted.add(javaType(target));
        return recipes.stream().filter(r -> javaType(r.getSourceType()).equals(javaType(source)))
                .filter(r -> accepted.isEmpty() || accepted.contains(javaType(r.getTargetType())))
                .toList();
    }

    /** Nearest downstream contract within this route. Never guesses across a branching router. */
    public static String downstreamTypes(org.ikasan.studio.core.model.ikasan.instance.FlowElement element) {
        var flow = element.getContainingFlow();
        if (flow == null || flow.getFlowRoute() == null) return null;
        var route = flow.getFlowRouteContaining(flow.getFlowRoute(), element);
        if (route == null || route.getFlowElements() == null) return null;
        int index = route.getFlowElements().indexOf(element);
        if (index < 0) return null;
        for (int i = index + 1; i < route.getFlowElements().size(); i++) {
            var next = route.getFlowElements().get(i);
            var meta = next.getComponentMeta();
            if (meta == null || meta.isRouter()) return null;
            if (meta.isDebug() || meta.isFilter() || meta.isInternalEndpoint()) continue;
            String types = next.getEffectiveInputTypeDescription();
            if (types != null && !types.isBlank() && !javaType(types).equals("java.lang.Object")) return types;
        }
        return null;
    }

    /** Stable recommendation order; other recipes remain available for deliberate type changes. */
    public static List<ConversionRecipeMeta> ranked(List<ConversionRecipeMeta> recipes, String source, String targets) {
        if (recipes == null) return List.of();
        List<ConversionRecipeMeta> result = new ArrayList<>(matching(recipes, source, targets));
        for (ConversionRecipeMeta recipe : recipes) if (!result.contains(recipe)) result.add(recipe);
        return List.copyOf(result);
    }
}
