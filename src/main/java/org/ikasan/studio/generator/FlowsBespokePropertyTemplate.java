package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.model.ikasan.instance.IkasanComponentPropertyInstance;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta;

import java.util.Map;

public class FlowsBespokePropertyTemplate extends Generator {

    public static void create(final Project project, final Module ikasanModule, final Flow ikasanFlow, FlowElement component) {
        for (IkasanComponentPropertyInstance property : component.getUserImplementedClassProperties()) {
            String newPackageName = GeneratorUtils.getBespokePackageName(ikasanModule, ikasanFlow);
            String clazzName = StudioUtils.toJavaClassName(property.getValueString());
            String prefix = GeneratorUtils.getUniquePrefix(ikasanModule, ikasanFlow, component);
            String templateString = generateContents(newPackageName, clazzName, property, prefix);
            createTemplateFile(project, newPackageName, clazzName, templateString, true, true);
        }
    }

    protected static String generateContents(String packageName, String clazzName, IkasanComponentPropertyInstance property, String prefix) {
        String interfaceName = property.getMeta().getUsageDataType();
        String templateName;
        if (IkasanComponentPropertyMeta.CONFIGURATION.equals(property.getMeta().getPropertyName())) {
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
