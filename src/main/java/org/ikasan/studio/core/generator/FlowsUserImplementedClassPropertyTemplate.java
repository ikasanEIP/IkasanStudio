package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;

import java.util.Map;

public class FlowsUserImplementedClassPropertyTemplate extends Generator {

    public static String create(ComponentProperty property, String newPackageName, String clazzName, String prefix) {
        return generateContents(newPackageName, clazzName, property, prefix);
    }

    protected static String generateContents(String packageName, String clazzName, ComponentProperty property, String prefix) {
        String interfaceName = property.getMeta().getUsageDataType();
        String templateName;
        if (ComponentPropertyMeta.CONFIGURATION.equals(property.getMeta().getPropertyName())) {
            templateName = "configurationTemplate_en.ftl";
        } else {
            templateName = "genericInterfaceTemplate_en.ftl";
        }
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, packageName);
        configs.put(CLASS_NAME_TAG, clazzName);
        configs.put(INTERFACE_NAME_TAG, interfaceName);
        configs.put(PREFIX_TAG, prefix);
        return FreemarkerUtils.generateFromTemplate(templateName, configs);
    }
}
