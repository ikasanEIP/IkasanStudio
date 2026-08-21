package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.StudioBuildUtils;
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
        super(project, componentInitialisation);
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
     * Ask the user to confirm regeneration of the user implemented class, naming exactly which changed
     * properties are driving that regeneration, and (where they can be determined) exactly which hand-written
     * class(es) will be regenerated as a result.
     * @return true if the user confirmed, false if they declined.
     */
    private boolean confirmRegenerateUserImplementedClass(List<String> changedPropertyLabels) {
        List<String> affectedClassDescriptions = getAffectedUserImplementedClassDescriptions();
        String message = affectedClassDescriptions.isEmpty()
                ? StudioBundle.message("message.ConfirmRegenerateUserImplementedClass", String.join(", ", changedPropertyLabels))
                : StudioBundle.message("message.ConfirmRegenerateUserImplementedClassNamed",
                        String.join(", ", changedPropertyLabels), String.join("\n", affectedClassDescriptions));
        int result = Messages.showYesNoDialog(project, message, StudioBundle.message("dialog.ConfirmRegenerateUserImplementedClass"), Messages.getWarningIcon());
        return result == Messages.YES;
    }

    /**
     * Name the hand-written class(es) the pending change will regenerate.
     * - Editing a {@link FlowUserImplementedElement} directly (Debug, CustomConverter, GenericConsumer, etc.)
     *   only ever affects that same component's own generated class.
     * - Editing the {@link Module} itself is different: name/applicationPackageName/version determine the
     *   package every {@link FlowUserImplementedElement} in every flow is generated into
     *   ({@link org.ikasan.studio.core.generator.GeneratorUtils#getUserImplementedClassesPackageName}), so a
     *   change there potentially affects every such class across the whole module.
     * Returns an empty list (rather than guessing) for any other case, or where a class name can't yet be
     * determined - the caller falls back to the generic, unnamed confirmation message.
     */
    private List<String> getAffectedUserImplementedClassDescriptions() {
        if (getSelectedComponent() instanceof Module module) {
            List<String> descriptions = new ArrayList<>();
            if (module.getFlows() != null) {
                for (Flow flow : module.getFlows()) {
                    descriptions.addAll(userImplementedClassDescriptionsForFlow(flow));
                }
            }
            return descriptions;
        } else if (getSelectedComponent() instanceof FlowUserImplementedElement flowUserImplementedElement
                && flowUserImplementedElement.getContainingFlow() != null) {
            String description = userImplementedClassDescription(flowUserImplementedElement.getContainingFlow(), flowUserImplementedElement);
            return description != null ? List.of(description) : List.of();
        }
        return List.of();
    }

    private List<String> userImplementedClassDescriptionsForFlow(Flow flow) {
        List<String> descriptions = new ArrayList<>();
        if (flow.getFlowRoute() == null) {
            return descriptions;
        }
        for (FlowElement component : flow.getFlowRoute().getConsumerAndFlowRouteElements()) {
            if (component instanceof FlowUserImplementedElement) {
                String description = userImplementedClassDescription(flow, component);
                if (description != null) {
                    descriptions.add(description);
                }
            }
        }
        return descriptions;
    }

    private String userImplementedClassDescription(Flow flow, FlowElement component) {
        ComponentProperty classNameProperty = component.getProperty(ComponentPropertyMeta.USER_IMPLEMENTED_CLASS_NAME);
        String className = classNameProperty != null ? (String) classNameProperty.getValue() : null;
        return className == null ? null : (flow.getIdentity() + ": " + className + ".java");
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
            @SuppressWarnings("rawtypes")
            JBPanel regeneratingPropertiesEditorPanel = new JBPanel(new GridBagLayout());
            regeneratingPropertiesEditorPanel.setBorder(null);
            componentPropertyEditRowList = new ArrayList<>();

            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.insets = JBUI.insets(3, 4);

            int mandatoryTabley = 0;
            int regenerateTabley = 0;
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
                            // Grouped property - keep it out of the always-visible sections even though it
                            // may also be userSuppliedClass; tucked into "Optional Properties".
                            groupedOptionalProperties.computeIfAbsent(property.getMeta().getPropertyGroup(), k -> new ArrayList<>()).add(property);
                        } else if (property.getMeta().isUserSuppliedClass()) {
                            componentPropertyEditRowList.add(addNameValueToPropertiesEditPanel(
                                    regeneratingPropertiesEditorPanel,
                                    property, gc, regenerateTabley++));
                        } else if (property.getMeta().isMandatory()) {
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

            if (regenerateTabley > 0) {
                setSubPanel(propertiesEditorPanel, regeneratingPropertiesEditorPanel, StudioBundle.message("label.UserCodeRegeneratingProperties"), getThemeAwareBorderColor(), gc1);
            }

            if (optionalTabley > 0) {
                optionalPropertiesExpandPanel.setVisible(true);
                setToggleOptionalPropertiesButton(false);
                optionalPropertiesEditorPanel.setVisible(false);
                setSubPanel(propertiesEditorPanel, optionalPropertiesExpandPanel, null, null, gc1);
                setSubPanel(propertiesEditorPanel, optionalPropertiesEditorPanel, StudioBundle.message("label.OptionalProperties"), getThemeAwareBorderColor(), gc1);
            } else if (optionalPropertiesExpandPanel != null) {
                optionalPropertiesExpandPanel.setVisible(false);
            }
            propertiesEditorScrollingContainer.add(propertiesEditorPanel);
            UiContext uiContext = project.getService(UiContext.class);
            uiContext.setRightTabbedPaneFocus(UiContext.PROPERTIES_TAB_INDEX);

            if (htmlScrollingDisplayPanel != null) {
                htmlScrollingDisplayPanel.setText(getSelectedComponent().getComponentMeta().getHelpText());
            }
        }
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
