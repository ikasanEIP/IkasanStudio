package org.ikasan.studio;

import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.NavigatableFileEditor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.Arrays;


public class Navigator {
    private static final Logger LOG = Logger.getInstance("#Navigator");
    private Navigator () {}

    public static void navigateToClass(Project project, String fullyQualifiedClassName) {
        // Find the class by its fully qualified name
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fullyQualifiedClassName, GlobalSearchScope.allScope(project));

        if (psiClass == null) {
            LOG.warn("STUDIO: WARNING, attempt to invoke navigator but the class was not found [" +
                    fullyQualifiedClassName + "]" + Arrays.toString(Thread.currentThread().getStackTrace()));
        }
        VirtualFile virtualFile = psiClass.getContainingFile().getVirtualFile();
        if (virtualFile == null) {
            LOG.warn("STUDIO: WARNING, attempt to invoke navigator but the virtualFile was not found [" +
                    fullyQualifiedClassName + "]" + Arrays.toString(Thread.currentThread().getStackTrace()));
        }

        // Navigate to the class
        PsiNavigationSupport.getInstance().createNavigatable(project, virtualFile, psiClass.getTextOffset()).navigate(true);
    }

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
