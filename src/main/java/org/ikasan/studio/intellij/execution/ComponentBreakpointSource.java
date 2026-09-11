package org.ikasan.studio.intellij.execution;

import org.ikasan.studio.core.generator.GeneratorUtils;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;

/** Pure source matching; no PSI, indexing or debugger evaluation on the paint thread. */
final class ComponentBreakpointSource {
    private ComponentBreakpointSource() { }

    static FlowElement uniqueMatch(String basePath, Module module, String source) {
        if (module == null || module.getFlows() == null || source == null) return null;
        var matches = module.getFlows().stream()
                .flatMap(flow -> flow.getFlowElementsNoExternalEndPoints().stream())
                .filter(element -> matches(basePath, module, element, source)).limit(2).toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    static boolean matches(String basePath, Module module, FlowElement element, String source) {
        if (basePath == null || module == null || element == null || source == null
                || element.getContainingFlow() == null || element.getComponentMeta() == null) return false;
        Object userClass = element.getPropertyValue("userImplementedClassName");
        if (userClass instanceof String name && !name.isBlank()) {
            String qualified = name.contains(".") ? name
                    : GeneratorUtils.getUserImplementedClassesPackageName(module, element.getContainingFlow()) + "." + name;
            String relative = qualified.replace('.', '/') + ".java";
            return source.equals(basePath + "/user/src/main/java/" + relative)
                    || source.equals(basePath + "/generated/src/main/java/" + relative);
        }
        String implementation = element.getComponentMeta().getImplementingClass();
        if (implementation == null || implementation.isBlank()) return false;
        String outerClass = implementation.split("\\$", 2)[0];
        return source.endsWith("/" + outerClass.replace('.', '/') + ".java");
    }
}
