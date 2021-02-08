package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJavaFile;
import org.ikasan.studio.model.Ikasan.IkasanComponentPropertyMeta;
import org.ikasan.studio.model.Ikasan.IkasanFlowComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * Currently both velocity and embedded strategy are being evaluated
 */
public class BespokeClassTemplate extends Generator {

    public static PsiJavaFile createClass(final Project project, final IkasanFlowComponent component) {
        String templateString = generateContents(component);
        String clazzName = (String)component.getProperty(IkasanComponentPropertyMeta.BESPOKE_CLASS_NAME).getValue();
        PsiJavaFile newFile = createTemplateFile(project, DEFAULT_STUDIO_PACKAGE, clazzName, templateString, true, false);
        return newFile;
    }

    public static String generateContents(IkasanFlowComponent ikasanFlowComponent) {
        String templateName = ikasanFlowComponent.getType().getElementCategory().toString() + "_Template.vm";
        Map<String, Object> configs = new HashMap<>();

        configs.put(COMPONENT_TAG, ikasanFlowComponent);
        String templateString = VelocityUtils.generateFromTemplate(templateName, configs);
        return templateString;
    }
}
