package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJavaFile;
import org.ikasan.studio.model.ikasan.IkasanFlow;
import org.ikasan.studio.model.ikasan.IkasanModule;

import java.util.Map;

/**
 * Currently both velocity and embedded strategy are being evaluated
 */
public class FlowsComponentFactoryTemplate extends Generator {
    public static String COMPONENT_FACTORY_CLASS_NAME = "ComponentFactory";
    private static String COMPONENT_FACTORY_VM = "ComponentFactory.vm";
    private static String COMPONENT_FACTORY_FTL = "ComponentFactory.ftl";

    public static void create(final Project project, final String packageName, final IkasanModule ikasanModule, final IkasanFlow ikasanFlow) {
//        IkasanModule ikasanModule = Context.getIkasanModule(project.getName());
        String templateString = generateContents(ikasanModule, ikasanFlow);

        PsiJavaFile newFile = createTemplateFile(project, packageName, COMPONENT_FACTORY_CLASS_NAME, templateString, true, true);
    }

    public static String generateContents(IkasanModule ikasanModule, IkasanFlow ikasanFlow) {
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(CLASS_NAME_TAG, COMPONENT_FACTORY_CLASS_NAME);
        configs.put(FLOW_TAG, ikasanFlow);
        configs.put(MODULE_TAG, ikasanModule);
//        String templateString = VelocityUtils.generateFromTemplate(COMPONENT_FACTORY_VM, configs);
        String templateString = FreemarkerUtils.generateFromTemplate(COMPONENT_FACTORY_FTL, configs);
        return templateString;
    }
}
