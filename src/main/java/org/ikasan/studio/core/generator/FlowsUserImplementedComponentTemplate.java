package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;

import java.util.Map;

/**
 * Template to create the user implemented classes (stubs) i.e. those with property for
 *      'userImplementedClassName' e.g. Broker, Filter, Converter
 * The type of template used depends on the component hence each component can have at most 1 userImplementedClassName
 */
public class FlowsUserImplementedComponentTemplate extends Generator {

    public static String create(String newPackageName, Module ikasanModule, Flow ikasanFow, FlowElement component) throws StudioGeneratorException {
        return generateContents(newPackageName, ikasanModule, ikasanFow, component);
    }

    protected static String generateContents(String packageName, Module ikasanModule, Flow ikasanFow, FlowElement flowElement) throws StudioGeneratorException {
        String templateName = flowElement.getProperty(ComponentPropertyMeta.USER_IMPLEMENTED_CLASS_NAME).getMeta().getUserImplementClassFtlTemplate();
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, packageName);
        configs.put(FLOW_ELEMENT_TAG, flowElement);
        configs.put(MODULE_TAG, ikasanModule);
        configs.put(FLOW_TAG, ikasanFow);
        return FreemarkerUtils.generateFromTemplate(templateName, configs);
    }
}
