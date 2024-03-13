package org.ikasan.studio.ui.component.canvas;

import com.intellij.ui.JBColor;
import org.ikasan.studio.Context;
import org.ikasan.studio.actions.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;

public class CanvasPanel extends JPanel {
    public CanvasPanel(String projectKey) {
        super();
        JTextArea canvasTextArea = new JTextArea();

        DesignerCanvas canvasPanel = new DesignerCanvas(projectKey);
        Context.setDesignerCanvas(projectKey, canvasPanel);

        JPanel canvasHeaderButtonPanel = new JPanel();

        addButtonsToPanel(canvasHeaderButtonPanel, "Blue", new LaunchDashboardAction(projectKey));
        addButtonsToPanel(canvasHeaderButtonPanel, "H2", new LaunchH2Action(projectKey));
        addButtonsToPanel(canvasHeaderButtonPanel, "Refresh", new ModelRefreshAction(projectKey));
        addButtonsToPanel(canvasHeaderButtonPanel, "Rebuild", new ModelRebuildAction(projectKey));
        addButtonsToPanel(canvasHeaderButtonPanel, "Save", new SaveAction(projectKey));
        addButtonsToPanel(canvasHeaderButtonPanel, "Debug", new DebugAction(projectKey));

        JCheckBox gridCheckBox = new JCheckBox("Show Grid");
        gridCheckBox.setSelected(false);
        gridCheckBox.addItemListener(e -> {
            DesignerCanvas designerCanvas = Context.getDesignerCanvas(projectKey);
            designerCanvas.setDrawGrid(e.getStateChange() == ItemEvent.SELECTED);
            designerCanvas.repaint();
        });
        canvasHeaderButtonPanel.add(gridCheckBox);

        JPanel canvasHeaderTitlePanel = new JPanel();
        JPanel canvasHeaderPanel = new JPanel();
        canvasHeaderPanel.add(canvasHeaderTitlePanel);
        canvasHeaderPanel.add(canvasHeaderButtonPanel);
        canvasHeaderPanel.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
        Context.setCanvasTextArea(projectKey, canvasTextArea);
        canvasTextArea.setLineWrap(true);

        setLayout(new BorderLayout());
        add(canvasHeaderPanel, BorderLayout.NORTH);
        add(canvasPanel, BorderLayout.CENTER);
    }

    private void addButtonsToPanel(JPanel canvasHeaderButtonPanel, String title, ActionListener al) {
        JButton newButton = new JButton(title);
        newButton.addActionListener(al);
        canvasHeaderButtonPanel.add(newButton);
    }
}
