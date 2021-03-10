package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.IkasanModule;

import java.util.Map;

/**
 * Currently both velocity and embedded strategy are being evaluated
 */
public class PropertiesTemplate extends Generator {
    public static String MODULE_PROPERTIES_FILENAME = "application";
    private static String MODULE_PROPERTIES_FTL = "PropertiesTemplate.ftl";

    public static void create(final Project project) {
        IkasanModule ikasanModule = Context.getIkasanModule(project.getName());
        String templateString = generateContents(ikasanModule);

        createResourceFile(project, null, MODULE_PROPERTIES_FILENAME, templateString, false);
    }

    //@todo it might be more effician to have 1 properties file per flow
    public static String generateContents(IkasanModule ikasanModule) {
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(MODULE_TAG, ikasanModule);
        String templateString = FreemarkerUtils.generateFromTemplate(MODULE_PROPERTIES_FTL, configs);
        return templateString;
    }
}
