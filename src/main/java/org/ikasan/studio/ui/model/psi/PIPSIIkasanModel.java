package org.ikasan.studio.ui.model.psi;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.ui.StudioBundle;
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
                            StudioPsiUtils.checkForDependencyChangesAndSaveIfChanged(project, module.getAllUniqueSortedJarDependencies(), module.getMetaVersion());
                        }
                        //@todo start making below conditional on state changed.
                        Long transactionTimeStamp = uiContext.getProjectRefreshTimestamp();
                        saveApplication(project, module);
                        saveStudioInjectController(project, module);
                        saveFlow(project, module);
                        generateAndSaveJavaCodeModuleConfig(project, module);
                        generateAndSavePropertiesConfig(project, module);
                        if (!transactionTimeStamp.equals(uiContext.getProjectRefreshTimestamp())) {
                            displayIdeaWarnMessage(project, StudioBundle.message("message.IntellijHasChangedTheProjectPartWayThroughTheSave"));
                        }
                        LOG.info("STUDIO: End ApplicationManager.getApplication().runWriteAction - source from model");
                    },
                    StudioBundle.message("action.GenerateSourceFromFlowDiagram"),
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
            StudioBundle.message("action.GenerateJSONFromFlowDiagram"),
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
            h2StartStopPomString = H2StartStopTemplate.create(module.getMetaVersion());
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
            displayIdeaWarnMessage(project, StudioBundle.message("message.AnErrorHasOccurredGeneratingTheApplicationTemplate", e.getMessage()));
        }
        if (applicationTemplateString != null) {
            StudioPsiUtils.createJavaSourceFile(project,
                    StudioPsiUtils.GENERATED_CONTENT_ROOT,
                    StudioPsiUtils.SRC_MAIN_JAVA_CODE,
                    ApplicationTemplate.STUDIO_BOOT_PACKAGE,
                    ApplicationTemplate.APPLICATION_CLASS_NAME, applicationTemplateString, null);
        }
    }

    /**
     * Save the Debug-mode-only REST controller used to inject synthetic test events into a flow's Consumer.
     * @param project is the Intellij project instance
     * @param module for this code
     */
    private void saveStudioInjectController(Project project, Module module) {
        String studioInjectControllerTemplateString = null;
        try {
            studioInjectControllerTemplateString = StudioInjectControllerTemplate.create(module);
        } catch (StudioGeneratorException e) {
            displayIdeaWarnMessage(project, StudioBundle.message("message.AnErrorHasOccurredAttemptingToContinue", e.getMessage()));
        }
        if (studioInjectControllerTemplateString != null) {
            StudioPsiUtils.createJavaSourceFile(project,
                    StudioPsiUtils.GENERATED_CONTENT_ROOT,
                    StudioPsiUtils.SRC_MAIN_JAVA_CODE,
                    StudioInjectControllerTemplate.STUDIO_BOOT_PACKAGE,
                    StudioInjectControllerTemplate.STUDIO_INJECT_CONTROLLER_CLASS_NAME, studioInjectControllerTemplateString, null);
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
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                StudioPsiUtils.deleteSubPackagesNotIn(project, StudioPsiUtils.GENERATED_CONTENT_ROOT, Generator.STUDIO_FLOW_PACKAGE, flowPackageNames));

    }

    private void generateAndSaveUserImplementClassStubsForFlow(Project project, Module module, Flow ikasanFlow) {
        if (!ikasanFlow.getFlowRoute().getConsumerAndFlowRouteElements().isEmpty()) {
            // Must do User Implemented class stubs first otherwise resolution will not auto generate imports.
            for (FlowElement component : ikasanFlow.getFlowRoute().getConsumerAndFlowRouteElements()) {
                IkasanFlowComponentViewHandler componentViewHandler = ViewHandlerCache.getFlowComponentViewHandler(project, component);
                if (component.hasUserSuppliedClass()) {
                    for (ComponentProperty property : component.getUserSuppliedClassProperties()) {
                        if (property.getMeta().isNoStubRequired()) {
                            // Always an externally-injected bean (e.g. a JTA transaction manager) - userSuppliedClass is
                            // still needed for the @Resource bean-wiring in the generated factory, but no stub makes sense.
                            continue;
                        }
                        boolean protectFromOverwrite = property.getMeta().isProtectFromOverwrite();
                        if (protectFromOverwrite && !property.isOverwriteEnabled()) {
                            // Bespoke, user-owned stub that has already been generated once - leave the user's code untouched.
                            continue;
                        }
                        String newPackageName = GeneratorUtils.getUserImplementedClassesPackageName(module, ikasanFlow);
                        String clazzName = StudioBuildUtils.toJavaClassName(property.getValueString());
                        String prefix = GeneratorUtils.getUniquePrefix(module, ikasanFlow, component);
                        String templateString = null;
                        try {
                            templateString = FlowsUserImplementedClassPropertyTemplate.create(module.getMetaVersion(), property, newPackageName, clazzName, prefix);
                        } catch (StudioGeneratorException e) {
                            displayIdeaWarnMessage(project, StudioBundle.message("message.AnErrorHasOccurredAttemptingToContinue", e.getMessage()));
                        }
                        if (templateString != null) {
                            String contentRoot = protectFromOverwrite ? StudioPsiUtils.USER_CONTENT_ROOT : StudioPsiUtils.GENERATED_CONTENT_ROOT;
                            StudioPsiUtils.createJavaSourceFile(project, contentRoot, StudioPsiUtils.SRC_MAIN_JAVA_CODE,
                                    newPackageName, clazzName, templateString, componentViewHandler);
                            if (protectFromOverwrite) {
                                property.setOverwriteEnabled(false);
                            }
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
                        displayIdeaWarnMessage(project, StudioBundle.message("message.AnErrorHasOccurredAttemptingToContinue", e.getMessage()));
                    }
                    if (templateString != null) {
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
            displayIdeaWarnMessage(project, StudioBundle.message("message.AnErrorHasOccurredAttemptingToContinue", e.getMessage()));
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
            displayIdeaWarnMessage(project, StudioBundle.message("message.AnErrorHasOccurredAttemptingToContinue", e.getMessage()));
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
        setFlowComponentNavigationTargets(ikasanFlow, flowViewHandler.getPsiFile());
    }

    /**
     * Default "jump to code" target for a flow component: the component's reference within the containing flow's
     * generated Java file (e.g. the {@code "My Consumer"} literal in {@code .consumer("My Consumer", ...)}), located
     * by text offset so navigation lands on the component's own line rather than just the top of the flow class.
     * Components that generate their own user-editable class (see {@link #setUserImplementedClassNavigationTargets})
     * have this superseded by a more specific target.
     */
    private void setFlowComponentNavigationTargets(Flow ikasanFlow, PsiFile flowPsiFile) {
        if (flowPsiFile == null) {
            return;
        }
        String flowFileText = flowPsiFile.getText();
        int searchFromOffset = 0;
        for (FlowElement flowElement : ikasanFlow.getFlowElementsNoExternalEndPoints()) {
            IkasanFlowComponentViewHandler flowComponentViewHandler = ViewHandlerCache.getFlowComponentViewHandler(project, flowElement);
            if (flowComponentViewHandler != null) {
                flowComponentViewHandler.setPsiFile(flowPsiFile);
                String componentName = flowElement.getComponentName();
                if (componentName != null) {
                    int offset = flowFileText.indexOf("\"" + componentName + "\"", searchFromOffset);
                    if (offset >= 0) {
                        flowComponentViewHandler.setOffsetInclassToNavigateTo(offset);
                        searchFromOffset = offset + componentName.length();
                    }
                }
            }
        }
    }

    /**
     * Components such as Debug or CustomConverter generate their own user-editable class under the project's
     * {@code user/src/main/java} tree (see {@link #generateAndSaveUserImplementClassStubsForFlow}). "Jump to code"
     * for these should navigate to that class rather than to the flow file. The class name is a persisted property
     * ({@link ComponentPropertyMeta#USER_IMPLEMENTED_CLASS_NAME}) so, unlike the flow-file offset above, this target
     * can be reconstructed on project reload without needing to regenerate anything or store extra state.
     */
    private void setUserImplementedClassNavigationTargets(Module module, Flow ikasanFlow, VirtualFile projectBaseDir) {
        if (ikasanFlow.getFlowRoute() == null) {
            return;
        }
        for (FlowElement component : ikasanFlow.getFlowRoute().getConsumerAndFlowRouteElements()) {
            if (!(component instanceof FlowUserImplementedElement)) {
                continue;
            }
            IkasanFlowComponentViewHandler componentViewHandler = ViewHandlerCache.getFlowComponentViewHandler(project, component);
            if (componentViewHandler == null) {
                continue;
            }
            ComponentProperty classNameProperty = component.getProperty(ComponentPropertyMeta.USER_IMPLEMENTED_CLASS_NAME);
            String className = classNameProperty != null ? (String) classNameProperty.getValue() : null;
            if (className == null) {
                continue;
            }
            String packageName = GeneratorUtils.getUserImplementedClassesPackageName(module, ikasanFlow);
            String relPath = StudioPsiUtils.USER_CONTENT_ROOT.substring(1) + "/" +
                    StudioPsiUtils.SRC_MAIN_JAVA_CODE + "/" +
                    packageName.replace(".", "/") + "/" +
                    className + ".java";
            VirtualFile vFile = projectBaseDir.findFileByRelativePath(relPath);
            if (vFile == null || !vFile.isValid()) {
                continue;
            }
            PsiFile userClassPsiFile = PsiManager.getInstance(project).findFile(vFile);
            if (userClassPsiFile != null) {
                componentViewHandler.setPsiFile(userClassPsiFile);
            }
        }
    }

    /**
     * On project load there is no need to regenerate all source code, but the PSI file handles that enable
     * "jump to code" navigation must still be resolved. This method looks up the already-generated flow
     * Java files on disk and sets them on every flow and flow-component view handler, replicating what
     * {@link #generateAndSaveJavaCodeIkasanFlow} does as a side-effect of code generation.
     * -
     * Must be called from a background thread after the model has been loaded from model.json.
     */
    public void initialisePsiFileHandles() {
        Module module = project.getService(UiContext.class).getIkasanModule();
        if (module == null || module.getFlows() == null || module.getFlows().isEmpty()) {
            return;
        }
        VirtualFile projectBaseDir = StudioPsiUtils.getProjectBaseDir(project);
        if (projectBaseDir == null) {
            LOG.warn("STUDIO: initialisePsiFileHandles could not determine project base directory");
            return;
        }
        for (Flow ikasanFlow : module.getFlows()) {
            String flowPackageName = Generator.STUDIO_FLOW_PACKAGE + "." + ikasanFlow.getJavaPackageName();
            // GENERATED_CONTENT_ROOT is "/generated" — strip the leading slash for findFileByRelativePath
            String relPath = StudioPsiUtils.GENERATED_CONTENT_ROOT.substring(1) + "/" +
                    StudioPsiUtils.SRC_MAIN_JAVA_CODE + "/" +
                    flowPackageName.replace(".", "/") + "/" +
                    ikasanFlow.getJavaClassName() + ".java";
            VirtualFile vFile = projectBaseDir.findFileByRelativePath(relPath);
            if (vFile == null || !vFile.isValid()) {
                LOG.info("STUDIO: initialisePsiFileHandles: no generated file found at " + relPath + ", skipping flow " + ikasanFlow.getComponentName());
                continue;
            }
            ReadAction.run(() -> {
                PsiFile flowPsiFile = PsiManager.getInstance(project).findFile(vFile);
                if (flowPsiFile == null) {
                    return;
                }
                IkasanFlowViewHandler flowViewHandler = ViewHandlerCache.getFlowViewHandler(project, ikasanFlow);
                if (flowViewHandler != null) {
                    flowViewHandler.setPsiFile(flowPsiFile);
                }
                setFlowComponentNavigationTargets(ikasanFlow, flowPsiFile);
                setUserImplementedClassNavigationTargets(module, ikasanFlow, projectBaseDir);
            });
        }
    }

    private void generateAndSaveJavaCodeModuleConfig(Project project, Module module) {
        String templateString = null;
        try {
            templateString = ModuleConfigTemplate.create(module);
        } catch (StudioGeneratorException e) {
            displayIdeaWarnMessage(project, StudioBundle.message("message.AnErrorHasOccurredAttemptingToContinue", e.getMessage()));
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
            displayIdeaWarnMessage(project, StudioBundle.message("message.AnErrorHasOccurredAttemptingToContinue", e.getMessage()));
        }
        if (templateString != null) {
//            StudioPsiUtils.createFile(project, StudioPsiUtils.GENERATED_CONTENT_ROOT, StudioPsiUtils.SRC_MAIN_RESOURCES, null, MODULE_PROPERTIES_FILENAME_WITH_EXTENSION, templateString, false);
            StudioPsiUtils.createPropertiesFile(project, templateString);
        }
    }
}
