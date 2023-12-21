package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.IkasanModule;

import java.util.Map;

/**
 * Template to create Application.java
 */
public class ApplicationTemplate extends Generator {
    public static final String APPLICATION_CLASS_NAME = "Application";
    private static final String APPLICATION_FTL = "applicationTemplate.ftl";

    public static void create(final Project project) {
        IkasanModule ikasanModule = Context.getIkasanModule(project.getName());
        String templateString = generateContents(ikasanModule);
        createTemplateFile(project, STUDIO_BOOT_PACKAGE, APPLICATION_CLASS_NAME, templateString, true, true);
    }

    protected static String generateContents(IkasanModule ikasanModule) {
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, STUDIO_BOOT_PACKAGE);
        configs.put(MODULE_TAG, ikasanModule);
        configs.put(CLASS_NAME_TAG, APPLICATION_CLASS_NAME);
        return FreemarkerUtils.generateFromTemplate(APPLICATION_FTL, configs);
    }
}

