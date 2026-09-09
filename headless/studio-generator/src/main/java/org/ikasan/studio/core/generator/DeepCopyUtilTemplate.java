package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.Map;

/**
 * Template to create DeepCopyUtil.java, the serialisation/reflection based deep-copy helper used by
 * DebugTransitionComponent so inspecting a payload while debugging can never mutate what the flow is
 * actually processing. Generated directly into the project rather than pulled in as a jar dependency.
 */
public class DeepCopyUtilTemplate extends Generator {
    public static final String DEEP_COPY_UTIL_PACKAGE = "org.ikasan.studio.component.utils";
    public static final String DEEP_COPY_UTIL_CLASS_NAME = "DeepCopyUtil";
    private static final String DEEP_COPY_UTIL_FTL = "deepCopyUtilTemplate_en.ftl";

    public static String create(Module ikasanModule) throws StudioGeneratorException {
        return generateContents(ikasanModule);
    }

    protected static String generateContents(Module ikasanModule) throws StudioGeneratorException {
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, DEEP_COPY_UTIL_PACKAGE);
        configs.put(MODULE_TAG, ikasanModule);
        configs.put(CLASS_NAME_TAG, DEEP_COPY_UTIL_CLASS_NAME);
        return FreemarkerUtils.generateFromTemplate(ikasanModule.getMetaVersion(), DEEP_COPY_UTIL_FTL, configs);
    }
}
