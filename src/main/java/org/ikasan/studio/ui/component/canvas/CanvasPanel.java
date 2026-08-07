package org.ikasan.studio.ui.component.canvas;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.actions.*;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionListener;

@SuppressWarnings("rawtypes")
public class CanvasPanel extends JBPanel implements Disposable {
    JButton h2Button = new JButton(StudioBundle.message("button.H2Start"));
    JButton runModuleButton = new JButton(StudioBundle.message("button.RunModule"));
    JTextArea canvasTextArea;
    public CanvasPanel(Project project) {
        super();
        DesignerCanvas designerCanvas = new DesignerCanvas(project);
        UiContext uiContext = project.getService(UiContext.class);
        uiContext.setDesignerCanvas(designerCanvas);

        @SuppressWarnings("rawtypes")
        JBPanel canvasHeaderButtonPanel = new JBPanel();
        canvasHeaderButtonPanel.setBorder(null);

        runModuleButton.setEnabled(false);
        addButtonsToPanel(canvasHeaderButtonPanel, h2Button, new LaunchH2Action(project, h2Button), StudioBundle.message("tooltip.StartTheH2ConsoleInABrowser"));
        addButtonsToPanel(canvasHeaderButtonPanel, runModuleButton, new LaunchApplicationAction(project), StudioBundle.message("tooltip.RunThisModuleUsingTheSelectedRunConfiguration"));
        addButtonsToPanel(canvasHeaderButtonPanel, new JButton(StudioBundle.message("label.Console")), new LaunchBlueAction(project), StudioBundle.message("tooltip.AfterModuleStartupCompletesOpenBlueConsole"));
        addButtonsToPanel(canvasHeaderButtonPanel, new JButton(StudioBundle.message("label.Load")), new ModelLoadAction(project), StudioBundle.message("tooltip.LoadTheModuleFromDisk"));
        addButtonsToPanel(canvasHeaderButtonPanel, new JButton(StudioBundle.message("button.Save")), new ModelRebuildAction(project), StudioBundle.message("tooltip.RegenerateTheCodeFromTheInMemoryModuleDefinition"));
//        addButtonsToPanel(canvasHeaderButtonPanel, new JButton("Save Img"), new SaveAction(project), "Save the module drawing as an image file");
//        addButtonsToPanel(canvasHeaderButtonPanel, new JButton("Debug"), new DebugAction(project), "Dump information to log files");

        // This may be redundant now we have Intellij Messaging
        canvasTextArea = new JTextArea();
        uiContext.setCanvasTextArea(canvasTextArea);
        canvasTextArea.setLineWrap(true);
        canvasTextArea.setWrapStyleWord(true);
        add(canvasTextArea, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(canvasHeaderButtonPanel, BorderLayout.NORTH);

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

    @Override
    public void dispose() {
        if (canvasTextArea.getCaret() instanceof DefaultCaret caret) {
            caret.setBlinkRate(0);
        }
    }

    /**
     * There are conditions where starting H2 is not appropriate
     * @param flag, if true will enable the button, otherwise disable it.
     */
    public void disableH2Button(boolean flag) {
        h2Button.setEnabled(!flag);
    }

    /**
     * The module can only be run once the canvas contains at least one complete, valid flow.
     * @param flag, if true will enable the Run module button, otherwise disable it.
     */
    public void setRunModuleEnabled(boolean flag) {
        runModuleButton.setEnabled(flag);
    }
}
