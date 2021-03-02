package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJavaFile;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.Ikasan.IkasanFlow;
import org.ikasan.studio.model.Ikasan.IkasanModule;

import java.util.Map;

/**
 * Currently both velocity and embedded strategy are being evaluated
 */
public class FlowTemplate extends Generator {
    public static String FLOW_CLASS_NAME = "Flow";
    private static String FLOW_VM = "FlowTemplate.vm";

    public static void create(final Project project) {
        IkasanModule ikasanModule = Context.getIkasanModule(project.getName());
        for (IkasanFlow ikasanFlow : ikasanModule.getFlows()) {
            String templateString = generateContents(ikasanModule, ikasanFlow);
            String packageName = STUDIO_BOOT_PACKAGE + "." + ikasanFlow.getJavaPackageName();
            if (!ikasanFlow.getFlowComponentList().isEmpty()) {
                FlowsComponentFactoryTemplate.create(project, packageName, ikasanModule, ikasanFlow);
                FlowsBespokeClassTemplate.create(project, packageName, ikasanModule, ikasanFlow);
            }
            PsiJavaFile newFile = createTemplateFile(
                project,
                packageName,
                ikasanFlow.getJavaClassName(),
                templateString,
                true,
                true);
            ikasanFlow.getViewHandler().setPsiJavaFile(newFile);
        }
    }

    public static String generateContents(IkasanModule ikasanModule, IkasanFlow ikasanFow) {
        Map<String, Object> configs = getVelocityConfigs();
        configs.put(MODULE_TAG, ikasanModule);
        configs.put(FLOW_TAG, ikasanFow);
        String templateString = VelocityUtils.generateFromTemplate(FLOW_VM, configs);
        return templateString;
    }

}
