package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.Ikasan.IkasanModule;

import java.util.HashMap;
import java.util.Map;

/**
 * Currently both velocity and embedded strategy are being evaluated
 */
public class PropertiesTemplate extends Generator {
    public static String MODULE_PROPERTIES_FILENAME = "application";
    private static String MODULE_PROPERTIES_VM = "PropertiesTemplate.vm";

    public static void create(final Project project) {
        IkasanModule ikasanModule = Context.getIkasanModule(project.getName());
        String templateString = createPropertiesVelocity(ikasanModule);

        createResourceFile(project, null, MODULE_PROPERTIES_FILENAME, templateString, false);
    }

    public static String createPropertiesVelocity(IkasanModule ikasanModule) {
        Map<String, Object> configs = getVelocityConfigs();
        configs.put(MODULE_TAG, ikasanModule);
        String templateString = VelocityUtils.generateFromTemplate(MODULE_PROPERTIES_VM, configs);
        return templateString;
    }
}
