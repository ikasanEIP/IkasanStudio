package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;

public class GeneratorUtils {

    // Enforce as utility class
    private GeneratorUtils() {}

    public static String getUserImplementedClassesPackageName(Module ikasanModule, Flow ikasanFlow) {
        return ikasanModule.getApplicationPackageName() + "." + ikasanFlow.getJavaPackageName();
    }

    public static String getUniquePrefix(Module ikasanModule, Flow ikasanFlow, FlowElement ikasanFlowComponent) {
        return ikasanModule.getJavaPackageName() + "." + ikasanFlow.getJavaPackageName() + "." + ikasanFlowComponent.getJavaPackageName();
    }

    public static String getDefaultResourceId(Module ikasanModule, Flow ikasanFlow, FlowElement ikasanFlowComponent) {
        return ikasanModule.getJavaPackageName() + "." + ikasanFlow.getJavaPackageName() + "." + ikasanFlowComponent.getJavaPackageName();
    }
}
