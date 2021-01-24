package org.ikasan.studio.ui.component;

import com.intellij.openapi.wm.ToolWindow;
import org.ikasan.studio.Context;
import org.ikasan.studio.actions.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class CanvasPanel extends JPanel {
    private static final int INITIAL_DIVIDER_LOCATION = 2000;  // Workaround for nested component heights not being known at time of creation.
    private String projectKey ;

    public CanvasPanel(String projectKey, ToolWindow toolWindow) {
        super();
        this.projectKey = projectKey;
        JTextArea canvasTextArea = new JTextArea();

        DesignerCanvas canvasPanel = new DesignerCanvas(projectKey);
        Context.setDesignerCanvas(projectKey, canvasPanel);

        JPanel canvasHeaderButtonPanel = new JPanel();

        addButtonsToPanel(canvasHeaderButtonPanel, "Launch", new LaunchDashboardAction(projectKey));
        addButtonsToPanel(canvasHeaderButtonPanel, "Refresh", new ModelRefreshAction(projectKey));
        addButtonsToPanel(canvasHeaderButtonPanel, "Rebuild", new ModelRebuildAction(projectKey));
        addButtonsToPanel(canvasHeaderButtonPanel, "Save", new SaveAction(projectKey));
        addButtonsToPanel(canvasHeaderButtonPanel, "Debug", new DebugAction(projectKey));

        JCheckBox gridCheckBox = new JCheckBox("Show Grid");
        gridCheckBox.setSelected(false);
        gridCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                DesignerCanvas designerCanvas = Context.getDesignerCanvas(projectKey);
                if (e.getStateChange()==1) {
                    designerCanvas.setDrawGrid(true);
                } else {
                    designerCanvas.setDrawGrid(false);
                }
                designerCanvas.repaint();
            }
        });
        canvasHeaderButtonPanel.add(gridCheckBox);

        JPanel canvasHeaderTitlePanel = new JPanel();;
        canvasHeaderTitlePanel.add(new JLabel("Canvas"));
        JPanel canvasHeaderPanel = new JPanel();
        canvasHeaderPanel.add(canvasHeaderTitlePanel);
        canvasHeaderPanel.add(canvasHeaderButtonPanel);
        canvasHeaderPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        Context.setCanvasTextArea(projectKey, canvasTextArea);
        canvasTextArea.setLineWrap(true);
        JPanel canvasBodyPanel = new JPanel(new BorderLayout());
        canvasBodyPanel.add(new JScrollPane(canvasTextArea), BorderLayout.SOUTH);

        canvasBodyPanel.add(canvasPanel, BorderLayout.CENTER);
        canvasBodyPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        setLayout(new BorderLayout());
        add(canvasHeaderPanel, BorderLayout.NORTH);
        add(canvasBodyPanel, BorderLayout.CENTER);
    }

    private void addButtonsToPanel(JPanel canvasHeaderButtonPanel, String title, ActionListener al) {
        JButton newButton = new JButton(title);
        newButton.addActionListener(al);
        canvasHeaderButtonPanel.add(newButton);
    }
}
