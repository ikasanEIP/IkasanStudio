package org.ikasan.studio.ui.component;

import org.ikasan.studio.Context;
import org.ikasan.studio.model.Ikasan.IkasanFlowElement;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

public class PropertiesPanel extends JPanel {
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

    public void updatePropertiesPanel(IkasanFlowElement selectedComponent) {
        JPanel propertiesEditorPanel = new JPanel();
        propertiesEditorPanel.setLayout(new GridBagLayout());
        propertiesEditorPanel.setBackground(Color.WHITE);
        propertiesEditorPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(3, 4, 3, 4);
        // Update using properties panel
        int tabley = 0;
        for (Map.Entry<String, Object> entry : selectedComponent.getProperties().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            JLabel keyLabel = new JLabel(key);
            JTextField valueEditor = new JTextField(value.toString());
            gc.gridx = 0;
            gc.gridy = tabley;
            propertiesEditorPanel.add(keyLabel, gc);
            gc.gridx = 1;
            propertiesEditorPanel.add(valueEditor, gc);
            tabley++;
        }
        JScrollPane scrollPane = new JScrollPane(propertiesEditorPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        propertiesBodyPanel.removeAll();
        propertiesBodyPanel.add(scrollPane, BorderLayout.NORTH);

        this.setBackground(Color.WHITE);

//        JSplitPane propertiesAndCanvasSplitPane = Context.getPropertiesAndCanvasPane();
//        propertiesAndCanvasSplitPane.setDividerLocation(((Double)propertiesBodyPanel.getPreferredSize().getWidth()).intValue()+20);
//        propertiesAndCanvasSplitPane.revalidate();
//        propertiesAndCanvasSplitPane.repaint();
        propertiesBodyPanel.revalidate();
        propertiesBodyPanel.repaint();
    }
}
