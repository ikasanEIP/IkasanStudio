package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.model.ikasan.instance.Module;

/**
 * Template to create the JSON representation of the module
 */
public class ModelTemplate extends Generator {
    public static final String APPLICATION_CLASS_NAME = "module";
    private static final String APPLICATION_FTL = "applicationTemplate.ftl";

    public static void create(final Project project) {
//        IkasanModule ikasanModule = Context.getIkasanModule(project.getComponentName());
//        String templateString = generateContents(ikasanModule);
//        createTemplateFile(project, STUDIO_BOOT_PACKAGE, APPLICATION_CLASS_NAME, templateString, true, true);
    }

    /**
     * Initially created to make the process more testable.
     * For the given component, generate the corresponding Java code in string format
     * @param ikasanModule used as the source of information
     * @return a string containing the java code representing the parameter input
     */
    protected static String generateContents(Module ikasanModule) {
//        Map<String, Object> configs = getBasicTemplateConfigs();
//        configs.put(STUDIO_PACKAGE_TAG, STUDIO_BOOT_PACKAGE);
//        configs.put(MODULE_TAG, ikasanModule);
//        configs.put(CLASS_NAME_TAG, APPLICATION_CLASS_NAME);
//        return FreemarkerUtils.generateFromTemplate(APPLICATION_FTL, configs);
        return "";
    }
}

