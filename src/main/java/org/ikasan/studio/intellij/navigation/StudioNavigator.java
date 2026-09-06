package org.ikasan.studio.intellij.navigation;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.application.ApplicationManager;
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
     * Navigates to source of given class at specified offset.
     * @param classToNavigateTo navigate to source of this class
     */
    public static void navigateToSource(Project project, PsiElement classToNavigateTo)
    {
        if (classToNavigateTo == null || !classToNavigateTo.isValid()) {
            LOG.warn("STUDIO: WARNING, attempt to invoke navigator but the class to navigate to was null or invalid [" +
                    classToNavigateTo + "], consider re-searching for file" + Arrays.toString(Thread.currentThread().getStackTrace()));
        } else {
            PsiFile containingFile = classToNavigateTo.getContainingFile();
            VirtualFile virtualFile = containingFile.getVirtualFile();
            if (virtualFile != null) {
                FileEditorManager manager = FileEditorManager.getInstance(project);
                FileEditor[] fileEditors = manager.openFile(virtualFile, true);
                if (fileEditors.length > 0) {
                    FileEditor fileEditor = fileEditors[0];
                    if (fileEditor instanceof NavigatableFileEditor navigatableFileEditor) {
                        Navigatable descriptor = new OpenFileDescriptor(project, virtualFile, classToNavigateTo.getTextOffset());
                        navigatableFileEditor.navigateTo(descriptor);
                    }
                }
            }
        }
    }

       /**
     * Navigates to source of given class at specified offset.
     * @param classToNavigateTo navigate to source of this class
     * @param offset navigate to this offset within source
     */
    public static void navigateToSource(Project project, PsiElement classToNavigateTo, int offset)
    {
        if (classToNavigateTo == null || !classToNavigateTo.isValid()) {
            Thread thread = Thread.currentThread();
            LOG.warn("STUDIO: WARNING, attempt to invoke navigator with offset but the class to navigate to was null or invalid, consider re-searching for file" + Arrays.toString(thread.getStackTrace()));
        } else {
            PsiFile containingFile = classToNavigateTo.getContainingFile ();
            VirtualFile virtualFile = containingFile.getVirtualFile ();
            if (virtualFile != null && containingFile.isValid())
            {
                FileEditorManager manager = FileEditorManager.getInstance (project);
                FileEditor[] fileEditors = manager.openFile (virtualFile, true);
                if (fileEditors.length > 0)
                {
                    FileEditor fileEditor = fileEditors [0];
                    if (fileEditor instanceof NavigatableFileEditor navigatableFileEditor)
                    {
                        Navigatable descriptor = new OpenFileDescriptor (project, virtualFile, offset);
                        navigatableFileEditor.navigateTo (descriptor);
                    }
                }
            }
        }
    }
}
