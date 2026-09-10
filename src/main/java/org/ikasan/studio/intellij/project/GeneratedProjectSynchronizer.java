package org.ikasan.studio.intellij.project;

import org.ikasan.studio.core.diagnostics.StudioDiagnosticEvent;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.ikasan.studio.StudioRuntimeException;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.generation.GenerationRequest;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.maven.IkasanPomModel;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.navigation.NavigationTarget;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandlerIntellij;
import org.ikasan.studio.ui.viewmodel.IkasanFlowComponentViewHandler;
import org.ikasan.studio.ui.viewmodel.IkasanFlowViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.ikasan.studio.core.generator.FlowsComponentFactoryTemplate.COMPONENT_FACTORY_CLASS_NAME;
import static org.ikasan.studio.intellij.project.StudioProjectFiles.replaceJsonModelFileSafely;
import static org.ikasan.studio.ui.StudioUIUtils.displayIdeaWarnMessage;

/**
 * Encapsulates the Intellij representation of the ikasan Module
 * The idea is to keep the ikasan Module clean of any Initellij specific details, this module will inspect the
 * code to generate the ikasan Module and update the code to reflect changes to the ikasan Module.
 */
public class GeneratedProjectSynchronizer {
    private static final Logger LOG = Logger.getInstance("#GeneratedProjectSynchronizer");
    private final Project project;

    /**
     * Plugin PSI (Program Structure Interface) Iksanan Model builder
     * @param project is the Intellij project instance
     *                   memory for multiple open projects, so each plugin IkasanModule virtualization needs to be keyed
     *                   by the project name. Hence, project is passed around most classes.
     */
    public GeneratedProjectSynchronizer(final Project project) {
        this.project = project;

    }

    /**
     * An update has been made to the diagram, so we need to reflect this into the code.
     */
    public CompletableFuture<Void> asynchGenerateSourceFromModelJsonInstanceAndSaveToDisk(GenerationRequest request) {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        if (request.scope() == GenerationRequest.Scope.MODEL_ONLY) {
            completion.complete(null);
            return completion;
        }
        if (project.isDisposed()) return CompletableFuture.failedFuture(new java.util.concurrent.CancellationException("Project closed"));
        AtomicReference<Boolean> pomDependenciesHaveChanged = new AtomicReference<>();
        UiContext uiContext = project.getService(UiContext.class);
        long revision = uiContext.beginGenerationRequest(completion, request);
        if (revision < 0) {
            completion.completeExceptionally(new IllegalStateException("A version migration is in progress."));
            return completion;
        }
        com.intellij.openapi.Disposable lifetime = () -> completion.completeExceptionally(
                new java.util.concurrent.CancellationException("Project closed during generation"));
        completion.whenComplete((result, failure) -> {
            uiContext.endGeneration();
            com.intellij.openapi.util.Disposer.dispose(lifetime);
            if (failure != null && uiContext.isLatestGeneration(revision)) reportGenerationFailure(failure);
        });
        try {
            // Project itself is deliberately not used as the Disposer parent here - the platform's plugin
            // guidelines flag Project/Application as parents to avoid, since they can accumulate disposables for
            // the whole session; StudioProjectInitialisationService is this plugin's own project-scoped
            // Disposable with the same effective lifetime (created for the project, disposed when it closes).
            com.intellij.openapi.util.Disposer.register(
                    project.getService(StudioProjectInitialisationService.class), lifetime);
        } catch (RuntimeException failure) {
            completion.completeExceptionally(failure);
            return completion;
        }
        // A newer request replaces pending work, so retain changes from every earlier scope.
        GenerationRequest effectiveRequest = uiContext.getPendingGenerationRequest();
        Module module = uiContext.getIkasanModule();

        try {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                if (stopObsoleteGeneration(uiContext, module, revision, completion)) return;
                // 1. Determine if the pom needs to be updated
            IkasanPomModel ikasanPomModel = uiContext.getIkasanPomModel();        // Not on EDT
            if (ikasanPomModel == null) {
                // Cache never warmed for this project (e.g. this is its first regeneration this session) -
                // UiContext.getIkasanPomModel() is deliberately cache-only (EDT callers must never hit disk),
                // but this whole block already runs on a pooled thread, so loading it here is safe and also
                // populates the cache for next time. Without this, every regeneration attempt for a
                // never-warmed project would fail before a single source file could be generated.
                ikasanPomModel = StudioProjectFiles.pomLoadFromVirtualDisk(project);
            }
            if (ikasanPomModel != null && ikasanPomModel.isNewDependency(module.getAllUniqueSortedJarDependencies())) {
                pomDependenciesHaveChanged.set(true);
            } else {
                pomDependenciesHaveChanged.set(false);
            }

            // ProjectManager.getInstance().reloadProject(uiContext.getProject(project))

            LOG.info("STUDIO: Start ApplicationManager.getApplication().runWriteAction - source from model");
            LOG.info(StudioDiagnosticEvent.format(StudioDiagnosticEvent.Event.GENERATION_STARTED, null, uiContext.getIkasanModule().getIdentity(), null, null));

            // 2. Generate only the artifacts affected by this request. Individual writes also
            // skip unchanged content before it enters PSI or code formatting.
            // Switch to UI thread for write action and undo block
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    if (stopObsoleteGeneration(uiContext, module, revision, completion)) return;
                    // Using the command  processor adds support for undo
                CommandProcessor.getInstance().executeCommand(
                    project,
                    () -> {
                        GenerationTransactionManager.begin();
                        try {
                        if (pomDependenciesHaveChanged.get()) {
                            // We have checked the in-memory model, below will also verify from the on-disk model.
                            StudioProjectFiles.checkForDependencyChangesAndSaveIfChanged(project, module.getAllUniqueSortedJarDependencies(), module.getMetaVersion());
                        }
                        Long transactionTimeStamp = uiContext.getProjectRefreshTimestamp();
                        saveAiProjectContract(project, module);
                        switch (effectiveRequest.scope()) {
                            case PROPERTIES -> generateAndSavePropertiesConfig(project, module);
                            case FLOW -> {
                                saveDebugSupportClasses(project, module);
                                saveFlow(project, module, effectiveRequest.affectedFlow());
                                // A component added to the flow (e.g. an Ftp Consumer) can require its own
                                // @ImportResource/@Import on ModuleConfig (see ComponentMeta#importResources /
                                // #importConfigurationClasses) - without this, adding such a component to an
                                // *existing* flow would leave ModuleConfig stale until some other change
                                // happened to trigger a MODULE_STRUCTURE/FULL regeneration.
                                generateAndSaveJavaCodeModuleConfig(project, module);
                                generateAndSavePropertiesConfig(project, module);
                            }
                            case MODULE_STRUCTURE -> {
                                saveDebugSupportClasses(project, module);
                                saveFlow(project, module, effectiveRequest.affectedFlow());
                                generateAndSaveJavaCodeModuleConfig(project, module);
                                generateAndSavePropertiesConfig(project, module);
                            }
                            case FULL -> {
                                saveApplication(project, module);
                                saveStudioInjectController(project, module);
                                saveDebugSupportClasses(project, module);
                                saveAllFlows(project, module);
                                generateAndSaveJavaCodeModuleConfig(project, module);
                                generateAndSavePropertiesConfig(project, module);
                            }
                            default -> {
                                // MODEL_ONLY already returned above before this was scheduled; this branch
                                // exists only so the switch stays exhaustive without naming a case that
                                // static analysis can prove is unreachable here.
                            }
                        }
                        GenerationTransactionManager.Summary summary = GenerationTransactionManager.commit(project);
                        String summaryMessage = "Generated project updated: " + summary.created() + " created, "
                                + summary.updated() + " updated, " + summary.unchanged() + " unchanged";
                        LOG.info(StudioDiagnosticEvent.format(StudioDiagnosticEvent.Event.GENERATION_COMPLETED, null, uiContext.getIkasanModule().getIdentity(), null, null));
                        StatusBar.Info.set(summaryMessage, project);
                        if (pomDependenciesHaveChanged.get()) {
                            MavenProjectsManager manager = MavenProjectsManager.getInstance(project);
                            if (manager != null) manager.forceUpdateAllProjectsOrFindAllAvailablePomFiles();
                        }
                        if (effectiveRequest.scope() == GenerationRequest.Scope.MODULE_STRUCTURE
                                || effectiveRequest.scope() == GenerationRequest.Scope.FULL) {
                            deleteStaleGeneratedFlowPackages(project, module);
                        }
                        if (!transactionTimeStamp.equals(uiContext.getProjectRefreshTimestamp())) {
                            displayIdeaWarnMessage(project, StudioBundle.message("message.IntellijHasChangedTheProjectPartWayThroughTheSave"));
                        }
                        LOG.info("STUDIO: End ApplicationManager.getApplication().runWriteAction - source from model");
                        } catch (RuntimeException failure) {
                            GenerationTransactionManager.abort();
                            throw failure;
                        }
                    },
                    StudioBundle.message("action.GenerateSourceFromFlowDiagram"),
                    "Undo group ID");
                completion.complete(null);
                } catch (Exception failure) {
                    completion.completeExceptionally(failure);
                }
            });
            } catch (Exception failure) {
                completion.completeExceptionally(failure);
            }
        });
        } catch (RuntimeException failure) {
            completion.completeExceptionally(failure);
        }
        return completion;
    }

    /** All asynchronous failure paths report once through the generation completion future. */
    private void reportGenerationFailure(Throwable failure) {
        if (project.isDisposed() || failure instanceof com.intellij.openapi.progress.ProcessCanceledException
                || failure instanceof java.util.concurrent.CancellationException) return;
        LOG.warn(StudioDiagnosticEvent.format(StudioDiagnosticEvent.Event.GENERATION_FAILED, failure, null, null, null));
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) {
                String detail = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
                org.ikasan.studio.ui.StudioUIUtils.displayIdeaErrorMessage(project,
                        "<html><b>Ikasan Studio code generation failed</b><br><br>"
                                + org.ikasan.studio.ui.StudioUIUtils.escapeHtml(detail)
                                + "<br><br>The requested code update did not complete. Review the details above before regenerating."
                                + "<br>The IDE log records this as GENERATION_FAILED.</html>");
            }
        });
    }

    private boolean stopObsoleteGeneration(UiContext context, Module module, long revision, CompletableFuture<Void> completion) {
        if (completion.isDone()) return true;
        if (project.isDisposed()) {
            completion.completeExceptionally(new java.util.concurrent.CancellationException("Project closed"));
            return true;
        }
        if (!context.isLatestGeneration(revision)) {
            context.getLatestGeneration().whenComplete((result, failure) -> {
                if (failure == null) completion.complete(null);
                else completion.completeExceptionally(failure);
            });
            return true;
        }
        if (context.getIkasanModule() != module) {
            completion.completeExceptionally(new java.util.concurrent.CancellationException("Model was reloaded during generation"));
            return true;
        }
        return false;
    }


    /** Keeps the offline AI contract derived from the same model and meta-pack APIs used by Studio itself. */
    private void saveAiProjectContract(Project project, Module module) {
        try {
            StudioProjectFiles.createFileWithDirectoriesIfMissing(project, "AGENTS.md", AiProjectContractGenerator.agentsGuide());
            StudioProjectFiles.createFileWithDirectories(project, "generated/IKASAN_STUDIO.md", AiProjectContractGenerator.studioGuide(module.getMetaVersion()), null);
            StudioProjectFiles.createFileWithDirectories(project, "generated/src/main/model/model.schema.json", AiProjectContractGenerator.modelSchema(), null);
            StudioProjectFiles.createFileWithDirectories(project, "generated/src/main/model/component-catalogue.json", AiProjectContractGenerator.componentCatalogue(module.getMetaVersion()), null);
        } catch (StudioBuildException | com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new StudioRuntimeException("Could not generate the AI project contract", e);
        }
    }

    /**
     * Take the Model from memory and persist it to disk
     */
    public void saveModelJsonToDisk() {
        if (project.isDisposed()) throw new java.util.concurrent.CancellationException("Project closed");
        UiContext uiContext = project.getService(UiContext.class);
        if (uiContext.isMigrationActive()) throw new IllegalStateException("A version migration is in progress.");
        if (uiContext.isModelPersistenceBlocked()) {
            String reason = uiContext.getModelPersistenceBlockReason();
            throw new IllegalStateException("Model saving is disabled because the model was not loaded safely" +
                    (reason == null ? "" : ": " + reason));
        }

        try {
            String templateString = ModelTemplate.create(uiContext.getIkasanModule());
            CommandProcessor.getInstance().executeCommand(
                project,
                () -> ApplicationManager.getApplication().runWriteAction(
                        () -> {
                            LOG.info("STUDIO: Start protected model.json write");
                            replaceJsonModelFileSafely(project, templateString);
                            LOG.info("STUDIO: End protected model.json write");
                        }),
                StudioBundle.message("action.GenerateJSONFromFlowDiagram"),
                "Undo group ID");
        } catch (RuntimeException failure) {
            LOG.warn(StudioDiagnosticEvent.format(StudioDiagnosticEvent.Event.MODEL_SAVE_FAILED, failure, null, null, null));
            displayIdeaWarnMessage(project, "Ikasan Studio could not safely save model.json. " +
                    failure.getMessage() + " Source generation was cancelled.");
            throw failure;
        }
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
        String h2StartStopPomString;
        try {
            h2StartStopPomString = H2StartStopTemplate.create(module.getMetaVersion());
        } catch (StudioGeneratorException e) {
            throw new StudioRuntimeException("Template generation failed; no project files were changed", e);
        }

        if (h2StartStopPomString != null) {
            StudioProjectFiles.createPomFile(project, StudioProjectFiles.GENERATED_CONTENT_ROOT, "h2", h2StartStopPomString);
        }

        // The SpringBoot startup
        String applicationTemplateString;
        try {
            applicationTemplateString = ApplicationTemplate.create(module);
        } catch (StudioGeneratorException e) {
            throw new StudioRuntimeException("Template generation failed; no project files were changed", e);
        }
        if (applicationTemplateString != null) {
            StudioProjectFiles.createJavaSourceFile(project,
                    StudioProjectFiles.GENERATED_CONTENT_ROOT,
                    StudioProjectFiles.SRC_MAIN_JAVA_CODE,
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
        String studioInjectControllerTemplateString;
        try {
            studioInjectControllerTemplateString = StudioInjectControllerTemplate.create(module);
        } catch (StudioGeneratorException e) {
            throw new StudioRuntimeException("Template generation failed; no project files were changed", e);
        }
        if (studioInjectControllerTemplateString != null) {
            StudioProjectFiles.createJavaSourceFile(project,
                    StudioProjectFiles.GENERATED_CONTENT_ROOT,
                    StudioProjectFiles.SRC_MAIN_JAVA_CODE,
                    StudioInjectControllerTemplate.STUDIO_BOOT_PACKAGE,
                    StudioInjectControllerTemplate.STUDIO_INJECT_CONTROLLER_CLASS_NAME, studioInjectControllerTemplateString, null);
        }
    }

    /**
     * Save the DebugTransitionComponent base class and its DeepCopyUtil helper - generated directly into
     * the project (rather than pulled in as a jar dependency) so they always compile against whichever
     * Ikasan Filter API the module's own metapack version resolves. Written into the "user" module, not
     * "generated" - the concrete per-flow Debug subclass is generated into "user" (see the isDebug()
     * branch below), and "generated" depends on "user", not the other way around, so a base class placed
     * in "generated" would be invisible to its own subclass.
     * @param project is the Intellij project instance
     * @param module for this code
     */
    private void saveDebugSupportClasses(Project project, Module module) {
        try {
            String debugTransitionComponentTemplateString = DebugTransitionComponentTemplate.create(module);
            StudioProjectFiles.authoriseUserJavaReplacement(DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_PACKAGE,
                    DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_CLASS_NAME);
            StudioProjectFiles.createJavaSourceFile(project,
                    StudioProjectFiles.USER_CONTENT_ROOT,
                    StudioProjectFiles.SRC_MAIN_JAVA_CODE,
                    DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_PACKAGE,
                    DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_CLASS_NAME, debugTransitionComponentTemplateString, null);

            String deepCopyUtilTemplateString = DeepCopyUtilTemplate.create(module);
            StudioProjectFiles.authoriseUserJavaReplacement(DeepCopyUtilTemplate.DEEP_COPY_UTIL_PACKAGE,
                    DeepCopyUtilTemplate.DEEP_COPY_UTIL_CLASS_NAME);
            StudioProjectFiles.createJavaSourceFile(project,
                    StudioProjectFiles.USER_CONTENT_ROOT,
                    StudioProjectFiles.SRC_MAIN_JAVA_CODE,
                    DeepCopyUtilTemplate.DEEP_COPY_UTIL_PACKAGE,
                    DeepCopyUtilTemplate.DEEP_COPY_UTIL_CLASS_NAME, deepCopyUtilTemplateString, null);
        } catch (StudioGeneratorException e) {
            throw new StudioRuntimeException("Template generation failed; no project files were changed", e);
        }
    }

    private void saveAllFlows(Project project, Module module) {
        for (Flow ikasanFlow : module.getFlows()) {
            saveFlow(project, module, ikasanFlow);
        }
    }

    private void deleteStaleGeneratedFlowPackages(Project project, Module module) {
        Set<String> flowPackageNames = new HashSet<>();
        for (Flow ikasanFlow : module.getFlows()) {
            flowPackageNames.add(ikasanFlow.getJavaPackageName());
        }
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                StudioProjectFiles.deleteSubPackagesNotIn(project, StudioProjectFiles.GENERATED_CONTENT_ROOT, Generator.STUDIO_FLOW_PACKAGE, flowPackageNames));
    }

    private void saveFlow(Project project, Module module, Flow ikasanFlow) {
        if (ikasanFlow == null || !module.getFlows().contains(ikasanFlow)) {
            return;
        }
        String flowPackageName = Generator.STUDIO_FLOW_PACKAGE + "." + ikasanFlow.getJavaPackageName();
        generateAndSaveJavaCodeIkasanComponentFactory(project, module, flowPackageName, ikasanFlow);
        generateAndSaveJavaCodeIkasanFlow(project, module, flowPackageName, ikasanFlow);
        generateAndSaveUserImplementClassStubsForFlow(project, module, ikasanFlow);
        // generateAndSaveJavaCodeIkasanFlow (above) unconditionally points every component's "jump to code"
        // target at the Flow.java file via setFlowComponentNavigationTargets - correct as a default, but wrong
        // for a FlowUserImplementedElement (Broker, Converter, etc.) whose stub, once generated, is normally
        // NOT regenerated on every save (see isOverwriteEnabled() in generateAndSaveUserImplementClassStubsForFlow
        // above), so nothing re-points the target back at its own class file after this first clobber. Re-running
        // this after every save (not just on project load, where it was previously the only caller) keeps "jump
        // to code" pointing at the user's own class regardless of whether its stub was actually rewritten.
        VirtualFile projectBaseDir = StudioProjectFiles.getProjectBaseDir(project);
        if (projectBaseDir != null) {
            setUserImplementedClassNavigationTargets(module, ikasanFlow, projectBaseDir);
        }
    }

    private void generateAndSaveUserImplementClassStubsForFlow(Project project, Module module, Flow ikasanFlow) {
        // FlowRoute#getConsumerAndFlowRouteElements() only ever returns ITS OWN route's elements - for a
        // router flow, that's just the consumer and whatever leads up to the router itself, never anything
        // inside a branch (route1/route2 etc). Using Flow#getFlowElementsNoExternalEndPoints() instead walks
        // every route recursively (see Flow#getAllFlowElementsInAnyRoute), so a Debug (or any other
        // user-implemented component) dropped into a router branch gets its stub generated too. Router
        // Endpoint markers are internal endpoints so they pass this method's own endpoint filter, but they
        // are neither hasUserSuppliedClass() nor a FlowUserImplementedElement, so the loop below skips them.
        if (!ikasanFlow.getFlowElementsNoExternalEndPoints().isEmpty()) {
            // Must do User Implemented class stubs first otherwise resolution will not auto generate imports.
            for (FlowElement component : ikasanFlow.getFlowElementsNoExternalEndPoints()) {
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
                        String templateString;
                        try {
                            templateString = FlowsUserImplementedClassPropertyTemplate.create(module.getMetaVersion(), property, newPackageName, clazzName, prefix);
                        } catch (StudioGeneratorException e) {
                            throw new StudioRuntimeException("Template generation failed; no project files were changed", e);
                        }
                        if (templateString != null) {
                            String contentRoot = protectFromOverwrite ? StudioProjectFiles.USER_CONTENT_ROOT : StudioProjectFiles.GENERATED_CONTENT_ROOT;
                            if (protectFromOverwrite && property.isOverwriteEnabled()) {
                                StudioProjectFiles.authoriseUserJavaReplacement(newPackageName, clazzName);
                            }
                            StudioProjectFiles.createJavaSourceFile(project, contentRoot, StudioProjectFiles.SRC_MAIN_JAVA_CODE,
                                    newPackageName, clazzName, templateString, componentViewHandler);
                            if (protectFromOverwrite) {
                                StudioProjectFiles.afterGenerationCommit(() -> property.setOverwriteEnabled(false));
                            }
                        }
                    }
                }

                if (component instanceof FlowUserImplementedElement && componentRequiresStub(component)) {
                    String newClassName = (String)component.getProperty(ComponentPropertyMeta.USER_IMPLEMENTED_CLASS_NAME).getValue();
                    String newPackageName = GeneratorUtils.getUserImplementedClassesPackageName(module, ikasanFlow);
                    // overwriteEnabled is a plain in-memory flag (see FlowUserImplementedElement) that
                    // ModuleDeserializer never sets true when rebuilding the model from model.json - every
                    // FlowUserImplementedElement comes back from a reload with it false, "protecting" a stub
                    // that, the very first time generation runs after that reload, was never actually written
                    // to disk yet. Without also checking for the file's actual absence here, a component whose
                    // stub generation failed (or was never reached) before the model was last reloaded can
                    // never have it created - the "don't clobber hand-written code" guard permanently blocks
                    // its own first-ever generation instead.
                    boolean stubMissing = StudioProjectFiles.getUserImplementedClassFile(project, newPackageName, newClassName) == null;
                    if (!(((FlowUserImplementedElement) component).isOverwriteEnabled()
                            || stubMissing)) {
                        continue;
                    }
                    String templateString;
                    try {
                        templateString = FlowsUserImplementedComponentTemplate.create(newPackageName, module, ikasanFlow, component);
                    } catch (StudioGeneratorException e) {
                        throw new StudioRuntimeException("Template generation failed; no project files were changed", e);
                    }
                    if (templateString != null) {
                        if (((FlowUserImplementedElement) component).isOverwriteEnabled()) {
                            StudioProjectFiles.authoriseUserJavaReplacement(newPackageName, newClassName);
                        }
                        StudioProjectFiles.createJavaSourceFile(project, StudioProjectFiles.USER_CONTENT_ROOT, StudioProjectFiles.SRC_MAIN_JAVA_CODE,
                                newPackageName, newClassName, templateString, componentViewHandler);
                        StudioProjectFiles.afterGenerationCommit(() -> ((FlowUserImplementedElement) component).setOverwriteEnabled(false));
                    }
                }
            }
        }
    }

    /**
     * Some {@link FlowUserImplementedElement}s (Debug, etc.) don't declare a "requiresStub" property at all,
     * meaning Studio always generates and manages their stub - true unless the property is present and
     * explicitly false. Components that can also point at an already-existing implementation (Broker, Consumer,
     * Converter, Filter, Producer, Splitter, Translator) expose this as a real, mandatory toggle: false means
     * userImplementedClassName is already a fully-qualified class the user supplies themselves (see
     * componentFactory_en.ftl, which stops assuming Studio's managed user-package for that same case).
     */
    private boolean componentRequiresStub(FlowElement component) {
        Object requiresStub = component.getPropertyValue(ComponentPropertyMeta.REQUIRES_STUB);
        return !(requiresStub instanceof Boolean) || (Boolean) requiresStub;
    }

    private void generateAndSaveJavaCodeIkasanComponentFactory(Project project, Module module, String flowPackageName, Flow ikasanFlow) {
        String componentFactoryTemplateString;
        try {
            componentFactoryTemplateString = FlowsComponentFactoryTemplate.create(flowPackageName, module, ikasanFlow);
        } catch (StudioGeneratorException e) {
            throw new StudioRuntimeException("Template generation failed; no project files were changed", e);
        }
        if (componentFactoryTemplateString != null) {
            StudioProjectFiles.createJavaSourceFile(project, StudioProjectFiles.GENERATED_CONTENT_ROOT, StudioProjectFiles.SRC_MAIN_JAVA_CODE, flowPackageName,
                    COMPONENT_FACTORY_CLASS_NAME + ikasanFlow.getJavaClassName(), componentFactoryTemplateString, null);
        }
    }

    private void generateAndSaveJavaCodeIkasanFlow(Project project, Module module, String flowPackageName, Flow ikasanFlow) {
        IkasanFlowViewHandler flowViewHandler = ViewHandlerCache.getFlowViewHandler(project, ikasanFlow);
        String flowTemplateString;
        try {
            flowTemplateString = FlowTemplate.create(flowPackageName, module, ikasanFlow);
        } catch (StudioGeneratorException e) {
            throw new StudioRuntimeException("Template generation failed; no project files were changed", e);
        }
        if (flowTemplateString != null) {
            StudioProjectFiles.createJavaSourceFile(
                    project,
                    StudioProjectFiles.GENERATED_CONTENT_ROOT,
                    StudioProjectFiles.SRC_MAIN_JAVA_CODE,
                    flowPackageName,
                    ikasanFlow.getJavaClassName(),
                    flowTemplateString,
                    flowViewHandler);
        }
        setFlowComponentNavigationTargets(ikasanFlow, flowViewHandler.getCodeNavigationTarget().psiFile());
    }

    /** Resolve document/PSI on a pooled read action; loading either can refresh the workspace index. */
    private void withNavigationText(PsiFile file, java.util.function.Consumer<NavigationTextReader.Snapshot> apply) {
        withNavigationText(() -> file, apply);
    }

    private void withNavigationText(java.util.function.Supplier<PsiFile> resolve,
                                    java.util.function.Consumer<NavigationTextReader.Snapshot> apply) {
        UiContext context = project.getService(UiContext.class);
        var owner = context.getViewHandlerFactory();
        var module = context.getIkasanModule();
        var generation = context.getLatestGeneration();
        if (owner == null) return;
        Runnable read = () -> NavigationTextReader.read(project, resolve, snapshot -> {
            if (context.getViewHandlerFactory() == owner && context.getIkasanModule() == module
                    && context.getLatestGeneration() == generation) apply.accept(snapshot);
        });
        if (GenerationTransactionManager.isActive()) StudioProjectFiles.afterGenerationCommit(read);
        else read.run();
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
        withNavigationText(flowPsiFile, snapshot -> {
            String flowFileText = snapshot.text();
            int searchFromOffset = 0;
            for (FlowElement flowElement : ikasanFlow.getFlowElementsNoExternalEndPoints()) {
                IkasanFlowComponentViewHandler flowComponentViewHandler = ViewHandlerCache.getFlowComponentViewHandler(project, flowElement);
                if (flowComponentViewHandler != null) {
                    // A user-class target assigned while this read was pending is more specific.
                    if (flowElement instanceof FlowUserImplementedElement
                            && flowComponentViewHandler.getCodeNavigationTarget().isPresent()
                            && flowComponentViewHandler.getCodeNavigationTarget().psiFile() != flowPsiFile) continue;
                    NavigationTarget target = snapshot.target();
                    String componentName = flowElement.getComponentName();
                    if (componentName != null) {
                        int offset = flowFileText.indexOf("\"" + componentName + "\"", searchFromOffset);
                        if (offset >= 0) {
                            target = target.withOffset(offset);
                            searchFromOffset = offset + componentName.length();
                        }
                    }
                    flowComponentViewHandler.setCodeNavigationTarget(target);
                }
            }
        });
    }

    /**
     * "Jump to Properties" targets: application.properties has no per-component markers, but the key text
     * itself is deterministic (built the same way the FTL builds it), so a component's target can be found by
     * the same text-offset-search technique {@link #setFlowComponentNavigationTargets} uses against Flow.java.
     * Covers two kinds of line the generator writes into application.properties:
     * - the generic per-property lines (any property with a propertyConfigFileLabel and a value), and
     * - the flow-level "ikasan.flow.configuration[...]" block (isRecording / recordedEventTimeToLive /
     *   invokeContextListeners), keyed by the flow's own (already-unique) identity.
     * Not covered: flowStartupType's indexed "flowStartupTypes[N]=..." line and the indexed
     * "wiretap.triggers[N]=..." lines - both need an FTL-side counter replicated to build a unique search key,
     * and wiretaps in particular have no single canvas element that naturally owns "jump to" for them.
     * @param module owning the flows, used the same way the FTL uses it to build each property's key
     * @param propertiesPsiFile the already-written, already-formatted application.properties, or null if it
     *                          hasn't been generated yet (nothing to search, silently does nothing)
     */
    private void setPropertiesFileNavigationTargets(Module module, PsiFile propertiesPsiFile) {
        if (propertiesPsiFile == null || module.getFlows() == null) {
            return;
        }
        withNavigationText(propertiesPsiFile, snapshot -> {
            String propertiesFileText = snapshot.text();
            int searchFromOffset = 0;
            for (Flow flow : module.getFlows()) {
                IkasanFlowViewHandler flowViewHandler = ViewHandlerCache.getFlowViewHandler(project, flow);
                if (flowViewHandler != null) {
                    String flowConfigPrefix = "ikasan.flow.configuration[" + StudioBuildUtils.escapeSpringPropertiesMapKey(flow.getIdentity()) + "].";
                    int flowOffset = propertiesFileText.indexOf(flowConfigPrefix);
                    flowViewHandler.setPropertiesNavigationTarget(flowOffset >= 0
                            ? snapshot.target().withOffset(flowOffset)
                            : NavigationTarget.none());
                }

                for (FlowElement flowElement : flow.ftlGetAllFlowElementsInAnyRouteNoEndpoints()) {
                    IkasanFlowComponentViewHandler flowComponentViewHandler = ViewHandlerCache.getFlowComponentViewHandler(project, flowElement);
                    if (flowComponentViewHandler == null) {
                        continue;
                    }
                    String key = firstApplicationPropertiesKeyFor(module, flow, flowElement);
                    int offset = key != null ? propertiesFileText.indexOf(key + "=", searchFromOffset) : -1;
                    flowComponentViewHandler.setPropertiesNavigationTarget(offset >= 0
                            ? snapshot.target().withOffset(offset)
                            : NavigationTarget.none());
                    if (offset >= 0) {
                        searchFromOffset = offset + key.length();
                    }
                }
            }
        });
    }

    /**
     * Mirrors the filter and key-construction the compress block in propertiesTemplate_en.ftl uses, so this
     * finds the exact same "first" property (getStandardComponentProperties() is a TreeMap, so both iterate the
     * same alphabetical order) that the FTL emits first for this component.
     * @return the application.properties key for this component's first externalized property, or null if it
     * has none
     */
    private String firstApplicationPropertiesKeyFor(Module module, Flow flow, FlowElement flowElement) {
        for (Map.Entry<String, ComponentProperty> entry : flowElement.getStandardComponentProperties().entrySet()) {
            ComponentProperty property = entry.getValue();
            ComponentPropertyMeta meta = property.getMeta();
            if (meta != null && meta.getPropertyConfigFileLabel() != null && !meta.getPropertyConfigFileLabel().isEmpty()
                    && property.getValue() != null && !meta.isUserSuppliedClass()) {
                return StudioBuildUtils.substitutePlaceholderInLowerCase(module, flow, flowElement, meta.getPropertyConfigFileLabel());
            }
        }
        return null;
    }

    /**
     * Components such as Debug or Converter generate their own user-editable class under the project's
     * {@code user/src/main/java} tree (see {@link #generateAndSaveUserImplementClassStubsForFlow}). "Jump to code"
     * for these should navigate to that class rather than to the flow file. The class name is a persisted property
     * ({@link ComponentPropertyMeta#USER_IMPLEMENTED_CLASS_NAME}) so, unlike the flow-file offset above, this target
     * can be reconstructed on project reload without needing to regenerate anything or store extra state.
     */
    private void setUserImplementedClassNavigationTargets(Module module, Flow ikasanFlow, VirtualFile projectBaseDir) {
        if (ikasanFlow.getFlowRoute() == null) {
            return;
        }
        // Router branches can contain further routers and other user-implemented components.
        // Use the same recursive traversal as generation and default flow-code navigation.
        for (FlowElement component : ikasanFlow.getFlowElementsNoExternalEndPoints()) {
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
            String relPath = StudioProjectFiles.USER_CONTENT_ROOT.substring(1) + "/" +
                    StudioProjectFiles.SRC_MAIN_JAVA_CODE + "/" +
                    packageName.replace(".", "/") + "/" +
                    className + ".java";
            withNavigationText(() -> {
                VirtualFile vFile = projectBaseDir.findFileByRelativePath(relPath);
                return vFile == null || !vFile.isValid() ? null : PsiManager.getInstance(project).findFile(vFile);
            }, snapshot -> componentViewHandler.setCodeNavigationTarget(snapshot.target()));
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
        VirtualFile projectBaseDir = StudioProjectFiles.getProjectBaseDir(project);
        if (projectBaseDir == null) {
            LOG.warn("STUDIO: initialisePsiFileHandles could not determine project base directory");
            return;
        }
        setPropertiesFileNavigationTargets(module, resolveApplicationPropertiesPsiFile(projectBaseDir));
        for (Flow ikasanFlow : module.getFlows()) {
            String flowPackageName = Generator.STUDIO_FLOW_PACKAGE + "." + ikasanFlow.getJavaPackageName();
            // GENERATED_CONTENT_ROOT is "/generated" — strip the leading slash for findFileByRelativePath
            String relPath = StudioProjectFiles.GENERATED_CONTENT_ROOT.substring(1) + "/" +
                    StudioProjectFiles.SRC_MAIN_JAVA_CODE + "/" +
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
                    flowViewHandler.setCodeNavigationTarget(NavigationTarget.forFile(flowPsiFile));
                }
                setFlowComponentNavigationTargets(ikasanFlow, flowPsiFile);
                setUserImplementedClassNavigationTargets(module, ikasanFlow, projectBaseDir);
            });
        }
    }

    private void generateAndSaveJavaCodeModuleConfig(Project project, Module module) {
        String templateString;
        try {
            templateString = ModuleConfigTemplate.create(module);
        } catch (StudioGeneratorException e) {
            throw new StudioRuntimeException("Template generation failed; no project files were changed", e);
        }
        if (templateString != null) {
            AbstractViewHandlerIntellij viewHandler = ViewHandlerCache.getAbstractViewHandler(project, module);
            StudioProjectFiles.createJavaSourceFile(project, StudioProjectFiles.GENERATED_CONTENT_ROOT, StudioProjectFiles.SRC_MAIN_JAVA_CODE,
                    ModuleConfigTemplate.STUDIO_BOOT_PACKAGE, ModuleConfigTemplate.MODULE_CLASS_NAME, templateString, viewHandler);
        }

    }

    public static final String MODULE_PROPERTIES_FILENAME_WITH_EXTENSION = "application.properties";
    private void generateAndSavePropertiesConfig(Project project, Module module) {
        String templateString;
        try {
            templateString = PropertiesTemplate.create(module);
            Map<String, String> applicationProperties = StudioBuildUtils.convertStringToMap(templateString);
            StudioProjectFiles.afterGenerationCommit(() ->
                    project.getService(UiContext.class).setApplicationProperties(applicationProperties));
        } catch (StudioGeneratorException e) {
            throw new StudioRuntimeException("Template generation failed; no project files were changed", e);
        }
        if (templateString != null) {
//            StudioProjectFiles.createFile(project, StudioProjectFiles.GENERATED_CONTENT_ROOT, StudioProjectFiles.SRC_MAIN_RESOURCES, null, MODULE_PROPERTIES_FILENAME_WITH_EXTENSION, templateString, false);
            StudioProjectFiles.createPropertiesFile(project, templateString);
            // Unlike initialisePsiFileHandles() (which calls resolveApplicationPropertiesPsiFile() from a
            // background pooled thread), this method runs inside CommandProcessor.executeCommand on the EDT
            // (see asynchGenerateSourceFromModelJsonInstanceAndSaveToDisk) - PsiManager.findFile() inside
            // resolveApplicationPropertiesPsiFile() hits the workspace file index, which the platform refuses
            // to do synchronously on the EDT ("Slow operations are prohibited on EDT"). ReadAction.nonBlocking
            // runs the lookup on a pooled thread and hops back for the (already read-action-safe) nav-target
            // update, the same pattern used in SendTestMessagePayloadDialog.
            VirtualFile projectBaseDir = StudioProjectFiles.getProjectBaseDir(project);
            ReadAction.nonBlocking(() -> resolveApplicationPropertiesPsiFile(projectBaseDir))
                    .finishOnUiThread(ModalityState.defaultModalityState(), propertiesPsiFile -> {
                        if (!project.isDisposed()) {
                            setPropertiesFileNavigationTargets(module, propertiesPsiFile);
                        }
                    })
                    .submit(AppExecutorUtil.getAppExecutorService());
        }
    }

    /**
     * Resolves the already-written, already-formatted application.properties as a PsiFile, the same way
     * {@link #initialisePsiFileHandles()} resolves already-generated .java files - by relative path lookup
     * against the project base directory, rather than depending on any deferred/async PSI-setting side effect
     * of the write itself.
     * @return the PsiFile, or null if it hasn't been generated (yet) or the project base directory is unknown
     */
    private PsiFile resolveApplicationPropertiesPsiFile(VirtualFile projectBaseDir) {
        if (projectBaseDir == null) {
            return null;
        }
        // GENERATED_CONTENT_ROOT is "/generated" — strip the leading slash for findFileByRelativePath
        String relPath = StudioProjectFiles.GENERATED_CONTENT_ROOT.substring(1) + "/" +
                StudioProjectFiles.SRC_MAIN_RESOURCES + "/" +
                MODULE_PROPERTIES_FILENAME_WITH_EXTENSION;
        VirtualFile vFile = projectBaseDir.findFileByRelativePath(relPath);
        if (vFile == null || !vFile.isValid()) {
            return null;
        }
        return ReadAction.compute(() -> PsiManager.getInstance(project).findFile(vFile));
    }
}
