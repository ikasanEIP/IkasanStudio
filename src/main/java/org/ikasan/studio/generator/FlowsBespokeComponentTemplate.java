package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJavaFile;
import org.ikasan.studio.model.ikasan.*;

import java.util.Map;

public class FlowsBespokeComponentTemplate extends Generator {

    public static void create(final Project project, final IkasanModule ikasanModule, final IkasanFlow ikasanFlow) {
        for (IkasanFlowComponent component : ikasanFlow.getFlowComponentList()) {
            if (component.hasUserImplementedClass()) {
                FlowsBespokePropertyTemplate.create(project, ikasanModule, ikasanFlow, component);
            }

            if (component instanceof IkasanFlowBeskpokeComponent && ((IkasanFlowBeskpokeComponent)component).isOverrideEnabled()) {
                String clazzName = (String)component.getProperty(IkasanComponentPropertyMeta.BESPOKE_CLASS_NAME).getValue();
                String newPackageName = GeneratorUtils.getBespokePackageName(ikasanModule, ikasanFlow);
                String templateString = generateContents(newPackageName, component);
                boolean overwriteClassIfExists = ((IkasanFlowBeskpokeComponent)component).isOverrideEnabled();
                PsiJavaFile newFile = createTemplateFile(project, newPackageName, clazzName, templateString, true, overwriteClassIfExists);
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
