package org.ikasan.studio.ui.model.psi;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.PsiJavaFile;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.StudioPsiUtils;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandlerIntellij;
import org.ikasan.studio.ui.viewmodel.IkasanFlowViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandlerFactoryIntellij;

import java.util.HashSet;
import java.util.Set;

import static org.ikasan.studio.core.generator.FlowsComponentFactoryTemplate.COMPONENT_FACTORY_CLASS_NAME;
import static org.ikasan.studio.ui.model.StudioPsiUtils.createJsonModelFile;

/**
 * Encapsulates the Intellij representation of the ikasan Module
 * The idea is to keep the ikasan Module clean of any Initellij specific details, this module will inspect the
 * code to generate the ikasan Module and update the code to reflect changes to the ikasan Module.
 */
public class PIPSIIkasanModel {
    private static final Logger LOG = Logger.getInstance("#PIPSIIkasanModel");
    private final String projectKey ;

    /**
     * Plugin PSI (Program Structure Interface) Iksanan Model builder
     * @param projectKey is the key to the cache shared by all components of the model. Note that Intellij shares the
     *                   memory for multiple open projects, so each plugin IkasanModule virtualization needs to be keyed
     *                   by the project name. Hence, projectKey is passed around most classes.
     */
    public PIPSIIkasanModel(final String projectKey) {
        this.projectKey = projectKey;

    }
//
//    protected Project getProject() {
//        return UiContext.getProject(projectKey);
//    }


    /**
     * An update has been made to the diagram, so we need to reflect this into the code.
     */
    public void generateSourceFromModelInstance3() {
        Boolean dependenciesHaveChanged = false;
        Project project = UiContext.getProject(projectKey);
        Module module = UiContext.getIkasanModule(project.getName());
        IkasanPomModel ikasanPomModel = UiContext.getIkasanPomModel(projectKey);
        if (!ikasanPomModel.hasDependency(module.getAllUniqueSortedJarDependencies())) {
            dependenciesHaveChanged = true;
        }
        generateSourceFromModelInstance2(dependenciesHaveChanged);
    }

    /**
     * An update has been made to the diagram, so we need to reflect this into the code.
     * @param dependenciesHaveChanged Set to tru iIf the caller has performed an action that could affect (add/remove) depeendant jars
     */
    private void generateSourceFromModelInstance2(Boolean dependenciesHaveChanged) {
        Project project = UiContext.getProject(projectKey);
        Module module = UiContext.getIkasanModule(project.getName());
        CommandProcessor.getInstance().executeCommand(
                project,
                () -> ApplicationManager.getApplication().runWriteAction(
                        () -> {
                            LOG.info("STUDIO: Start ApplicationManager.getApplication().runWriteAction - source from model");
                            LOG.debug(UiContext.getIkasanModule(projectKey).toString());
                            if (dependenciesHaveChanged) {
                                // We have checked the in-memory model, below will also verify from the on-disk model.
                                StudioPsiUtils.checkForDependencyChangesAndSaveIfChanged(projectKey, module.getAllUniqueSortedJarDependencies());
                            }
                            //@todo start making below conditional on state changed.
                            saveApplication(project, module);
                            saveFlow(project, module);
                            saveModuleConfig(project, module);
                            savePropertiesConfig(project, module);

                            LOG.info("STUDIO: End ApplicationManager.getApplication().runWriteAction - source from model");
                        }),
                "Generate Source from Flow Diagram",
                "Undo group ID");
        ApplicationManager.getApplication().runReadAction(
                () -> {
                    LOG.info("STUDIO: ApplicationManager.getApplication().runReadAction");
                    // reloadProject needed to re-read POM, must not be done till add Dependancies
                    // fully complete, hence in next executeCommand block
                    if (dependenciesHaveChanged && UiContext.getOptions(projectKey).isAutoReloadMavenEnabled()) {
                        ProjectManager.getInstance().reloadProject(project);
                    }
                    LOG.info("STUDIO: End ApplicationManager.getApplication().runReadAction");
                });
    }


    /**
     * Take the Model from memory and persist it to disk
     */
    public void generateJsonFromModelInstance() {
        Project project = UiContext.getProject(projectKey);
        CommandProcessor.getInstance().executeCommand(
                project,
                () -> ApplicationManager.getApplication().runWriteAction(
                        () -> {
                            LOG.info("STUDIO: Start ApplicationManager.getApplication().runWriteAction - json from model");
                            String templateString = ModelTemplate.create(UiContext.getIkasanModule(project.getName()));
                            createJsonModelFile(project, StudioPsiUtils.GENERATED_CONTENT_ROOT, templateString);
                            LOG.info("STUDIO: End ApplicationManager.getApplication().runWriteAction - json from model");
                            LOG.debug("STUDIO: model now" + UiContext.getIkasanModule(projectKey));
                        }),
                "Generate JSON from Flow Diagram",
                "Undo group ID");
    }

    /**
     * Save the Spring Boot Application class
     * @param project to key by
     * @param module for this code
     */
    private void saveApplication(Project project, Module module) {
        String templateString  = ApplicationTemplate.create(module);
        StudioPsiUtils.createJavaSourceFile(project, StudioPsiUtils.GENERATED_CONTENT_ROOT, ApplicationTemplate.STUDIO_BOOT_PACKAGE, ApplicationTemplate.APPLICATION_CLASS_NAME, templateString, true, true);
    }

    private void saveFlow(Project project, Module module) {
        Set<String> flowPackageNames = new HashSet<>();
        for (Flow ikasanFlow : module.getFlows()) {

            IkasanFlowViewHandler viewHandler = ViewHandlerFactoryIntellij.getOrCreateFlowViewHandler(projectKey, ikasanFlow);

            String flowPackageName = Generator.STUDIO_FLOW_PACKAGE + "." + ikasanFlow.getJavaPackageName();
            flowPackageNames.add(ikasanFlow.getJavaPackageName());
            if (!ikasanFlow.getFlowRoute().getConsumerAndFlowRouteElements().isEmpty()) {
                // Must do User Implemented class stubs first otherwise resolution will not auto generate imports.
                for (FlowElement component : ikasanFlow.getFlowRoute().getConsumerAndFlowRouteElements()) {
                    if (component.hasUserSuppliedClass()) {
                        for (ComponentProperty property : component.getUserSuppliedClassProperties()) {
                            String newPackageName = GeneratorUtils.getUserImplementedClassesPackageName(module, ikasanFlow);
                            String clazzName = StudioBuildUtils.toJavaClassName(property.getValueString());
                            String prefix = GeneratorUtils.getUniquePrefix(module, ikasanFlow, component);
                            String templateString = FlowsUserImplementedClassPropertyTemplate.create(property, newPackageName,clazzName, prefix);
                            PsiJavaFile newFile = StudioPsiUtils.createJavaSourceFile(project, StudioPsiUtils.GENERATED_CONTENT_ROOT, newPackageName, clazzName, templateString, true, true);
                            if (viewHandler != null) {
                                viewHandler.setPsiJavaFile(newFile);
                            }
                        }
                    }

                    if (component instanceof FlowUserImplementedElement && ((FlowUserImplementedElement)component).isOverwriteEnabled()) {
                        String newClassName = (String)component.getProperty(ComponentPropertyMeta.USER_IMPLEMENTED_CLASS_NAME).getValue();
                        String newPackageName = GeneratorUtils.getUserImplementedClassesPackageName(module, ikasanFlow);
                        String templateString = FlowsUserImplementedComponentTemplate.create(newPackageName, component);
                        boolean overwriteClassIfExists = ((FlowUserImplementedElement)component).isOverwriteEnabled();
                        PsiJavaFile newFile = StudioPsiUtils.createJavaSourceFile(project, StudioPsiUtils.USER_CONTENT_ROOT, newPackageName, newClassName, templateString, true, overwriteClassIfExists);
                        ((FlowUserImplementedElement)component).setOverwriteEnabled(false);
                        if (viewHandler != null) {
                            viewHandler.setPsiJavaFile(newFile);
                        }
                    }
                }
            }
            // Component Factory java file
            String templateString = FlowsComponentFactoryTemplate.create(flowPackageName, module, ikasanFlow);
            StudioPsiUtils.createJavaSourceFile(project, StudioPsiUtils.GENERATED_CONTENT_ROOT, flowPackageName, COMPONENT_FACTORY_CLASS_NAME + ikasanFlow.getJavaClassName(), templateString, true, true);

            // Flow java file
            templateString = FlowTemplate.create(module, ikasanFlow, flowPackageName);
            PsiJavaFile newFile = StudioPsiUtils.createJavaSourceFile(
                    project,
                    StudioPsiUtils.GENERATED_CONTENT_ROOT,
                    flowPackageName,
                    ikasanFlow.getJavaClassName(),
                    templateString,
                    true,
                    true);
            if (viewHandler != null) {
                viewHandler.setPsiJavaFile(newFile);
            }
        }
        // we have the flowPackageNames that ARE valid
        StudioPsiUtils.deleteSubPackagesNotIn(project, StudioPsiUtils.GENERATED_CONTENT_ROOT, Generator.STUDIO_FLOW_PACKAGE, flowPackageNames);
    }

    private void saveModuleConfig(Project project, Module module) {
        String templateString = ModuleConfigTemplate.create(module);
        PsiJavaFile newFile = StudioPsiUtils.createJavaSourceFile(project, StudioPsiUtils.GENERATED_CONTENT_ROOT, ModuleConfigTemplate.STUDIO_BOOT_PACKAGE, ModuleConfigTemplate.MODULE_CLASS_NAME, templateString, true, true);

        AbstractViewHandlerIntellij viewHandler = ViewHandlerFactoryIntellij.getOrCreateAbstracttViewHandler(projectKey, module);
        if (viewHandler != null) {
            viewHandler.setPsiJavaFile(newFile);
        }
    }

    public static final String MODULE_PROPERTIES_FILENAME_WITH_EXTENSION = "application.properties";
    private void savePropertiesConfig(Project project, Module module) {
        String templateString = PropertiesTemplate.create(module);
        StudioPsiUtils.createResourceFile(project, StudioPsiUtils.GENERATED_CONTENT_ROOT, null, MODULE_PROPERTIES_FILENAME_WITH_EXTENSION, templateString, false);
    }
}
