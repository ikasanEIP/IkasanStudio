package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJavaFile;
import org.ikasan.studio.model.ikasan.IkasanComponentPropertyMeta;
import org.ikasan.studio.model.ikasan.IkasanFlowBeskpokeComponent;
import org.ikasan.studio.model.ikasan.IkasanFlowComponent;

import java.util.Map;

/**
 * Currently both velocity and embedded strategy are being evaluated
 */
public class BespokeClassTemplate extends Generator {

    public static void create(final Project project, final IkasanFlowComponent component) {
        boolean overwriteClassIsExists = false;
        String templateString = generateContents(component);
        String clazzName = (String)component.getProperty(IkasanComponentPropertyMeta.BESPOKE_CLASS_NAME).getValue();
        if (component instanceof IkasanFlowBeskpokeComponent) {
            overwriteClassIsExists = ((IkasanFlowBeskpokeComponent)component).isOverrideEnabled();
        }
        PsiJavaFile newFile = createTemplateFile(project, STUDIO_BOOT_PACKAGE, clazzName, templateString, true, overwriteClassIsExists);
        if (component instanceof IkasanFlowBeskpokeComponent) {
            ((IkasanFlowBeskpokeComponent)component).setOverrideEnabled(false);
        }
        component.getViewHandler().setPsiJavaFile(newFile);
    }

    public static String generateContents(IkasanFlowComponent ikasanFlowComponent) {
        String templateName = ikasanFlowComponent.getType().getElementCategory().toString() + "_Template.vm";
        Map<String, Object> configs = getBasicTemplateConfigs();

        configs.put(COMPONENT_TAG, ikasanFlowComponent);
        String templateString = VelocityUtils.generateFromTemplate(templateName, configs);
        return templateString;
    }
}
