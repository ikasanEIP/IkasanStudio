package org.ikasan.studio.generator;

import org.ikasan.studio.model.ikasan.IkasanFlow;
import org.ikasan.studio.model.ikasan.IkasanModule;

public class GeneratorUtils {

    // Enforce as utility class
    private GeneratorUtils() {}

    public static String getBespokePackageName(IkasanModule ikasanModule, IkasanFlow ikasanFlow) {
        return ikasanModule.getApplicationPackageName() + "." + ikasanFlow.getJavaPackageName();
    }
}
