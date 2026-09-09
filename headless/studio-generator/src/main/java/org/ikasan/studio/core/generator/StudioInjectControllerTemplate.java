package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.Map;

/**
 * Template to create StudioInjectController.java, a Debug-mode-only REST endpoint for injecting synthetic
 * test events into a flow's Consumer entry point.
 */
public class StudioInjectControllerTemplate extends Generator {
    public static final String STUDIO_INJECT_CONTROLLER_CLASS_NAME = "StudioInjectController";
    private static final String STUDIO_INJECT_CONTROLLER_FTL = "studioInjectControllerTemplate_en.ftl";

    public static String create(Module ikasanModule) throws StudioGeneratorException {
        return generateContents(ikasanModule);
    }

    protected static String generateContents(Module ikasanModule) throws StudioGeneratorException {
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, STUDIO_BOOT_PACKAGE);
        configs.put(MODULE_TAG, ikasanModule);
        configs.put(CLASS_NAME_TAG, STUDIO_INJECT_CONTROLLER_CLASS_NAME);
        return FreemarkerUtils.generateFromTemplate(ikasanModule.getMetaVersion(), STUDIO_INJECT_CONTROLLER_FTL, configs);
    }
}
