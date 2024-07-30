package org.ikasan.studio.ui.component.canvas;

import com.intellij.ui.JBColor;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.actions.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;

public class CanvasPanel extends JPanel {
    public CanvasPanel(String projectKey) {
        super();
        DesignerCanvas designerCanvas = new DesignerCanvas(projectKey);
        UiContext.setDesignerCanvas(projectKey, designerCanvas);

        JPanel canvasHeaderButtonPanel = new JPanel();

        JButton h2Button = new JButton("H2 start");
        addButtonsToPanel(canvasHeaderButtonPanel, new JButton("Blue"), new LaunchBlueAction(projectKey), "Start the blue consolein a browser");
        addButtonsToPanel(canvasHeaderButtonPanel, h2Button, new LaunchH2Action(projectKey, h2Button), "Start the H2 console in a browser");
        addButtonsToPanel(canvasHeaderButtonPanel, new JButton("Refresh"), new ModelRefreshAction(projectKey), "Refresh the in memory module definition from the JSON");
        addButtonsToPanel(canvasHeaderButtonPanel, new JButton("Rebuild"), new ModelRebuildAction(projectKey), "Rebuild the code from the in memory module definition");
        addButtonsToPanel(canvasHeaderButtonPanel, new JButton("Save"), new SaveAction(projectKey), "Save the module drawing as an image file");
        addButtonsToPanel(canvasHeaderButtonPanel, new JButton("Debug"), new DebugAction(projectKey), "Dump information to log files");

        JCheckBox gridCheckBox = new JCheckBox("Show Grid");
        gridCheckBox.setSelected(false);
        gridCheckBox.addItemListener(e -> {
            // Function, so don't use instance above
            DesignerCanvas designerCanvasRef = UiContext.getDesignerCanvas(projectKey);
            designerCanvasRef.setDrawGrid(e.getStateChange() == ItemEvent.SELECTED);
            designerCanvasRef.repaint();
        });
        canvasHeaderButtonPanel.add(gridCheckBox);

        JPanel canvasHeaderTitlePanel = new JPanel();
        JPanel canvasHeaderPanel = new JPanel();
        canvasHeaderPanel.add(canvasHeaderTitlePanel);
        canvasHeaderPanel.add(canvasHeaderButtonPanel);
        canvasHeaderPanel.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));

        // This may be redundant now we have Intellij Messaging
        JTextArea canvasTextArea = new JTextArea();
        UiContext.setCanvasTextArea(projectKey, canvasTextArea);
        canvasTextArea.setLineWrap(true);
        canvasTextArea.setWrapStyleWord(true);
        add(canvasTextArea, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(canvasHeaderPanel, BorderLayout.NORTH);

        JScrollPane canvasScrollPane = new JScrollPane();
        canvasScrollPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        canvasScrollPane.getViewport().add(designerCanvas);
        canvasScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(canvasScrollPane, BorderLayout.CENTER);
    }

    private void addButtonsToPanel(JPanel canvasHeaderButtonPanel, JButton newButton, ActionListener al, String tooltip) {
        newButton.addActionListener(al);
        newButton.setToolTipText(tooltip);
        canvasHeaderButtonPanel.add(newButton);
    }

//    private void addButtonsToPanel(JPanel canvasHeaderButtonPanel, String title, ActionListener al, String tooltip) {
//        JButton newButton = new JButton(title);
//        newButton.addActionListener(al);
//        newButton.setToolTipText(tooltip);
//        canvasHeaderButtonPanel.add(newButton);
//    }
}
