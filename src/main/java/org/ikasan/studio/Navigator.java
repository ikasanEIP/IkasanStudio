package org.ikasan.studio;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.NavigatableFileEditor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.ikasan.studio.ui.Context;
import org.jetbrains.annotations.NotNull;


public class Navigator {
    private Navigator () {}

    /**
     * Navigates to source of given class at specified offset.
     * @param classToNavigateTo navigate to source of this class
     */
    public static void navigateToSource (String projectKey, @NotNull PsiElement classToNavigateTo)
    {
        PsiFile containingFile = classToNavigateTo.getContainingFile ();
        VirtualFile virtualFile = containingFile.getVirtualFile ();
        if (virtualFile != null)
        {
            FileEditorManager manager = FileEditorManager.getInstance (Context.getProject(projectKey));
            FileEditor[] fileEditors = manager.openFile (virtualFile, true);
            if (fileEditors.length > 0)
            {
                FileEditor fileEditor = fileEditors [0];
                if (fileEditor instanceof NavigatableFileEditor navigatableFileEditor)
                {
                    Navigatable descriptor = new OpenFileDescriptor (Context.getProject(projectKey), virtualFile, classToNavigateTo.getTextOffset ());
                    navigatableFileEditor.navigateTo (descriptor);
                }
            }
        }
    }

       /**
     * Navigates to source of given class at specified offset.
     * @param classToNavigateTo navigate to source of this class
     * @param offset navigate to this offset within source
     */
       public static void navigateToSource (String projectKey, @NotNull PsiElement classToNavigateTo, int offset)
    {
        PsiFile containingFile = classToNavigateTo.getContainingFile ();
        VirtualFile virtualFile = containingFile.getVirtualFile ();
        if (virtualFile != null)
        {
            FileEditorManager manager = FileEditorManager.getInstance (Context.getProject(projectKey));
            FileEditor[] fileEditors = manager.openFile (virtualFile, true);
            if (fileEditors.length > 0)
            {
                FileEditor fileEditor = fileEditors [0];
                if (fileEditor instanceof NavigatableFileEditor navigatableFileEditor)
                {
                    Navigatable descriptor = new OpenFileDescriptor (Context.getProject(projectKey), virtualFile, offset);
                    navigatableFileEditor.navigateTo (descriptor);
                }
            }
        }
    }
}
