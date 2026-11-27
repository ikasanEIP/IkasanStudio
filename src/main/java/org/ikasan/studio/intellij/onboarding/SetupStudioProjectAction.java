package org.ikasan.studio.intellij.onboarding;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import java.nio.file.Path;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.project.MavenProjectBootstrap;
import org.ikasan.studio.core.project.MavenProjectBootstrap.Eligibility;
import org.ikasan.studio.core.project.MavenProjectBootstrap.Plan;
import org.ikasan.studio.core.project.MavenProjectBootstrap.Options;
import org.ikasan.studio.intellij.editor.IkasanStudioEditorService;
import org.ikasan.studio.intellij.migration.MigrationJdk;
import org.ikasan.studio.intellij.project.StudioProjectInitialisationService;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

/** Explicit setup of an existing, simple Maven project; no project is converted automatically. */
public final class SetupStudioProjectAction extends DumbAwareAction {
    @Override public ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
    @Override public void update(AnActionEvent event) { event.getPresentation().setEnabled(event.getProject() != null && !event.getProject().isDisposed()); }
    @Override public void actionPerformed(AnActionEvent event) { open(event.getProject()); }

    public static void open(Project project) {
        if (project == null || project.isDisposed()) return;
        String title = StudioBundle.message("setup.Title");
        UiContext context = project.getService(UiContext.class);
        boolean acquired = false, applied = false, attempted = false;
        try {
            String base = project.getBasePath();
            if (base == null || base.isBlank()) throw new IllegalStateException(StudioBundle.message("setup.OpenMaven"));
            Path root = Path.of(base);
            Eligibility eligibility = background(project, title, () -> MavenProjectBootstrap.inspect(root));
            if (!eligibility.eligible()) { Messages.showInfoMessage(project, eligibility.detail(), title); return; }
            Map<String, Integer> packs = background(project, title, () -> {
                Map<String, Integer> versions = new LinkedHashMap<>();
                for (String pack : ComponentLibrary.getMetapackList(false))
                    versions.put(pack, Integer.parseInt(ComponentLibrary.getMetaPackManifest(pack).javaVersion()));
                return versions;
            });
            if (packs.isEmpty()) throw new IllegalStateException(StudioBundle.message("setup.NoPacks"));
            SetupStudioProjectDialog choices = new SetupStudioProjectDialog(project, eligibility, packs);
            if (!choices.showAndGet()) return;
            if (!context.tryBeginMigration()) throw new IllegalStateException(StudioBundle.message("message.WaitForTheCurrentGenerationOrMigrationToFinish"));
            acquired = true;
            checkSaved(project);
            Options options = choices.options();
            Plan plan = background(project, title, () -> MavenProjectBootstrap.prepare(root, options));
            if (!new SetupStudioProjectReviewDialog(project, plan).showAndGet()) return;
            checkSaved(project);
            Sdk sdk = choices.sdk();
            int required = choices.requiredJava();
            if (!MigrationJdk.matches(sdk, required) || sdk.getHomePath() == null || !JavaSdk.getInstance().isValidSdkHome(sdk.getHomePath()))
                throw new IllegalStateException(StudioBundle.message("message.SelectAnInstalledJdk", required));
            attempted = true;
            Path backup = background(project, title, () -> MavenProjectBootstrap.apply(root, plan, ProgressManager::checkCanceled));
            applied = true;
            refreshProjectFiles(project, root, title);
            MigrationJdk.apply(project, sdk, required);
            context.setIkasanPomModel(null);
            context.setApplicationProperties(null);
            context.allowModelPersistence();
            MavenProjectsManager.getInstance(project).forceUpdateAllProjectsOrFindAllAvailablePomFiles();
            project.getService(StudioProjectInitialisationService.class).reloadModel();
            project.getService(IkasanStudioEditorService.class).open();
            StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("setup.Complete", backup));
        } catch (ProcessCanceledException cancelled) { throw cancelled; }
        catch (Exception failure) {
            if (attempted && !applied && !project.isDisposed()) {
                try { refreshProjectFiles(project, Path.of(project.getBasePath()), title); }
                catch (Exception refreshFailure) { failure.addSuppressed(refreshFailure); }
            }
            if (failure.getCause() instanceof ProcessCanceledException cancelled && failure.getSuppressed().length == 0) throw cancelled;
            if (!project.isDisposed()) Messages.showErrorDialog(project, StudioBundle.message(applied ? "setup.ReloadFailed" : "setup.Failed", failure.getMessage()), title);
        } finally { if (acquired) context.endMigration(); }
    }

    private static void refreshProjectFiles(Project project, Path root, String title) throws Exception {
        background(project, title, () -> {
            VirtualFile workspace = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(root);
            if (workspace != null) workspace.refresh(false, true);
            return null;
        });
        VirtualFile pom = LocalFileSystem.getInstance().findFileByNioFile(root.resolve("pom.xml"));
        Document document = pom == null ? null : FileDocumentManager.getInstance().getCachedDocument(pom);
        if (document != null) ApplicationManager.getApplication().runWriteAction(() -> {
            FileDocumentManager.getInstance().reloadFromDisk(document);
            PsiDocumentManager.getInstance(project).commitDocument(document);
        });
    }

    private static void checkSaved(Project project) {
        String root = project.getBasePath();
        for (Document document : FileDocumentManager.getInstance().getUnsavedDocuments()) {
            VirtualFile file = FileDocumentManager.getInstance().getFile(document);
            if (file != null && root != null && file.getPath().startsWith(root + "/"))
                throw new IllegalStateException(StudioBundle.message("setup.SaveFirst"));
        }
    }
    private static <T> T background(Project project, String title, Callable<T> work) throws Exception {
        AtomicReference<T> result = new AtomicReference<>(); AtomicReference<Exception> failure = new AtomicReference<>();
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try { result.set(work.call()); }
            catch (ProcessCanceledException cancelled) { throw cancelled; }
            catch (Exception error) { failure.set(error); }
        }, title, false, project);
        if (failure.get() != null) throw failure.get();
        return result.get();
    }
}
