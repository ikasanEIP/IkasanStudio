package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJavaFile;
import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.model.ikasan.IkasanComponentProperty;
import org.ikasan.studio.model.ikasan.IkasanFlow;
import org.ikasan.studio.model.ikasan.IkasanFlowComponent;
import org.ikasan.studio.model.ikasan.IkasanModule;

import java.util.Map;

public class FlowsResourceFactoryTemplate extends Generator {

    public static void create(final Project project, final IkasanModule ikasanModule, final IkasanFlow ikasanFlow, IkasanFlowComponent component) {
        for (IkasanComponentProperty property : component.getUserImplementedClassProperties()) {
            String newPackageName = GeneratorUtils.getBespokePackageName(ikasanModule, ikasanFlow);
            String clazzName = StudioUtils.toJavaClassName(property.getValueString());
            String templateString = generateContents(newPackageName, clazzName, property);
            PsiJavaFile newFile = createTemplateFile(project, newPackageName, clazzName, templateString, true, true);
            property.setRegenerateAllowed(false);
        }
    }

    public static String generateContents(String packageName, String clazzName, IkasanComponentProperty property) {
        String interfaceName = property.getMeta().getUsageDataType();
        String templateName = "GenericInterfaceTemplate_en_GB.ftl";
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, packageName);
        configs.put(CLASS_NAME_TAG, clazzName);
        configs.put(INTERFACE_NAME_TAG, interfaceName);
        String templateString = FreemarkerUtils.generateFromTemplate(templateName, configs);
        return templateString;
    }
}