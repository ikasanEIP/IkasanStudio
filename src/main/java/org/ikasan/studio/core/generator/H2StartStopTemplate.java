package org.ikasan.studio.core.generator;

import java.util.Map;

/**
 * Template to create Application.java
 */
public class H2StartStopTemplate extends Generator {
    private static final String H2_START_STOP_POM_FTL = "h2StartStopPom.ftl";

    public static String create() throws StudioGeneratorException {
        return generateContents();
    }

    protected static String generateContents() throws StudioGeneratorException {
        Map<String, Object> configs = getBasicTemplateConfigs();
        return FreemarkerUtils.generateFromTemplate(H2_START_STOP_POM_FTL, configs);
    }
}

