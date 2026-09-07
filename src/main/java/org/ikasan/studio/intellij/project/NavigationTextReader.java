package org.ikasan.studio.intellij.project;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.ikasan.studio.intellij.navigation.NavigationTarget;
import java.util.function.Consumer;

/** No document loading, PSI validity checks or Java class lookup on the event dispatch thread. */
final class NavigationTextReader {
    record Snapshot(String text, NavigationTarget target) { }
    private NavigationTextReader() { }

    static void read(Project project, PsiFile file, Consumer<Snapshot> apply) {
        read(project, () -> file, apply);
    }

    static void read(Project project, java.util.function.Supplier<PsiFile> resolve, Consumer<Snapshot> apply) {
        // Project itself is deliberately not used as the expiry Disposable here - the platform's plugin
        // guidelines flag Project/Application as parents to avoid, since they can accumulate disposables for
        // the whole session; StudioProjectInitialisationService is this plugin's own project-scoped Disposable
        // with the same effective lifetime (created for the project, disposed when it closes) - see
        // GeneratedProjectSynchronizer's own identical comment on the same tradeoff.
        ReadAction.nonBlocking(() -> readSnapshot(resolve.get()))
                .expireWith(project.getService(StudioProjectInitialisationService.class))
                .finishOnUiThread(ModalityState.defaultModalityState(), snapshot -> {
                    if (!project.isDisposed() && snapshot != null) apply.accept(snapshot);
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    static Snapshot readSnapshot(PsiFile file) {
        // assertIsNonDispatchThread() is @ApiStatus.Experimental - isDispatchThread() is the long-standing
        // stable API this codebase already relies on elsewhere (e.g. IkasanDebugSessionService), so check and
        // throw manually rather than depend on an unstable assertion method.
        if (ApplicationManager.getApplication().isDispatchThread()) {
            throw new IllegalStateException("NavigationTextReader.readSnapshot() must not run on the EDT");
        }
        ApplicationManager.getApplication().assertReadAccessAllowed();
        if (file == null || !file.isValid()) return null;
        var virtualFile = file.getVirtualFile();
        var document = virtualFile == null ? null : FileDocumentManager.getInstance().getDocument(virtualFile);
        return new Snapshot(document == null ? file.getText() : document.getText(), NavigationTarget.forFile(file));
    }
}
