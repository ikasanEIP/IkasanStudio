package org.ikasan.studio.intellij.navigation;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.NavigatableFileEditor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.concurrency.AppExecutorUtil;

import java.nio.file.Path;
import java.util.Arrays;


public final class StudioNavigator {
    private static final Logger LOG = Logger.getInstance("#Navigator");
    private StudioNavigator () {}

    /**
     * Resolves an arbitrary on-disk file (not necessarily inside a project content root, e.g. the embedded
     * test FTP server's seed file) to the IntelliJ VFS and opens it in an editor tab - callers never need to
     * depend on {@code VirtualFile}/{@code LocalFileSystem} themselves (see
     * ArchUnitBoundaryTest#platformHeavyApisRemainBehindKnownAdapters). Safe to call off the EDT; the actual
     * navigation is deferred via invokeLater.
     * @param path of the file to open, may be null
     * @return true if the file was found and an open was scheduled, false if it could not be resolved
     */
    public static boolean openFileByNioPath(Project project, Path path) {
        VirtualFile virtualFile = path == null ? null : LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
        if (virtualFile == null) {
            return false;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) new OpenFileDescriptor(project, virtualFile).navigate(true);
        });
        return true;
    }

    /**
     * Resolves an arbitrary on-disk directory (e.g. the embedded test FTP server's root) to the IntelliJ VFS
     * and selects it in the Project view - same rationale/threading as {@link #openFileByNioPath}.
     * @param path of the directory to select, may be null
     * @return true if the directory was found and a selection was scheduled, false if it could not be resolved
     */
    public static boolean selectInProjectView(Project project, Path path) {
        VirtualFile virtualFile = path == null ? null : LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
        if (virtualFile == null) {
            return false;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) ProjectView.getInstance(project).selectCB(null, virtualFile, true);
        });
        return true;
    }

//    public static void navigateToClass(Project project, String fullyQualifiedClassName) {
//        // Find the class by its fully qualified name
//        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fullyQualifiedClassName, GlobalSearchScope.allScope(project));
//
//        if (psiClass == null) {
//            LOG.warn("STUDIO: WARNING, attempt to invoke navigator but the class was not found [" +
//                    fullyQualifiedClassName + "]" + Arrays.toString(Thread.currentThread().getStackTrace()));
//            return;
//        }
//        VirtualFile virtualFile = psiClass.getContainingFile().getVirtualFile();
//        if (virtualFile == null) {
//            LOG.warn("STUDIO: WARNING, attempt to invoke navigator but the virtualFile was not found [" +
//                    fullyQualifiedClassName + "]" + Arrays.toString(Thread.currentThread().getStackTrace()));
//            return;
//        }
//
//        // Navigate to the class
//        PsiNavigationSupport.getInstance().createNavigatable(project, virtualFile, psiClass.getTextOffset()).navigate(true);
//    }

    /**
     * Navigates to source of given class, at the element's own text offset.
     * @param classToNavigateTo navigate to source of this class
     */
    public static void navigateToSource(Project project, PsiElement classToNavigateTo)
    {
        navigateToSourceAsync(project, classToNavigateTo, null);
    }

    /**
     * Navigates to a {@link NavigationTarget} resolved by a view handler - lets callers outside the
     * {@code intellij} package (e.g. NavigateToCodeAction/NavigateToPropertiesAction) trigger navigation without
     * themselves depending on {@code com.intellij.psi.*} (see
     * ArchUnitBoundaryTest#platformHeavyApisRemainBehindKnownAdapters).
     * @param target to navigate to; a no-op if null or {@link NavigationTarget#isPresent()} is false
     * @param preferOffset true to navigate to {@link NavigationTarget#offset()} when the target has a non-zero
     *                     one, false to always navigate to the top of the resolved element
     */
    public static void navigateToSource(Project project, NavigationTarget target, boolean preferOffset) {
        if (target == null || !target.isPresent()) {
            return;
        }
        if (preferOffset && target.offset() != 0) {
            navigateToSource(project, target.elementToNavigateTo(), target.offset());
        } else {
            navigateToSource(project, target.elementToNavigateTo());
        }
    }

    /**
     * Navigates to source of given class at specified offset.
     * @param classToNavigateTo navigate to source of this class
     * @param offset navigate to this offset within source
     */
    public static void navigateToSource(Project project, PsiElement classToNavigateTo, int offset)
    {
        navigateToSourceAsync(project, classToNavigateTo, offset);
    }

    private record ResolvedNavigation(VirtualFile virtualFile, int offset) {}

    /**
     * Every caller here (a canvas double-click, a "Jump to code" menu item) reaches this already running on the
     * EDT - {@code PsiElement#isValid()}/{@code getContainingFile()} touch the stub index, which IntelliJ
     * 2024.3+ hard-refuses to do synchronously there ("Slow operations are prohibited on EDT" - see
     * SlowOperations.assertSlowOperationsAreAllowed). {@link ReadAction#nonBlocking} runs that PSI resolution on
     * a pooled thread under a read action, then hops back to the EDT - at the default modality, since callers
     * here are plain canvas actions, not a modal dialog with its own modality to match (contrast
     * StudioPsiUtils's callers) - to do the actual editor open/navigate, which itself must stay on the EDT.
     * expireWith(project) cancels the read action if the project closes before it completes.
     * @param offset null to navigate to the resolved element's own text offset, otherwise navigate to this
     *               specific offset instead (both still resolved off the EDT, since PsiElement#getTextOffset()
     *               is itself PSI access).
     */
    private static void navigateToSourceAsync(Project project, PsiElement classToNavigateTo, Integer offset) {
        ReadAction.nonBlocking(() -> {
                    if (classToNavigateTo == null || !classToNavigateTo.isValid()) {
                        return null;
                    }
                    PsiFile containingFile = classToNavigateTo.getContainingFile();
                    VirtualFile virtualFile = containingFile != null ? containingFile.getVirtualFile() : null;
                    if (virtualFile == null || !containingFile.isValid()) {
                        return null;
                    }
                    int resolvedOffset = offset != null ? offset : classToNavigateTo.getTextOffset();
                    return new ResolvedNavigation(virtualFile, resolvedOffset);
                })
                .expireWith(project)
                .finishOnUiThread(ModalityState.defaultModalityState(), resolved -> {
                    if (resolved == null) {
                        LOG.warn("STUDIO: WARNING, attempt to invoke navigator but the class to navigate to was null or invalid [" +
                                classToNavigateTo + "], consider re-searching for file" + Arrays.toString(Thread.currentThread().getStackTrace()));
                        return;
                    }
                    FileEditorManager manager = FileEditorManager.getInstance(project);
                    FileEditor[] fileEditors = manager.openFile(resolved.virtualFile(), true);
                    if (fileEditors.length > 0 && fileEditors[0] instanceof NavigatableFileEditor navigatableFileEditor) {
                        Navigatable descriptor = new OpenFileDescriptor(project, resolved.virtualFile(), resolved.offset());
                        navigatableFileEditor.navigateTo(descriptor);
                    }
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }
}
