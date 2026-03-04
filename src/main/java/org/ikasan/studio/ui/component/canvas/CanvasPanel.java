package org.ikasan.studio.ui.component.canvas;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.actions.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;

@SuppressWarnings("rawtypes")
public class CanvasPanel extends JBPanel {
    public CanvasPanel(Project project) {
        super();
        DesignerCanvas designerCanvas = new DesignerCanvas(project);
        UiContext uiContext = project.getService(UiContext.class);
        uiContext.setDesignerCanvas(designerCanvas);

        @SuppressWarnings("rawtypes")
        JBPanel canvasHeaderButtonPanel = new JBPanel();
        canvasHeaderButtonPanel.setBorder(null);
        JButton h2Button = new JButton("H2 start");
        JButton applicationButton = new JButton("Module start");
        addButtonsToPanel(canvasHeaderButtonPanel, new JButton("Blue"), new LaunchBlueAction(project), "Start the blue console in a browser (user/pass - admin/admin)");
        addButtonsToPanel(canvasHeaderButtonPanel, h2Button, new LaunchH2Action(project, h2Button), "Start the H2 console in a browser");
        addButtonsToPanel(canvasHeaderButtonPanel, applicationButton, new LaunchApplicationAction(project, applicationButton), "Start the Module");
        addButtonsToPanel(canvasHeaderButtonPanel, new JButton("Load"), new ModelLoadAction(project), "Load the in module from disk");
        addButtonsToPanel(canvasHeaderButtonPanel, new JButton("Save"), new ModelRebuildAction(project), "Regenerate the code from the in-memory module definition");
//        addButtonsToPanel(canvasHeaderButtonPanel, new JButton("Save Img"), new SaveAction(project), "Save the module drawing as an image file");
//        addButtonsToPanel(canvasHeaderButtonPanel, new JButton("Debug"), new DebugAction(project), "Dump information to log files");

        JCheckBox gridCheckBox = new JCheckBox("Show Grid");
        gridCheckBox.setSelected(false);
        gridCheckBox.addItemListener(e -> {
            // Function, so don't use instance above
            DesignerCanvas designerCanvasRef = uiContext.getDesignerCanvas();
            designerCanvasRef.setDrawGrid(e.getStateChange() == ItemEvent.SELECTED);
            designerCanvasRef.repaint();
        });
        canvasHeaderButtonPanel.add(gridCheckBox);
        @SuppressWarnings("rawtypes")
        JBPanel canvasHeaderPanel = new JBPanel();
        canvasHeaderPanel.setBorder(BorderFactory.createMatteBorder(0,0,JBUI.scale(1),0, StudioUIUtils.getLineColor()));
        canvasHeaderPanel.add(canvasHeaderButtonPanel);

        // This may be redundant now we have Intellij Messaging
        JTextArea canvasTextArea = new JTextArea();
        uiContext.setCanvasTextArea(canvasTextArea);
        canvasTextArea.setLineWrap(true);
        canvasTextArea.setWrapStyleWord(true);
        add(canvasTextArea, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(canvasHeaderPanel, BorderLayout.NORTH);

        JBScrollPane canvasScrollPane = new JBScrollPane();
        canvasScrollPane.setBorder(JBUI.Borders.empty());
        canvasScrollPane.getViewport().add(designerCanvas);
        canvasScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(canvasScrollPane, BorderLayout.CENTER);
    }

    @SuppressWarnings("rawtypes")
    private void addButtonsToPanel(JBPanel canvasHeaderButtonPanel, JButton newButton, ActionListener al, String tooltip) {
        newButton.addActionListener(al);
        newButton.setToolTipText(tooltip);
        canvasHeaderButtonPanel.add(newButton);
    }
}
