package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJavaFile;
import org.ikasan.studio.model.ikasan.instance.FlowBeskpokeElement;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.model.ikasan.meta.ComponentPropertyMeta;

import java.util.Map;

/**
 * Template to create the bespoke classes i.e. those with property for
 *      'BespokeClassName' e.g. Filter, Converter
 *      'Configuration'
 * The type of template used depends on the component hence each component can have at most 1 BespokeClassName
 */
public class FlowsBespokeComponentTemplate extends Generator {

    public static void create(final Project project, final Module ikasanModule, final Flow ikasanFlow) {
        for (FlowElement component : ikasanFlow.getFlowElements()) {
            if (component.hasUserImplementedClass()) {
                FlowsBespokePropertyTemplate.create(project, ikasanModule, ikasanFlow, component);
            }

            if (component instanceof FlowBeskpokeElement && ((FlowBeskpokeElement)component).isOverrideEnabled()) {
                String clazzName = (String)component.getProperty(ComponentPropertyMeta.BESPOKE_CLASS_NAME).getValue();
                createSourceFile(clazzName, component, project, ikasanModule, ikasanFlow);
//                String newPackageName = GeneratorUtils.getBespokePackageName(ikasanModule, ikasanFlow);
//                String templateString = generateContents(newPackageName, component);
//                boolean overwriteClassIfExists = ((IkasanFlowBeskpokeComponent)component).isOverrideEnabled();
//                PsiJavaFile newFile = createJavaSourceFile(project, newPackageName, clazzName, templateString, true, overwriteClassIfExists);
//                ((IkasanFlowBeskpokeComponent)component).setOverrideEnabled(false);
//
//                component.getViewHandler().setPsiJavaFile(newFile);
            }

            if (component.getProperty(ComponentPropertyMeta.CONFIGURATION) != null) {

            }

        }
    }

    private static void createSourceFile(String newClassName, FlowElement component, Project project, Module ikasanModule, Flow ikasanFlow) {
        String newPackageName = GeneratorUtils.getBespokePackageName(ikasanModule, ikasanFlow);
        String templateString = generateContents(newPackageName, component);
        boolean overwriteClassIfExists = ((FlowBeskpokeElement)component).isOverrideEnabled();
        PsiJavaFile newFile = createJavaSourceFile(project, newPackageName, newClassName, templateString, true, overwriteClassIfExists);
        ((FlowBeskpokeElement)component).setOverrideEnabled(false);

        component.getViewHandler().setPsiJavaFile(newFile);
    }

    protected static String generateContents(String packageName, FlowElement ikasanFlowComponent) {
        String templateName = ikasanFlowComponent.getComponentMeta().getName().toLowerCase() + "Template.ftl";
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, packageName);
        configs.put(COMPONENT_TAG, ikasanFlowComponent);
        return FreemarkerUtils.generateFromTemplate(templateName, configs);
    }
}
