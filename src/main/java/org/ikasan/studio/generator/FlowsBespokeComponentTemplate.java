package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJavaFile;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.model.ikasan.instance.IkasanFlowBeskpokeElement;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta;

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

            if (component instanceof IkasanFlowBeskpokeElement && ((IkasanFlowBeskpokeElement)component).isOverrideEnabled()) {
                String clazzName = (String)component.getProperty(IkasanComponentPropertyMeta.BESPOKE_CLASS_NAME).getValue();
                createSourceFile(clazzName, component, project, ikasanModule, ikasanFlow);
//                String newPackageName = GeneratorUtils.getBespokePackageName(ikasanModule, ikasanFlow);
//                String templateString = generateContents(newPackageName, component);
//                boolean overwriteClassIfExists = ((IkasanFlowBeskpokeComponent)component).isOverrideEnabled();
//                PsiJavaFile newFile = createJavaSourceFile(project, newPackageName, clazzName, templateString, true, overwriteClassIfExists);
//                ((IkasanFlowBeskpokeComponent)component).setOverrideEnabled(false);
//
//                component.getViewHandler().setPsiJavaFile(newFile);
            }

            if (component.getProperty(IkasanComponentPropertyMeta.CONFIGURATION) != null) {

            }

        }
    }

    private static void createSourceFile(String newClassName, FlowElement component, Project project, Module ikasanModule, Flow ikasanFlow) {
        String clazzName = newClassName;
        String newPackageName = GeneratorUtils.getBespokePackageName(ikasanModule, ikasanFlow);
        String templateString = generateContents(newPackageName, component);
        boolean overwriteClassIfExists = ((IkasanFlowBeskpokeElement)component).isOverrideEnabled();
        PsiJavaFile newFile = createJavaSourceFile(project, newPackageName, clazzName, templateString, true, overwriteClassIfExists);
        ((IkasanFlowBeskpokeElement)component).setOverrideEnabled(false);

        component.getViewHandler().setPsiJavaFile(newFile);
    }

    protected static String generateContents(String packageName, FlowElement ikasanFlowComponent) {
        String templateName = ikasanFlowComponent.getIkasanComponentMeta().getName().toString().toLowerCase() + "Template.ftl";
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, packageName);
        configs.put(COMPONENT_TAG, ikasanFlowComponent);
        return FreemarkerUtils.generateFromTemplate(templateName, configs);
    }
}
