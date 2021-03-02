package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJavaFile;
import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.model.Ikasan.*;

import java.util.Map;

/**
 * Currently both velocity and embedded strategy are being evaluated
 */
public class FlowsBespokeClassTemplate extends Generator {

    public static void create(final Project project, final String packageName, final IkasanModule ikasanModule, final IkasanFlow ikasanFlow) {

        for (IkasanFlowComponent component : ikasanFlow.getFlowComponentList()) {
            if (component instanceof IkasanFlowBeskpokeComponent) {
                boolean overwriteClassIsExists = false;
                String templateString = generateContents(component);
                String clazzAndPackageName = (String)component.getProperty(IkasanComponentPropertyMeta.BESPOKE_CLASS_NAME).getValue();
                String clazzName = null;
                String newPackageName = null;
                if (clazzAndPackageName.contains(".")) {
                    clazzName = StudioUtils.getLastToken("\\.", clazzAndPackageName);
                    newPackageName = StudioUtils.getAllButLastToken("\\.", clazzAndPackageName);
                } else {
                    clazzName = clazzAndPackageName;
                    newPackageName = STUDIO_BESPOKE_PACKAGE;
                }
                overwriteClassIsExists = ((IkasanFlowBeskpokeComponent)component).isOverrideEnabled();
                PsiJavaFile newFile = createTemplateFile(project, newPackageName, clazzName, templateString, true, overwriteClassIsExists);
                ((IkasanFlowBeskpokeComponent)component).setOverrideEnabled(false);
                component.getViewHandler().setPsiJavaFile(newFile);
            }
        }
    }

    public static String generateContents(IkasanFlowComponent ikasanFlowComponent) {
        String templateName = ikasanFlowComponent.getType().getElementCategory().toString() + "_Template.vm";
        Map<String, Object> configs = getVelocityConfigs();

        configs.put(COMPONENT_TAG, ikasanFlowComponent);
        String templateString = VelocityUtils.generateFromTemplate(templateName, configs);
        return templateString;
    }
}
