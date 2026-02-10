package org.ikasan.studio.ui.model.psi;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.StudioPsiUtils;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandlerIntellij;
import org.ikasan.studio.ui.viewmodel.IkasanFlowComponentViewHandler;
import org.ikasan.studio.ui.viewmodel.IkasanFlowViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.ikasan.studio.core.generator.FlowsComponentFactoryTemplate.COMPONENT_FACTORY_CLASS_NAME;
import static org.ikasan.studio.ui.StudioUIUtils.displayIdeaWarnMessage;
import static org.ikasan.studio.ui.model.StudioPsiUtils.createJsonModelFile;

/**
 * Encapsulates the Intellij representation of the ikasan Module
 * The idea is to keep the ikasan Module clean of any Initellij specific details, this module will inspect the
 * code to generate the ikasan Module and update the code to reflect changes to the ikasan Module.
 */
public class PIPSIIkasanModel {
    private static final Logger LOG = Logger.getInstance("#PIPSIIkasanModel");
    private final Project project;

    /**
     * Plugin PSI (Program Structure Interface) Iksanan Model builder
     * @param project is the Intellij project instance
     *                   memory for multiple open projects, so each plugin IkasanModule virtualization needs to be keyed
     *                   by the project name. Hence, project is passed around most classes.
     */
    public PIPSIIkasanModel(final Project project) {
        this.project = project;

    }

    /**
     * An update has been made to the diagram, so we need to reflect this into the code.
     */
    public void asynchGenerateSourceFromModelJsonInstanceAndSaveToDisk() {
        AtomicReference<Boolean> pomDependenciesHaveChanged = new AtomicReference<>();
        UiContext uiContext = project.getService(UiContext.class);
        Module module = uiContext.getIkasanModule();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // 1. Determine if the pom needs to be updated
            IkasanPomModel ikasanPomModel = uiContext.getIkasanPomModel();        // Not on EDT
            if (ikasanPomModel.isNewDependency(module.getAllUniqueSortedJarDependencies())) {
                pomDependenciesHaveChanged.set(true);
            } else {
                pomDependenciesHaveChanged.set(false);
            }

            // ProjectManager.getInstance().reloadProject(uiContext.getProject(project))

            LOG.info("STUDIO: Start ApplicationManager.getApplication().runWriteAction - source from model");
            LOG.debug(uiContext.getIkasanModule().toString());

            // 2. Re-generate and save all the source code. @TODO going forward we only want to regenerate if its changed.
            // Switch to UI thread for write action and undo block
            ApplicationManager.getApplication().invokeLater(() -> {
                // Using the command  processor adds support for undo
                CommandProcessor.getInstance().executeCommand(
                    project,
                    () -> {
                        if (pomDependenciesHaveChanged.get()) {
                            // We have checked the in-memory model, below will also verify from the on-disk model.
                            StudioPsiUtils.checkForDependencyChangesAndSaveIfChanged(project, module.getAllUniqueSortedJarDependencies());
                        }
                        //@todo start making below conditional on state changed.
                        Long transactionTimeStamp = uiContext.getProjectRefreshTimestamp();
                        saveApplication(project, module);
                        saveFlow(project, module);
                        generateAndSaveJavaCodeModuleConfig(project, module);
                        generateAndSavePropertiesConfig(project, module);
                        if (!transactionTimeStamp.equals(uiContext.getProjectRefreshTimestamp())) {
                            displayIdeaWarnMessage(project, "Intellij has changed the project part way through the save, consider resaving");
                        }
                        LOG.info("STUDIO: End ApplicationManager.getApplication().runWriteAction - source from model");
                    },
                    "Generate Source from Flow Diagram",
                    "Undo group ID");
            });
        });
    }


    /**
     * Take the Model from memory and persist it to disk
     */
    public void saveModelJsonToDisk() {
        UiContext uiContext = project.getService(UiContext.class);
        String templateString = ModelTemplate.create(uiContext.getIkasanModule());
        // Using the command processor add support for undo
        CommandProcessor.getInstance().executeCommand(
            project,
            () -> ApplicationManager.getApplication().runWriteAction(
                    () -> {
                        LOG.info("STUDIO: Start ApplicationManager.getApplication().runWriteAction - json from model");

                        createJsonModelFile(project, templateString);
                        LOG.info("STUDIO: End ApplicationManager.getApplication().runWriteAction - json from model");
                        LOG.debug("STUDIO: model now" + uiContext.getIkasanModule());
                    }),
            "Generate JSON from Flow Diagram",
            "Undo group ID");
    }

//    public static long timeLog(long startTime, String message) {
//
//        if (startTime == 0) {
//            startTime = System.currentTimeMillis();
//            LOG.warn("STUDIO: time log START- " + message);
//            return startTime;
//        } else {
//            long endNow = java.lang.System.currentTimeMillis();
//            long diff = endNow - startTime;
//            LOG.warn("STUDIO: time log END - " + message + " - " + diff);
//            return 0;
//        }
//    }

    /**
     * Save the Spring Boot Application class
     * @param project is the Intellij project instance
     * @param module for this code
     */
    private void saveApplication(Project project, Module module) {
        // The H2 Launcher
        // @TODO this only needs to be done once.
        String h2StartStopPomString  = null;
        try {
            h2StartStopPomString = H2StartStopTemplate.create();
        } catch (StudioGeneratorException e) {
            displayIdeaWarnMessage(project, "An error has occurred generating the h2StartStopPomString, attempting to continue. Error was " + e.getMessage());
        }

        if (h2StartStopPomString != null) {
            StudioPsiUtils.createPomFile(project, StudioPsiUtils.GENERATED_CONTENT_ROOT, "h2", h2StartStopPomString);
        }

        // The SpringBoot startup
        String applicationTemplateString  = null;
        try {
            applicationTemplateString = ApplicationTemplate.create(module);
        } catch (StudioGeneratorException e) {
            displayIdeaWarnMessage(project, "An error has occurred generating the applicationTemplate, attempting to continue. Error was " + e.getMessage());
        }
        if (applicationTemplateString != null) {
            StudioPsiUtils.createJavaSourceFile(project,
                    StudioPsiUtils.GENERATED_CONTENT_ROOT,
                    StudioPsiUtils.SRC_MAIN_JAVA_CODE,
                    ApplicationTemplate.STUDIO_BOOT_PACKAGE,
                    ApplicationTemplate.APPLICATION_CLASS_NAME, applicationTemplateString, null);
        }
    }

    private void saveFlow(Project project, Module module) {
        Set<String> flowPackageNames = new HashSet<>();
        for (Flow ikasanFlow : module.getFlows()) {

            String flowPackageName = Generator.STUDIO_FLOW_PACKAGE + "." + ikasanFlow.getJavaPackageName();
            flowPackageNames.add(ikasanFlow.getJavaPackageName());
            // Component Factory java file
            generateAndSaveJavaCodeIkasanComponentFactory(project, module, flowPackageName, ikasanFlow);
            generateAndSaveJavaCodeIkasanFlow(project, module, flowPackageName, ikasanFlow);
            generateAndSaveUserImplementClassStubsForFlow(project, module, ikasanFlow);
        }
        // we have the flowPackageNames that ARE valid
        LOG.warn("STUDIO: WARNING: this feature was disabled temporarily to support a tight deadline for demo");
//        ApplicationManager.getApplication().executeOnPooledThread(() -> {
//            StudioPsiUtils.deleteSubPackagesNotIn(project, StudioPsiUtils.GENERATED_CONTENT_ROOT, Generator.STUDIO_FLOW_PACKAGE, flowPackageNames);
//        });

    }

    private void generateAndSaveUserImplementClassStubsForFlow(Project project, Module module, Flow ikasanFlow) {
        if (!ikasanFlow.getFlowRoute().getConsumerAndFlowRouteElements().isEmpty()) {
            // Must do User Implemented class stubs first otherwise resolution will not auto generate imports.
            for (FlowElement component : ikasanFlow.getFlowRoute().getConsumerAndFlowRouteElements()) {
                IkasanFlowComponentViewHandler componentViewHandler = ViewHandlerCache.getFlowComponentViewHandler(project, component);
                if (component.hasUserSuppliedClass()) {
                    for (ComponentProperty property : component.getUserSuppliedClassProperties()) {
                        String newPackageName = GeneratorUtils.getUserImplementedClassesPackageName(module, ikasanFlow);
                        String clazzName = StudioBuildUtils.toJavaClassName(property.getValueString());
                        String prefix = GeneratorUtils.getUniquePrefix(module, ikasanFlow, component);
                        String templateString = null;
                        try {
                            templateString = FlowsUserImplementedClassPropertyTemplate.create(property, newPackageName,clazzName, prefix);
                        } catch (StudioGeneratorException e) {
                            displayIdeaWarnMessage(project, "An error has occurred, attempting to continue. Error was " + e.getMessage());
                        }
                        if (templateString != null) {
                            StudioPsiUtils.createJavaSourceFile(project, StudioPsiUtils.GENERATED_CONTENT_ROOT, StudioPsiUtils.SRC_MAIN_JAVA_CODE,
                                    newPackageName, clazzName, templateString, componentViewHandler);
                        }
                    }
                }

                if (    component instanceof FlowUserImplementedElement &&
                        (((FlowUserImplementedElement)component).isOverwriteEnabled() || component.getComponentMeta().isDebug())) {
                    String newClassName = (String)component.getProperty(ComponentPropertyMeta.USER_IMPLEMENTED_CLASS_NAME).getValue();
                    String newPackageName = GeneratorUtils.getUserImplementedClassesPackageName(module, ikasanFlow);
                    String templateString = null;
                    try {
                        templateString = FlowsUserImplementedComponentTemplate.create(newPackageName, module, ikasanFlow, component);
                    } catch (StudioGeneratorException e) {
                        displayIdeaWarnMessage(project, "An error has occurred, attempting to continue. Error was " + e.getMessage());
                    }
                    if (templateString != null) {
                        ((FlowUserImplementedElement)component).isOverwriteEnabled();
                        StudioPsiUtils.createJavaSourceFile(project, StudioPsiUtils.USER_CONTENT_ROOT, StudioPsiUtils.SRC_MAIN_JAVA_CODE,
                                newPackageName, newClassName, templateString, componentViewHandler);
                        ((FlowUserImplementedElement)component).setOverwriteEnabled(false);
                    }
                }
            }
        }
    }

    private void generateAndSaveJavaCodeIkasanComponentFactory(Project project, Module module, String flowPackageName, Flow ikasanFlow) {
        String componentFactoryTemplateString = null;
        try {
            componentFactoryTemplateString = FlowsComponentFactoryTemplate.create(flowPackageName, module, ikasanFlow);
        } catch (StudioGeneratorException e) {
            displayIdeaWarnMessage(project, "An error has occurred, attempting to continue. Error was " + e.getMessage());
        }
        if (componentFactoryTemplateString != null) {
            StudioPsiUtils.createJavaSourceFile(project, StudioPsiUtils.GENERATED_CONTENT_ROOT, StudioPsiUtils.SRC_MAIN_JAVA_CODE, flowPackageName,
                    COMPONENT_FACTORY_CLASS_NAME + ikasanFlow.getJavaClassName(), componentFactoryTemplateString, null);
        }
    }

    private void generateAndSaveJavaCodeIkasanFlow(Project project, Module module, String flowPackageName, Flow ikasanFlow) {
        IkasanFlowViewHandler flowViewHandler = ViewHandlerCache.getFlowViewHandler(project, ikasanFlow);
        String flowTemplateString = null;
        try {
            flowTemplateString = FlowTemplate.create(flowPackageName, module, ikasanFlow);
        } catch (StudioGeneratorException e) {
            displayIdeaWarnMessage(project, "An error has occurred, attempting to continue. Error was " + e.getMessage());
        }
        if (flowTemplateString != null) {
            StudioPsiUtils.createJavaSourceFile(
                    project,
                    StudioPsiUtils.GENERATED_CONTENT_ROOT,
                    StudioPsiUtils.SRC_MAIN_JAVA_CODE,
                    flowPackageName,
                    ikasanFlow.getJavaClassName(),
                    flowTemplateString,
                    flowViewHandler);
        }
    }

    private void generateAndSaveJavaCodeModuleConfig(Project project, Module module) {
        String templateString = null;
        try {
            templateString = ModuleConfigTemplate.create(module);
        } catch (StudioGeneratorException e) {
            displayIdeaWarnMessage(project, "An error has occurred, attempting to continue. Error was " + e.getMessage());
        }
        if (templateString != null) {
            AbstractViewHandlerIntellij viewHandler = ViewHandlerCache.getAbstractViewHandler(project, module);
            StudioPsiUtils.createJavaSourceFile(project, StudioPsiUtils.GENERATED_CONTENT_ROOT, StudioPsiUtils.SRC_MAIN_JAVA_CODE,
                    ModuleConfigTemplate.STUDIO_BOOT_PACKAGE, ModuleConfigTemplate.MODULE_CLASS_NAME, templateString, viewHandler);
        }

    }

    public static final String MODULE_PROPERTIES_FILENAME_WITH_EXTENSION = "application.properties";
    private void generateAndSavePropertiesConfig(Project project, Module module) {
        String templateString = null;
        try {
            templateString = PropertiesTemplate.create(module);
            Map<String, String> applicationProperties = StudioBuildUtils.convertStringToMap(templateString);
            project.getService(UiContext.class).setApplicationProperties(applicationProperties);
        } catch (StudioGeneratorException e) {
            displayIdeaWarnMessage(project, "An error has occurred, attempting to continue. Error was " + e.getMessage());
        }
        if (templateString != null) {
//            StudioPsiUtils.createFile(project, StudioPsiUtils.GENERATED_CONTENT_ROOT, StudioPsiUtils.SRC_MAIN_RESOURCES, null, MODULE_PROPERTIES_FILENAME_WITH_EXTENSION, templateString, false);
            StudioPsiUtils.createPropertiesFile(project, templateString);
        }
    }
}
