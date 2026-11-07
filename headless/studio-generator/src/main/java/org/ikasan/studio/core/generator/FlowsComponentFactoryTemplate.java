package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.Map;

public class FlowsComponentFactoryTemplate extends Generator {
    public static final String COMPONENT_FACTORY_CLASS_NAME = "ComponentFactory";
    private static final String COMPONENT_FACTORY_FTL = "componentFactory_en.ftl";

    public static String create(final String packageName, final Module ikasanModule, final Flow ikasanFlow) throws StudioGeneratorException {
        return generateContents(packageName, ikasanModule, ikasanFlow);
    }

    protected static String generateContents(String packageName, Module ikasanModule, Flow ikasanFlow) throws StudioGeneratorException {
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, packageName);
        configs.put(CLASS_NAME_TAG, COMPONENT_FACTORY_CLASS_NAME);
        configs.put(MODULE_TAG, ikasanModule);
        configs.put(FLOW_TAG, ikasanFlow);
        boolean hasHarness = ikasanFlow.getFlowElementsNoExternalEndPoints().stream()
                .anyMatch(e -> !org.ikasan.studio.core.model.analysis.HarnessOverrides.kind(ikasanModule, e).isEmpty());
        configs.put("harnessSupport", hasHarness ?
                "@org.springframework.beans.factory.annotation.Autowired\nprivate org.springframework.core.env.Environment studioHarnessEnvironment;\n" : "");
        return FreemarkerUtils.generateFromTemplate(ikasanModule.getMetaVersion(), COMPONENT_FACTORY_FTL, configs);
    }
}
