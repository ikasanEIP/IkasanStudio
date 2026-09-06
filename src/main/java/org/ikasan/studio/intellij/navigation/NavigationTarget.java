package org.ikasan.studio.intellij.navigation;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;

/**
 * Platform-neutral handle to a "jump to code"/"jump to properties" target - wraps whatever PSI state IntelliJ
 * needs to navigate there, so view handlers (which live outside the {@code intellij} package) can hold and pass
 * this around without depending on {@code com.intellij.psi.*} themselves (see
 * ArchUnitBoundaryTest#platformHeavyApisRemainBehindKnownAdapters). Only this class and {@link StudioNavigator}
 * unwrap the underlying PSI types.
 */
public final class NavigationTarget {
    private static final NavigationTarget NONE = new NavigationTarget(null, null, 0);

    private final PsiFile psiFile;
    private final PsiClass psiClass;
    private final int offset;

    private NavigationTarget(PsiFile psiFile, PsiClass psiClass, int offset) {
        this.psiFile = psiFile;
        this.psiClass = psiClass;
        this.offset = offset;
    }

    public static NavigationTarget none() {
        return NONE;
    }

    /**
     * @param psiFile the file to navigate to, or null (yields {@link #none()})
     * @return a target for {@code psiFile}, at offset 0, resolving its main class (for a Java file) up front so
     * navigation lands on the class declaration rather than just the top of the file
     */
    public static NavigationTarget forFile(PsiFile psiFile) {
        if (psiFile == null) {
            return NONE;
        }
        PsiClass mainClass = null;
        if (psiFile instanceof PsiJavaFile javaFile) {
            PsiClass[] allClasses = javaFile.getClasses();
            if (allClasses.length > 0) {
                // for now, assume the main class is the first in the array
                mainClass = allClasses[0];
            }
        }
        return new NavigationTarget(psiFile, mainClass, 0);
    }

    /**
     * @return a copy of this target at {@code offset}, or {@link #none()} unchanged if this target is already empty
     */
    public NavigationTarget withOffset(int offset) {
        return isPresent() ? new NavigationTarget(psiFile, psiClass, offset) : this;
    }

    public boolean isPresent() {
        return psiFile != null;
    }

    public int offset() {
        return offset;
    }

    public PsiFile psiFile() {
        return psiFile;
    }

    /** @return the most specific element to navigate to - the resolved class if there is one, else the file itself */
    public PsiElement elementToNavigateTo() {
        return psiClass != null ? psiClass : psiFile;
    }
}
