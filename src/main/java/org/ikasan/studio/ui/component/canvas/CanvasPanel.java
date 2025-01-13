package org.ikasan.studio.ui.component.canvas;

import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.actions.*;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;

public class CanvasPanel extends JPanel {
    public CanvasPanel(String projectKey) {
        super();
        DesignerCanvas designerCanvas = new DesignerCanvas(projectKey);
        UiContext.setDesignerCanvas(projectKey, designerCanvas);

        JPanel canvasHeaderButtonPanel = new JPanel();
        canvasHeaderButtonPanel.setBorder(null);
        JButton h2Button = new JButton("H2 start");
        JButton applicationButton = new JButton("Module start");
        addButtonsToPanel(canvasHeaderButtonPanel, new JButton("Blue"), new LaunchBlueAction(projectKey), "Start the blue console in a browser (user/pass - admin/admin)");
        addButtonsToPanel(canvasHeaderButtonPanel, h2Button, new LaunchH2Action(projectKey, h2Button), "Start the H2 console in a browser");
        addButtonsToPanel(canvasHeaderButtonPanel, applicationButton, new LaunchApplicationAction(projectKey, applicationButton), "Start the Module");
        addButtonsToPanel(canvasHeaderButtonPanel, new JButton("Read Module"), new ModelRefreshAction(projectKey), "Refresh the in memory module definition from saved code");
        addButtonsToPanel(canvasHeaderButtonPanel, new JButton("Save"), new ModelRebuildAction(projectKey), "Regenrate the code from the in-memory module definition");
//        addButtonsToPanel(canvasHeaderButtonPanel, new JButton("Save Img"), new SaveAction(projectKey), "Save the module drawing as an image file");
//        addButtonsToPanel(canvasHeaderButtonPanel, new JButton("Debug"), new DebugAction(projectKey), "Dump information to log files");

        JCheckBox gridCheckBox = new JCheckBox("Show Grid");
        gridCheckBox.setSelected(false);
        gridCheckBox.addItemListener(e -> {
            // Function, so don't use instance above
            DesignerCanvas designerCanvasRef = UiContext.getDesignerCanvas(projectKey);
            designerCanvasRef.setDrawGrid(e.getStateChange() == ItemEvent.SELECTED);
            designerCanvasRef.repaint();
        });
        canvasHeaderButtonPanel.add(gridCheckBox);
        JPanel canvasHeaderPanel = new JPanel();
        canvasHeaderPanel.setBorder(new MatteBorder(0,0,1,0, StudioUIUtils.getLineColor()));
        canvasHeaderPanel.add(canvasHeaderButtonPanel);

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
}
