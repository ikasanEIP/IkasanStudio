package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.generation.GenerationRequest;
import org.ikasan.studio.core.generator.GeneratorUtils;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.theme.ThemeAwareColors;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.VERSION;
import static org.ikasan.studio.ui.UiContext.PALETTE_TAB_INDEX;

/**
 * Encapsulate the properties entry from a UI and validity perspective.
 */
@SuppressWarnings("rawtypes")
public class ComponentPropertiesPanel extends PropertiesPanel {
    public static final Logger LOG = Logger.getInstance("ComponentPropertiesPanel");
    private transient List<ComponentPropertyEditRow> componentPropertyEditRowList;
    private HtmlScrollingDisplayPanel htmlScrollingDisplayPanel;
    private boolean isExpanded;
    @SuppressWarnings("rawtypes")
    private JBPanel optionalPropertiesEditorPanel;
    @SuppressWarnings("rawtypes")
    private JBPanel optionalPropertiesExpandPanel;
    private JButton toggleOptionalPropertiesButton;
    private JButton setDefaultsButton;
    private JButton clearDefaultsButton;
    // Only visible when the selected component is a FlowUserImplementedElement (Broker, Converter, etc.) - see
    // populatePropertiesEditorPanel(). Lets the user force a resync of a hand-written class against its current
    // properties without first having to change (and revert) some unrelated property just to trigger doOKAction's
    // changed-property-driven regenerate.
    private JButton regenerateClassButton;
    private final SimpleChangeListener listenerForAnyEditChanges;
    private final Map<String, ComponentPropertyEditRow> componentPropertyEditBoxMap = new HashMap<>();

    /**
     * Group display order: for everything else, the order groups were first encountered while walking properties
     * sorted by propertyDisplayOrder - i.e. whichever group's lowest-propertyDisplayOrder member is smallest comes
     * first, letting metapack authors control group sequence the same way they control in-group property order.
     * "Miscellaneous" is always second-to-last and "advanced" is always last, regardless of that natural order.
     */
    private static Comparator<String> groupDisplayOrderFor(Map<String, ?> groupedOptionalProperties) {
        List<String> naturalOrder = new ArrayList<>(groupedOptionalProperties.keySet());
        return Comparator.comparingInt(ComponentPropertiesPanel::groupDisplayRank).thenComparingInt(naturalOrder::indexOf);
    }

    private static int groupDisplayRank(String group) {
        if (ComponentPropertyMeta.PROPERTY_GROUP_ADVANCED.equalsIgnoreCase(group)) return 2;
        if (ComponentPropertyMeta.PROPERTY_GROUP_MISCELLANEOUS.equalsIgnoreCase(group)) return 1;
        return 0;
    }

    private static String groupDisplayLabel(String group) {
        if (group == null || group.isEmpty()) return group;
        return Character.toUpperCase(group.charAt(0)) + group.substring(1);
    }

    /**
     * Convenience wrapper for cleaner code in this class.
     * Delegates to centralized ThemeAwareColors utility.
     */
    private static Color getThemeAwareBackgroundColor() {
        return ThemeAwareColors.getBackgroundColor();
    }

    /**
     * Convenience wrapper for cleaner code in this class.
     * Delegates to centralized ThemeAwareColors utility.
     */
    private static Color getThemeAwareBorderColor() {
        return ThemeAwareColors.getBorderColor();
    }
    /**
     * Create the ComponentPropertiesPanel
     * Note that this panel could be reused for different ComponentPropertiesPanel, it is the super.updateTargetComponent
     * that will set the property to be exposed / edited.
     * @param project is the Intellij project instance
     * @param componentInitialisation true if this is for the popup version i.e. the first configuration of this component,
     *                                false if this is for the canvas sidebar.
     */
    public ComponentPropertiesPanel(Project project, boolean componentInitialisation) {
        super(project, componentInitialisation, true);
        this.setBorder(null);
        listenerForAnyEditChanges = () -> {
            List<ValidationInfo> validationIssues = doValidateAll();
            boolean hasValidationIssues = !validationIssues.isEmpty();
            boolean okToProcess = dataHasChangedAndOKToProcess() && !hasValidationIssues;
            // A disabled button still shows its tooltip on hover (Swing dispatches hover/mouse-motion events to
            // disabled components; only the click itself is suppressed), and the pulsating border draws the
            // developer's eye there in the first place - without both, a validation failure (e.g. a duplicate
            // component name) just disabled the button with no visible explanation at all. Applied to both the
            // in-canvas sidebar's own button AND, via getPropertiesDialogue(), the first-time popup's OK button -
            // DialogWrapper's own doValidateAll()-based balloon requires explicitly opting in to continuous
            // validation (startTrackingValidation()), which this codebase never does, so the popup had exactly
            // the same silent-disable problem.
            String tooltip = hasValidationIssues ? StudioUIUtils.joinValidationMessages(validationIssues) : null;
            if (updateCodeButton != null) {
                updateCodeButton.setEnabled(okToProcess);
                updateCodeButton.setToolTipText(tooltip);
                StudioUIUtils.setAttentionPulse(updateCodeButton, hasValidationIssues);
            }
            if (getPropertiesDialogue() != null) {
                getPropertiesDialogue().setOKActionEnabled(okToProcess);
                getPropertiesDialogue().setValidationFeedback(hasValidationIssues, tooltip);
            }
            // Regenerating the stub class only makes sense once the model is in a settled state - not while
            // Update Code is enabled (there are unsaved edits it would ignore, regenerating against stale
            // values) and not while it's pulsating (the edits aren't even valid yet). Disabling it in both
            // cases avoids a confusing "regenerate against what, exactly?" moment.
            if (regenerateClassButton != null) {
                regenerateClassButton.setEnabled(!okToProcess && !hasValidationIssues);
            }
        };
        if (footerPanel != null) {
            regenerateClassButton = new JButton(StudioBundle.message("button.RegenerateClass"));
            regenerateClassButton.setToolTipText(StudioBundle.message("tooltip.RegenerateClass"));
            regenerateClassButton.addActionListener(e -> regenerateSelectedUserImplementedClass());
            regenerateClassButton.setVisible(false);
            footerPanel.add(regenerateClassButton);
        }
    }

    /**
     * This method is invoked when we have checked it's OK to process the panel i.e. all items are valid
     */
    protected void doOKAction() {
        // Changed properties that affect a user implemented (autogenerated stub) class require explicit,
        // per-save confirmation before we regenerate that class - declining aborts the whole save.
        List<String> changedAffectingProperties = getChangedAffectsUserImplementedClassPropertyLabels();
        if (!changedAffectingProperties.isEmpty() && !confirmRegenerateUserImplementedClass(changedAffectingProperties)) {
            return;
        }

        if (dataHasChangedAndOKToProcess()) {
            UiContext uiContext = project.getService(UiContext.class);
            StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.CodeGenerationInProgressPleaseWait"));
            // If the meta version has changed, we need to rerender the screen
            boolean metaPackChanged = getSelectedComponent().getComponentMeta().isModule() && propertyHasChanged(VERSION);
            updateComponentsWithNewValues();
            if (getSelectedComponent() instanceof FlowElement router
                    && router.getComponentMeta().isRouter()
                    && router.getContainingFlowRoute() != null) {
                // routeNames may just have changed (a route added/renamed) - childRoutes only ever get built
                // from it during a full model.json reload, so a live edit needs the same sync a fresh router
                // drop does (see DesignerCanvas#syncChildRoutesForRouter), or new branches have nothing to
                // drop components into.
                try {
                    router.getContainingFlowRoute().syncChildRoutesForRouter(uiContext.getIkasanModule().getMetaVersion(), router);
                } catch (StudioBuildException se) {
                    LOG.warn("STUDIO: A studio exception was raised while syncing child routes for router " + router.getIdentity() + ", please investigate: " + se.getMessage() + " Trace: " + Arrays.asList(se.getStackTrace()));
                }
            }
            // ComponentPropertyEditRow#initialValue is captured once, at row construction, and is immutable -
            // rebuilding the rows against the just-committed model is the only way to make
            // dataHasChangedAndOKToProcess() (and so isDebugModuleRunning-time callers like
            // LaunchApplicationAction's unsaved-changes check) correctly see "no changes" again after a
            // successful save, rather than comparing forever against the pre-edit values.
            populatePropertiesEditorPanel();
            redrawPanel();
            // This will force a regeneration of the component
            if (getSelectedComponent() instanceof FlowUserImplementedElement) {
                ((FlowUserImplementedElement)getSelectedComponent()).setOverwriteEnabled(true);
            }
            applyProtectFromOverwritePermissions();
            GenerationRequest generationRequest;
            if (getSelectedComponent() instanceof Module) {
                // Module properties include package/meta-pack switches whose impact is intentionally broad.
                generationRequest = GenerationRequest.full();
            } else if (getSelectedComponent() instanceof Flow flow) {
                generationRequest = GenerationRequest.moduleStructure(flow);
            } else if (getSelectedComponent() instanceof FlowElement flowElement
                    && flowElement.getContainingFlow() != null) {
                generationRequest = GenerationRequest.flow(flowElement.getContainingFlow());
            } else {
                generationRequest = GenerationRequest.full();
            }
            StudioProjectFiles.refreshCodeFromModel(project, generationRequest);
            // Intellij startup is multi-threaded so caution is required.
            if (metaPackChanged && uiContext.getPalettePanel() != null) {
                uiContext.getPalettePanel().resetPallette();
            }
            uiContext.getCanvasPanel().disableH2Button(uiContext.getIkasanModule().getUseEmbeddedH2());
            uiContext.getDesignerCanvas().setInitialiseAllDimensions(true);
            uiContext.getDesignerCanvas().repaint();
            uiContext.getPalettePanel().repaint();
            // Stay on the Properties tab after Update Code - populatePropertiesEditorPanel() (called above) and
            // updateComponentsWithNewValues() (called above that) both already leave it here; this used to
            // switch to Palette afterwards, which discarded that and jumped the user away from the panel they
            // were just working in.
            uiContext.setRightTabbedPaneFocus(UiContext.PROPERTIES_TAB_INDEX);
        } else {
            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.DataHasntChangedIgnoringOKAction"));
        }
    }

    /**
     * "Regenerate Class" button handler: force-regenerates the selected {@link FlowUserImplementedElement}'s own
     * hand-written class from its current properties, without requiring any property to actually change first -
     * contrast {@link #doOKAction()}, whose confirm+regenerate flow only ever fires as a side effect of a real
     * edit. Reuses the same confirm/backup ({@link #confirmForceRegenerateUserImplementedClass()}) and
     * overwrite-enable/refresh plumbing doOKAction uses for a component self-edit, so this doesn't need to
     * duplicate any of the generation logic itself.
     */
    private void regenerateSelectedUserImplementedClass() {
        if (!(getSelectedComponent() instanceof FlowUserImplementedElement flowUserImplementedElement)
                || flowUserImplementedElement.getContainingFlow() == null) {
            return;
        }
        if (!confirmForceRegenerateUserImplementedClass()) {
            return;
        }
        StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.CodeGenerationInProgressPleaseWait"));
        flowUserImplementedElement.setOverwriteEnabled(true);
        StudioProjectFiles.refreshCodeFromModel(project, GenerationRequest.flow(flowUserImplementedElement.getContainingFlow()));
    }

    /**
     * @return the display labels of changed properties that affect a user implemented class, in row order.
     */
    private List<String> getChangedAffectsUserImplementedClassPropertyLabels() {
        List<String> changedPropertyLabels = new ArrayList<>();
        if (componentPropertyEditRowList != null) {
            for (ComponentPropertyEditRow componentPropertyEditRow : componentPropertyEditRowList) {
                ComponentPropertyMeta meta = componentPropertyEditRow.getMeta();
                if (meta.isAffectsUserImplementedClass() && componentPropertyEditRow.propertyValueHasChanged()) {
                    changedPropertyLabels.add(meta.getDisplayLabel() != null ? meta.getDisplayLabel() : meta.getPropertyName());
                }
            }
        }
        return changedPropertyLabels;
    }

    /**
     * A hand-written class the pending change relates to: {@code flow}/{@code className} to compute old/new
     * package (module-level relocation case), {@code description} for the confirmation dialog, {@code file}
     * (may be null if nothing has been generated yet) for the optional backup (component self-edit case).
     */
    private record AffectedUserImplementedClass(Flow flow, String className, String description, VirtualFile file) {}

    /**
     * Ask the user to confirm the pending change, wording it according to what will actually happen to any
     * hand-written class(es) it names:
     * - Editing a {@link FlowUserImplementedElement} directly (Debug, Converter, GenericConsumer, etc.)
     *   really does regenerate that component's own class - {@link #confirmComponentSelfEdit} offers to back it
     *   up first.
     * - Editing the {@link Module} itself does not: {@code doOKAction} only ever flips a component's own
     *   {@code overwriteEnabled}/{@code protectFromOverwrite} gates for the component currently being edited, so
     *   a Module-level property change (name/applicationPackageName/version) never actually touches any other
     *   flow's hand-written class - those files are simply left where they are. If applicationPackageName is
     *   among the changed properties, {@link #confirmModuleLevelEdit} instead names the exact old package to new
     *   package moves the user needs to make themselves (e.g. via IntelliJ's own "Move Package" refactor, which
     *   correctly updates internal references in a way Studio copying a file never could).
     * @return true if the user confirmed, false if they declined.
     */
    private boolean confirmRegenerateUserImplementedClass(List<String> changedPropertyLabels) {
        if (getSelectedComponent() instanceof Module module) {
            return confirmModuleLevelEdit(module, changedPropertyLabels);
        }
        return confirmComponentSelfEdit(changedPropertyLabels);
    }

    /**
     * Editing a {@link FlowUserImplementedElement} directly: its own class really will be regenerated, so name
     * it and offer to back it up first (ticked by default).
     */
    private boolean confirmComponentSelfEdit(List<String> changedPropertyLabels) {
        List<AffectedUserImplementedClass> affected = getAffectedUserImplementedClasses();
        if (affected.isEmpty()) {
            String message = StudioBundle.message("message.ConfirmRegenerateUserImplementedClass", String.join(", ", changedPropertyLabels));
            return Messages.showYesNoDialog(project, message, StudioBundle.message("dialog.ConfirmRegenerateUserImplementedClass"), Messages.getWarningIcon()) == Messages.YES;
        }

        List<String> descriptions = new ArrayList<>();
        for (AffectedUserImplementedClass affectedClass : affected) {
            descriptions.add(affectedClass.description());
        }
        String message = StudioBundle.message("message.ConfirmRegenerateUserImplementedClassNamed",
                String.join(", ", changedPropertyLabels), String.join("\n", descriptions));
        return confirmWithOptionalBackup(message, affected);
    }

    /**
     * "Regenerate Class" button handler's confirmation: same backup-checkbox dialog {@link #confirmComponentSelfEdit}
     * uses for a changed-property-triggered regenerate, but worded for an explicit force-regenerate - there are
     * no changed properties to name here, since this button doesn't require anything to have actually changed
     * (unlike doOKAction, where a real property edit is what triggers the confirm+regenerate flow in the first place).
     */
    private boolean confirmForceRegenerateUserImplementedClass() {
        List<AffectedUserImplementedClass> affected = getAffectedUserImplementedClasses();
        if (affected.isEmpty()) {
            String message = StudioBundle.message("message.ConfirmForceRegenerateUserImplementedClass");
            return Messages.showYesNoDialog(project, message, StudioBundle.message("dialog.ConfirmRegenerateUserImplementedClass"), Messages.getWarningIcon()) == Messages.YES;
        }

        List<String> descriptions = new ArrayList<>();
        for (AffectedUserImplementedClass affectedClass : affected) {
            descriptions.add(affectedClass.description());
        }
        String message = StudioBundle.message("message.ConfirmForceRegenerateUserImplementedClassNamed", String.join("\n", descriptions));
        return confirmWithOptionalBackup(message, affected);
    }

    /**
     * Shared backup-checkbox confirmation dialog for both the changed-property-triggered regenerate
     * ({@link #confirmComponentSelfEdit}) and the on-demand "Regenerate Class" button
     * ({@link #confirmForceRegenerateUserImplementedClass}) - only the message text differs between the two.
     */
    private boolean confirmWithOptionalBackup(String message, List<AffectedUserImplementedClass> affected) {
        // showCheckboxMessageDialog's exitFunc is only invoked once, purely to translate the pressed button's
        // index into a return value - it's just a convenient hook to also capture the checkbox's final state
        // into an array the enclosing method can still read once the (modal) call below returns.
        boolean[] backupTicked = {true};
        int result = Messages.showCheckboxMessageDialog(
                message,
                StudioBundle.message("dialog.ConfirmRegenerateUserImplementedClass"),
                new String[]{Messages.getYesButton(), Messages.getNoButton()},
                StudioBundle.message("checkbox.BackupUserImplementedClassBeforeOverwrite"),
                true,
                -1,
                -1,
                Messages.getWarningIcon(),
                (exitCode, checkbox) -> {
                    backupTicked[0] = checkbox.isSelected();
                    return exitCode;
                });
        boolean confirmed = result == Messages.YES;
        if (confirmed && backupTicked[0]) {
            for (AffectedUserImplementedClass affectedClass : affected) {
                StudioProjectFiles.backupFile(project, affectedClass.file());
            }
        }
        return confirmed;
    }

    /**
     * Editing the Module: no other flow's hand-written class is actually touched by this save. If
     * applicationPackageName changed, name the exact old-package-to-new-package move for every affected class so
     * the user can do it themselves; otherwise just note that hand-written classes are not auto-regenerated here.
     */
    private boolean confirmModuleLevelEdit(Module module, List<String> changedPropertyLabels) {
        List<AffectedUserImplementedClass> affected = getAffectedUserImplementedClasses();
        String pendingApplicationPackageName = getPendingApplicationPackageName();

        List<String> relocations = new ArrayList<>();
        if (pendingApplicationPackageName != null) {
            for (AffectedUserImplementedClass affectedClass : affected) {
                String oldPackage = GeneratorUtils.getUserImplementedClassesPackageName(module, affectedClass.flow());
                String newPackage = pendingApplicationPackageName + "." + affectedClass.flow().getJavaPackageName();
                if (!oldPackage.equals(newPackage)) {
                    relocations.add(oldPackage + "." + affectedClass.className() + "  ->  " + newPackage + "." + affectedClass.className());
                }
            }
        }

        String message = relocations.isEmpty()
                ? StudioBundle.message("message.ConfirmModulePropertiesAffectUserImplementedClass", String.join(", ", changedPropertyLabels))
                : StudioBundle.message("message.ConfirmModulePropertiesRequireManualRelocation",
                        String.join(", ", changedPropertyLabels), String.join("\n", relocations));
        return Messages.showYesNoDialog(project, message, StudioBundle.message("dialog.ModulePropertiesAffectUserImplementedClass"), Messages.getWarningIcon()) == Messages.YES;
    }

    /**
     * @return the not-yet-saved applicationPackageName the user has entered, or null if that property isn't
     * among the ones changed on this save (nothing to relocate).
     */
    private String getPendingApplicationPackageName() {
        ComponentPropertyEditRow row = componentPropertyEditBoxMap.get(ComponentPropertyMeta.APPLICATION_PACKAGE_NAME);
        if (row == null || !row.propertyValueHasChanged()) {
            return null;
        }
        Object value = row.getValue();
        return value instanceof String ? (String) value : null;
    }

    /**
     * Name the hand-written class(es) associated with the pending change - see {@link #confirmRegenerateUserImplementedClass}
     * for what each caller does with them. Returns an empty list (rather than guessing) where a class name can't
     * yet be determined - the caller falls back to a generic confirmation message.
     */
    private List<AffectedUserImplementedClass> getAffectedUserImplementedClasses() {
        if (getSelectedComponent() instanceof Module module) {
            List<AffectedUserImplementedClass> affected = new ArrayList<>();
            if (module.getFlows() != null) {
                for (Flow flow : module.getFlows()) {
                    affected.addAll(affectedUserImplementedClassesForFlow(module, flow));
                }
            }
            return affected;
        } else if (getSelectedComponent() instanceof FlowUserImplementedElement flowUserImplementedElement
                && flowUserImplementedElement.getContainingFlow() != null) {
            Module module = project.getService(UiContext.class).getIkasanModule();
            AffectedUserImplementedClass affectedClass = describeAffectedUserImplementedClass(module, flowUserImplementedElement.getContainingFlow(), flowUserImplementedElement);
            return affectedClass != null ? List.of(affectedClass) : List.of();
        }
        return List.of();
    }

    private List<AffectedUserImplementedClass> affectedUserImplementedClassesForFlow(Module module, Flow flow) {
        List<AffectedUserImplementedClass> affected = new ArrayList<>();
        if (flow.getFlowRoute() == null) {
            return affected;
        }
        for (FlowElement component : flow.getFlowRoute().getConsumerAndFlowRouteElements()) {
            if (component instanceof FlowUserImplementedElement) {
                AffectedUserImplementedClass affectedClass = describeAffectedUserImplementedClass(module, flow, component);
                if (affectedClass != null) {
                    affected.add(affectedClass);
                }
            }
        }
        return affected;
    }

    private AffectedUserImplementedClass describeAffectedUserImplementedClass(Module module, Flow flow, FlowElement component) {
        ComponentProperty classNameProperty = component.getProperty(ComponentPropertyMeta.USER_IMPLEMENTED_CLASS_NAME);
        String className = classNameProperty != null ? (String) classNameProperty.getValue() : null;
        if (className == null) {
            return null;
        }
        String description = flow.getIdentity() + ": " + className + ".java";
        VirtualFile file = module != null
                ? StudioProjectFiles.getUserImplementedClassFile(project, GeneratorUtils.getUserImplementedClassesPackageName(module, flow), className)
                : null;
        return new AffectedUserImplementedClass(flow, className, description, file);
    }

    /**
     * For each property row protected from overwrite (a bespoke, user-owned stub), record whether the user has
     * given permission to regenerate it this round - either the row's own overwrite checkbox is ticked, or the
     * property never had a value before (first-time generation, nothing to protect). Unlike the component-level
     * {@link FlowUserImplementedElement#isOverwriteEnabled()} flag set above, this runs independently of whether
     * the row's value text itself changed, since the user may tick the box purely to force a regenerate.
     */
    private void applyProtectFromOverwritePermissions() {
        if (componentPropertyEditRowList != null) {
            for (ComponentPropertyEditRow componentPropertyEditRow : componentPropertyEditRowList) {
                if (componentPropertyEditRow.isProtectedFromOverwrite()) {
                    componentPropertyEditRow.getComponentProperty().setOverwriteEnabled(componentPropertyEditRow.isRowOverwriteAllowed());
                }
            }
        }
    }


    /**
     * When updateTargetComponent is called, it will set the component to be exposed / edited, it will then
     * delegate update of the editor pane to this component so that we can specialise for different components.
     * For the given component, get all the editable properties and add them to the properties edit panel.
     */
    protected void populatePropertiesEditorPanel() {
        if (!componentInitialisation) {
            updateCodeButton.setEnabled(false);
            // Guarded: this is first called from the PropertiesPanel superclass constructor, before this
            // subclass's own constructor body (which creates regenerateClassButton) has run.
            if (regenerateClassButton != null) {
                // Debug excluded: its stub is a transient debugging aid regenerated on every save anyway (and
                // never persisted once the project closes - see the Debug component's own helpText), not a
                // long-lived user-customised class someone would deliberately ask to regenerate on demand.
                regenerateClassButton.setVisible(getSelectedComponent() instanceof FlowUserImplementedElement
                        && !getSelectedComponent().getComponentMeta().isDebug());
                // Fresh/just-saved state: nothing pending, nothing invalid - matches listenerForAnyEditChanges'
                // own baseline, so Regenerate Class starts enabled here rather than waiting for a first edit.
                regenerateClassButton.setEnabled(true);
            }
        }

        if (getSelectedComponent() != null && getSelectedComponent().getComponentMeta() != null) {
            propertiesEditorScrollingContainer.removeAll();

            propertiesEditorPanel = new JBPanel(new GridBagLayout());
            propertiesEditorPanel.setBackground(getThemeAwareBackgroundColor());

            JBPanel mandatoryPropertiesEditorPanel = new JBPanel(new GridBagLayout());
            mandatoryPropertiesEditorPanel.setBorder(null);
            optionalPropertiesEditorPanel = new JBPanel(new GridBagLayout());
            optionalPropertiesEditorPanel.setBorder(null);
            if (optionalPropertiesExpandPanel == null) {
                optionalPropertiesExpandPanel = getOptionalPropertiesExpandPanel();
            }
            componentPropertyEditRowList = new ArrayList<>();

            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.insets = JBUI.insets(3, 4);

            int mandatoryTabley = 0;
            int optionalTabley = 0;
            if (getSelectedComponent().getComponentMeta().isModule()) {
                // Always refresh the list of choosable metapacks
                List<String> installedMetapacks = ComponentLibrary.getMetapackList();
                if (installedMetapacks != null && ! installedMetapacks.isEmpty()) {
                    getSelectedComponent().getComponentMeta().getAllowableProperties().get(VERSION).setChoices(installedMetapacks);
                }
            }

            // Component identity should be the first property if it exists
            if (getSelectedComponent().getIdentityPropertyMetaKey() != null) {
                componentPropertyEditRowList.add(
                    addNameValueToPropertiesEditPanel(
                        mandatoryPropertiesEditorPanel, getSelectedComponent().getProperty(getSelectedComponent().getIdentityPropertyMetaKey()), gc, mandatoryTabley++));
            }

            if (!getSelectedComponent().getComponentMeta().getAllowableProperties().isEmpty()) {
                List<Map.Entry<String, ComponentPropertyMeta>> sortedProperties = getSelectedComponent().getComponentMeta().getAllowableProperties().entrySet()
                        .stream()
                        .sorted(Comparator.comparingInt((Map.Entry<String, ComponentPropertyMeta> e) -> e.getValue().getPropertyDisplayOrder())
                                .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
                        .toList();
                Map<String, List<ComponentProperty>> groupedOptionalProperties = new LinkedHashMap<>();
                GridBagConstraints mandatoryHeadingGc = new GridBagConstraints();
                mandatoryHeadingGc.fill = GridBagConstraints.HORIZONTAL;
                mandatoryHeadingGc.insets = JBUI.insets(3, 4);
                mandatoryHeadingGc.gridx = 0;
                mandatoryHeadingGc.weightx = 1;
                // Consecutive mandatory-section properties sharing the same mandatorySectionHeading (e.g. Email
                // Producer's six recipient fields, all "At least one of...") are clustered into one titled
                // sub-panel rather than added as flat rows - see flushMandatoryHeadingGroup. Buffered here and
                // flushed as soon as a differently-headed (or header-less) property is encountered, so a group
                // stays contiguous and lands exactly where its properties' own propertyDisplayOrder puts it,
                // the same way groupedOptionalProperties below does for the Optional Properties section.
                String openMandatoryHeading = null;
                List<ComponentProperty> openMandatoryHeadingProperties = new ArrayList<>();
                for (Map.Entry<String, ComponentPropertyMeta> entry : sortedProperties) {
                    String key = entry.getKey();
                    if (!ComponentPropertyMeta.isIdentityKey(key) && !entry.getValue().isHiddenProperty() && !entry.getValue().isIgnoreProperty()) {
                        ComponentProperty property = getSelectedComponent().getProperty(key);
                        if (property == null) {
                            // This property has not yet been set for the component
                            property = new ComponentProperty((getSelectedComponent()).getComponentMeta().getMetadata(key));
                        }
                        if (property.getMeta().isGroupedProperty()) {
                            // Grouped property - keep it out of the always-visible Mandatory section even
                            // though it may also be userSuppliedClass; tucked into "Optional Properties".
                            // A userSuppliedClass property that affects the user-implemented class already
                            // gets the warning icon (getAffectsUserImplementedClassIndicator) and the save-time
                            // confirmation dialog - no separate "regenerating" section needed any more.
                            if (openMandatoryHeading != null) {
                                mandatoryTabley = flushMandatoryHeadingGroup(mandatoryPropertiesEditorPanel, mandatoryHeadingGc, mandatoryTabley, openMandatoryHeading, openMandatoryHeadingProperties, gc);
                                openMandatoryHeading = null;
                                openMandatoryHeadingProperties = new ArrayList<>();
                            }
                            groupedOptionalProperties.computeIfAbsent(property.getMeta().getPropertyGroup(), k -> new ArrayList<>()).add(property);
                        } else if (property.getMeta().isMandatory() || property.getMeta().hasMandatoryUnlessAnyOf()) {
                            // hasMandatoryUnlessAnyOf: e.g. an SFTP consumer's password/privateKeyFilename - one
                            // of the two is genuinely required, so (as long as neither carries its own
                            // propertyGroup) both belong in the always-visible Mandatory section rather than
                            // hidden behind the Optional Properties toggle. A pairwise case like that carries
                            // the "(or ...)" label cue instead of a mandatorySectionHeading - see
                            // ComponentPropertyEditRow; a wider group (e.g. email's six recipient fields) uses
                            // a shared heading instead, since a five-way "(or a / b / c / d / e)" suffix on
                            // every row is unreadable.
                            if (property.getMeta().hasMandatorySectionHeading()) {
                                String heading = property.getMeta().getMandatorySectionHeading();
                                if (openMandatoryHeading != null && !openMandatoryHeading.equals(heading)) {
                                    mandatoryTabley = flushMandatoryHeadingGroup(mandatoryPropertiesEditorPanel, mandatoryHeadingGc, mandatoryTabley, openMandatoryHeading, openMandatoryHeadingProperties, gc);
                                    openMandatoryHeadingProperties = new ArrayList<>();
                                }
                                openMandatoryHeading = heading;
                                openMandatoryHeadingProperties.add(property);
                            } else {
                                if (openMandatoryHeading != null) {
                                    mandatoryTabley = flushMandatoryHeadingGroup(mandatoryPropertiesEditorPanel, mandatoryHeadingGc, mandatoryTabley, openMandatoryHeading, openMandatoryHeadingProperties, gc);
                                    openMandatoryHeading = null;
                                    openMandatoryHeadingProperties = new ArrayList<>();
                                }
                                componentPropertyEditRowList.add(addNameValueToPropertiesEditPanel(
                                        mandatoryPropertiesEditorPanel,
                                        property, gc, mandatoryTabley++));
                            }
                        } else {
                            if (openMandatoryHeading != null) {
                                mandatoryTabley = flushMandatoryHeadingGroup(mandatoryPropertiesEditorPanel, mandatoryHeadingGc, mandatoryTabley, openMandatoryHeading, openMandatoryHeadingProperties, gc);
                                openMandatoryHeading = null;
                                openMandatoryHeadingProperties = new ArrayList<>();
                            }
                            groupedOptionalProperties.computeIfAbsent(ComponentPropertyMeta.PROPERTY_GROUP_MISCELLANEOUS, k -> new ArrayList<>()).add(property);
                        }
                    }
                }
                if (openMandatoryHeading != null) {
                    mandatoryTabley = flushMandatoryHeadingGroup(mandatoryPropertiesEditorPanel, mandatoryHeadingGc, mandatoryTabley, openMandatoryHeading, openMandatoryHeadingProperties, gc);
                }

                GridBagConstraints groupGc = new GridBagConstraints();
                groupGc.fill = GridBagConstraints.HORIZONTAL;
                groupGc.insets = JBUI.insets(3, 4);
                groupGc.gridx = 0;
                groupGc.weightx = 1;
                groupGc.gridy = 0;
                for (String groupName : groupedOptionalProperties.keySet().stream().sorted(groupDisplayOrderFor(groupedOptionalProperties)).toList()) {
                    JBPanel groupPanel = new JBPanel(new GridBagLayout());
                    groupPanel.setBorder(null);
                    int groupTabley = 0;
                    for (ComponentProperty property : groupedOptionalProperties.get(groupName)) {
                        componentPropertyEditRowList.add(addNameValueToPropertiesEditPanel(
                                groupPanel, property, gc, groupTabley++));
                        optionalTabley++;
                    }
                    setSubPanel(optionalPropertiesEditorPanel, groupPanel, groupDisplayLabel(groupName), getThemeAwareBorderColor(), groupGc);
                }
            }

            GridBagConstraints gc1 = new GridBagConstraints();
            gc1.fill = GridBagConstraints.HORIZONTAL;
            gc1.insets = JBUI.insets(3, 4);
            gc1.gridx = 0;
            gc1.weightx = 1;
            gc1.gridy = 0;

            if (mandatoryTabley > 0) {
                setSubPanel(propertiesEditorPanel, mandatoryPropertiesEditorPanel, StudioBundle.message("label.MandatoryProperties"), ThemeAwareColors.getImportantBorderColor(), gc1);
            }

            if (optionalTabley > 0) {
                optionalPropertiesExpandPanel.setVisible(true);
                setToggleOptionalPropertiesButton(false);
                optionalPropertiesEditorPanel.setVisible(false);
                setSubPanel(propertiesEditorPanel, optionalPropertiesExpandPanel, null, null, gc1);
                setSubPanel(propertiesEditorPanel, optionalPropertiesEditorPanel, null, null, gc1);
            } else if (optionalPropertiesExpandPanel != null) {
                optionalPropertiesExpandPanel.setVisible(false);
            }
            propertiesEditorScrollingContainer.add(propertiesEditorPanel);
            UiContext uiContext = project.getService(UiContext.class);
            uiContext.setRightTabbedPaneFocus(UiContext.PROPERTIES_TAB_INDEX);

            if (htmlScrollingDisplayPanel != null) {
                htmlScrollingDisplayPanel.setText(getDisplayedHelpTextForSelectedComponent());
            }

            // Rows are freshly rebuilt above - re-apply any search text already typed (e.g. the user switched
            // to a different component on the canvas while a filter was active) rather than silently dropping it.
            if (propertySearchField != null) {
                applyPropertySearchFilter(propertySearchField.getText());
            }
        }
    }

    /**
     * The static component help text, with the component's own name as a heading, a compact "Input:/Output:"
     * type summary, and a best-effort upstream type-mismatch warning all prepended - see
     * StudioUIUtils#buildComponentSummaryHtml and FlowElement#getEffectiveInputTypeDescription /
     * #getEffectiveOutputTypeDescription / #getUpstreamTypeMismatchWarning. Kept separate from the raw meta
     * helpText (which no longer carries its own hardcoded name heading - see buildComponentSummaryHtml) so none
     * of this leaks into anything that reads getComponentMeta().getHelpText() directly.
     */
    private String getDisplayedHelpTextForSelectedComponent() {
        ComponentMeta componentMeta = getSelectedComponent().getComponentMeta();
        String helpText = componentMeta.getHelpText();
        if (getSelectedComponent() instanceof FlowElement flowElement) {
            String implementingClassName = componentMeta.isUseImplementingClassInFactory() ? componentMeta.getImplementingClass() : null;
            StringBuilder prefix = new StringBuilder(StudioUIUtils.buildComponentSummaryHtml(componentMeta.getName(), implementingClassName,
                    flowElement.getEffectiveInputTypeDescription(), flowElement.getEffectiveOutputTypeDescription(), false));
            String warning = flowElement.getUpstreamTypeMismatchWarning(this::isConfirmedSerializable);
            if (warning != null) {
                prefix.append("<p><b><font color=\"red\">Warning: ").append(StudioUIUtils.escapeHtml(warning)).append("</font></b></p>");
            }
            helpText = prefix + helpText;
        }
        String moreInfo = StudioUIUtils.buildMoreInfoLinkHtml(componentMeta.getWebHelpURL());
        return moreInfo != null ? helpText + moreInfo : helpText;
    }

    // class name -> confirmed answer, once a background resolution has actually completed for it.
    private final Map<String, Boolean> serializableResolutionCache = new ConcurrentHashMap<>();
    // Guards against firing a second background resolution for a class that's already got one in flight -
    // e.g. rapidly clicking between flow elements that share the same upstream type before the first
    // lookup has returned.
    private final Set<String> serializableResolutionInFlight = ConcurrentHashMap.newKeySet();

    /**
     * Resolves whether a fully-qualified class implements java.io.Serializable, for
     * FlowElement#getUpstreamTypeMismatchWarning(Function)'s one Serializable candidate - which that method
     * can't check itself, since it lives in the framework-independent core layer and must never depend on PSI
     * (see LayerBoundaryTest/ArchitectureBoundaryTest). Mirrors SendTestMessagePayloadDialog's own
     * InheritanceUtil.isInheritor usage for the identical question.
     * -
     * PREVIOUSLY ran this synchronously via runReadAction, on the (wrong) assumption that a single class lookup
     * plus one interface check was cheap enough to do inline - that broke in practice: this is called from
     * getDisplayedHelpTextForSelectedComponent(), invoked directly off DesignerCanvas's mouse-click handling
     * (i.e. on the EDT) every time a component is selected, and IntelliJ's platform enforces "Slow operations
     * are prohibited on EDT" for exactly this kind of PSI/index access (confirmed via a real stack trace off
     * that exact call chain, not assumed) - the very SlowOperations violation SendTestMessagePayloadDialog's
     * own comment already warns about, which its own async ReadAction.nonBlocking already avoids. Now mirrors
     * that: resolves in the background, caches the answer once known, and returns null (unknown) immediately
     * for any not-yet-resolved class - which getUpstreamTypeMismatchWarning already treats as "stay silent",
     * so the very first render simply shows no Serializable-related warning until the async lookup catches up,
     * at which point the properties panel's help text is refreshed once for the currently-selected component
     * (guarded so a late callback for a component the user has since deselected doesn't overwrite what's shown).
     * -
     * expireWith(this) - PropertiesPanel now implements Disposable purely so panel-scoped async work like this
     * has a real, narrow-lived parent to hang off (JetBrains' own "Choosing a Disposable Parent" guidance warns
     * against using Project itself here, which is what this originally did before that warning was raised - a
     * whole-project-lived anchor for a single panel's background lookup was a mismatch). PropertiesPopupDialogue
     * registers whichever panel it wraps against its own getDisposable(), and DesignerUI's persistent
     * canvas-sidebar instance is registered the same way CanvasPanel already is - so this panel is genuinely
     * disposed, and any in-flight resolution cancelled, whenever its own dialog/sidebar goes away.
     */
    private Boolean isConfirmedSerializable(String fullyQualifiedClassName) {
        Boolean cached = serializableResolutionCache.get(fullyQualifiedClassName);
        if (cached != null) {
            return cached;
        }
        if (serializableResolutionInFlight.add(fullyQualifiedClassName)) {
            BasicElement requestingComponent = getSelectedComponent();
            ReadAction.nonBlocking(() -> {
                        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fullyQualifiedClassName, GlobalSearchScope.allScope(project));
                        return psiClass != null ? InheritanceUtil.isInheritor(psiClass, "java.io.Serializable") : null;
                    })
                    .expireWith(this)
                    .finishOnUiThread(ModalityState.any(), resolved -> {
                        serializableResolutionInFlight.remove(fullyQualifiedClassName);
                        if (resolved != null) {
                            serializableResolutionCache.put(fullyQualifiedClassName, resolved);
                            if (htmlScrollingDisplayPanel != null && getSelectedComponent() == requestingComponent) {
                                htmlScrollingDisplayPanel.setText(getDisplayedHelpTextForSelectedComponent());
                            }
                        }
                    })
                    .submit(AppExecutorUtil.getAppExecutorService());
        }
        return null;
    }

    /**
     * Live-filters property rows by name/help text as the user types into propertySearchField - see that field's
     * javadoc in PropertiesPanel for why. A property whose row lives in the (possibly collapsed) Optional
     * Properties section is forced visible while a search is active and matches something there, so the user
     * never has to manually expand it first; the section reverts to whatever the Expand/Ignore toggle last left
     * it at once the search is cleared.
     * @param query the search field's current, un-trimmed text.
     */
    @Override
    protected void onPropertySearchChanged(String query) {
        applyPropertySearchFilter(query);
    }

    private void applyPropertySearchFilter(String query) {
        if (componentPropertyEditRowList == null) {
            return;
        }
        boolean searching = query != null && !query.isBlank();
        boolean anyOptionalMatch = false;
        for (ComponentPropertyEditRow row : componentPropertyEditRowList) {
            boolean matches = row.matchesSearch(query);
            row.setRowVisible(matches);
            if (matches && isInOptionalSection(row.getMeta())) {
                anyOptionalMatch = true;
            }
        }
        if (optionalPropertiesEditorPanel != null) {
            optionalPropertiesEditorPanel.setVisible(searching ? anyOptionalMatch : isExpanded);
        }
        // false: don't steal focus back to the first row - the user is actively typing in propertySearchField.
        redrawPanel(false);
    }

    /**
     * Mirrors the placement decision made while building the panel (see the loop in populatePropertiesEditorPanel
     * above), inverted: a property lands in the always-visible Mandatory section only if it isn't grouped and is
     * mandatory (unconditionally or via mandatoryUnlessAnyOf) - everything else, including mandatoryIfTrue
     * properties like FtpConsumer's ftpsKeyStoreFilePath, lives in the Optional section, i.e. this is true for it.
     */
    private boolean isInOptionalSection(ComponentPropertyMeta meta) {
        return meta.isGroupedProperty() || (!meta.isMandatory() && !meta.hasMandatoryUnlessAnyOf());
    }

    private void toggleOptionalSection() {
        isExpanded = !isExpanded;
        setToggleOptionalPropertiesButton(isExpanded);
        for (ComponentPropertyEditRow componentPropertyEditRow : componentPropertyEditRowList) {
            // isInOptionalSection(), not getMeta().isOptional() - isOptional() only checks the raw `mandatory`
            // flag and knows nothing of hasMandatoryUnlessAnyOf(), so a field like Email Producer's toRecipient
            // (mandatory-unless-any-of, not unconditionally mandatory) read as "optional" here even though it
            // lives in the always-visible Mandatory section. resetDataEntryComponentsWithNewValues() re-derives
            // the widget's text from the still-uncommitted model value (typed text is never live-bound to the
            // model - see updateValueObjectWithEnteredValues(), only called from "Update Code") - so calling it
            // on a Mandatory-section row wiped out whatever the user had just typed there, the moment they
            // clicked Expand/Collapse on the unrelated Optional Properties section.
            if (isInOptionalSection(componentPropertyEditRow.getMeta())) {
                componentPropertyEditRow.resetDataEntryComponentsWithNewValues();
            }
        }
        if (getPropertiesDialogue() != null) {
            getPropertiesDialogue().pack();
        } else {
            Window window = SwingUtilities.getWindowAncestor(this);
            Window ideWindow = WindowManager.getInstance().getFrame(project);
            if (window != null && window != ideWindow) {
                window.pack();
            }
        }
    }

    private void setToggleOptionalPropertiesButton(boolean enable) {
        optionalPropertiesEditorPanel.setVisible(enable);
        setDefaultsButton.setEnabled(enable);
        clearDefaultsButton.setEnabled(enable);
        toggleOptionalPropertiesButton.setText(enable ? StudioBundle.message("button.Ignore") : StudioBundle.message("button.Expand"));
    }

    @SuppressWarnings("rawtypes")
    protected JBPanel getOptionalPropertiesExpandPanel() {
        @SuppressWarnings("rawtypes")
        JBPanel optionalPropertiesPanel = new JBPanel(new FlowLayout(FlowLayout.LEFT));
        optionalPropertiesPanel.setBorder(null);
        JLabel optionalPropertiesLabel = new JLabel(StudioBundle.message("label.OptionalProperties"));
        // Create the buttons
        toggleOptionalPropertiesButton = new JButton(StudioBundle.message("button.Expand"));
        toggleOptionalPropertiesButton.addActionListener(e -> toggleOptionalSection());
        setDefaultsButton = new JButton(StudioBundle.message("button.SetDefaults"));
        setDefaultsButton.addActionListener(e -> setOptionalPropertiesToDefaultVales());
        clearDefaultsButton = new JButton(StudioBundle.message("button.ClearDefaults"));
        clearDefaultsButton.addActionListener(e -> clearOptionalProperties());
        clearDefaultsButton.setEnabled(false);
        // Add buttons to the panel
        optionalPropertiesPanel.add(optionalPropertiesLabel);
        optionalPropertiesPanel.add(toggleOptionalPropertiesButton);
        optionalPropertiesPanel.add(setDefaultsButton);
        optionalPropertiesPanel.add(clearDefaultsButton);

        return optionalPropertiesPanel;
    }

    protected void clearOptionalProperties() {
        for (ComponentPropertyEditRow componentPropertyEditRow : componentPropertyEditRowList) {
            // isInOptionalSection(), not getMeta().isOptional() - see the identical fix/comment in
            // toggleOptionalSection(). Here the stakes are higher than a display refresh: clearValue() commits
            // componentProperty.setValue(null), so this button would otherwise silently wipe a genuinely
            // conditionally-mandatory field's saved value (e.g. Email Producer's toRecipient, SFTP's password)
            // while believing it was only clearing optional ones.
            if (isInOptionalSection(componentPropertyEditRow.getMeta())) {
                componentPropertyEditRow.clearValue();
            }
        }
        redrawPanel();
    }

    protected void setOptionalPropertiesToDefaultVales() {
        for (ComponentPropertyEditRow componentPropertyEditRow : componentPropertyEditRowList) {
            // isInOptionalSection(), not getMeta().isOptional() - see the identical fix/comment in
            // toggleOptionalSection() and clearOptionalProperties() above.
            if (isInOptionalSection(componentPropertyEditRow.getMeta())) {
                componentPropertyEditRow.setDefaultValue();
            }
        }
        redrawPanel();
    }

    /**
     * Get the field that should be given the focus in popup or in-screen form
     * @return the component that should be given focus or null
     */
    public JComponent getFirstFocusField() {
        JComponent firstComponent = null;
        if (componentPropertyEditRowList != null && !componentPropertyEditRowList.isEmpty()) {
            firstComponent = componentPropertyEditRowList.get(0).getInputField().getFirstFocusComponent();
        }
        return firstComponent;
    }

    /**
     * Builds a titled sub-panel for one run of mandatory-section properties that share a
     * {@code mandatorySectionHeading} (e.g. Email Producer's six recipient fields under "At least one of..."),
     * adds it into {@code mandatoryPropertiesEditorPanel} at the given row, and returns the next free row -
     * mirrors the optional-groups loop in {@link #populatePropertiesEditorPanel()} but embedded inside the
     * Mandatory Properties section instead of Optional Properties, so these fields stay always-visible (see
     * feedback_mandatory_properties_must_stay_ungrouped).
     * @param mandatoryPropertiesEditorPanel the always-visible Mandatory section panel to add the sub-panel into
     * @param headingGc layout constraints tracking the next free row in mandatoryPropertiesEditorPanel - mutated
     *                  (gridy advanced) by this call, same pattern as setSubPanel's own gc1
     * @param mandatoryTabley the row this heading group's sub-panel should be placed at
     * @param heading the shared mandatorySectionHeading text, used as this sub-panel's titled border
     * @param properties every buffered property sharing this heading, in display order
     * @param rowGc layout constraints reused for each property's own row inside the sub-panel
     * @return the next free row in mandatoryPropertiesEditorPanel, for the caller to resume flat-row placement at
     */
    private int flushMandatoryHeadingGroup(JBPanel mandatoryPropertiesEditorPanel, GridBagConstraints headingGc, int mandatoryTabley,
                                            String heading, List<ComponentProperty> properties, GridBagConstraints rowGc) {
        JBPanel headingPanel = new JBPanel(new GridBagLayout());
        headingPanel.setBorder(null);
        int innerTabley = 0;
        for (ComponentProperty property : properties) {
            componentPropertyEditRowList.add(addNameValueToPropertiesEditPanel(headingPanel, property, rowGc, innerTabley++));
        }
        headingGc.gridy = mandatoryTabley;
        setSubPanel(mandatoryPropertiesEditorPanel, headingPanel, heading, getThemeAwareBorderColor(), headingGc);
        return headingGc.gridy;
    }

    /**
     * The properties panel has a series of subsections for mandatory, options and code regenerating components
     * @param allPropertiesEditorPanel is the parent
     * @param subPanel is the subsection (e.g. mandatory, optional, code regenerating)
     * @param title to place on the subsection
     * @param borderColor of the subsection
     * @param gc1 is used to dictate layout and relay layout to the next subsection.
     */
    private void setSubPanel(JBPanel allPropertiesEditorPanel, JBPanel subPanel, String title, Color borderColor, GridBagConstraints gc1) {
        subPanel.setBackground(getThemeAwareBackgroundColor());
        if (title != null) {
        subPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(borderColor),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP));
        }
        allPropertiesEditorPanel.add(subPanel, gc1);
        gc1.gridy += 1;
    }

    /**
     *
     * @param propertiesEditorPanel to hold the name/value pair
     * @param componentProperty being added
     * @param gc is used to dictate layout and relay layout to the next subsection.
     * @param tabley is used to convey the row number
     * @return a populated 'row' i.e. a container that supports the edit of the supplied name / value pair.
     */
    private ComponentPropertyEditRow addNameValueToPropertiesEditPanel(JBPanel propertiesEditorPanel, ComponentProperty componentProperty, GridBagConstraints gc, int tabley) {
        ComponentPropertyEditRow componentPropertyEditRow = new ComponentPropertyEditRow(project, componentProperty, componentInitialisation, listenerForAnyEditChanges, componentPropertyEditBoxMap);
        addLabelAndParamInput(propertiesEditorPanel, gc, tabley, componentPropertyEditRow.getPropertyTitleField(), componentPropertyEditRow.getDataValidationHelper(), componentPropertyEditRow.getDefaultValueButton(), componentPropertyEditRow.getChooseClassButton(), componentPropertyEditRow.getRowOverwriteCheckBox(), componentPropertyEditRow.getAffectsUserImplementedClassIndicator(), componentPropertyEditRow.getInputField(), componentPropertyEditRow.getMeta());
        return componentPropertyEditRow;
    }

    private void addLabelAndParamInput(JBPanel propertiesEditorPanel, GridBagConstraints gc, int tabley, JLabel propertyLabel, JButton helpButton, JButton defaultValueButton, JButton chooseClassButton, JCheckBox overwriteCheckBox, JLabel affectsUserImplementedClassIndicator, ComponentInput componentInput, ComponentPropertyMeta meta) {
        gc.weightx = 0.0;
        gc.gridx = 0;
        gc.gridy = tabley;
        propertiesEditorPanel.add(propertyLabel, gc);
        ++gc.gridx;
        List<JComponent> auxiliaryWidgets = new ArrayList<>();
        if (helpButton != null) auxiliaryWidgets.add(helpButton);
        if (defaultValueButton != null) auxiliaryWidgets.add(defaultValueButton);
        if (chooseClassButton != null) auxiliaryWidgets.add(chooseClassButton);
        if (overwriteCheckBox != null) auxiliaryWidgets.add(overwriteCheckBox);
        if (affectsUserImplementedClassIndicator != null) auxiliaryWidgets.add(affectsUserImplementedClassIndicator);
        if (auxiliaryWidgets.size() > 1) {
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            buttonPanel.setBackground(getThemeAwareBackgroundColor());
            auxiliaryWidgets.forEach(buttonPanel::add);
            propertiesEditorPanel.add(buttonPanel, gc);
        } else if (auxiliaryWidgets.size() == 1) {
            propertiesEditorPanel.add(auxiliaryWidgets.get(0), gc);
        }
        ++gc.gridx;
        if (!componentInput.isBooleanInput()) {
            gc.weightx = 1.0;
            propertiesEditorPanel.add(componentInput.getFirstFocusComponent(), gc);
        } else {
            // Match the text-field branch's weightx=1.0: without it, this column only gets stretch/left-anchor
            // behaviour when some OTHER row sharing this group panel's GridBagLayout happens to be a text field
            // that sets it - a boolean-only group (e.g. a single checkbox row with no sibling text rows) would
            // otherwise get weightx=0 for every column, which GridBagLayout centers instead of left-anchoring.
            gc.weightx = 1.0;
            JBPanel booleanPanel = new JBPanel(new FlowLayout(FlowLayout.LEFT));
            booleanPanel.setBackground(getThemeAwareBackgroundColor());
            String trueLabel = (meta != null && meta.getTrueLabel() != null) ? meta.getTrueLabel() : StudioBundle.message("label.True");
            String falseLabel = (meta != null && meta.getFalseLabel() != null) ? meta.getFalseLabel() : StudioBundle.message("label.False");
            booleanPanel.add(new JLabel(trueLabel));
            booleanPanel.add(componentInput.getTrueBox());
            booleanPanel.add(new JLabel(falseLabel));
            booleanPanel.add(componentInput.getFalseBox());
            propertiesEditorPanel.add(booleanPanel, gc);
        }
    }

    @Override
    protected BasicElement getSelectedComponent() {
        return (BasicElement)super.getSelectedComponent();
    }

    /**
     * Check the before and after images of the fields if they differ, then data has changed.
     * @return true if the above conditions hold, otherwise false.
     */
    @Override
    public boolean dataHasChangedAndOKToProcess() {
        boolean modelUpdated = false;
        if (componentPropertyEditRowList != null) {
            for (final ComponentPropertyEditRow componentPropertyEditRow : componentPropertyEditRowList) {
                if (componentPropertyEditRow.propertyValueHasChanged()) {
                    LOG.info("STUDIO: Component " + componentPropertyEditRow.getComponentProperty().getMeta().getPropertyName() + " new value is " + componentPropertyEditRow.getValue());
                    modelUpdated = true;
                    break;
                }
            }
        }
        return modelUpdated;
    }

    /**
     * Determine if the named property has changed
     * @return true if the named property has changed
     */
    public boolean propertyHasChanged(String propertyNameToSearchFor) {
        boolean propertyHasChanged = false;
        if (componentPropertyEditRowList != null) {
            for (final ComponentPropertyEditRow componentPropertyEditRow : componentPropertyEditRowList) {
                if (componentPropertyEditRow.getMeta().getPropertyName().equals(propertyNameToSearchFor)) {
                    if (componentPropertyEditRow.propertyValueHasChanged()) {
                        propertyHasChanged = true;
                        break;
                    }
                }
            }
        }
        return propertyHasChanged;
    }


    /**
     * Check to see if any new values have been entered, update the model and return true if that is the case.
     */
    public void updateComponentsWithNewValues() {
        if (componentPropertyEditRowList != null) {
            for (final ComponentPropertyEditRow componentPropertyEditRow : componentPropertyEditRowList) {
                if (componentPropertyEditRow.propertyValueHasChanged()) {
                    if (getSelectedComponent() instanceof FlowUserImplementedElement) {
                        ((FlowUserImplementedElement)getSelectedComponent()).setOverwriteEnabled(true);
                    }
                    // Property has been unset e.g. a boolean, validation would ensure mandatory must be set.
                    if (!componentPropertyEditRow.editBoxHasValue()) {
                        getSelectedComponent().removeProperty(componentPropertyEditRow.getPropertyKey());
                    } else { // update existing
                        ComponentProperty componentProperty = componentPropertyEditRow.updateValueObjectWithEnteredValues();
                        // If its new this will insert, existing will just overwrite.
                        getSelectedComponent().addComponentProperty(componentPropertyEditRow.getPropertyKey(), componentProperty);
                    }
                    UiContext uiContext = project.getService(UiContext.class);
                    uiContext.setRightTabbedPaneFocus(PALETTE_TAB_INDEX);
                }
            }
        }
    }

    public List<ComponentPropertyEditRow> getComponentPropertyEditBoxList() {
        return componentPropertyEditRowList;
    }

    /**
     * Validates the values populated
     * @return a populated ValidationInfo array if there are any validation issues.
     */
    protected java.util.List<ValidationInfo> doValidateAll() {
        List<ValidationInfo> result = new ArrayList<>();
        for (final ComponentPropertyEditRow editPair: getComponentPropertyEditBoxList()) {
            result.addAll(editPair.doValidateAll());
        }
        result.addAll(validateComponentNameIsUniqueInFlow());
        return result;
    }

    /**
     * componentName's own helpText already documents "should be unique for the flow", but nothing previously
     * enforced it - two components silently sharing a name overwrite each other's generated bean, since
     * componentName becomes the Spring bean id. A flow only ever has a handful of components, so this is a
     * single small in-memory pass, not worth worrying about cost.
     * @return a single ValidationInfo if the currently-edited component's name collides with a sibling in the
     * same flow, otherwise empty.
     */
    private List<ValidationInfo> validateComponentNameIsUniqueInFlow() {
        if (!(getSelectedComponent() instanceof FlowElement flowElement) || flowElement.getContainingFlow() == null) {
            return List.of();
        }
        String identityKey = flowElement.getIdentityPropertyMetaKey();
        ComponentPropertyEditRow componentNameRow = getComponentPropertyEditBoxList().stream()
                .filter(row -> identityKey.equals(row.getPropertyKey()))
                .findFirst().orElse(null);
        if (componentNameRow == null) {
            return List.of();
        }
        Object candidateValue = componentNameRow.getValue();
        if (!(candidateValue instanceof String candidateName) || candidateName.isBlank()) {
            return List.of();
        }
        boolean duplicate = flowElement.getContainingFlow().ftlGetConsumerAndFlowElements().stream()
                .anyMatch(sibling -> sibling != flowElement && candidateName.equals(sibling.getIdentity()));
        if (duplicate) {
            return List.of(new ValidationInfo(
                    StudioBundle.message("message.ComponentNameMustBeUniqueInFlow", candidateName),
                    componentNameRow.getOverridingInputField()));
        }
        return List.of();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }

    public void setComponentDescription(HtmlScrollingDisplayPanel htmlScrollingDisplayPanel) {
        this.htmlScrollingDisplayPanel = htmlScrollingDisplayPanel;
    }
}
