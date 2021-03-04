package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.Context;
import org.ikasan.studio.model.Ikasan.*;
import org.ikasan.studio.ui.component.ScrollableGridbagPanel;
import org.ikasan.studio.ui.model.IkasanFlowUIComponent;
import org.ikasan.studio.ui.model.IkasanFlowUIComponentFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
    private JCheckBox overrideCheckBox ;
    JButton okButton = new JButton("Update Code");
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
            okButton.addActionListener(getOkButtonListener());
            JPanel footerPanel = new JPanel();
            footerPanel.add(okButton);
            footerPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            add(footerPanel, BorderLayout.SOUTH);
        }
    }

    protected ActionListener getOkButtonListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean modelUpdated = false;
                modelUpdated = processEditedFlowComponents();
                if (modelUpdated) {
                    Context.getPipsiIkasanModel(projectKey).generateSourceFromModel();
                    Context.getDesignerCanvas(projectKey).setInitialiseAllDimensions(true);
                    Context.getDesignerCanvas(projectKey).repaint();
                }
            }
        };
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
            if (type.getIkasanFlowComponentType() != IkasanFlowComponentType.UNKNOWN) {
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
     * @return
     */
    private JPanel populatePropertiesEditorPanel() {
        JPanel allPropertiesEditorPanel = new JPanel(new GridBagLayout());
        allPropertiesEditorPanel.setBackground(Color.WHITE);

        JPanel mandatoryPropertiesEditorPanel = new JPanel(new GridBagLayout());

        JPanel optionalPropertiesEditorPanel = new JPanel(new GridBagLayout());


        if (selectedComponent != null) {
            scrollableGridbagPanel.removeAll();
            componentPropertyEditBoxList = new ArrayList<>();

            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.insets = new Insets(3, 4, 3, 4);

            int mandaoryTabley = 0;
            int optionalTabley = 0;
            if (selectedComponent.getProperty(IkasanComponentPropertyMeta.NAME) != null) {
                componentPropertyEditBoxList.add(addNameValueToPropertiesEditPanel(mandatoryPropertiesEditorPanel, selectedComponent.getProperty(IkasanComponentPropertyMeta.NAME), gc, mandaoryTabley++));
            }
            if (selectedComponent.getProperty(IkasanComponentPropertyMeta.DESCRIPTION) != null) {
                componentPropertyEditBoxList.add(addNameValueToPropertiesEditPanel(mandatoryPropertiesEditorPanel, selectedComponent.getProperty(IkasanComponentPropertyMeta.DESCRIPTION), gc, mandaoryTabley++));
            }

            if (selectedComponent instanceof IkasanFlowComponent) {
                IkasanFlowComponent selectedFlowComponent = (IkasanFlowComponent) selectedComponent;
                if (selectedFlowComponent.getType().getProperties().size() > 0) {
                    for (Map.Entry<String, IkasanComponentPropertyMeta> entry : selectedFlowComponent.getType().getProperties().entrySet()) {
                        String key = entry.getKey();
                        if (!key.equals(IkasanComponentPropertyMeta.NAME) && !key.equals(IkasanComponentPropertyMeta.DESCRIPTION)) {
                            IkasanComponentProperty property = selectedComponent.getProperty(key);
                            if (property == null) {
                                // This property has not yet been set for the component
                                property = new IkasanComponentProperty(((IkasanFlowComponent) selectedComponent).getType().getMetaDataForPropertyName(key));
                            }
                            if (property.getMeta().isMandatory()) {
                                componentPropertyEditBoxList.add(addNameValueToPropertiesEditPanel(
                                        mandatoryPropertiesEditorPanel,
                                        property, gc, mandaoryTabley++));
//                                mandaoryTabley++;
                            } else {
                                componentPropertyEditBoxList.add(addNameValueToPropertiesEditPanel(
                                        optionalPropertiesEditorPanel,
                                        property, gc, optionalTabley++));
//                                optionalTabley++;
                            }

                        }
                    }
                }
                if (selectedFlowComponent.getType().isBespokeClass() && !popupMode) {
                    addOverrideCheckBoxToPropertiesEditPanel(mandatoryPropertiesEditorPanel, gc, mandaoryTabley++);
                } else {
                    okButton.setEnabled(true);
                }
            }

            GridBagConstraints gc1 = new GridBagConstraints();
            gc1.fill = GridBagConstraints.HORIZONTAL;
            gc1.insets = new Insets(3, 4, 3, 4);
            gc1.gridx = 0;
            gc1.weightx = 1;
            gc1.gridy = 0;

            if (mandaoryTabley > 0) {
                mandatoryPropertiesEditorPanel.setBackground(Color.WHITE);
                mandatoryPropertiesEditorPanel.setBorder(BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(Color.orange), "Mandatory Properties",
                        TitledBorder.LEFT,
                        TitledBorder.TOP));

                allPropertiesEditorPanel.add(mandatoryPropertiesEditorPanel, gc1);
                gc1.gridy = 1;
            }

            if (optionalTabley > 0) {
                optionalPropertiesEditorPanel.setBackground(Color.WHITE);
                optionalPropertiesEditorPanel.setBorder(BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(Color.black), "Optional Properties",
                        TitledBorder.LEFT,
                        TitledBorder.TOP));
                allPropertiesEditorPanel.add(optionalPropertiesEditorPanel, gc1);
            }

            scrollableGridbagPanel.add(allPropertiesEditorPanel);
        }

        return allPropertiesEditorPanel;
    }

    private ComponentPropertyEditBox addOverrideCheckBoxToPropertiesEditPanel(JPanel propertiesEditorPanel, GridBagConstraints gc, int tabley) {
        JLabel overrideLabel = new JLabel("Override Existing Code");
        overrideLabel.setToolTipText("Check the box if you wish to rewrite / overwrite the existing code for this beskpoke implementation");
        overrideCheckBox = new JCheckBox();
        overrideCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ie) {
                okButton.setEnabled(ie.getStateChange() == 1);
            }
        });
        okButton.setEnabled(false);
        addLabelAndInputEditor(propertiesEditorPanel, gc, tabley, overrideLabel, overrideCheckBox);
        return null;
    }

    private ComponentPropertyEditBox addNameValueToPropertiesEditPanel(JPanel propertiesEditorPanel, IkasanComponentProperty componentProperty, GridBagConstraints gc, int tabley) {
        ComponentPropertyEditBox componentPropertyEditBox = new ComponentPropertyEditBox(componentProperty);
        addLabelAndInputEditor(propertiesEditorPanel, gc, tabley, componentPropertyEditBox.getLabelField(), componentPropertyEditBox.getInputField());
        return componentPropertyEditBox;
    }

    private void addLabelAndInputEditor(JPanel propertiesEditorPanel, GridBagConstraints gc, int tabley, JLabel label, JComponent inputField) {
        gc.weightx = 0;
        gc.gridx = 0;
        gc.gridy = tabley;
        propertiesEditorPanel.add(label, gc);
        gc.gridx = 1;
        gc.weightx = 1;
        propertiesEditorPanel.add(inputField, gc);
    }


    /**
     * Check to see if any new values have been entered, update the model and return true if that is the case.
     * @return true if the model has been updated with new values.
     */
    private boolean processEditedFlowComponents() {
        boolean modelUpdated = false;
        if (componentPropertyEditBoxList != null) {
            for (final ComponentPropertyEditBox editPair: componentPropertyEditBoxList) {
                final String key = editPair.getLabel();
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
                    }
                }
            }
        }
        return modelUpdated;
    }

    /**
     *
     * @param property of the existing Ikasan Component
     * @param propertyEditBox should never be null when called.
     * @return
     */
    private boolean propertyValueHasChanged(IkasanComponentProperty property, ComponentPropertyEditBox propertyEditBox) {
        return (((property == null || property.getValue() == null) && editBoxHasValue(propertyEditBox)) ||
                (property != null && editBoxHasValue(propertyEditBox) && !property.getValue().equals(propertyEditBox.getValue())));
    }

    /**
     * Determine if the edit box has a valid value that would warrant the source code to be updated
     * @param propertyEditBox to examine.
     * @return true if the editbox has a non-whitespace / real value.
     */
    private boolean editBoxHasValue(ComponentPropertyEditBox propertyEditBox) {
        boolean hasValue = false;
        if (propertyEditBox != null) {
            Object value = propertyEditBox.getValue();
            if (value instanceof String) {
                if (((String) value).length() > 0) {
                    return true;
                }
            } else {
                return (value != null);
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
