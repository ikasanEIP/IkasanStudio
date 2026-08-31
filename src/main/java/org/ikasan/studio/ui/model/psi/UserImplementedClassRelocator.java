package org.ikasan.studio.ui.model.psi;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesUtil;
import com.intellij.refactoring.rename.RenameProcessor;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.generator.GeneratorUtils;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.model.StudioPsiUtils;

import static org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta.REQUIRES_STUB;
import static org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta.USER_IMPLEMENTED_CLASS_NAME;

/**
 * Keeps a component's generated Java class in step with a cross-flow canvas move (drag-and-drop) - see
 * {@code DesignerCanvas#completeComponentMove}, the sole caller.
 * -
 * {@code FlowElementMove#move} deliberately leaves {@code USER_IMPLEMENTED_CLASS_NAME} untouched (its own
 * javadoc: identity survives a move unchanged) since it is framework-independent core code with no PSI
 * access - but the PACKAGE a user-implemented class gets regenerated into is always recomputed fresh from
 * whichever flow currently contains the component. Left alone, a cross-flow move therefore ends with the
 * original file orphaned at the old flow's package while any later regeneration writes a fresh one at the new
 * package - and for anything other than Debug (whose stub is regenerated unconditionally on every save, see
 * {@code PIPSIIkasanModel}'s {@code isDebug()} gate), that regeneration would silently produce a blank stub,
 * discarding whatever the user had hand-written, since the model no longer has any record of where their real
 * content went. (For Debug specifically, and only Debug, the class's simple name is also flow-derived in the
 * first place - every other component type defaults its name from its own componentName field instead, so a
 * move never needs to touch the name, only the package - see {@link #computeNewClassName}.)
 * -
 * This class is the UI-layer follow-up that actually relocates the class once a cross-flow move has been
 * accepted, using the same IDE-grade refactor engine behind IntelliJ's own "Move Class"/"Rename" (rather than
 * copying file content by hand, which - per the analogous, deliberately-manual precedent for a module package
 * rename, see {@code ComponentPropertiesPanel#confirmModuleLevelEdit} - "never could" fix up every reference
 * as correctly as the real refactor).
 */
public final class UserImplementedClassRelocator {
    private static final Logger LOG = Logger.getInstance("#UserImplementedClassRelocator");

    private UserImplementedClassRelocator() {}

    /**
     * @param project is the Intellij project instance
     * @param module the moved component belongs to
     * @param movedElement the component that has just been (containment-wise) moved from oldFlow to newFlow
     * @param oldFlow the flow movedElement was in immediately before the move
     * @param newFlow the flow movedElement is in immediately after the move
     */
    public static void relocateIfNeeded(Project project, Module module, FlowElement movedElement, Flow oldFlow, Flow newFlow) {
        if (project == null || module == null || movedElement == null || oldFlow == null || newFlow == null
                || oldFlow == newFlow || movedElement.getComponentMeta() == null
                || !movedElement.getComponentMeta().isGeneratesUserImplementedClass()) {
            return;
        }

        Object requiresStubValue = movedElement.getPropertyValue(REQUIRES_STUB);
        if (requiresStubValue instanceof Boolean requiresStub && !requiresStub) {
            // userImplementedClassName points at an existing, user-supplied class Studio doesn't own/generate
            // for this component - never touch it.
            return;
        }

        Object rawOldClassName = movedElement.getPropertyValue(USER_IMPLEMENTED_CLASS_NAME);
        String oldClassName = rawOldClassName != null ? rawOldClassName.toString() : null;
        if (oldClassName == null || oldClassName.isBlank()) {
            // Nothing generated/named yet for this component - nothing to relocate.
            return;
        }

        String oldPackageName = GeneratorUtils.getUserImplementedClassesPackageName(module, oldFlow);
        String newClassName = computeNewClassName(module, newFlow, movedElement);
        String newPackageName = GeneratorUtils.getUserImplementedClassesPackageName(module, newFlow);

        if (newClassName.equals(oldClassName) && newPackageName.equals(oldPackageName)) {
            return;
        }

        VirtualFile oldFile = StudioPsiUtils.getUserImplementedClassFile(project, oldPackageName, oldClassName);
        if (oldFile == null) {
            // Nothing has been physically generated yet - just retarget the identity, the normal generation
            // path will create it correctly at the new location next pass.
            movedElement.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, newClassName);
            return;
        }

        if (movedElement.getComponentMeta().isDebug()) {
            // Debug's stub is unconditionally regenerated on every save
            // (PIPSIIkasanModel.generateAndSaveUserImplementClassStubsForFlow's
            // "|| component.getComponentMeta().isDebug()" gate), so nothing hand-written in debug() ever
            // survives a regeneration anyway - there is no content here worth preserving via a real refactor.
            StudioPsiUtils.deleteFile(project, oldFile);
            movedElement.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, newClassName);
            return;
        }

        relocateHandWrittenClass(project, movedElement, oldFile, newPackageName, newClassName);
    }

    /**
     * If the component TYPE's own {@code USER_IMPLEMENTED_CLASS_NAME} default-value template genuinely embeds
     * "__flow" (currently only Debug's "__flow__component", read the pristine template straight off the
     * component TYPE metadata's default value and re-derive it the same way
     * {@code DesignerCanvas#assignDebugIdentityAndClassName} does at creation time - substituted against the
     * destination flow. Every other component type's default template ("__fieldName:componentName" - derive
     * from the component's own name field, not from the flow) leaves the current class name exactly as-is;
     * only its package changes. Either way, guards against the destination flow already containing a
     * differently-created component whose class happens to resolve to the same name, appending 2, 3, ...
     * exactly like {@code assignDebugIdentityAndClassName}'s own anchor-collision suffixing does for Debug,
     * generalised here to cover every user-implemented-class component type.
     */
    private static String computeNewClassName(Module module, Flow newFlow, FlowElement movedElement) {
        ComponentPropertyMeta meta = movedElement.getComponentMeta().getMetadata(USER_IMPLEMENTED_CLASS_NAME);
        String pristineTemplate = meta != null && meta.getDefaultValue() != null ? meta.getDefaultValue().toString() : null;
        // Only Debug's default value template genuinely embeds "__flow" (e.g. "__flow__component") - every
        // other user-implemented-class component type defaults its class name from its own componentName
        // field instead (the "__fieldName:componentName" convention - see ComponentPropertyEditRow's
        // "Default" button), independent of which flow contains it. Checked generically on the template's
        // actual content, not by component type, so any future component type that does embed "__flow" is
        // picked up automatically too. A cross-flow move for anything that ISN'T flow-derived only needs its
        // PACKAGE to change - the class's simple name is left exactly as the user/component already has it.
        String base = pristineTemplate != null && pristineTemplate.contains(ComponentPropertyMeta.SUBSTITUTION_PREFIX_FLOW)
                ? StudioBuildUtils.substitutePlaceholderInPascalCase(module, newFlow, movedElement, pristineTemplate)
                : stringOrNull(movedElement.getPropertyValue(USER_IMPLEMENTED_CLASS_NAME));

        String candidate = base;
        int suffix = 2;
        while (isClassNameTakenInFlow(candidate, movedElement, newFlow)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private static boolean isClassNameTakenInFlow(String candidateClassName, FlowElement movedElement, Flow flow) {
        return flow.ftlGetConsumerAndFlowElements().stream()
                .anyMatch(sibling -> sibling != movedElement
                        && candidateClassName.equals(stringOrNull(sibling.getPropertyValue(USER_IMPLEMENTED_CLASS_NAME))));
    }

    private static String stringOrNull(Object value) {
        return value != null ? value.toString() : null;
    }

    /**
     * Physically relocate a component's already-generated, potentially hand-edited class to a new
     * package/name, preserving its content - via the same engine IntelliJ's own "Move Class" then "Rename"
     * refactors use, run non-interactively (no usage-preview UI, since this must complete unattended as part
     * of finishing a drag). Falls back to leaving everything exactly as it was (today's already-safe, if
     * imperfect, behaviour) and surfaces a one-time notification if anything about the refactor fails -
     * never partially updates the model's USER_IMPLEMENTED_CLASS_NAME unless the physical move actually
     * succeeded.
     */
    private static void relocateHandWrittenClass(Project project, FlowElement movedElement, VirtualFile oldFile,
                                                  String newPackageName, String newClassName) {
        StudioPsiUtils.backupFile(project, oldFile);
        boolean[] moved = {false};
        try {
            PsiFile psiFile = ReadAction.compute(() -> PsiManager.getInstance(project).findFile(oldFile));
            if (psiFile instanceof PsiJavaFile javaFile && javaFile.getClasses().length > 0) {
                PsiClass psiClass = javaFile.getClasses()[0];
                VirtualFile destinationDir = StudioPsiUtils.ensureUserImplementedClassPackageDirectory(project, newPackageName);
                if (destinationDir != null) {
                    PsiDirectory psiDestinationDir = ReadAction.compute(() -> PsiManager.getInstance(project).findDirectory(destinationDir));
                    if (psiDestinationDir != null) {
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            MoveClassesOrPackagesUtil.doMoveClass(psiClass, psiDestinationDir);
                        });
                        if (!newClassName.equals(psiClass.getName())) {
                            // RenameProcessor manages its own write action/command internally - run it as a
                            // separate step rather than nesting it inside the move's WriteCommandAction above.
                            new RenameProcessor(project, psiClass, newClassName, false, false).run();
                        }
                        moved[0] = true;
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("STUDIO: WARN: Unable to relocate user implemented class [" + movedElement.getComponentName() +
                    "] to package [" + newPackageName + "] class [" + newClassName + "] exception was " + e.getMessage());
        }

        if (moved[0]) {
            movedElement.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, newClassName);
        } else {
            StudioUIUtils.displayIdeaWarnMessage(project,
                    StudioBundle.message("message.CouldNotRelocateUserImplementedClass", movedElement.getComponentName()));
        }
    }
}
