package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJavaFile;
import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.FlowUserImplementedElement;
import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.ikasan.meta.ComponentPropertyMeta;

import java.util.Map;

/**
 * Template to create the user implemented classes (stubs) i.e. those with property for
 *      'userImplementedClassName' e.g. Filter, Converter
 * The type of template used depends on the component hence each component can have at most 1 userImplementedClassName
 */
public class FlowsUserImplementedComponentTemplate extends Generator {

    public static void create(final Project project, final Module ikasanModule, final Flow ikasanFlow) {
        for (FlowElement component : ikasanFlow.ftlGetConsumerAndFlowElements()) {
            if (component.hasUserSuppliedClass()) {
                FlowsUserImplementedClassPropertyTemplate.create(project, ikasanModule, ikasanFlow, component);
            }

            if (component instanceof FlowUserImplementedElement && ((FlowUserImplementedElement)component).isOverwriteEnabled()) {
                String clazzName = (String)component.getProperty(ComponentPropertyMeta.USER_IMPLEMENTED_CLASS_NAME).getValue();
                createSourceFile(clazzName, component, project, ikasanModule, ikasanFlow);
            }
        }
    }

    private static void createSourceFile(String newClassName, FlowElement component, Project project, Module ikasanModule, Flow ikasanFlow) {
        String newPackageName = GeneratorUtils.getUserImplementedClassesPackageName(ikasanModule, ikasanFlow);
        String templateString = generateContents(newPackageName, component);
        boolean overwriteClassIfExists = ((FlowUserImplementedElement)component).isOverwriteEnabled();
        PsiJavaFile newFile = createJavaSourceFile(project, newPackageName, newClassName, templateString, true, overwriteClassIfExists);
        ((FlowUserImplementedElement)component).setOverwriteEnabled(false);

        component.getViewHandler().setPsiJavaFile(newFile);
    }

    protected static String generateContents(String packageName, FlowElement flowElement) {
        String templateName = flowElement.getProperty("userImplementedClassName").getMeta().getUserImplementClassFtlTemplate();
        Map<String, Object> configs = getBasicTemplateConfigs();
        configs.put(STUDIO_PACKAGE_TAG, packageName);
        configs.put(FLOW_ELEMENT_TAG, flowElement);
        return FreemarkerUtils.generateFromTemplate(templateName, configs);
    }
}
