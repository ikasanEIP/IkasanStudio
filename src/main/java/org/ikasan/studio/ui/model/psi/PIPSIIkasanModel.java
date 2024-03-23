package org.ikasan.studio.ui.model.psi;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.PsiJavaFile;
import org.apache.maven.model.Dependency;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.StudioPsiUtils;

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

    protected Project getProject() {
        return UiContext.getProject(projectKey);
    }


    /**
     * An update has been made to the diagram, so we need to reflect this into the code.
     * @param checkForJarChanges Set to tru iIf the caller has performed an action that could affect (add/remove) depeendant jars
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
    public void generateSourceFromModelInstance2(Boolean dependenciesHaveChanged) {
        Project project = UiContext.getProject(projectKey);
        Module module = UiContext.getIkasanModule(project.getName());
        CommandProcessor.getInstance().executeCommand(
                project,
                () -> ApplicationManager.getApplication().runWriteAction(
                        () -> {
                            LOG.info("Start ApplicationManager.getApplication().runWriteAction - source from model" + UiContext.getIkasanModule(projectKey));
                            if (dependenciesHaveChanged) {
                                // We have checked the in-memory model, below will also verify from the on-disk model.
                                StudioPsiUtils.checkForDependencyChangesAndSaveIfChanged(projectKey, module.getAllUniqueSortedJarDependencies());
                            }
                            //@todo start making below conditional on state changed.
                            saveApplication(project, module);
                            saveFlow(project, module);
                            saveModuleConfig(project, module);
                            savePropertiesConfig(project, module);

                            LOG.info("End ApplicationManager.getApplication().runWriteAction - source from model");
                        }),
                "Generate Source from Flow Diagram",
                "Undo group ID");
        ApplicationManager.getApplication().runReadAction(
                () -> {
                    LOG.info("ApplicationManager.getApplication().runReadAction");
                    // reloadProject needed to re-read POM, must not be done till addDependancies
                    // fully complete, hence in next executeCommand block
                    if (dependenciesHaveChanged && UiContext.getOptions(projectKey).isAutoReloadMavenEnabled()) {
//                    if (newDependencies != null && !newDependencies.isEmpty() && UiContext.getOptions(projectKey).isAutoReloadMavenEnabled()) {
                        ProjectManager.getInstance().reloadProject(project);
                    }
                    LOG.info("End ApplicationManager.getApplication().runReadAction");
                });
    }

    /**
     * An update has been made to the diagram, so we need to reverse this into the code.
     */
    public void generateSourceFromModelInstance(Set<Dependency> newDependencies) {
        Project project = UiContext.getProject(projectKey);
        CommandProcessor.getInstance().executeCommand(
                project,
                () -> ApplicationManager.getApplication().runWriteAction(
                        () -> {
                            LOG.info("Start ApplicationManager.getApplication().runWriteAction - source from model" + UiContext.getIkasanModule(projectKey));
                            StudioPsiUtils.checkForDependencyChangesAndSaveIfChanged(projectKey, newDependencies);
                            //@todo start making below conditional on state changed.
                            Module module = UiContext.getIkasanModule(project.getName());
                            saveApplication(project, module);
                            saveFlow(project, module);
                            saveModuleConfig(project, module);
                            savePropertiesConfig(project, module);

                            LOG.info("End ApplicationManager.getApplication().runWriteAction - source from model");
                        }),
                "Generate Source from Flow Diagram",
                "Undo group ID");
        ApplicationManager.getApplication().runReadAction(
                () -> {
                    LOG.info("ApplicationManager.getApplication().runReadAction");
                    // reloadProject needed to re-read POM, must not be done till addDependancies
                    // fully complete, hence in next executeCommand block
                    if (newDependencies != null && !newDependencies.isEmpty() && UiContext.getOptions(projectKey).isAutoReloadMavenEnabled()) {
                        ProjectManager.getInstance().reloadProject(project);
                    }
                    LOG.info("End ApplicationManager.getApplication().runReadAction");
                });
    }

    public void generateJsonFromModelInstance() {
        Project project = UiContext.getProject(projectKey);
        CommandProcessor.getInstance().executeCommand(
                project,
                () -> ApplicationManager.getApplication().runWriteAction(
                        () -> {
                            LOG.info("Start ApplicationManager.getApplication().runWriteAction - json from model");
                            String templateString = ModelTemplate.create(UiContext.getIkasanModule(project.getName()));
                            createJsonModelFile(project, templateString);
                            LOG.info("End ApplicationManager.getApplication().runWriteAction - json from model");
                            LOG.info("model now" + UiContext.getIkasanModule(projectKey));
                        }),
                "Generate JSON from Flow Diagram",
                "Undo group ID");
    }

    private void saveApplication(Project project, Module module) {
        String templateString  = ApplicationTemplate.create(module);
        StudioPsiUtils.createJavaSourceFile(project, ApplicationTemplate.STUDIO_BOOT_PACKAGE, ApplicationTemplate.APPLICATION_CLASS_NAME, templateString, true, true);
    }

    private void saveFlow(Project project, Module module) {
        for (Flow ikasanFlow : module.getFlows()) {
            String packageName = Generator.STUDIO_BOOT_PACKAGE + "." + ikasanFlow.getJavaPackageName();
            if (!ikasanFlow.ftlGetConsumerAndFlowElements().isEmpty()) {
                // Must do User Implemented class stubs first otherwise resolution will not auto generate imports.
                for (FlowElement component : ikasanFlow.ftlGetConsumerAndFlowElements()) {
                    if (component.hasUserSuppliedClass()) {
                        for (ComponentProperty property : component.getUserSuppliedClassProperties()) {
                            String newPackageName = GeneratorUtils.getUserImplementedClassesPackageName(module, ikasanFlow);
                            String clazzName = StudioBuildUtils.toJavaClassName(property.getValueString());
                            String prefix = GeneratorUtils.getUniquePrefix(module, ikasanFlow, component);
                            String templateString = FlowsUserImplementedClassPropertyTemplate.create(property, newPackageName,clazzName, prefix);
                            StudioPsiUtils.createJavaSourceFile(project, newPackageName, clazzName, templateString, true, true);
                        }
                    }

                    if (component instanceof FlowUserImplementedElement && ((FlowUserImplementedElement)component).isOverwriteEnabled()) {
                        String newClassName = (String)component.getProperty(ComponentPropertyMeta.USER_IMPLEMENTED_CLASS_NAME).getValue();
                        String newPackageName = GeneratorUtils.getUserImplementedClassesPackageName(module, ikasanFlow);
                        String templateString = FlowsUserImplementedComponentTemplate.create(newPackageName, component);
                        boolean overwriteClassIfExists = ((FlowUserImplementedElement)component).isOverwriteEnabled();
                        PsiJavaFile newFile = StudioPsiUtils.createJavaSourceFile(project, newPackageName, newClassName, templateString, true, overwriteClassIfExists);
                        ((FlowUserImplementedElement)component).setOverwriteEnabled(false);
                        //        component.getViewHandler().setPsiJavaFile(newFile);
                    }
                }
            }
            String templateString = FlowsComponentFactoryTemplate.create(packageName, module, ikasanFlow);
            PsiJavaFile newFile = StudioPsiUtils.createJavaSourceFile(project, packageName, COMPONENT_FACTORY_CLASS_NAME + ikasanFlow.getJavaClassName(), templateString, true, true);
            // @TODO save the newFile for hotlink
            templateString = FlowTemplate.create(module, ikasanFlow, packageName);
            newFile = StudioPsiUtils.createJavaSourceFile(
                    project,
                    packageName,
                    ikasanFlow.getJavaClassName(),
                    templateString,
                    true,
                    true);
            // @TODO save the newFile for hotlink
//            ikasanFlow.getViewHandler().setPsiJavaFile(newFile);
        }
    }

    private void saveModuleConfig(Project project, Module module) {
        String templateString = ModuleConfigTemplate.create(module);
        PsiJavaFile newFile = StudioPsiUtils.createJavaSourceFile(project, ModuleConfigTemplate.STUDIO_BOOT_PACKAGE, ModuleConfigTemplate.MODULE_CLASS_NAME, templateString, true, true);
        // @TODO save the newFile for hotlink
//        ikasanModule.getViewHandler().setPsiJavaFile(newFile);
    }

    public static final String MODULE_PROPERTIES_FILENAME_WITH_EXTENSION = "application.properties";
    private void savePropertiesConfig(Project project, Module module) {
        String templateString = PropertiesTemplate.create(module);
        StudioPsiUtils.createResourceFile(project, null, MODULE_PROPERTIES_FILENAME_WITH_EXTENSION, templateString, false);
    }
}
