package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.ui.ValidationInfo;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Encapsulate the properties entry from a UI and validity perspective.
 */
public class ComponentPropertiesPanel extends PropertiesPanel {
//    private static final String PROPERTIES_TAG = "Properties";
//    private transient IkasanComponent getSelectedComponent();
    private transient List<ComponentPropertyEditBox> componentPropertyEditBoxList;

//    private String projectKey;
//    private boolean popupMode;
//    private JLabel propertiesHeaderLabel = new JLabel(PROPERTIES_TAG);
    private JCheckBox beskpokeComponentOverrideCheckBox;
//    JButton okButton;
//    private ScrollableGridbagPanel scrollableGridbagPanel;
//    private JLabel trueLabel = new JLabel("true");
//    private JLabel falseLabel = new JLabel("false");

    /**
     * Create the ComponentPropertiesPanel
     *
     * Note that this panel could be reused for different ComponentPropertiesPanel, it is the super.updateTargetComponent
     * that will set the property to be exposed / edited.
     *
     * @param projectKey for this project
     * @param popupMode true if this is for the popup version, false if this is for the canvas sidebar.
     */
    public ComponentPropertiesPanel(String projectKey, boolean popupMode) {
        super(projectKey, popupMode);
//        super();
//        this.projectKey = projectKey ;
//        this.popupMode = popupMode;
//        setLayout(new BorderLayout());
//        setBackground(Color.WHITE);

//        if (! popupMode) {
//            JPanel propertiesHeaderPanel = new JPanel();
//            propertiesHeaderLabel.setBorder(new EmptyBorder(12,0,12,0));
//            propertiesHeaderPanel.add(propertiesHeaderLabel);
//            propertiesHeaderPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
//            add(propertiesHeaderPanel, BorderLayout.NORTH);
//        }

//        JPanel propertiesBodyPanel = new JPanel(new BorderLayout());
//        propertiesBodyPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
//        propertiesBodyPanel.setBackground(Color.WHITE);

//
//        if (! popupMode) {
//            okButton = new JButton("Update code");
//            okButton.addActionListener(e -> {
//                   OkActionListener();
//                   doOKAction();
//                   //@todo popup a temp message 'no changes detected'
//               }
//            );
//            JPanel footerPanel = new JPanel();
//            footerPanel.add(okButton);
//            footerPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
//            add(footerPanel, BorderLayout.SOUTH);
//        }
//
//        JPanel propertiesEditorPanel = populatePropertiesEditorPanel();
//        scrollableGridbagPanel = new ScrollableGridbagPanel(propertiesEditorPanel);
//        JScrollPane scrollPane = new JScrollPane(scrollableGridbagPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//        scrollPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
//        propertiesBodyPanel.add(scrollPane, BorderLayout.CENTER);
//        scrollPane.setBackground(Color.WHITE);
//        scrollPane.getViewport().setBackground(Color.WHITE);
//        add(propertiesBodyPanel, BorderLayout.CENTER);
//        setFocusOnFirstComponent();
    }

//    protected void OkActionListener(ActionEvent ae) {
//        List<ValidationInfo> infoList = doValidateAll();
//        if (!infoList.isEmpty()) {
//            ValidationInfo firstInfo = infoList.get(0);
//            if (firstInfo.component != null && firstInfo.component.isVisible()) {
//                IdeFocusManager.getInstance(null).requestFocus(firstInfo.component, true);
//            }
//            StringBuilder validationErrors = new StringBuilder();
//            for (ValidationInfo info : infoList) {
//                validationErrors.append(info.message).append('\n');
//            }
//            JOptionPane.showMessageDialog(((JButton)ae.getSource()).getParent().getParent(),
//                    validationErrors.toString(),
//                    "Validation Error",
//                    JOptionPane.ERROR_MESSAGE);
//            if (infoList.stream().anyMatch(info1 -> !info1.okEnabled)) {
//                return;
//            }
//        }
//    }
    
    /**
     * This method is invoked when we have checked its OK to process the panel i.e. all items are valid
     */
    protected void doOKAction() {
        if (dataHasChanged()) {
            processEditedFlowComponents();
            Context.getPipsiIkasanModel(projectKey).generateSourceFromModel();
            Context.getDesignerCanvas(projectKey).setInitialiseAllDimensions(true);
            Context.getDesignerCanvas(projectKey).repaint();
            resetCheckboxes();
        }
    }

    private void resetCheckboxes() {
        for (ComponentPropertyEditBox componentPropertyEditBox : componentPropertyEditBoxList) {
            componentPropertyEditBox.disableRegeneratingFeilds();
        }
        if (beskpokeComponentOverrideCheckBox != null) {
            beskpokeComponentOverrideCheckBox.setSelected(false);
        }
    }

//    /**
//     * Given the component within, generate an appropriate Panel title
//     * @return A String containing the panel title.
//     */
//    public String getPropertiesPanelTitle() {
//        String propertyType = "";
//        if (getSelectedComponent() instanceof IkasanModule) {
//            propertyType = "Module " + PROPERTIES_TAG;
//        } else if (getSelectedComponent() instanceof IkasanFlow) {
//            propertyType = "Flow " + PROPERTIES_TAG;
//        } else if (getSelectedComponent() instanceof IkasanFlowComponent) {
//            IkasanFlowUIComponent type = IkasanFlowUIComponentFactory
//                    .getInstance()
//                    .getIkasanFlowUIComponentFromType((((IkasanFlowComponent) getSelectedComponent()).getType()));
//            if (type.getIkasanComponentType() != IkasanComponentType.UNKNOWN) {
//                propertyType = type.getTitle() + " " + PROPERTIES_TAG;
//            } else {
//                propertyType = "Component " + PROPERTIES_TAG;
//            }
//        }
//        return propertyType;
//
//    }
//
//    public void updatePropertiesPanel(IkasanComponent getSelectedComponent()) {
//        this.getSelectedComponent() = getSelectedComponent();
//        if (! popupMode) {
//            propertiesHeaderLabel.setText(getPropertiesPanelTitle());
//        }
//        populatePropertiesEditorPanel();
//        scrollableGridbagPanel.revalidate();
//        scrollableGridbagPanel.repaint();
//        setFocusOnFirstComponent();
//    }

    /**
     * When updateTargetComponent is called, it will set the component to be exposed / edited, it will then
     * delegate update of the editor pane to this component so that we can specialise for different components.
     *
     * For the given component, get all the editable properties and add them the to properties edit panel.
     * @return the fully populated editor panel
     */
    protected JPanel populatePropertiesEditorPanel() {
        JPanel containerPanel = new JPanel(new GridBagLayout());
        containerPanel.setBackground(Color.WHITE);

        JPanel mandatoryPropertiesEditorPanel = new JPanel(new GridBagLayout());
        JPanel optionalPropertiesEditorPanel = new JPanel(new GridBagLayout());
        JPanel regeneratingPropertiesEditorPanel = new JPanel(new GridBagLayout());

        if (!popupMode) {
            okButton.setEnabled(false);
        }

        if (getSelectedComponent() != null) {
            scrollableGridbagPanel.removeAll();
            componentPropertyEditBoxList = new ArrayList<>();

            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.insets = new Insets(3, 4, 3, 4);

            int mandatoryTabley = 0;
            int regenerateTabley = 0;
            int optionalTabley = 0;
            if (getSelectedComponent().getProperty(IkasanComponentPropertyMeta.NAME) != null) {
                componentPropertyEditBoxList.add(addNameValueToPropertiesEditPanel(mandatoryPropertiesEditorPanel,
                        getSelectedComponent().getProperty(IkasanComponentPropertyMeta.NAME), gc, mandatoryTabley++));
            }
            if (getSelectedComponent().getType().getMetadataMap().size() > 0) {
                for (Map.Entry<IkasanComponentPropertyMetaKey, IkasanComponentPropertyMeta> entry : getSelectedComponent().getType().getMetadataMap().entrySet()) {
                    IkasanComponentPropertyMetaKey key = entry.getKey();
                    if (!key.equals(IkasanComponentPropertyMeta.NAME)) {
                        IkasanComponentProperty property = getSelectedComponent().getProperty(key);
                        if (property == null) {
                            // This property has not yet been set for the component
                            property = new IkasanComponentProperty(((IkasanFlowComponent) getSelectedComponent()).getType().getMetaDataForPropertyName(key));
                        }
                        if (property.getMeta().isMandatory()) {
                            componentPropertyEditBoxList.add(addNameValueToPropertiesEditPanel(
                                    mandatoryPropertiesEditorPanel,
                                    property, gc, mandatoryTabley++));
                        } else if (property.getMeta().isUserImplementedClass()) {
                            componentPropertyEditBoxList.add(addNameValueToPropertiesEditPanel(
                                    regeneratingPropertiesEditorPanel,
                                    property, gc, regenerateTabley++));
                        } else {
                            componentPropertyEditBoxList.add(addNameValueToPropertiesEditPanel(
                                    optionalPropertiesEditorPanel,
                                    property, gc, optionalTabley++));
                        }
                    }
                }
                if (!popupMode && getSelectedComponent().getType().isBespokeClass()) {
                    addOverrideCheckBoxToPropertiesEditPanel(mandatoryPropertiesEditorPanel, gc, mandatoryTabley++);
                }
            }

            GridBagConstraints gc1 = new GridBagConstraints();
            gc1.fill = GridBagConstraints.HORIZONTAL;
            gc1.insets = new Insets(3, 4, 3, 4);
            gc1.gridx = 0;
            gc1.weightx = 1;
            gc1.gridy = 0;

            if (mandatoryTabley > 0) {
                setSubPanel(containerPanel, mandatoryPropertiesEditorPanel, "Mandatory Properties", Color.red, gc1);
            }

            if (regenerateTabley > 0) {
                setSubPanel(containerPanel, regeneratingPropertiesEditorPanel, "Code Regenerating Properties", Color.orange, gc1);
            }

            if (optionalTabley > 0) {
                setSubPanel(containerPanel, optionalPropertiesEditorPanel, "Optional Properties", Color.LIGHT_GRAY, gc1);
            }
            scrollableGridbagPanel.add(containerPanel);

            if (!popupMode && !getSelectedComponent().getType().isBespokeClass() && ! getComponentPropertyEditBoxList().isEmpty()) {
                okButton.setEnabled(true);
            }
        }

        return containerPanel;
    }

//    public void setFocusOnFirstComponent() {
//        JComponent firstComponent = getFirstFocusField();
//        if (firstComponent != null) {
//            Context.getProject(projectKey);
//            IdeFocusManager.getInstance(Context.getProject(projectKey)).requestFocus(firstComponent, true);
//        }
//    }

    /**
     * Get the field that should be given the focus in popup or inscreen form
     * @return the component that should be given focus or null
     */
    public JComponent getFirstFocusField() {
        JComponent firstComponent = null;
        if (componentPropertyEditBoxList != null && !componentPropertyEditBoxList.isEmpty()) {
            firstComponent = componentPropertyEditBoxList.get(0).getInputField().getFirstFocusComponent();
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
    private void setSubPanel(JPanel allPropertiesEditorPanel, JPanel subPanel, String title, Color borderColor, GridBagConstraints gc1) {
        subPanel.setBackground(Color.WHITE);
        subPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(borderColor), title,
                TitledBorder.LEFT,
                TitledBorder.TOP));
        allPropertiesEditorPanel.add(subPanel, gc1);
        gc1.gridy += 1;
    }

    /**
     * The override checkbox safeguards autogenerated code that the user might have subsequently customised
     * @param propertiesEditorPanel is the parent
     * @param gc is used to dictate layout and relay layout to the next subsection.
     * @param tabley is used to convey the row number
     */
    private void addOverrideCheckBoxToPropertiesEditPanel(JPanel propertiesEditorPanel, GridBagConstraints gc, int tabley) {
        JLabel overrideLabel = new JLabel("Regenerate Code");
        overrideLabel.setToolTipText("Check the box if you wish to rewrite / overwrite the existing code for this beskpoke implementation");
        beskpokeComponentOverrideCheckBox = new JCheckBox();
        beskpokeComponentOverrideCheckBox.addItemListener(ie ->
            okButton.setEnabled(ie.getStateChange() == 1)
        );
        beskpokeComponentOverrideCheckBox.setBackground(Color.WHITE);
        addLabelAndSimpleInput(propertiesEditorPanel, gc, tabley, overrideLabel, beskpokeComponentOverrideCheckBox);
    }

    /**
     *
     * @param propertiesEditorPanel to hold the name/value pair
     * @param componentProperty being added
     * @param gc is used to dictate layout and relay layout to the next subsection.
     * @param tabley is used to convey the row number
     * @return a populated 'row' i.e. a container that supports the edit of the supplied name / value pair.
     */
    private ComponentPropertyEditBox addNameValueToPropertiesEditPanel(JPanel propertiesEditorPanel, IkasanComponentProperty componentProperty, GridBagConstraints gc, int tabley) {
        ComponentPropertyEditBox componentPropertyEditBox = new ComponentPropertyEditBox(componentProperty, popupMode);

        if (componentProperty.isUserImplementedClass() && !popupMode) {
            addLabelInputEditorAndGenerateSource(propertiesEditorPanel, gc, tabley,
                    componentPropertyEditBox.getPropertyTitleField(), componentPropertyEditBox.getOverridingInputField(),
                    componentPropertyEditBox.getRegenerateLabel(), componentPropertyEditBox.getRegenerateSourceCheckBox());
        } else {
            addLabelAndParamInput(propertiesEditorPanel, gc, tabley, componentPropertyEditBox.getPropertyTitleField(), componentPropertyEditBox.getInputField());
        }
        return componentPropertyEditBox;
    }

    private void addLabelAndSimpleInput(JPanel propertiesEditorPanel, GridBagConstraints gc, int tabley, JLabel propertyLabel, JComponent propertyInputField) {
        gc.weightx = 0.0;
        gc.gridx = 0;
        gc.gridy = tabley;
        propertiesEditorPanel.add(propertyLabel, gc);
        gc.weightx = 1.0;
        gc.gridx = 1;
        propertiesEditorPanel.add(propertyInputField, gc);
    }

    private void addLabelAndParamInput(JPanel propertiesEditorPanel, GridBagConstraints gc, int tabley, JLabel propertyLabel, ComponentInput componentInput) {
        gc.weightx = 0.0;
        gc.gridx = 0;
        gc.gridy = tabley;
        propertiesEditorPanel.add(propertyLabel, gc);
        ++gc.gridx;
        if (!componentInput.isBooleanInput()) {
            gc.weightx = 1.0;
            propertiesEditorPanel.add(componentInput.getFirstFocusComponent(), gc);
        } else {
            JPanel booleanPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            booleanPanel.setBackground(Color.WHITE);
            booleanPanel.add(new JLabel("true"));
            booleanPanel.add(componentInput.getTrueBox());
            booleanPanel.add(new JLabel("false"));
            booleanPanel.add(componentInput.getFalseBox());
            propertiesEditorPanel.add(booleanPanel, gc);
        }
    }

    private void addLabelInputEditorAndGenerateSource(JPanel propertiesEditorPanel, GridBagConstraints gc, int tabley,
                                                      JLabel propertyLabel, JComponent propertyInputField,
                                                      JLabel generateSourceLabel, JCheckBox generateSourceCheckbox) {
        gc.weightx = 0.0;
        gc.gridx = 0;
        gc.gridy = tabley;
        propertiesEditorPanel.add(propertyLabel, gc);
        gc.weightx = 1.0;
        gc.gridx = 1;
        propertiesEditorPanel.add(propertyInputField, gc);

        gc.weightx = 0.0;
        gc.gridx = 2;
        propertiesEditorPanel.add(generateSourceLabel, gc);
        gc.gridx = 3;
        propertiesEditorPanel.add(generateSourceCheckbox, gc);
    }

    protected IkasanComponent getSelectedComponent() {
        return (IkasanComponent)super.getSelectedComponent();
    }

    @Override
    public boolean dataHasChanged() {
        boolean modelUpdated = false;
        if (componentPropertyEditBoxList != null) {
            for (final ComponentPropertyEditBox componentPropertyEditBox : componentPropertyEditBoxList) {
                if (componentPropertyEditBox.propertyValueHasChanged()) {
                    modelUpdated = true;
                    break;
                }
            }
        }
        return modelUpdated;
    }

    /**
     * Check to see if any new values have been entered, update the model and return true if that is the case.
     * @return true if the model has been updated with new values.
     */
    public void processEditedFlowComponents() {
        if (componentPropertyEditBoxList != null) {
            for (final ComponentPropertyEditBox componentPropertyEditBox: componentPropertyEditBoxList) {
//                IkasanComponentProperty componentProperty = componentPropertyEditBox.getComponentProperty();
                if (componentPropertyEditBox.propertyValueHasChanged()) {
                    if (getSelectedComponent() instanceof IkasanFlowBeskpokeComponent) {
                        ((IkasanFlowBeskpokeComponent)getSelectedComponent()).setOverrideEnabled(true);
                    }

//                    // Not sure its ever null ??
//                    if (componentProperty == null) {
//                        // New property added for the first time, for now only components will have new properties not flows
//                        if (editPair.getValue() != null) {
//                            componentProperty = new IkasanComponentProperty(((IkasanFlowComponent) getSelectedComponent()).getType().getMetaDataForPropertyName(key), editPair.getValue());
//                            getSelectedComponent().addComponentProperty(key, componentProperty);
//                        }
//                    } else {
                        // Property has been unset e.g. a boolean
                        if (!componentPropertyEditBox.editBoxHasValue()) {
                            getSelectedComponent().removeProperty(componentPropertyEditBox.getPropertyKey());
                        } else { // update existing
                            componentPropertyEditBox.updateValueObjectWithEnteredValues();
//                            componentProperty.setValue(editPair.getValue());
//                            if (editPair.isUserMaintainedClassWithPermissionToRegenerate()) {
//                                componentProperty.setRegenerateAllowed(true);
//                            }
                        }
//                    }
                }
            }
        }
    }

//    /**
//     * Determine if the property has been updated.
//     * @param property of the existing ikasan Component
//     * @param componentPropertyEditBox should never be null when called.
//     * @return true if the property has been altered
//     */
//    private boolean propertyValueHasChanged(IkasanComponentProperty property, @NotNull ComponentPropertyEditBox componentPropertyEditBox) {
//        Object propertyValue = null;
//        if (property != null) {
//            propertyValue = property.getValue();
//        }
//
//        return ((propertyValue == null && componentPropertyEditBox.editBoxHasValue()) ||
////                (property != null && editBoxHasValue(componentPropertyEditBox) && !property.getValue().equals(componentPropertyEditBox.getValue())) ||
//                (propertyValue != null && !property.getValue().equals(componentPropertyEditBox.getValue())) ||
//                (componentPropertyEditBox != null && componentPropertyEditBox.isUserMaintainedClassWithPermissionToRegenerate() && editBoxHasValue(componentPropertyEditBox)) );
//    }

//    /**
//     * Determine if the edit box has a valid value that would warrant the source code to be updated
//     * If the property generates source code, also check if 'regenerate' is selected (note this is assumed true in popup mode).
//     * @param componentPropertyEditBox to examine.
//     * @return true if the editbox has a non-whitespace / real value.
//     */
//    private boolean editBoxHasValue(ComponentPropertyEditBox componentPropertyEditBox) {
//        boolean hasValue = false;
//        if (componentPropertyEditBox != null) {
//            Object value = componentPropertyEditBox.getValue();
//            if (value instanceof String) {
//                if (((String) value).length() > 0) {
//                    hasValue = true;
//                }
//            } else {
//                hasValue = (value != null);
//            }
//        }
//        return hasValue;
//    }

    public List<ComponentPropertyEditBox> getComponentPropertyEditBoxList() {
        return componentPropertyEditBoxList;
    }

//    public IkasanComponent getSelectedComponent() {
//        return getSelectedComponent();
//    }

    /**
     * Validates the values populated
     * @return a populated ValidationInfo array if htere are any validation issues.
     */
    protected java.util.List<ValidationInfo> doValidateAll() {
        List<ValidationInfo> result = new ArrayList<>();
        for (final ComponentPropertyEditBox editPair: getComponentPropertyEditBoxList()) {
            result.addAll(editPair.doValidateAll());

//            final String key = editPair.getPropertyKey();
//            IkasanComponentProperty componentProperty = getSelectedComponent().getProperty(key);
//            // Only mandatory properties are always populated.
//            if (componentProperty != null &&
//                    componentProperty.getMeta().isMandatory() &&
//                    !editPair.isBooleanProperty() &&
//                    editPair.inputfieldIsUnset()) {
//
//                result.add(new ValidationInfo(editPair.getPropertyKey() + " must be set to a valid value", editPair.getOverridingInputField()));
//            }
        }
        return result;
    }
}
