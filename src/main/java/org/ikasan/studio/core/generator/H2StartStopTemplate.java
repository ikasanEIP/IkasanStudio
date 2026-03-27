package org.ikasan.studio.core.generator;

import java.util.Map;

/**
 * Template to create Application.java
 */
public class H2StartStopTemplate extends Generator {
    private static final String H2_START_STOP_POM_FTL = "h2StartStopPom_en.ftl";

    public static String create(String metapackVersion) throws StudioGeneratorException {
        return generateContents(metapackVersion);
    }

    protected static String generateContents(String metapackVersion) throws StudioGeneratorException {
        Map<String, Object> configs = getBasicTemplateConfigs();
        return FreemarkerUtils.generateFromTemplate(metapackVersion, H2_START_STOP_POM_FTL, configs);
    }
}