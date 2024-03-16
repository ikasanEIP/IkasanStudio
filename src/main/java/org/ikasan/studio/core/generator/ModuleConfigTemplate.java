package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.Map;

public class ModuleConfigTemplate extends Generator {
    public static final String MODULE_CLASS_NAME = "ModuleConfig";
    private static final String MODULE_FTL = "moduleConfigTemplate.ftl";

    public static String create(Module ikasanModule) {
        return generateContents(ikasanModule);
    }

    protected static String generateContents(Module ikasanModule) {
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, STUDIO_BOOT_PACKAGE);
        configs.put(CLASS_NAME_TAG, MODULE_CLASS_NAME);
        configs.put(FLOWS_TAG, ikasanModule.getFlows());
        configs.put(MODULE_TAG, ikasanModule);
        return FreemarkerUtils.generateFromTemplate(MODULE_FTL, configs);
    }
}
