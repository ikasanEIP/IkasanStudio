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
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;

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
    // The icon the Run/Debug buttons swap to once that mode is already running - see setRunModuleState/
    // setDebugModuleState, which replicate IntelliJ's own toolbar "Restart" affordance here. Deliberately a
    // custom-painted icon rather than AllIcons.Actions.Restart: that platform icon bakes in a grey "stop"
    // square alongside its green arrow (see its own SVG source), which reads as mostly disabled-grey at
    // toolbar-button size. This project already hit and fixed the same class of problem for the canvas's flow
    // transport-control buttons - see FlowTransportAction's fixed-colour comment: a clickable control needs to
    // read as unambiguously coloured. RestartIcon paints itself straight from the button's own enabled state,
    // so it never depends on IconLoader's disabled-icon derivation of a baked two-tone icon.
    private static final Icon RESTART_ICON = new RestartIcon();

    /** See the RESTART_ICON field comment for why this exists instead of AllIcons.Actions.Restart. */
    private static final class RestartIcon implements Icon {
        private static final int SIZE = 16;

        @Override
        public void paintIcon(Component component, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.translate(x, y);
                g2d.setColor(component.isEnabled() ? ThemeAwareColors.getSuccessColor() : ThemeAwareColors.getDisabledTextColor());
                g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
                double cx = 8, cy = 8.5, radius = 5.2, startDeg = -50, extentDeg = 300;
                g2d.draw(new Arc2D.Double(cx - radius, cy - radius, radius * 2, radius * 2, startDeg, extentDeg, Arc2D.OPEN));

                // Arrowhead at the trailing end of the arc, pointing in the arc's direction of travel.
                double endRad = Math.toRadians(startDeg + extentDeg);
                double tipX = cx + radius * Math.cos(endRad);
                double tipY = cy - radius * Math.sin(endRad);
                double travelRad = endRad - Math.PI / 2;
                double back = 3.6, half = 2.4;
                double baseX = tipX - back * Math.cos(travelRad);
                double baseY = tipY + back * Math.sin(travelRad);
                double normal = travelRad + Math.PI / 2;
                Path2D.Double arrow = new Path2D.Double();
                arrow.moveTo(tipX, tipY);
                arrow.lineTo(baseX + half * Math.cos(normal), baseY - half * Math.sin(normal));
                arrow.lineTo(baseX - half * Math.cos(normal), baseY + half * Math.sin(normal));
                arrow.closePath();
                g2d.fill(arrow);
            } finally {
                g2d.dispose();
            }
        }

        @Override public int getIconWidth() { return SIZE; }
        @Override public int getIconHeight() { return SIZE; }
    }
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

        setRunModuleState(false, false);
        setDebugModuleState(false, false);
        stopModuleButton.setEnabled(false);
        stopModuleButton.setDisabledIcon(IconLoader.getDisabledIcon(stopModuleButton.getIcon()));
        runModuleButton.getAccessibleContext().setAccessibleName(StudioBundle.message("button.RunModule"));
        debugModuleButton.getAccessibleContext().setAccessibleName(StudioBundle.message("button.DebugModule"));
        stopModuleButton.getAccessibleContext().setAccessibleName(StudioBundle.message("button.StopModule"));
        addButtonsToPanel(canvasHeaderButtonPanel, h2Button, new LaunchH2Action(project, h2Button), StudioBundle.message("tooltip.StartTheH2ConsoleInABrowser"));

        // Harness is placed left of Module because a harness (e.g. the test FTP server) needs to be started
        // before the module that depends on it, matching the order the user actually has to click them in.
        HarnessControlActions harnessActions = new HarnessControlActions(project);
        harnessGroupPanel = titledButtonGroup(StudioBundle.message("label.Harness"));
        addButtonsToPanel(harnessGroupPanel, startHarnessButton, harnessActions.startAction(), StudioBundle.message("tooltip.StartHarnesses"));
        addButtonsToPanel(harnessGroupPanel, stopHarnessButton, harnessActions.stopAction(), StudioBundle.message("tooltip.StopHarnesses"));
        startHarnessButton.getAccessibleContext().setAccessibleName(StudioBundle.message("accessible.StartHarnesses"));
        stopHarnessButton.getAccessibleContext().setAccessibleName(StudioBundle.message("accessible.StopHarnesses"));
        harnessGroupPanel.setVisible(harnessActions.isAvailable());
        canvasHeaderButtonPanel.add(harnessGroupPanel);

        JBPanel moduleGroupPanel = titledButtonGroup(StudioBundle.message("label.Module"));
        addButtonsToPanel(moduleGroupPanel, runModuleButton, new LaunchApplicationAction(project), StudioBundle.message("tooltip.RunThisModuleUsingTheSelectedRunConfiguration"));
        addButtonsToPanel(moduleGroupPanel, debugModuleButton, new LaunchApplicationAction(project, true), StudioBundle.message("tooltip.DebugThisModuleUsingTheSelectedRunConfiguration"));
        addButtonsToPanel(moduleGroupPanel, stopModuleButton, new StopApplicationAction(project), StudioBundle.message("tooltip.StopModule"));
        canvasHeaderButtonPanel.add(moduleGroupPanel);

        addButtonsToPanel(canvasHeaderButtonPanel, consoleButton, new LaunchBlueAction(project), StudioBundle.message("tooltip.AfterModuleStartupCompletesOpenBlueConsole"));
        addButtonsToPanel(canvasHeaderButtonPanel, loadModuleButton, new ModelLoadAction(project), StudioBundle.message("tooltip.LoadTheModuleFromDisk"));
        addButtonsToPanel(canvasHeaderButtonPanel, new JButton(StudioBundle.message("button.ImportModelJson")),
                event -> org.ikasan.studio.intellij.project.ModelImporter.openImportDialog(project),
                StudioBundle.message("tooltip.ImportModelJson"));
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
     * The Run module button is enabled either when the canvas has a complete, valid flow and nothing is
     * currently running, or when a plain (non-debug) run is the one currently active - in which case it swaps
     * to the "Restart" icon/tooltip, mirroring IntelliJ's own Run toolbar button once its configuration is
     * already running (clicking it then stops and reruns - see LaunchApplicationAction).
     * @param enabled whether the button should be enabled at all
     * @param running true if a plain run is the mode currently active, false for the normal "not yet started" look
     */
    public void setRunModuleState(boolean enabled, boolean running) {
        runModuleButton.setEnabled(enabled);
        if (running) {
            // RESTART_ICON paints itself from the button's own isEnabled() at paint time (see its class
            // comment), so the same instance covers both the icon and disabledIcon slots correctly.
            runModuleButton.setIcon(RESTART_ICON);
            runModuleButton.setDisabledIcon(RESTART_ICON);
        } else {
            runModuleButton.setIcon(AllIcons.Actions.Execute);
            runModuleButton.setDisabledIcon(IconLoader.getDisabledIcon(AllIcons.Actions.Execute));
        }
        runModuleButton.setToolTipText(running
                ? StudioBundle.message("tooltip.RestartThisModuleUsingTheSelectedRunConfiguration")
                : StudioBundle.message("tooltip.RunThisModuleUsingTheSelectedRunConfiguration"));
    }

    /**
     * The Debug module button's counterpart to {@link #setRunModuleState} - swaps to "Restart" once a debug
     * session is the mode currently active.
     * @param enabled whether the button should be enabled at all
     * @param running true if a debug session is the mode currently active, false for the normal "not yet started" look
     */
    public void setDebugModuleState(boolean enabled, boolean running) {
        debugModuleButton.setEnabled(enabled);
        if (running) {
            debugModuleButton.setIcon(RESTART_ICON);
            debugModuleButton.setDisabledIcon(RESTART_ICON);
        } else {
            debugModuleButton.setIcon(AllIcons.Actions.StartDebugger);
            debugModuleButton.setDisabledIcon(IconLoader.getDisabledIcon(AllIcons.Actions.StartDebugger));
        }
        debugModuleButton.setToolTipText(running
                ? StudioBundle.message("tooltip.RestartThisModuleInDebugModeUsingTheSelectedRunConfiguration")
                : StudioBundle.message("tooltip.DebugThisModuleUsingTheSelectedRunConfiguration"));
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
