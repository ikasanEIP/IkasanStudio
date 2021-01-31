package org.ikasan.studio.ui.component.properties;

import org.apache.log4j.Logger;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.Ikasan.IkasanComponent;
import org.ikasan.studio.model.Ikasan.IkasanComponentProperty;
import org.ikasan.studio.model.Ikasan.IkasanComponentPropertyMeta;
import org.ikasan.studio.model.Ikasan.IkasanFlowComponent;
import org.ikasan.studio.ui.component.ScrollableGridbagPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PropertiesPanel extends JPanel {
    private static final Logger log = Logger.getLogger(PropertiesPanel.class);
    String projectKey;
    IkasanComponent selectedComponent;
    List<ComponentPropertyEditBox> componentPropertyEditBoxList;
    ScrollableGridbagPanel scrollableGridbagPanel;

    public PropertiesPanel(String projectKey) {
        super();
        this.projectKey = projectKey ;

        JLabel propertiesHeaderLabel =  new JLabel("Properties");
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

        JButton okButton = new JButton("Update Code");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean modelUpdated = false;
                modelUpdated = processEditedFlowComponents();
                if (modelUpdated) {
                    Context.getPipsiIkasanModel(projectKey).generateSourceFromModel();
                    Context.getDesignerCanvas(projectKey).setInitialiseCanvas(true);
                    Context.getDesignerCanvas(projectKey).repaint();
                }
            }
        });

        JPanel footerPanel = new JPanel();
        footerPanel.add(okButton);
        footerPanel.add(new JButton("Cancel"));
        footerPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        add(propertiesHeaderPanel, BorderLayout.NORTH);
        add(propertiesBodyPanel, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
        Context.setPropertiesPanel(projectKey,this);
    }

    public void updatePropertiesPanel(IkasanComponent selectedComponent) {
        this.selectedComponent = selectedComponent;
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
            }
            scrollableGridbagPanel.add(propertiesEditorPanel);
        }

        return propertiesEditorPanel;
    }

    private ComponentPropertyEditBox addNameValueToPropertiesEditPanel(JPanel propertiesEditorPanel, IkasanComponentProperty componentProperty, GridBagConstraints gc, int tabley) {
        ComponentPropertyEditBox componentPropertyEditBox = new ComponentPropertyEditBox(componentProperty);
        gc.weightx = 0;
        gc.gridx = 0;
        gc.gridy = tabley;
        propertiesEditorPanel.add(componentPropertyEditBox.getLabelField(), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        propertiesEditorPanel.add(componentPropertyEditBox.getTextField(), gc);
        return componentPropertyEditBox;
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
                IkasanComponentProperty componentProperty = (IkasanComponentProperty)selectedComponent.getProperties().get(key);
                if (propertyValueHasChanged(componentProperty, editPair)) {
                    modelUpdated = true;
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
     * @param property on the existing Ikasan Component
     * @param propertyEditBox should never be null when called.
     * @return
     */
    private boolean propertyValueHasChanged(IkasanComponentProperty property, ComponentPropertyEditBox propertyEditBox) {
        if ((property == null && editBoxHasValue(propertyEditBox)) ||
                (property != null && editBoxHasValue(propertyEditBox) && !property.getValue().equals(propertyEditBox.getValue())))   {
            return true;
        }
        else {
            return false;
        }
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
                if (value != null && ((String) value).length() > 0) {
                    return true;
                }
            } else {
                return (value != null);
            }
        }
        return hasValue;
    }
}
