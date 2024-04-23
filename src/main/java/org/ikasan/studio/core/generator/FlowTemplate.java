package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.Map;

public class FlowTemplate extends Generator {
    private static final String FLOW_FTL = "flowTemplate.ftl";

    public static String create(Module ikasanModule, Flow ikasanFlow, String packageName) throws StudioGeneratorException {
        return  generateContents(packageName, ikasanModule, ikasanFlow);
    }

    protected static String generateContents(String packageName, Module ikasanModule, Flow ikasanFow) throws StudioGeneratorException {
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, packageName);
        configs.put(MODULE_TAG, ikasanModule);
        configs.put(FLOW_TAG, ikasanFow);
        return  FreemarkerUtils.generateFromTemplate(FLOW_FTL, configs);
    }
}
