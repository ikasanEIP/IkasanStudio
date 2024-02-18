package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.model.ikasan.instance.IkasanComponentProperty;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.model.ikasan.instance.Module;

import java.util.Map;

public class FlowsResourceFactoryTemplate extends Generator {

    public static void create(final Project project, final Module ikasanModule, final Flow ikasanFlow, FlowElement component) {
        for (IkasanComponentProperty property : component.getUserImplementedClassProperties()) {
            String newPackageName = GeneratorUtils.getBespokePackageName(ikasanModule, ikasanFlow);
            String clazzName = StudioUtils.toJavaClassName(property.getValueString());
            String templateString = generateContents(newPackageName, clazzName, property);
            createJavaSourceFile(project, newPackageName, clazzName, templateString, true, true);
        }
    }

    protected static String generateContents(String packageName, String clazzName, IkasanComponentProperty property) {
        String interfaceName = property.getMeta().getUsageDataType();
        String templateName = "genericInterfaceTemplate_en.ftl";
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, packageName);
        configs.put(CLASS_NAME_TAG, clazzName);
        configs.put(INTERFACE_NAME_TAG, interfaceName);
        return FreemarkerUtils.generateFromTemplate(templateName, configs);
    }
}
