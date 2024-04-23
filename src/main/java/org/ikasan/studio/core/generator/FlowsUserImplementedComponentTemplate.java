package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.model.ikasan.instance.FlowElement;

import java.util.Map;

/**
 * Template to create the user implemented classes (stubs) i.e. those with property for
 *      'userImplementedClassName' e.g. Broker, Filter, Converter
 * The type of template used depends on the component hence each component can have at most 1 userImplementedClassName
 */
public class FlowsUserImplementedComponentTemplate extends Generator {

    public static String create(String newPackageName, FlowElement component) throws StudioGeneratorException {
        return generateContents(newPackageName, component);
    }

    protected static String generateContents(String packageName, FlowElement flowElement) throws StudioGeneratorException {
        String templateName = flowElement.getProperty("userImplementedClassName").getMeta().getUserImplementClassFtlTemplate();
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, packageName);
        configs.put(FLOW_ELEMENT_TAG, flowElement);
        return FreemarkerUtils.generateFromTemplate(templateName, configs);
    }
}
