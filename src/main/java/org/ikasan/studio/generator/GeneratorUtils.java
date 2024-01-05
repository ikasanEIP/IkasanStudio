package org.ikasan.studio.generator;

import org.ikasan.studio.model.ikasan.Flow;
import org.ikasan.studio.model.ikasan.FlowElement;
import org.ikasan.studio.model.ikasan.Module;

public class GeneratorUtils {

    // Enforce as utility class
    private GeneratorUtils() {}

    public static String getBespokePackageName(Module ikasanModule, Flow ikasanFlow) {
        return ikasanModule.getApplicationPackageName() + "." + ikasanFlow.getJavaPackageName();
    }

    public static String getUniquePrefix(Module ikasanModule, Flow ikasanFlow, FlowElement ikasanFlowComponent) {
        return ikasanModule.getJavaPackageName() + "." + ikasanFlow.getJavaPackageName() + "." + ikasanFlowComponent.getJavaPackageName();
    }

    public static String getDefaultResourceId(Module ikasanModule, Flow ikasanFlow, FlowElement ikasanFlowComponent) {
        return ikasanModule.getJavaPackageName() + "." + ikasanFlow.getJavaPackageName() + "." + ikasanFlowComponent.getJavaPackageName();
    }
}
