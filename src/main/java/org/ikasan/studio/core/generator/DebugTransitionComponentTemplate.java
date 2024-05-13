package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.Map;

/**
 * Template to create DeubgTransitionComponent.java
 */
public class DebugTransitionComponentTemplate extends Generator {
    private static final String DEBUG_TRANSITION_COMPONENT_FTL = "debugTransitionComponentTemplate.ftl";
    public static final String STUDIO_DEBUG_COMPONENT_PACKAGE = "org.ikasan.studio.boot.component";
    public static final String STUDIO_DEBUG_COMPONENT_CLASS_NAME = "DebugTransitionComponent";

    public static String create(Module ikasanModule) throws StudioGeneratorException {
        return generateContents();
    }

    protected static String generateContents() throws StudioGeneratorException {
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, STUDIO_DEBUG_COMPONENT_PACKAGE);
        configs.put(CLASS_NAME_TAG, STUDIO_DEBUG_COMPONENT_CLASS_NAME);
        return FreemarkerUtils.generateFromTemplate(DEBUG_TRANSITION_COMPONENT_FTL, configs);
    }
}

