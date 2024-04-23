package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.Map;

public class PropertiesTemplate extends Generator {
    public static final String MODULE_PROPERTIES_FILENAME = "application";

    private static final String MODULE_PROPERTIES_FTL = "propertiesTemplate.ftl";

    public static String create(Module ikasanModule) throws StudioGeneratorException {
        return generateContents(ikasanModule);
    }

    //@todo it might be more efficient to have 1 properties file per flow
    protected static String generateContents(Module ikasanModule) throws StudioGeneratorException {
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(MODULE_TAG, ikasanModule);
        return FreemarkerUtils.generateFromTemplate(MODULE_PROPERTIES_FTL, configs);
    }
}
