package org.ikasan.studio.core.generator;

import java.util.Map;
import java.util.TreeMap;

public abstract class Generator {
    public static final String STUDIO_PACKAGE_TAG = "studioPackageTag";
    public static final String CLASS_NAME_TAG = "className";
    public static final String FLOW_ELEMENT_TAG = "flowElement";
    public static final String INTERFACE_NAME_TAG = "interfaceName";
    public static final String PREFIX_TAG = "prefix";
    public static final String FLOWS_TAG = "flows";
    public static final String FLOW_TAG = "flow";
    public static final String MODULE_TAG = "module";
    public static final String STUDIO_BASE_PACKAGE_TAG = "studioBasePackage";
    public static final String STUDIO_FLOW_BASE_PACKAGE_TAG = "studioFlowBasePackage";
    public static final String STUDIO_BOOT_PACKAGE = "org.ikasan.studio.boot";
    public static final String STUDIO_FLOW_PACKAGE = "org.ikasan.studio.boot.flow";

    // Enforce Utility class.
    protected Generator() {}

    /**
     * Create the configs map used by the template language, pre-populate with configs used by all templates
     * @return The String to Object map used to populate templates.
     */
    protected static Map<String, Object> getBasicTemplateConfigs() {
        Map<String, Object> configs = new TreeMap<>();
        configs.put(STUDIO_BASE_PACKAGE_TAG, STUDIO_BOOT_PACKAGE);
        configs.put(STUDIO_FLOW_BASE_PACKAGE_TAG, STUDIO_FLOW_PACKAGE);
        return configs;
    }
}
