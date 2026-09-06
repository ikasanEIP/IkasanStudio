package org.ikasan.studio.ui.component.canvas;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.actions.*;
import org.ikasan.studio.ui.theme.ThemeAwareColors;
import org.ikasan.studio.intellij.settings.IkasanStudioSettings;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionListener;

@SuppressWarnings("rawtypes")
public class CanvasPanel extends JBPanel implements Disposable {
    private static final Icon H2_ICON = IconLoader.getIcon("/studio/icons/h2.svg", CanvasPanel.class);
    private static final Icon CONSOLE_ICON = IconLoader.getIcon("/studio/icons/console.svg", CanvasPanel.class);
    private static final Icon LOAD_ICON = IconLoader.getIcon("/studio/icons/load.svg", CanvasPanel.class);
    private static final Icon SAVE_ICON = IconLoader.getIcon("/studio/icons/save.svg", CanvasPanel.class);

    JButton h2Button = new JButton(StudioBundle.message("button.H2Start"), H2_ICON);
    JButton runModuleButton = new JButton(AllIcons.Actions.Execute);
    JButton debugModuleButton = new JButton(AllIcons.Actions.StartDebugger);
    JButton stopModuleButton = new JButton(AllIcons.Actions.Suspend);
    // Independent, always-enabled Start/Stop buttons rather than one context-sensitive toggle - see
    // HarnessControlActions javadoc for why (a toggle that infers "already running" can end up hiding the one
    // option - Stop - the developer actually needs, if that inference is wrong).
    JButton startHarnessButton = new JButton(AllIcons.Actions.Execute);
    JButton stopHarnessButton = new JButton(AllIcons.Actions.Suspend);
    private final JBPanel harnessGroupPanel;
    private final Timer harnessRefreshTimer;
    private final DesignerCanvas designerCanvas;
    // These are the less commonly needed controls, gated behind the "Show advanced controls" setting (see
    // IkasanStudioSettings) rather than removed outright, since each is still a legitimate escape hatch:
    // H2 console and Blue console are secondary debugging aids, and Load is only for manually reloading
    // model.json from disk after an external change, which most users never need day to day.
    JButton loadModuleButton = new JButton(StudioBundle.message("label.Load"), LOAD_ICON);
    JButton consoleButton = new JButton(StudioBundle.message("label.Console"), CONSOLE_ICON);
    JTextArea canvasTextArea;
    public CanvasPanel(Project project) {
        super();
        designerCanvas = new DesignerCanvas(project);
        UiContext uiContext = project.getService(UiContext.class);
        uiContext.setDesignerCanvas(designerCanvas);

        @SuppressWarnings("rawtypes")
        JBPanel canvasHeaderButtonPanel = new JBPanel();
        canvasHeaderButtonPanel.setBorder(null);

        runModuleButton.setEnabled(false);
        debugModuleButton.setEnabled(false);
        stopModuleButton.setEnabled(false);
        runModuleButton.setDisabledIcon(IconLoader.getDisabledIcon(runModuleButton.getIcon()));
        debugModuleButton.setDisabledIcon(IconLoader.getDisabledIcon(debugModuleButton.getIcon()));
        stopModuleButton.setDisabledIcon(IconLoader.getDisabledIcon(stopModuleButton.getIcon()));
        runModuleButton.getAccessibleContext().setAccessibleName(StudioBundle.message("button.RunModule"));
        debugModuleButton.getAccessibleContext().setAccessibleName(StudioBundle.message("button.DebugModule"));
        stopModuleButton.getAccessibleContext().setAccessibleName(StudioBundle.message("button.StopModule"));
        addButtonsToPanel(canvasHeaderButtonPanel, h2Button, new LaunchH2Action(project, h2Button), StudioBundle.message("tooltip.StartTheH2ConsoleInABrowser"));

        JBPanel moduleGroupPanel = titledButtonGroup(StudioBundle.message("label.Module"));
        addButtonsToPanel(moduleGroupPanel, runModuleButton, new LaunchApplicationAction(project), StudioBundle.message("tooltip.RunThisModuleUsingTheSelectedRunConfiguration"));
        addButtonsToPanel(moduleGroupPanel, debugModuleButton, new LaunchApplicationAction(project, true), StudioBundle.message("tooltip.DebugThisModuleUsingTheSelectedRunConfiguration"));
        addButtonsToPanel(moduleGroupPanel, stopModuleButton, new StopApplicationAction(project), StudioBundle.message("tooltip.StopModule"));
        canvasHeaderButtonPanel.add(moduleGroupPanel);

        HarnessControlActions harnessActions = new HarnessControlActions(project);
        harnessGroupPanel = titledButtonGroup(StudioBundle.message("label.Harness"));
        addButtonsToPanel(harnessGroupPanel, startHarnessButton, harnessActions.startAction(), StudioBundle.message("tooltip.StartHarnesses"));
        addButtonsToPanel(harnessGroupPanel, stopHarnessButton, harnessActions.stopAction(), StudioBundle.message("tooltip.StopHarnesses"));
        startHarnessButton.getAccessibleContext().setAccessibleName(StudioBundle.message("accessible.StartHarnesses"));
        stopHarnessButton.getAccessibleContext().setAccessibleName(StudioBundle.message("accessible.StopHarnesses"));
        harnessGroupPanel.setVisible(harnessActions.isAvailable());
        canvasHeaderButtonPanel.add(harnessGroupPanel);

        addButtonsToPanel(canvasHeaderButtonPanel, consoleButton, new LaunchBlueAction(project), StudioBundle.message("tooltip.AfterModuleStartupCompletesOpenBlueConsole"));
        addButtonsToPanel(canvasHeaderButtonPanel, loadModuleButton, new ModelLoadAction(project), StudioBundle.message("tooltip.LoadTheModuleFromDisk"));
        refreshAdvancedControlsVisibility();
        addButtonsToPanel(canvasHeaderButtonPanel, new JButton(StudioBundle.message("button.RegenerateCode"), SAVE_ICON), new ModelRebuildAction(project), StudioBundle.message("tooltip.RegenerateTheCodeFromTheInMemoryModuleDefinition"));
        addButtonsToPanel(canvasHeaderButtonPanel, new JButton("Migrate…"),
                event -> org.ikasan.studio.intellij.migration.MigrationController.open(project, false),
                "Preview migration to another Ikasan version and save a recovery snapshot");
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

        harnessRefreshTimer = new Timer(1000, event -> refreshHarnessGroupVisibility(harnessActions));
        harnessRefreshTimer.setRepeats(true);
        harnessRefreshTimer.start();

        ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(
                ApplicationActivationListener.TOPIC,
                new ApplicationActivationListener() {
                    // Deliberately not @NotNull-annotated: this project avoids @NotNull (see CLAUDE.md)
                    // because the IntelliJ Gradle plugin instruments it with a runtime assertion that
                    // would surface as an uncaught plugin exception rather than failing gracefully.
                    @SuppressWarnings("NullableProblems")
                    @Override
                    public void applicationActivated(IdeFrame ideFrame) {
                        designerCanvas.notifyApplicationReactivated();
                    }
                });
    }

    @SuppressWarnings("rawtypes")
    private void addButtonsToPanel(JBPanel canvasHeaderButtonPanel, JButton newButton, ActionListener al, String tooltip) {
        newButton.addActionListener(al);
        newButton.setToolTipText(tooltip);
        canvasHeaderButtonPanel.add(newButton);
    }

    /**
     * A small titled group box for related toolbar buttons (e.g. Module's Run/Debug/Stop, Harness's Start/Stop) -
     * reuses the same titled-line-border styling as the property panels' subsections
     * (see ComponentPropertiesPanel#setSubPanel) rather than introducing a new visual convention.
     */
    @SuppressWarnings("rawtypes")
    private JBPanel titledButtonGroup(String title) {
        JBPanel group = new JBPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        group.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeAwareColors.getBorderColor()),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP));
        return group;
    }

    /**
     * The harness group's own Start/Stop buttons are deliberately always enabled (see HarnessControlActions) -
     * only the group's overall visibility still needs polling, since whether the model even references a test
     * harness at all can change as the user edits the canvas.
     */
    private void refreshHarnessGroupVisibility(HarnessControlActions harnessActions) {
        boolean available = harnessActions.isAvailable();
        if (harnessGroupPanel.isVisible() != available) {
            harnessGroupPanel.setVisible(available);
            harnessGroupPanel.getParent().revalidate();
            harnessGroupPanel.getParent().repaint();
        }
    }

    @Override
    public void dispose() {
        harnessRefreshTimer.stop();
        designerCanvas.disposeCanvas();
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

    /**
     * The module can only be debugged once the canvas contains at least one complete, valid flow.
     * @param flag, if true will enable the Debug module button, otherwise disable it.
     */
    public void setDebugModuleEnabled(boolean flag) {
        debugModuleButton.setEnabled(flag);
    }

    public void setStopModuleEnabled(boolean flag) {
        stopModuleButton.setEnabled(flag);
    }

    /**
     * Re-applies the "Show advanced controls" setting to whichever controls it currently gates (H2 start,
     * Console and Load). Called once at construction and again from IkasanStudioSettingsConfigurable when
     * the user changes the setting on an already-open canvas.
     */
    public void refreshAdvancedControlsVisibility() {
        boolean showAdvancedControls = IkasanStudioSettings.isShowAdvancedControlsEnabled();
        h2Button.setVisible(showAdvancedControls);
        consoleButton.setVisible(showAdvancedControls);
        loadModuleButton.setVisible(showAdvancedControls);
    }
}
