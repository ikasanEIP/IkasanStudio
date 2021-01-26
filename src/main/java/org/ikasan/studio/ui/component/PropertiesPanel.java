package org.ikasan.studio.ui.component;

import org.apache.log4j.Logger;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.Ikasan.IkasanComponent;
import org.ikasan.studio.model.Ikasan.IkasanComponentProperty;
import org.ikasan.studio.model.Ikasan.IkasanComponentPropertyMeta;
import org.ikasan.studio.model.Ikasan.IkasanFlowComponent;
import org.ikasan.studio.ui.component.palette.ComponentPropertyEditBox;

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
    JPanel propertiesBodyPanel ;

    public PropertiesPanel(String projectKey) {
        super();
        this.projectKey = projectKey ;
        JLabel propertiesHeaderLabel =  new JLabel("Properties");

        JPanel propertiesHeaderPanel = new JPanel();
        propertiesHeaderLabel.setBorder(new EmptyBorder(9,0,9,0));
        propertiesHeaderPanel.add(propertiesHeaderLabel);
        propertiesHeaderPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        propertiesBodyPanel = new JPanel(new BorderLayout());
        propertiesBodyPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        propertiesBodyPanel.setBackground(Color.WHITE);

        setLayout(new BorderLayout());
        add(propertiesHeaderPanel, BorderLayout.NORTH);
        add(propertiesBodyPanel, BorderLayout.CENTER);
        Context.setPropertiesPanel(projectKey,this);

//        JTextArea propertieTextArea = new JTextArea(1,1);
//        Context.register("propertieTextArea", propertieTextArea);
    }


//
//    public void updatePropertiesPanel(IkasanFlow selectedFlow) {
//        List<ComponentPropertyEditBox> componentPropertyEditBoxList = new ArrayList<>();
//    }

    /**
     * For the given component, get all the editable properties and add them the to properties edit panel.
     * @param componentPropertyEditBoxList
     * @param selectedComponent
     * @param propertiesEditorPanel
     * @param gc
     * @return
     */
    private List<ComponentPropertyEditBox> populateComponentEdit(List<ComponentPropertyEditBox> componentPropertyEditBoxList, IkasanComponent selectedComponent, JPanel propertiesEditorPanel, GridBagConstraints gc) {
        int tabley = 0;
        if (selectedComponent.getProperty(IkasanComponentPropertyMeta.NAME) != null) {
            componentPropertyEditBoxList.add(addFieldToEdit(propertiesEditorPanel, selectedComponent.getProperty(IkasanComponentPropertyMeta.NAME), gc, tabley++));
        }
        if (selectedComponent.getProperty(IkasanComponentPropertyMeta.DESCRIPTION) != null) {
            componentPropertyEditBoxList.add(addFieldToEdit(propertiesEditorPanel, selectedComponent.getProperty(IkasanComponentPropertyMeta.DESCRIPTION), gc, tabley++));
        }

        if (selectedComponent instanceof IkasanFlowComponent) {
            IkasanFlowComponent selectedFlowComponent = (IkasanFlowComponent) selectedComponent;
            if (selectedFlowComponent.getType().getProperties().size() > 0) {
                for (Map.Entry<String, IkasanComponentPropertyMeta> entry : selectedFlowComponent.getType().getProperties().entrySet()) {
                    String key = entry.getKey();
                    if (!key.equals(IkasanComponentPropertyMeta.NAME) && !key.equals(IkasanComponentPropertyMeta.DESCRIPTION)) {
                        IkasanComponentProperty property = selectedComponent.getProperty(key);
                        componentPropertyEditBoxList.add(addFieldToEdit(propertiesEditorPanel, property, gc, tabley++));
                        tabley++;
                    }
                }
            }
        }
        return componentPropertyEditBoxList;
    }

    private ComponentPropertyEditBox addFieldToEdit(JPanel propertiesEditorPanel, IkasanComponentProperty componentProperty, GridBagConstraints gc, int tabley) {

        ComponentPropertyEditBox componentPropertyEditBox = new ComponentPropertyEditBox(componentProperty);
        gc.gridx = 0;
        gc.gridy = tabley;
        propertiesEditorPanel.add(componentPropertyEditBox.getLabelField(), gc);
        gc.gridx = 1;
        propertiesEditorPanel.add(componentPropertyEditBox.getTextField(), gc);
        return componentPropertyEditBox;
    }


    private boolean processEditedFlowComponents(List<ComponentPropertyEditBox> componentPropertyEditBoxList, IkasanComponent selectedComponent) {
        boolean modelUpdated = false;
        for (ComponentPropertyEditBox editPair: componentPropertyEditBoxList) {
            String key = editPair.getLabel();
            IkasanComponentProperty componentProperty = (IkasanComponentProperty)selectedComponent.getProperties().get(key);
            if (hasChanged(componentProperty.getValue(), editPair.getValue())) {
                modelUpdated = true;
                componentProperty.setValue(editPair.getValue());
            }
        }
        return modelUpdated;
    }

    private boolean processEditedFlow(List<ComponentPropertyEditBox> componentPropertyEditBoxList, IkasanComponent selectedComponent) {
        boolean modelUpdated = false;
        for (ComponentPropertyEditBox editPair: componentPropertyEditBoxList) {
            String key = editPair.getLabel();
            IkasanComponentProperty componentProperty = (IkasanComponentProperty)selectedComponent.getProperties().get(key);
            if (hasChanged(componentProperty.getValue(), editPair.getValue())) {
                modelUpdated = true;
                componentProperty.setValue(editPair.getValue());
            }
        }
        return modelUpdated;
    }

    public void updatePropertiesPanel(IkasanComponent selectedComponent) {
        List<ComponentPropertyEditBox> componentPropertyEditBoxList = new ArrayList<>();

        JPanel propertiesEditorPanel = new JPanel();
        propertiesEditorPanel.setLayout(new GridBagLayout());
        propertiesEditorPanel.setBackground(Color.WHITE);
        propertiesEditorPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(3, 4, 3, 4);

//        if (selectedComponent instanceof IkasanFlowComponent) {
//            populateComponentEdit(componentPropertyEditBoxList, selectedComponent, propertiesEditorPanel, gc);
//        }

        populateComponentEdit(componentPropertyEditBoxList, selectedComponent, propertiesEditorPanel, gc);

        JScrollPane scrollPane = new JScrollPane(propertiesEditorPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        propertiesBodyPanel.removeAll();
        propertiesBodyPanel.add(scrollPane, BorderLayout.NORTH);
        JPanel footerPanel = new JPanel();
        JButton okButton = new JButton("Update Model");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean modelUpdated = false;
//                if (selectedComponent instanceof IkasanFlowComponent) {
//                    modelUpdated = processEditedFlowComponents(componentPropertyEditBoxList, selectedComponent);
//                }
                modelUpdated = processEditedFlowComponents(componentPropertyEditBoxList, selectedComponent);
                if (modelUpdated) {
                    Context.getPipsiIkasanModel(projectKey).generateSourceFromModel();
                    Context.getDesignerCanvas(projectKey).setInitialiseCanvas(true);
                    Context.getDesignerCanvas(projectKey).repaint();
                }
            }
        });

        footerPanel.add(okButton);
        footerPanel.add(new JButton("Cancel"));
        propertiesBodyPanel.add(footerPanel, BorderLayout.SOUTH);

        this.setBackground(Color.WHITE);

//        JSplitPane propertiesAndCanvasSplitPane = Context.getPropertiesAndCanvasPane();
//        propertiesAndCanvasSplitPane.setDividerLocation(((Double)propertiesBodyPanel.getPreferredSize().getWidth()).intValue()+20);
//        propertiesAndCanvasSplitPane.revalidate();
//        propertiesAndCanvasSplitPane.repaint();
        propertiesBodyPanel.revalidate();
        propertiesBodyPanel.repaint();
    }


    private boolean hasChanged(Object o1, Object o2) {
        if ((o1 == null && o2 == null) ||
            (o1 != null && o2 != null && o1.equals(o2))) {
            return false;
        } else {
            return true;
        }
    }
}
