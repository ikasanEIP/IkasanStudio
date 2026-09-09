package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.Map;

/**
 * Template to create DebugTransitionComponent.java, the base class Debug flow elements extend so users have
 * a fixed method to set a breakpoint on and inspect a flow's payload mid-transition. Generated directly into
 * the project rather than pulled in as a jar dependency, so it always compiles against whichever Ikasan
 * Filter API the target module's own metapack version resolves.
 */
public class DebugTransitionComponentTemplate extends Generator {
    public static final String DEBUG_TRANSITION_COMPONENT_PACKAGE = "org.ikasan.studio.component";
    public static final String DEBUG_TRANSITION_COMPONENT_CLASS_NAME = "DebugTransitionComponent";
    private static final String DEBUG_TRANSITION_COMPONENT_FTL = "debugTransitionComponentTemplate_en.ftl";

    public static String create(Module ikasanModule) throws StudioGeneratorException {
        return generateContents(ikasanModule);
    }

    protected static String generateContents(Module ikasanModule) throws StudioGeneratorException {
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, DEBUG_TRANSITION_COMPONENT_PACKAGE);
        configs.put(MODULE_TAG, ikasanModule);
        configs.put(CLASS_NAME_TAG, DEBUG_TRANSITION_COMPONENT_CLASS_NAME);
        return FreemarkerUtils.generateFromTemplate(ikasanModule.getMetaVersion(), DEBUG_TRANSITION_COMPONENT_FTL, configs);
    }
}
