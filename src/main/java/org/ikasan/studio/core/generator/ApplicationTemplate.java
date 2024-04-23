package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.Map;

/**
 * Template to create Application.java
 */
public class ApplicationTemplate extends Generator {
    public static final String APPLICATION_CLASS_NAME = "Application";
    private static final String APPLICATION_FTL = "applicationTemplate.ftl";

    public static String create(Module ikasanModule) throws StudioGeneratorException {
        return generateContents(ikasanModule);
    }

    protected static String generateContents(Module ikasanModule) throws StudioGeneratorException {
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, STUDIO_BOOT_PACKAGE);
        configs.put(MODULE_TAG, ikasanModule);
        configs.put(CLASS_NAME_TAG, APPLICATION_CLASS_NAME);
        return FreemarkerUtils.generateFromTemplate(APPLICATION_FTL, configs);
    }
}

