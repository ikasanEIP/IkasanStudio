package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJavaFile;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.IkasanFlow;
import org.ikasan.studio.model.ikasan.IkasanModule;

import java.util.Map;

public class FlowTemplate extends Generator {
    private static String FLOW_FTL = "FlowTemplate.ftl";

    public static void create(final Project project) {
        IkasanModule ikasanModule = Context.getIkasanModule(project.getName());
        for (IkasanFlow ikasanFlow : ikasanModule.getFlows()) {
            String packageName = STUDIO_BOOT_PACKAGE + "." + ikasanFlow.getJavaPackageName();
            String templateString = generateContents(packageName, ikasanModule, ikasanFlow);
            if (!ikasanFlow.getFlowComponentList().isEmpty()) {
                // Must do Beskpoke classes first otherwise resolution will not auto generate imports.
                FlowsBespokeComponentTemplate.create(project, ikasanModule, ikasanFlow);
                FlowsComponentFactoryTemplate.create(project, packageName, ikasanModule, ikasanFlow);
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

    public static String generateContents(String packageName, IkasanModule ikasanModule, IkasanFlow ikasanFow) {
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, packageName);
        configs.put(MODULE_TAG, ikasanModule);
        configs.put(FLOW_TAG, ikasanFow);
        String templateString = FreemarkerUtils.generateFromTemplate(FLOW_FTL, configs);
        return templateString;
    }

}
