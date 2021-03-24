package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJavaFile;
import org.ikasan.studio.model.ikasan.IkasanFlow;
import org.ikasan.studio.model.ikasan.IkasanModule;

import java.util.Map;

public class FlowsComponentFactoryTemplate extends Generator {
    public static String COMPONENT_FACTORY_CLASS_NAME = "ComponentFactory";
    private static String COMPONENT_FACTORY_FTL = "ComponentFactory.ftl";

    public static void create(final Project project, final String packageName, final IkasanModule ikasanModule, final IkasanFlow ikasanFlow) {
        String templateString = generateContents(packageName, ikasanModule, ikasanFlow);
        PsiJavaFile newFile = createTemplateFile(project, packageName, COMPONENT_FACTORY_CLASS_NAME, templateString, true, true);
    }

    public static String generateContents(String packageName, IkasanModule ikasanModule, IkasanFlow ikasanFlow) {
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, packageName);
        configs.put(CLASS_NAME_TAG, COMPONENT_FACTORY_CLASS_NAME);
        configs.put(MODULE_TAG, ikasanModule);
        configs.put(FLOW_TAG, ikasanFlow);
        String templateString = FreemarkerUtils.generateFromTemplate(COMPONENT_FACTORY_FTL, configs);
        return templateString;
    }
}
