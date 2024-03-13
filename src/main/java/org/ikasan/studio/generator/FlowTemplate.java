package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJavaFile;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.Module;

import java.util.Map;

public class FlowTemplate extends Generator {
    private static final String FLOW_FTL = "flowTemplate.ftl";

    public static void create(final Project project) {
        Module ikasanModule = Context.getIkasanModule(project.getName());
        for (Flow ikasanFlow : ikasanModule.getFlows()) {
            String packageName = STUDIO_BOOT_PACKAGE + "." + ikasanFlow.getJavaPackageName();
            String templateString = generateContents(packageName, ikasanModule, ikasanFlow);
            if (!ikasanFlow.ftlGetConsumerAndFlowElements().isEmpty()) {
                // Must do User Implemented class stubs first otherwise resolution will not auto generate imports.
                FlowsUserImplementedComponentTemplate.create(project, ikasanModule, ikasanFlow);
                FlowsComponentFactoryTemplate.create(project, packageName, ikasanModule, ikasanFlow);
            }
            PsiJavaFile newFile = createJavaSourceFile(
                project,
                packageName,
                ikasanFlow.getJavaClassName(),
                templateString,
                true,
                true);
            ikasanFlow.getViewHandler().setPsiJavaFile(newFile);
        }
    }

    protected static String generateContents(String packageName, Module ikasanModule, Flow ikasanFow) {
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, packageName);
        configs.put(MODULE_TAG, ikasanModule);
        configs.put(FLOW_TAG, ikasanFow);
        return  FreemarkerUtils.generateFromTemplate(FLOW_FTL, configs);
    }
}
