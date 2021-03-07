package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;

import java.util.Map;

/**
 * Currently both velocity and embedded strategy are being evaluated
 */
public class ApplicationTemplate extends Generator {
    public static final String APPLICATION_CLASS_NAME = "Application";
    private static final String APPLICATION_VM = "ApplicationTemplate.vm";
    private static final String APPLICATION_FM = "ApplicationTemplate.ftl";

    public static void create(final Project project) {
        String templateString = generateContents();
        createTemplateFile(project, STUDIO_BOOT_PACKAGE, APPLICATION_CLASS_NAME, templateString, true, true);
    }

    public static String generateContents() {
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(CLASS_NAME_TAG, APPLICATION_CLASS_NAME);
//        return VelocityUtils.generateFromTemplate(APPLICATION_VM, configs);
        return FreemarkerUtils.generateFromTemplate(APPLICATION_FM, configs);
    }
}

