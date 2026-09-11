package org.ikasan.studio.intellij.psi;

import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.concurrency.AppExecutorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Centralised PSI API calls for UI classes that need to resolve/inspect project classes, so those classes
 * never depend on {@code com.intellij.psi.*} themselves (see
 * ArchUnitBoundaryTest#platformHeavyApisRemainBehindKnownAdapters). Every lookup here runs off the EDT via
 * {@link ReadAction#nonBlocking}, since the platform now hard-refuses stub-index access on the EDT, and
 * reports back on the UI thread at the caller's chosen modality.
 */
public final class StudioPsiUtils {
    private StudioPsiUtils() {}

    /**
     * Shows IntelliJ's project-scope "choose a class" dialog.
     * @return the chosen class's fully qualified name, or null if nothing was chosen
     */
    public static String chooseProjectClassQualifiedName(Project project, String dialogTitle) {
        TreeClassChooser chooser = TreeClassChooserFactory.getInstance(project).createProjectScopeChooser(dialogTitle);
        chooser.showDialog();
        PsiClass selected = chooser.getSelected();
        return selected != null ? selected.getQualifiedName() : null;
    }

    /**
     * Shows the class chooser including project sources, dependencies and the configured JDK.
     * Class-literal properties can name any of these types, such as java.lang.String.
     * @return the chosen class's fully qualified name, or null on cancellation
     */
    public static String chooseClassQualifiedName(Project project, String dialogTitle) {
        TreeClassChooser chooser = TreeClassChooserFactory.getInstance(project).createAllProjectScopeChooser(dialogTitle);
        chooser.showDialog();
        PsiClass selected = chooser.getSelected();
        return selected != null ? selected.getQualifiedName() : null;
    }

    /** Shows the native package chooser; cancellation leaves the caller's value unchanged. */
    public static String chooseProjectPackageQualifiedName(Project project, String dialogTitle) {
        var chooser = new com.intellij.ide.util.PackageChooserDialog(dialogTitle, project);
        if (!chooser.showAndGet()) {
            return null;
        }
        var selected = chooser.getSelectedPackage();
        return selected != null ? selected.getQualifiedName() : null;
    }

    /**
     * Resolves {@code qualifiedClassName} and reports whether it is assignable to {@code java.io.Serializable}.
     * @param includeLibrariesAndSdk true to also search libraries/SDK classes (matches
     *                                {@code GlobalSearchScope#allScope}), false to search only the project's
     *                                own sources (matches {@code GlobalSearchScope#projectScope})
     * @param callback told null if the class could not be resolved at all, otherwise the inheritance result
     */
    public static void isClassSerializable(Project project, Disposable parentDisposable, ModalityState modality,
                                            String qualifiedClassName, boolean includeLibrariesAndSdk,
                                            Consumer<Boolean> callback) {
        ReadAction.nonBlocking(() -> {
                    GlobalSearchScope scope = includeLibrariesAndSdk
                            ? GlobalSearchScope.allScope(project) : GlobalSearchScope.projectScope(project);
                    PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedClassName, scope);
                    return psiClass != null ? InheritanceUtil.isInheritor(psiClass, "java.io.Serializable") : null;
                })
                .expireWith(parentDisposable)
                .finishOnUiThread(modality, callback)
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    /** A class field's name and canonical type text (e.g. "java.lang.String", "int[]") - no PsiField/PsiType. */
    public record FieldSample(String name, String canonicalTypeText) {}

    /**
     * Resolves {@code qualifiedClassName} (searched within the project's own sources only) and reports its own
     * non-static/non-transient fields.
     * @param callback told null if the class could not be resolved, otherwise its field samples
     */
    public static void resolveClassFields(Project project, Disposable parentDisposable, ModalityState modality,
                                           String qualifiedClassName, Consumer<List<FieldSample>> callback) {
        ReadAction.nonBlocking(() -> {
                    PsiClass psiClass = JavaPsiFacade.getInstance(project)
                            .findClass(qualifiedClassName, GlobalSearchScope.projectScope(project));
                    if (psiClass == null) {
                        return null;
                    }
                    List<FieldSample> fields = new ArrayList<>();
                    for (PsiField field : psiClass.getAllFields()) {
                        if (field.hasModifierProperty(PsiModifier.STATIC) || field.hasModifierProperty(PsiModifier.TRANSIENT)) {
                            continue;
                        }
                        fields.add(new FieldSample(field.getName(), field.getType().getCanonicalText()));
                    }
                    return fields;
                })
                .expireWith(parentDisposable)
                .finishOnUiThread(modality, callback)
                .submit(AppExecutorUtil.getAppExecutorService());
    }
}
