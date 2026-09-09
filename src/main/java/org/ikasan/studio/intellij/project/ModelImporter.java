package org.ikasan.studio.intellij.project;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.MetaPackManifest;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.migration.MigrationJdk;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;

import java.util.ArrayList;
import java.util.List;

/**
 * Imports a persisted {@code model.json} (from another project or anywhere on disk, either chosen via a file
 * dialog or pasted as text) into this project. The model records its own meta-pack version, so import selects
 * that version in the canvas chooser, loads its component library, regenerates all Studio-owned artefacts (which
 * copies the model into this project's {@code generated/src/main/model/model.json}) and, when a matching JDK is
 * installed, applies the meta-pack's Java version to the IDE project/Maven/Run configuration settings.
 */
public final class ModelImporter {
    private static final Logger LOG = Logger.getInstance(ModelImporter.class);

    private ModelImporter() { }

    public static void openImportDialog(Project project) {
        new ImportModelJsonDialog(project).show();
    }

    /**
     * Validates and commits the supplied model JSON.
     * @param project owning project
     * @param json the model.json content
     * @param sourceDescription human-readable origin, used in error messages (e.g. the chosen file name)
     * @throws StudioBuildException with an actionable, user-facing message when the JSON is invalid or its
     *         recorded meta-pack is not installed
     */
    public static void importFromText(Project project, String json, String sourceDescription) throws StudioBuildException {
        if (project.isDisposed()) {
            throw new StudioBuildException("The project is closing; import was not started.");
        }
        ApplicationManager.getApplication().assertIsDispatchThread();
        java.util.concurrent.atomic.AtomicReference<Module> validated = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<StudioBuildException> validationFailure = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger javaVersion = new java.util.concurrent.atomic.AtomicInteger(-1);
        boolean completed = com.intellij.openapi.progress.ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                Module candidate = ComponentIO.validatePersistedModuleJson(json, sourceDescription, false);
                String version = candidate.getVersion();
                if (version == null || version.isBlank() || !ComponentLibrary.getMetapackList().contains(version)) {
                    throw new StudioBuildException(StudioBundle.message("message.ImportModelMetapackUnavailable", version));
                }
                if (ComponentLibrary.versionNotContained(version)) ComponentLibrary.refreshComponentLibrary(version);
                javaVersion.set(requiredJava(version));
                validated.set(candidate);
            } catch (StudioBuildException failure) {
                validationFailure.set(failure);
            }
        }, StudioBundle.message("dialog.ImportModelJson"), false, project);
        if (validationFailure.get() != null) throw validationFailure.get();
        if (!completed || project.isDisposed()) throw new com.intellij.openapi.progress.ProcessCanceledException();
        Module module = validated.get();
        String version = module.getVersion();
        int requiredJava = javaVersion.get();
        UiContext uiContext = project.getService(UiContext.class);
        Module previousModule = uiContext.getIkasanModule();
        String previousPackage = uiContext.getOptions().getPackageName();
        uiContext.setIkasanModule(module);
        if (module.getApplicationPackageName() != null && !module.getApplicationPackageName().isBlank()) {
            uiContext.getOptions().setPackageName(module.getApplicationPackageName());
        }
        java.util.concurrent.CompletableFuture<Void> generation;
        try {
            generation = StudioProjectFiles.refreshCodeFromModel(project,
                    org.ikasan.studio.core.generation.GenerationRequest.full());
        } catch (RuntimeException failure) {
            uiContext.setIkasanModule(previousModule);
            uiContext.getOptions().setPackageName(previousPackage);
            throw new StudioBuildException(failure.getMessage(), failure);
        }
        DesignerCanvas canvas = uiContext.getDesignerCanvas();
        if (canvas != null && !canvas.isDisposed()) {
            canvas.setSelectedMetapackVersion(version);
            canvas.disableModuleInitialiseProcess();
        }
        uiContext.resetSelectionAfterDeletion();
        generation.whenComplete((ignored, failure) -> ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed() || uiContext.getIkasanModule() != module) return;
                    if (failure != null) {
                        StudioUIUtils.displayIdeaWarnMessage(project,
                                StudioBundle.message("message.ImportModelFailed", failure.getMessage()));
                        return;
                    }
                    try {
                        applyMatchingJdk(project, requiredJava);
                    } catch (com.intellij.openapi.progress.ProcessCanceledException cancelled) {
                        throw cancelled;
                    } catch (RuntimeException settingsFailure) {
                        LOG.warn("STUDIO: Could not configure the imported model's JDK", settingsFailure);
                        StudioUIUtils.displayIdeaWarnMessage(project,
                                StudioBundle.message("message.ImportModelJdkNotConfigured", requiredJava));
                    }
                    StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.ImportModelSuccess",
                            version, requiredJava > 0 ? String.valueOf(requiredJava) : StudioBundle.message("label.Unknown")));
                }));

        // Render the imported instance; reloading from disk would replace its identity while generation is pending.
        refreshDesignerFromModule(project);
    }

    /**
     * Brings the open designer up to date from the module already held in {@link UiContext} on the EDT.
     */
    private static void refreshDesignerFromModule(Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }
            UiContext uiContext = project.getService(UiContext.class);
            Module imported = uiContext.getIkasanModule();
            if (imported == null) {
                return;
            }
            if (uiContext.getCanvasPanel() != null) {
                uiContext.getCanvasPanel().disableH2Button(imported.getUseEmbeddedH2());
            }
            if (uiContext.getPalettePanel() != null) {
                uiContext.getPalettePanel().resetPallette();
            }
            DesignerCanvas canvas = uiContext.getDesignerCanvas();
            if (canvas != null && !canvas.isDisposed()) {
                canvas.disableModuleInitialiseProcess();
                canvas.setInitialiseAllDimensions(true);
                canvas.revalidate();
                canvas.repaint();
            }
        });
    }

    private static int requiredJava(String version) {
        try {
            MetaPackManifest manifest = ComponentLibrary.getMetaPackManifest(version);
            return manifest == null || manifest.javaVersion() == null ? -1 : Integer.parseInt(manifest.javaVersion());
        } catch (Exception e) {
            LOG.warn("STUDIO: Could not determine the Java version required by meta-pack " + version, e);
            return -1;
        }
    }

    /**
     * Applies the imported model's Java version to the IDE when a matching JDK is already registered. Missing JDKs
     * never block the import itself (the generated POM still carries the correct compiler settings) - the developer
     * gets a warning explaining what to install instead.
     */
    private static void applyMatchingJdk(Project project, int requiredJava) {
        if (requiredJava <= 0) {
            return;
        }
        List<Sdk> matching = new ArrayList<>();
        for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
            if (MigrationJdk.matches(sdk, requiredJava)) {
                matching.add(sdk);
            }
        }
        if (matching.isEmpty()) {
            StudioUIUtils.displayIdeaWarnMessage(project,
                    StudioBundle.message("message.ImportModelJdkNotConfigured", requiredJava));
            return;
        }
        Sdk currentSdk = ProjectRootManager.getInstance(project).getProjectSdk();
        Sdk chosen = matching.stream()
                .filter(sdk -> currentSdk != null && sdk.getName().equals(currentSdk.getName()))
                .findFirst()
                .orElse(matching.get(0));
        MigrationJdk.apply(project, chosen, requiredJava);
    }
}
