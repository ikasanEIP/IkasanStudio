package org.ikasan.studio.intellij.navigation;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;

/**
 * Navigation handle held by view handlers without exposing PSI to the UI package.
 * Keep the file identity rather than a Java class: regeneration and document commits
 * can replace the class's PSI while the canvas still holds this target.
 */
public final class NavigationTarget {
    private static final NavigationTarget NONE = new NavigationTarget(null, null, null, 0);

    private final Project project;
    private final VirtualFile virtualFile;
    private final SmartPsiElementPointer<PsiFile> filePointer;
    private final int offset;

    private NavigationTarget(Project project, VirtualFile virtualFile,
                             SmartPsiElementPointer<PsiFile> filePointer, int offset) {
        this.project = project;
        this.virtualFile = virtualFile;
        this.filePointer = filePointer;
        this.offset = offset;
    }

    public static NavigationTarget none() { return NONE; }

    /** Create under a read action; resolve the current class only when navigating. */
    public static NavigationTarget forFile(PsiFile psiFile) {
        if (psiFile == null || !psiFile.isValid()) return NONE;
        return new NavigationTarget(psiFile.getProject(), psiFile.getVirtualFile(),
                SmartPointerManager.getInstance(psiFile.getProject()).createSmartPsiElementPointer(psiFile), 0);
    }

    public NavigationTarget withOffset(int offset) {
        return isPresent() ? new NavigationTarget(project, virtualFile, filePointer, offset) : this;
    }

    /** Does not touch PSI; safe for canvas action availability on the EDT. */
    public boolean isPresent() { return filePointer != null; }

    public int offset() { return offset; }

    /** Resolve under a read action, off the EDT in production. */
    public PsiFile psiFile() {
        if (!isPresent() || project.isDisposed()) return null;
        PsiFile file = filePointer.getElement();
        if (file != null && file.isValid()) return file;
        return virtualFile != null && virtualFile.isValid()
                ? PsiManager.getInstance(project).findFile(virtualFile) : null;
    }

    /** Resolve the current Java declaration, falling back to the file if it has no class. */
    public PsiElement elementToNavigateTo() {
        PsiFile file = psiFile();
        if (file instanceof PsiJavaFile javaFile) {
            var classes = javaFile.getClasses();
            if (classes.length > 0) return classes[0];
        }
        return file;
    }
}
