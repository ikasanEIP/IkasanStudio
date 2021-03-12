package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJavaFile;
import org.ikasan.studio.model.ikasan.*;

import java.util.Map;

/**
 * Currently both velocity and embedded strategy are being evaluated
 */
public class FlowsBespokeClassTemplate extends Generator {

    public static void create(final Project project, final IkasanModule ikasanModule, final IkasanFlow ikasanFlow) {
        for (IkasanFlowComponent component : ikasanFlow.getFlowComponentList()) {
            if (component instanceof IkasanFlowBeskpokeComponent && ((IkasanFlowBeskpokeComponent)component).isOverrideEnabled()) {
                boolean overwriteClassIsExists = false;
                String clazzName = (String)component.getProperty(IkasanComponentPropertyMeta.BESPOKE_CLASS_NAME).getValue();
                String newPackageName = ikasanModule.getApplicationPackageName() + "." + ikasanFlow.getJavaPackageName();
                String templateString = generateContents(newPackageName, component);
                overwriteClassIsExists = ((IkasanFlowBeskpokeComponent)component).isOverrideEnabled();
                PsiJavaFile newFile = createTemplateFile(project, newPackageName, clazzName, templateString, true, overwriteClassIsExists);
                ((IkasanFlowBeskpokeComponent)component).setOverrideEnabled(false);
                component.getViewHandler().setPsiJavaFile(newFile);
            }
        }
    }

    public static String generateContents(String packageName, IkasanFlowComponent ikasanFlowComponent) {
        String templateName = ikasanFlowComponent.getType().getElementCategory().toString() + "_Template.ftl";
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, packageName);
        configs.put(COMPONENT_TAG, ikasanFlowComponent);
        String templateString = FreemarkerUtils.generateFromTemplate(templateName, configs);
        return templateString;
    }
}
