package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.Context;
import org.ikasan.studio.model.Ikasan.*;
import org.ikasan.studio.ui.component.ScrollableGridbagPanel;
import org.ikasan.studio.ui.model.IkasanFlowUIComponent;
import org.ikasan.studio.ui.model.IkasanFlowUIComponentFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
    private JLabel propertiesHeaderLabel = new JLabel(PROPERTIES_TAG);
    private JCheckBox overrideCheckBox ;
    JButton okButton = new JButton("Update Code");
    private ScrollableGridbagPanel scrollableGridbagPanel;

    public PropertiesPanel(String projectKey) {
        super();
        this.projectKey = projectKey ;

        JPanel propertiesHeaderPanel = new JPanel();
        propertiesHeaderLabel.setBorder(new EmptyBorder(12,0,12,0));
        propertiesHeaderPanel.add(propertiesHeaderLabel);
        propertiesHeaderPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

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

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean modelUpdated = false;
                modelUpdated = processEditedFlowComponents();
                if (modelUpdated) {
                    Context.getPipsiIkasanModel(projectKey).generateSourceFromModel();
                    Context.getDesignerCanvas(projectKey).setInitialiseAllDimensions(true);
                    Context.getDesignerCanvas(projectKey).repaint();
                }
            }
        });

        JPanel footerPanel = new JPanel();
        footerPanel.add(okButton);
        footerPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        add(propertiesHeaderPanel, BorderLayout.NORTH);
        add(propertiesBodyPanel, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
        Context.setPropertiesPanel(projectKey,this);
    }

    private void updatePropertiesPanelTitle() {
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
        propertiesHeaderLabel.setText(propertyType);
    }

    public void updatePropertiesPanel(IkasanComponent selectedComponent) {
        this.selectedComponent = selectedComponent;
        updatePropertiesPanelTitle();
        populatePropertiesEditorPanel();
        scrollableGridbagPanel.revalidate();
        scrollableGridbagPanel.repaint();
    }

    /**
     * For the given component, get all the editable properties and add them the to properties edit panel.
     * @return
     */
    private JPanel populatePropertiesEditorPanel() {

        JPanel propertiesEditorPanel = new JPanel(new GridBagLayout());
        propertiesEditorPanel.setBackground(Color.WHITE);
        propertiesEditorPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

        if (selectedComponent != null) {
            scrollableGridbagPanel.removeAll();
            componentPropertyEditBoxList = new ArrayList<>();

            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.insets = new Insets(3, 4, 3, 4);

            int tabley = 0;
            if (selectedComponent.getProperty(IkasanComponentPropertyMeta.NAME) != null) {
                componentPropertyEditBoxList.add(addNameValueToPropertiesEditPanel(propertiesEditorPanel, selectedComponent.getProperty(IkasanComponentPropertyMeta.NAME), gc, tabley++));
            }
            if (selectedComponent.getProperty(IkasanComponentPropertyMeta.DESCRIPTION) != null) {
                componentPropertyEditBoxList.add(addNameValueToPropertiesEditPanel(propertiesEditorPanel, selectedComponent.getProperty(IkasanComponentPropertyMeta.DESCRIPTION), gc, tabley++));
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
                            componentPropertyEditBoxList.add(addNameValueToPropertiesEditPanel(propertiesEditorPanel, property, gc, tabley++));
                            tabley++;
                        }
                    }
                }
                if (selectedFlowComponent.getType().isBespokeClass()) {
                    addOverrideCheckBoxToPropertiesEditPanel(propertiesEditorPanel, gc, tabley++);
                }
            }

            scrollableGridbagPanel.add(propertiesEditorPanel);
        }

        return propertiesEditorPanel;
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
//        gc.weightx = 0;
//        gc.gridx = 0;
//        gc.gridy = tabley;
//        propertiesEditorPanel.add(componentPropertyEditBox.getLabelField(), gc);
//        gc.gridx = 1;
//        gc.weightx = 1;
//        propertiesEditorPanel.add(componentPropertyEditBox.getTextField(), gc);
        addLabelAndInputEditor(propertiesEditorPanel, gc, tabley, componentPropertyEditBox.getLabelField(), componentPropertyEditBox.getTextField());
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
}
