package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.generator.GeneratorUtils;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowUserImplementedElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.StudioPsiUtils;
import org.ikasan.studio.ui.model.psi.GenerationRequest;
import org.ikasan.studio.ui.theme.ThemeAwareColors;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta.VERSION;
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
            boolean okToProcess = dataHasChangedAndOKToProcess() && doValidateAll().isEmpty();
            if (updateCodeButton != null) {
                updateCodeButton.setEnabled(okToProcess);
            }
            if (getPropertiesDialogue() != null) {
                getPropertiesDialogue().setOKActionEnabled(okToProcess);
            }
        };
        if (footerPanel != null) {
            regenerateClassButton = new JButton(StudioBundle.message("button.RegenerateClass"));
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
            // ComponentPropertyEditRow#initialValue is captured once, at row construction, and is immutable -
            // rebuilding the rows against the just-committed model is the only way to make
            // dataHasChangedAndOKToProcess() (and so isDebugModuleRunning-time callers like
            // LaunchApplicationAction's unsaved-changes check) correctly see "no changes" again after a
            // successful save, rather than comparing forever against the pre-edit values.
            populatePropertiesEditorPanel();
            redrawPanel();
            if (metaPackChanged) {
                Module module = uiContext.getIkasanModule();
                // If the version has changed, we need to update the component meta
                // Can't update the metapack until all changes are inthe current model.
                StudioBuildUtils.changeMetaPack(module);
            }
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
            StudioPsiUtils.refreshCodeFromModel(project, generationRequest);
            // Intellij startup is multi-threaded so caution is required.
            if (metaPackChanged && uiContext.getPalettePanel() != null) {
                uiContext.getPalettePanel().resetPallette();
            }
            uiContext.getCanvasPanel().disableH2Button(uiContext.getIkasanModule().getUseEmbeddedH2());
            uiContext.getDesignerCanvas().setInitialiseAllDimensions(true);
            uiContext.getDesignerCanvas().repaint();
            uiContext.getPalettePanel().repaint();
            uiContext.setRightTabbedPaneFocus(PALETTE_TAB_INDEX);
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
        StudioPsiUtils.refreshCodeFromModel(project, GenerationRequest.flow(flowUserImplementedElement.getContainingFlow()));
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
     * - Editing a {@link FlowUserImplementedElement} directly (Debug, CustomConverter, GenericConsumer, etc.)
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
                StudioPsiUtils.backupFile(project, affectedClass.file());
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
                ? StudioPsiUtils.getUserImplementedClassFile(project, GeneratorUtils.getUserImplementedClassesPackageName(module, flow), className)
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
                regenerateClassButton.setVisible(getSelectedComponent() instanceof FlowUserImplementedElement);
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
                List<String> installedMetapacks = IkasanComponentLibrary.getMetapackList();
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
                            groupedOptionalProperties.computeIfAbsent(property.getMeta().getPropertyGroup(), k -> new ArrayList<>()).add(property);
                        } else if (property.getMeta().isMandatory() || property.getMeta().hasMandatoryUnlessAnyOf()) {
                            // hasMandatoryUnlessAnyOf: e.g. an SFTP consumer's password/privateKeyFilename - one
                            // of the two is genuinely required, so (as long as neither carries its own
                            // propertyGroup) both belong in the always-visible Mandatory section rather than
                            // hidden behind the Optional Properties toggle. The row's label carries the "(or ...)"
                            // cue so it's clear at a glance that only one of the pair, not both, needs a value.
                            componentPropertyEditRowList.add(addNameValueToPropertiesEditPanel(
                                    mandatoryPropertiesEditorPanel,
                                    property, gc, mandatoryTabley++));
                        } else {
                            groupedOptionalProperties.computeIfAbsent(ComponentPropertyMeta.PROPERTY_GROUP_MISCELLANEOUS, k -> new ArrayList<>()).add(property);
                        }
                    }
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
                htmlScrollingDisplayPanel.setText(getSelectedComponent().getComponentMeta().getHelpText());
            }

            // Rows are freshly rebuilt above - re-apply any search text already typed (e.g. the user switched
            // to a different component on the canvas while a filter was active) rather than silently dropping it.
            if (propertySearchField != null) {
                applyPropertySearchFilter(propertySearchField.getText());
            }
        }
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
            if (matches && !isInMandatorySection(row.getMeta())) {
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
     * above): a property lands in the always-visible Mandatory section only if it isn't grouped and is mandatory
     * (unconditionally or via mandatoryUnlessAnyOf) - everything else, including mandatoryIfTrue properties like
     * FtpConsumer's ftpsKeyStoreFilePath, lives in the Optional section.
     */
    private boolean isInMandatorySection(ComponentPropertyMeta meta) {
        return !meta.isGroupedProperty() && (meta.isMandatory() || meta.hasMandatoryUnlessAnyOf());
    }

    private void toggleOptionalSection() {
        isExpanded = !isExpanded;
        setToggleOptionalPropertiesButton(isExpanded);
        for (ComponentPropertyEditRow componentPropertyEditRow : componentPropertyEditRowList) {
            if (componentPropertyEditRow.getMeta().isOptional()) {
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
            if (componentPropertyEditRow.getMeta().isOptional()) {
                componentPropertyEditRow.clearValue();
            }
        }
        redrawPanel();
    }

    protected void setOptionalPropertiesToDefaultVales() {
        for (ComponentPropertyEditRow componentPropertyEditRow : componentPropertyEditRowList) {
            if (componentPropertyEditRow.getMeta().isOptional()) {
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
        addLabelAndParamInput(propertiesEditorPanel, gc, tabley, componentPropertyEditRow.getPropertyTitleField(), componentPropertyEditRow.getDataValidationHelper(), componentPropertyEditRow.getDefaultValueButton(), componentPropertyEditRow.getRowOverwriteCheckBox(), componentPropertyEditRow.getAffectsUserImplementedClassIndicator(), componentPropertyEditRow.getInputField(), componentPropertyEditRow.getMeta());
        return componentPropertyEditRow;
    }

    private void addLabelAndParamInput(JBPanel propertiesEditorPanel, GridBagConstraints gc, int tabley, JLabel propertyLabel, JButton helpButton, JButton defaultValueButton, JCheckBox overwriteCheckBox, JLabel affectsUserImplementedClassIndicator, ComponentInput componentInput, ComponentPropertyMeta meta) {
        gc.weightx = 0.0;
        gc.gridx = 0;
        gc.gridy = tabley;
        propertiesEditorPanel.add(propertyLabel, gc);
        ++gc.gridx;
        List<JComponent> auxiliaryWidgets = new ArrayList<>();
        if (helpButton != null) auxiliaryWidgets.add(helpButton);
        if (defaultValueButton != null) auxiliaryWidgets.add(defaultValueButton);
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
        return result;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }

    public void setComponentDescription(HtmlScrollingDisplayPanel htmlScrollingDisplayPanel) {
        this.htmlScrollingDisplayPanel = htmlScrollingDisplayPanel;
    }
}
