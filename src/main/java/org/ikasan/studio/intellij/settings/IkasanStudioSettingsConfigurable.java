package org.ikasan.studio.intellij.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.panel.ComponentPanelBuilder;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/** Provides the Ikasan Studio settings page under Settings → Tools → Ikasan Studio. */
public class IkasanStudioSettingsConfigurable implements Configurable {

    private JCheckBox gettingStartedHintsCheckBox;
    private JCheckBox promptBeforeDeletingUserCodeCheckBox;
    private JCheckBox showAdvancedControlsCheckBox;
    private JCheckBox showJmsConnectorsCheckBox;
    private JCheckBox testMailServerLivePollingCheckBox;
    private JCheckBox flowErrorMonitoringCheckBox;
    private JSpinner componentDistanceSpinner;
    private JSpinner flowDistanceSpinner;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return StudioBundle.message("ikasan.studio.settings.displayName");
    }

    @Override
    public JComponent createComponent() {
        gettingStartedHintsCheckBox = new JCheckBox(StudioBundle.message("checkbox.ShowGettingStartedHints"));

        JPanel hintsPanel = new JPanel(new BorderLayout(0, 4));
        hintsPanel.setBorder(BorderFactory.createTitledBorder(StudioBundle.message("label.Onboarding")));
        hintsPanel.add(gettingStartedHintsCheckBox, BorderLayout.NORTH);

        JLabel hintsNote = wrappingNote("label.HintsNote");
        hintsPanel.add(hintsNote, BorderLayout.CENTER);

        promptBeforeDeletingUserCodeCheckBox = new JCheckBox(StudioBundle.message("checkbox.PromptBeforeDeletingUserCode"));

        JPanel userCodePanel = new JPanel(new BorderLayout(0, 4));
        userCodePanel.setBorder(BorderFactory.createTitledBorder(StudioBundle.message("label.UserCodeDeletion")));
        userCodePanel.add(promptBeforeDeletingUserCodeCheckBox, BorderLayout.NORTH);

        JLabel userCodeNote = wrappingNote("label.UserCodeDeletionNote");
        userCodePanel.add(userCodeNote, BorderLayout.CENTER);

        showAdvancedControlsCheckBox = new JCheckBox(StudioBundle.message("checkbox.ShowAdvancedControls"));

        JPanel advancedControlsPanel = new JPanel(new BorderLayout(0, 4));
        advancedControlsPanel.setBorder(BorderFactory.createTitledBorder(StudioBundle.message("label.AdvancedControls")));
        advancedControlsPanel.add(showAdvancedControlsCheckBox, BorderLayout.NORTH);

        JLabel advancedControlsNote = wrappingNote("label.AdvancedControlsNote");
        advancedControlsPanel.add(advancedControlsNote, BorderLayout.CENTER);

        showJmsConnectorsCheckBox = new JCheckBox(StudioBundle.message("checkbox.ShowJmsConnectors"));

        JPanel jmsConnectorsPanel = new JPanel(new BorderLayout(0, 4));
        jmsConnectorsPanel.setBorder(BorderFactory.createTitledBorder(StudioBundle.message("label.JmsConnectors")));
        jmsConnectorsPanel.add(showJmsConnectorsCheckBox, BorderLayout.NORTH);

        JLabel jmsConnectorsNote = wrappingNote("label.JmsConnectorsNote");
        jmsConnectorsPanel.add(jmsConnectorsNote, BorderLayout.CENTER);

        testMailServerLivePollingCheckBox = new JCheckBox(StudioBundle.message("checkbox.TestMailServerLivePolling"));

        JPanel testMailServerPanel = new JPanel(new BorderLayout(0, 4));
        testMailServerPanel.setBorder(BorderFactory.createTitledBorder(StudioBundle.message("label.TestMailServerLivePolling")));
        testMailServerPanel.add(testMailServerLivePollingCheckBox, BorderLayout.NORTH);

        JLabel testMailServerNote = wrappingNote("label.TestMailServerLivePollingNote");
        testMailServerPanel.add(testMailServerNote, BorderLayout.CENTER);

        flowErrorMonitoringCheckBox = new JCheckBox(StudioBundle.message("checkbox.FlowErrorMonitoring"));

        JPanel flowErrorMonitoringPanel = new JPanel(new BorderLayout(0, 4));
        flowErrorMonitoringPanel.setBorder(BorderFactory.createTitledBorder(StudioBundle.message("label.FlowErrorMonitoring")));
        flowErrorMonitoringPanel.add(flowErrorMonitoringCheckBox, BorderLayout.NORTH);

        JLabel flowErrorMonitoringNote = wrappingNote("label.FlowErrorMonitoringNote");
        flowErrorMonitoringPanel.add(flowErrorMonitoringNote, BorderLayout.CENTER);

        componentDistanceSpinner = canvasDistanceSpinner(IkasanStudioSettings.DEFAULT_COMPONENT_DISTANCE);
        flowDistanceSpinner = canvasDistanceSpinner(IkasanStudioSettings.DEFAULT_FLOW_DISTANCE);
        JPanel canvasLayoutFields = new JPanel();
        canvasLayoutFields.setLayout(new BoxLayout(canvasLayoutFields, BoxLayout.Y_AXIS));
        canvasLayoutFields.add(canvasDistanceRow(
                StudioBundle.message("label.ComponentDistance"), componentDistanceSpinner));
        canvasLayoutFields.add(canvasDistanceRow(
                StudioBundle.message("label.FlowDistance"), flowDistanceSpinner));
        JButton resetCanvasDistancesButton = new JButton(
                StudioBundle.message("button.ResetCanvasLayoutDefaults"));
        resetCanvasDistancesButton.addActionListener(event -> resetCanvasDistancesToDefaults());
        JPanel resetCanvasDistancesRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        resetCanvasDistancesRow.add(resetCanvasDistancesButton);
        canvasLayoutFields.add(resetCanvasDistancesRow);

        JPanel canvasLayoutPanel = new JPanel(new BorderLayout(0, 4));
        canvasLayoutPanel.setBorder(BorderFactory.createTitledBorder(
                StudioBundle.message("label.CanvasLayout")));
        canvasLayoutPanel.add(canvasLayoutFields, BorderLayout.NORTH);
        JLabel canvasLayoutNote = wrappingNote("label.CanvasLayoutNote");
        canvasLayoutPanel.add(canvasLayoutNote, BorderLayout.CENTER);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(hintsPanel);
        northPanel.add(userCodePanel);
        northPanel.add(advancedControlsPanel);
        northPanel.add(jmsConnectorsPanel);
        northPanel.add(canvasLayoutPanel);
        northPanel.add(testMailServerPanel);
        northPanel.add(flowErrorMonitoringPanel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(northPanel, BorderLayout.NORTH);

        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        return gettingStartedHintsCheckBox.isSelected() != IkasanStudioSettings.areGettingStartedHintsEnabled()
                || promptBeforeDeletingUserCodeCheckBox.isSelected() != IkasanStudioSettings.isPromptBeforeDeletingUserCode()
                || showAdvancedControlsCheckBox.isSelected() != IkasanStudioSettings.isShowAdvancedControlsEnabled()
                || showJmsConnectorsCheckBox.isSelected() != IkasanStudioSettings.areJmsConnectorsEnabled()
                || spinnerValue(componentDistanceSpinner) != IkasanStudioSettings.getComponentDistance()
                || spinnerValue(flowDistanceSpinner) != IkasanStudioSettings.getFlowDistance()
                || testMailServerLivePollingCheckBox.isSelected() != IkasanStudioSettings.isTestMailServerLivePollingEnabled()
                || flowErrorMonitoringCheckBox.isSelected() != IkasanStudioSettings.isFlowErrorMonitoringEnabled();
    }

    @Override
    public void apply() {
        IkasanStudioSettings instance = IkasanStudioSettings.getInstance();
        IkasanStudioSettings.State state = instance != null ? instance.getState() : null;
        if (state != null) {
            state.gettingStartedHintsEnabled = gettingStartedHintsCheckBox.isSelected();
            state.promptBeforeDeletingUserCode = promptBeforeDeletingUserCodeCheckBox.isSelected();
            state.showAdvancedControls = showAdvancedControlsCheckBox.isSelected();
            state.jmsConnectorsEnabled = showJmsConnectorsCheckBox.isSelected();
            state.componentDistance = spinnerValue(componentDistanceSpinner);
            state.flowDistance = spinnerValue(flowDistanceSpinner);
            state.testMailServerLivePollingEnabled = testMailServerLivePollingCheckBox.isSelected();
            state.flowErrorMonitoringEnabled = flowErrorMonitoringCheckBox.isSelected();
        }
        repaintOpenCanvases();
    }

    @Override
    public void reset() {
        gettingStartedHintsCheckBox.setSelected(IkasanStudioSettings.areGettingStartedHintsEnabled());
        promptBeforeDeletingUserCodeCheckBox.setSelected(IkasanStudioSettings.isPromptBeforeDeletingUserCode());
        showAdvancedControlsCheckBox.setSelected(IkasanStudioSettings.isShowAdvancedControlsEnabled());
        showJmsConnectorsCheckBox.setSelected(IkasanStudioSettings.areJmsConnectorsEnabled());
        componentDistanceSpinner.setValue(IkasanStudioSettings.getComponentDistance());
        flowDistanceSpinner.setValue(IkasanStudioSettings.getFlowDistance());
        testMailServerLivePollingCheckBox.setSelected(IkasanStudioSettings.isTestMailServerLivePollingEnabled());
        flowErrorMonitoringCheckBox.setSelected(IkasanStudioSettings.isFlowErrorMonitoringEnabled());
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return gettingStartedHintsCheckBox;
    }

    private void repaintOpenCanvases() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            UiContext context = project.getService(UiContext.class);
            if (context != null && context.getDesignerCanvas() != null) {
                context.getDesignerCanvas().setInitialiseAllDimensions(true);
                context.getDesignerCanvas().repaint();
            }
            if (context != null && context.getCanvasPanel() != null) {
                context.getCanvasPanel().refreshAdvancedControlsVisibility();
            }
        }
    }

    private static JSpinner canvasDistanceSpinner(int defaultValue) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(defaultValue,
                IkasanStudioSettings.MINIMUM_CANVAS_DISTANCE,
                IkasanStudioSettings.MAXIMUM_CANVAS_DISTANCE, 1));
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "0 px"));
        return spinner;
    }

    private static JPanel canvasDistanceRow(String label, JSpinner spinner) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setLabelFor(spinner);
        row.add(fieldLabel);
        row.add(spinner);
        return row;
    }

    private static int spinnerValue(JSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }

    // ComponentPanelBuilder is deprecated at the class level (bare @Deprecated, no forRemoval=true - confirmed
    // from the platform jar's bytecode, not assumed) in favour of JetBrains' newer Kotlin UI DSL
    // (com.intellij.ui.dsl.builder). This whole settings panel is plain Java Swing, not built on that DSL, and
    // it's not scheduled for imminent removal - rewriting the entire panel to adopt the DSL is a disproportionate
    // fix for one static-analysis nag, so this narrowly suppresses it at the one call site instead.
    @SuppressWarnings("deprecation")
    private static JLabel wrappingNote(String messageKey) {
        JLabel note = ComponentPanelBuilder.createCommentComponent(
                StudioBundle.message(messageKey), true);
        note.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0));
        return note;
    }

    void resetCanvasDistancesToDefaults() {
        componentDistanceSpinner.setValue(IkasanStudioSettings.DEFAULT_COMPONENT_DISTANCE);
        flowDistanceSpinner.setValue(IkasanStudioSettings.DEFAULT_FLOW_DISTANCE);
    }
}
