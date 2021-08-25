package org.ikasan.studio.generator;

import org.ikasan.studio.model.ikasan.IkasanFlow;
import org.ikasan.studio.model.ikasan.IkasanFlowComponent;
import org.ikasan.studio.model.ikasan.IkasanModule;

public class GeneratorUtils {

    // Enforce as utility class
    private GeneratorUtils() {}

    public static String getBespokePackageName(IkasanModule ikasanModule, IkasanFlow ikasanFlow) {
        return ikasanModule.getApplicationPackageName() + "." + ikasanFlow.getJavaPackageName();
    }

    public static String getUniquePrefix(IkasanModule ikasanModule, IkasanFlow ikasanFlow, IkasanFlowComponent ikasanFlowComponent) {
        return ikasanModule.getJavaPackageName() + "." + ikasanFlow.getJavaPackageName() + "." + ikasanFlowComponent.getJavaPackageName();
    }

    public static String getDefaultResourceId(IkasanModule ikasanModule, IkasanFlow ikasanFlow, IkasanFlowComponent ikasanFlowComponent) {
        return ikasanModule.getJavaPackageName() + "." + ikasanFlow.getJavaPackageName() + "." + ikasanFlowComponent.getJavaPackageName();
    }
}
