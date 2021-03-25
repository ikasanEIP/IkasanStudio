package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.*;
import org.ikasan.studio.ui.component.ScrollableGridbagPanel;
import org.ikasan.studio.ui.model.IkasanFlowUIComponent;
import org.ikasan.studio.ui.model.IkasanFlowUIComponentFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PropertiesPanel extends JPanel {
    private static final String PROPERTIES_TAG = "Properties";
    private transient IkasanComponent selectedComponent;
    private transient List<ComponentPropertyEditBox> componentPropertyEditBoxList;

    private String projectKey;
    private Boolean popupMode;
    private JLabel propertiesHeaderLabel = new JLabel(PROPERTIES_TAG);
    private JCheckBox beskpokeComponentOverrideCheckBox;
    JButton okButton;
    private ScrollableGridbagPanel scrollableGridbagPanel;

    public PropertiesPanel(String projectKey, Boolean popupMode) {
        super();
        this.projectKey = projectKey ;
        this.popupMode = popupMode;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        if (! popupMode) {
            JPanel propertiesHeaderPanel = new JPanel();
            propertiesHeaderLabel.setBorder(new EmptyBorder(12,0,12,0));
            propertiesHeaderPanel.add(propertiesHeaderLabel);
            propertiesHeaderPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            add(propertiesHeaderPanel, BorderLayout.NORTH);
        }

        JPanel propertiesBodyPanel = new JPanel(new BorderLayout());
        propertiesBodyPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        propertiesBodyPanel.setBackground(Color.WHITE);

        JPanel propertiesEditorPanel = populatePropertiesEditorPanel();
        scrollableGridbagPanel = new ScrollableGridbagPanel(propertiesEditorPanel);
        JScrollPane scrollPane = new JScrollPane(scrollableGridbagPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        propertiesBodyPanel.add(scrollPane, BorderLayout.CENTER);
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(propertiesBodyPanel, BorderLayout.CENTER);

        if (! popupMode) {
            okButton = new JButton(new UpdateCodeAction("Update code", "Update the code with properties settings", KeyEvent.VK_U));
            JPanel footerPanel = new JPanel();
            footerPanel.add(okButton);
            footerPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            add(footerPanel, BorderLayout.SOUTH);
        }
    }

    // @todo (optional) confirmation box if overriding, maybe include a list of classes (including appropriate properties that may be overriden).
    class UpdateCodeAction extends AbstractAction {
        public UpdateCodeAction(String text,
                          String desc, Integer mnemonic) {
            super(text);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
        }
        public void actionPerformed(ActionEvent e) {
            if (processEditedFlowComponents()) {
                Context.getPipsiIkasanModel(projectKey).generateSourceFromModel();
                Context.getDesignerCanvas(projectKey).setInitialiseAllDimensions(true);
                Context.getDesignerCanvas(projectKey).repaint();
                resetCheckboxes();
            }
            //@todo popup a temp message 'no changes detected'
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


    /**
     * Given the component within, generate an appropriate Panel title
     * @return A String containing the panel title.
     */
    public String getPropertiesPanelTitle() {
        String propertyType = "";
        if (selectedComponent instanceof IkasanModule) {
            propertyType = "Module " + PROPERTIES_TAG;
        } else if (selectedComponent instanceof IkasanFlow) {
            propertyType = "Flow " + PROPERTIES_TAG;
        } else if (selectedComponent instanceof IkasanFlowComponent) {
            IkasanFlowUIComponent type = IkasanFlowUIComponentFactory
                    .getInstance()
                    .getIkasanFlowUIComponentFromType((((IkasanFlowComponent) selectedComponent).getType()));
            if (type.getIkasanComponentType() != IkasanComponentType.UNKNOWN) {
                propertyType = type.getTitle() + " " + PROPERTIES_TAG;
            } else {
                propertyType = "Component " + PROPERTIES_TAG;
            }
        }
        return propertyType;

    }

    public void updatePropertiesPanel(IkasanComponent selectedComponent) {
        this.selectedComponent = selectedComponent;
        if (! popupMode) {
            propertiesHeaderLabel.setText(getPropertiesPanelTitle());
        }
        populatePropertiesEditorPanel();
        scrollableGridbagPanel.revalidate();
        scrollableGridbagPanel.repaint();
    }

    /**
     * For the given component, get all the editable properties and add them the to properties edit panel.
     * @return the fully populated editor panel
     */
    private JPanel populatePropertiesEditorPanel() {
        JPanel allPropertiesEditorPanel = new JPanel(new GridBagLayout());
        allPropertiesEditorPanel.setBackground(Color.WHITE);

        JPanel mandatoryPropertiesEditorPanel = new JPanel(new GridBagLayout());
        JPanel optionalPropertiesEditorPanel = new JPanel(new GridBagLayout());
        JPanel regeneratingPropertiesEditorPanel = new JPanel(new GridBagLayout());


        if (selectedComponent != null) {
            scrollableGridbagPanel.removeAll();
            componentPropertyEditBoxList = new ArrayList<>();

            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.insets = new Insets(3, 4, 3, 4);

            int mandatoryTabley = 0;
            int regenerateTabley = 0;
            int optionalTabley = 0;
            if (selectedComponent.getProperty(IkasanComponentPropertyMeta.NAME) != null) {
                componentPropertyEditBoxList.add(addNameValueToPropertiesEditPanel(mandatoryPropertiesEditorPanel,
                        selectedComponent.getProperty(IkasanComponentPropertyMeta.NAME), gc, mandatoryTabley++));
            }

            if (selectedComponent instanceof IkasanComponent) {
                IkasanComponent selectedFlowComponent = (IkasanComponent) selectedComponent;
                if (selectedFlowComponent.getType().getMetadataMap().size() > 0) {
                    for (Map.Entry<String, IkasanComponentPropertyMeta> entry : selectedFlowComponent.getType().getMetadataMap().entrySet()) {
                        String key = entry.getKey();
                        if (!key.equals(IkasanComponentPropertyMeta.NAME)
                        ) {
                            IkasanComponentProperty property = selectedComponent.getProperty(key);
                            if (property == null) {
                                // This property has not yet been set for the component
                                property = new IkasanComponentProperty(((IkasanFlowComponent) selectedComponent).getType().getMetaDataForPropertyName(key));
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
                }
                if (selectedFlowComponent.getType().isBespokeClass() && !popupMode) {
                    addOverrideCheckBoxToPropertiesEditPanel(mandatoryPropertiesEditorPanel, gc, mandatoryTabley++);
                } else {
                    if (okButton != null) {
                        okButton.setEnabled(true);
                    }
                }
            }

            GridBagConstraints gc1 = new GridBagConstraints();
            gc1.fill = GridBagConstraints.HORIZONTAL;
            gc1.insets = new Insets(3, 4, 3, 4);
            gc1.gridx = 0;
            gc1.weightx = 1;
            gc1.gridy = 0;

            if (mandatoryTabley > 0) {
                setSubPanel(allPropertiesEditorPanel, mandatoryPropertiesEditorPanel, "Mandatory Properties", Color.red, gc1);
            }

            if (regenerateTabley > 0) {
                setSubPanel(allPropertiesEditorPanel, regeneratingPropertiesEditorPanel, "Code Regenerating Properties", Color.orange, gc1);
            }

            if (optionalTabley > 0) {
                setSubPanel(allPropertiesEditorPanel, optionalPropertiesEditorPanel, "Optional Properties", Color.LIGHT_GRAY, gc1);
            }
            scrollableGridbagPanel.add(allPropertiesEditorPanel);
        }

        return allPropertiesEditorPanel;
    }


    private void setSubPanel(JPanel allPropertiesEditorPanel, JPanel subPanel, String title, Color borderColor, GridBagConstraints gc1) {
        subPanel.setBackground(Color.WHITE);
        subPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(borderColor), title,
                TitledBorder.LEFT,
                TitledBorder.TOP));
        allPropertiesEditorPanel.add(subPanel, gc1);
        gc1.gridy += 1;
    }

    private void addOverrideCheckBoxToPropertiesEditPanel(JPanel propertiesEditorPanel, GridBagConstraints gc, int tabley) {
        JLabel overrideLabel = new JLabel("Regenerate Code");
        overrideLabel.setToolTipText("Check the box if you wish to rewrite / overwrite the existing code for this beskpoke implementation");
        beskpokeComponentOverrideCheckBox = new JCheckBox();
        beskpokeComponentOverrideCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ie) {
                okButton.setEnabled(ie.getStateChange() == 1);
            }
        });
        beskpokeComponentOverrideCheckBox.setBackground(Color.WHITE);
        okButton.setEnabled(false);
        addLabelAndInputEditor(propertiesEditorPanel, gc, tabley, overrideLabel, beskpokeComponentOverrideCheckBox);
    }

    private ComponentPropertyEditBox addNameValueToPropertiesEditPanel(JPanel propertiesEditorPanel, IkasanComponentProperty componentProperty, GridBagConstraints gc, int tabley) {
        ComponentPropertyEditBox componentPropertyEditBox = new ComponentPropertyEditBox(componentProperty, popupMode);
        if (componentProperty.getMeta().isUserImplementedClass() && !popupMode) {
            addLabelInputEditorAndGenerateSource(propertiesEditorPanel, gc, tabley,
                    componentPropertyEditBox.getPropertyTitleField(), componentPropertyEditBox.getInputField(),
                    componentPropertyEditBox.getRegenerateLabel(), componentPropertyEditBox.getRegenerateSourceCheckBox());
        } else {
            addLabelAndInputEditor(propertiesEditorPanel, gc, tabley, componentPropertyEditBox.getPropertyTitleField(), componentPropertyEditBox.getInputField());
        }
        return componentPropertyEditBox;
    }

    private void addLabelAndInputEditor(JPanel propertiesEditorPanel, GridBagConstraints gc, int tabley, JLabel propertyLabel, JComponent propertyInputField) {
        gc.weightx = 0.0;
        gc.gridx = 0;
        gc.gridy = tabley;
        propertiesEditorPanel.add(propertyLabel, gc);
        gc.weightx = 1.0;
        gc.gridx = 1;
        propertiesEditorPanel.add(propertyInputField, gc);
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



    /**
     * Check to see if any new values have been entered, update the model and return true if that is the case.
     * @return true if the model has been updated with new values.
     */
    public boolean processEditedFlowComponents() {
        boolean modelUpdated = false;
        if (componentPropertyEditBoxList != null) {
            for (final ComponentPropertyEditBox editPair: componentPropertyEditBoxList) {
                final String key = editPair.getPropertyLabel();
                IkasanComponentProperty componentProperty = selectedComponent.getProperties().get(key);
                if (propertyValueHasChanged(componentProperty, editPair)) {
                    modelUpdated = true;
                    if (selectedComponent instanceof IkasanFlowBeskpokeComponent) {
                        ((IkasanFlowBeskpokeComponent)selectedComponent).setOverrideEnabled(true);
                    }
                    if (componentProperty == null) {
                        // New property added for the first time, for now only components will have new properties not flows
                        componentProperty = new IkasanComponentProperty(((IkasanFlowComponent) selectedComponent).getType().getMetaDataForPropertyName(key), editPair.getValue());
                        selectedComponent.addComponentProperty(key, componentProperty);
                    } else {
                        // update existing
                        componentProperty.setValue(editPair.getValue());
                        if (editPair.isUserMaintainedClassWithPermissionToRegenerate()) {
                            componentProperty.setRegenerateAllowed(true);
                        }

                    }
                }
            }
        }
        return modelUpdated;
    }

    /**
     * Determine if the property has been updated.
     * @param property of the existing ikasan Component
     * @param propertyEditBox should never be null when called.
     * @return true if the property has been altered
     */
    private boolean propertyValueHasChanged(IkasanComponentProperty property, ComponentPropertyEditBox propertyEditBox) {
        return (((property == null || property.getValue() == null) && editBoxHasValue(propertyEditBox)) ||
                (property != null && editBoxHasValue(propertyEditBox) && !property.getValue().equals(propertyEditBox.getValue())) ||
                (propertyEditBox.isUserMaintainedClassWithPermissionToRegenerate() && editBoxHasValue(propertyEditBox)));
    }

    /**
     * Determine if the edit box has a valid value that would warrant the source code to be updated
     * If the property generates source code, also check if 'regenerate' is selected (note this is assumed true in popup mode).
     * @param propertyEditBox to examine.
     * @return true if the editbox has a non-whitespace / real value.
     */
    private boolean editBoxHasValue(ComponentPropertyEditBox propertyEditBox) {
        boolean hasValue = false;
        if (propertyEditBox != null) {
//            boolean regeneratePermission = true;
            Object value = propertyEditBox.getValue();
//            if (propertyEditBox.isUserMaintainedClass()) {
//                regeneratePermission = propertyEditBox.isUserMaintainedClassWithPermissionToRegenerate();
//            }
            if (value instanceof String) {
                if (((String) value).length() > 0) {
                    hasValue = true;
//                    hasValue = true && regeneratePermission;
                }
            } else {
                hasValue = (value != null);
//                hasValue = (value != null) && regeneratePermission;
            }
        }
        return hasValue;
    }

    public List<ComponentPropertyEditBox> getComponentPropertyEditBoxList() {
        return componentPropertyEditBoxList;
    }

    public IkasanComponent getSelectedComponent() {
        return selectedComponent;
    }
}
