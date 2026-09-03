package org.ikasan.studio.intellij.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.panel.ComponentPanelBuilder;
import com.intellij.util.ui.JBUI;
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

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
    private JSpinner flowXStartPointSpinner;
    private JSpinner flowYStartPointSpinner;

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
        flowXStartPointSpinner = flowStartPointSpinner(IkasanStudioSettings.DEFAULT_FLOW_X_START_POINT);
        flowYStartPointSpinner = flowStartPointSpinner(IkasanStudioSettings.DEFAULT_FLOW_Y_START_POINT);

        // GridBagLayout (not the individually-packed FlowLayout rows this used to be) so every field starts at
        // the same x regardless of its own label's length - the label column is sized to its widest member and
        // every row's field then lines up against it, matching the aligned look of IntelliJ's own settings pages
        // (e.g. Settings > Tools > Tasks). Neither column stretches (no weightx), so this stays a compact form
        // rather than spreading the spinners across the panel's full width.
        JPanel canvasLayoutFields = new JPanel(new GridBagLayout());
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = JBUI.insets(2, 0, 2, 8);
        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.anchor = GridBagConstraints.WEST;
        fieldConstraints.insets = JBUI.insets(2, 0);

        addCanvasLayoutFieldRow(canvasLayoutFields, labelConstraints, fieldConstraints, 0,
                StudioBundle.message("label.ComponentDistance"), componentDistanceSpinner);
        addCanvasLayoutFieldRow(canvasLayoutFields, labelConstraints, fieldConstraints, 1,
                StudioBundle.message("label.FlowDistance"), flowDistanceSpinner);
        addCanvasLayoutFieldRow(canvasLayoutFields, labelConstraints, fieldConstraints, 2,
                StudioBundle.message("label.FlowXStartPoint"), flowXStartPointSpinner);
        addCanvasLayoutFieldRow(canvasLayoutFields, labelConstraints, fieldConstraints, 3,
                StudioBundle.message("label.FlowYStartPoint"), flowYStartPointSpinner);

        JButton resetCanvasDistancesButton = new JButton(
                StudioBundle.message("button.ResetCanvasLayoutDefaults"));
        resetCanvasDistancesButton.addActionListener(event -> resetCanvasDistancesToDefaults());
        GridBagConstraints resetButtonConstraints = new GridBagConstraints();
        resetButtonConstraints.gridx = 0;
        resetButtonConstraints.gridy = 4;
        resetButtonConstraints.gridwidth = 2;
        resetButtonConstraints.anchor = GridBagConstraints.WEST;
        resetButtonConstraints.insets = JBUI.insetsTop(4);
        canvasLayoutFields.add(resetCanvasDistancesButton, resetButtonConstraints);

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
                || spinnerValue(flowXStartPointSpinner) != IkasanStudioSettings.getFlowXStartPoint()
                || spinnerValue(flowYStartPointSpinner) != IkasanStudioSettings.getFlowYStartPoint()
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
            state.flowXStartPoint = spinnerValue(flowXStartPointSpinner);
            state.flowYStartPoint = spinnerValue(flowYStartPointSpinner);
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
        flowXStartPointSpinner.setValue(IkasanStudioSettings.getFlowXStartPoint());
        flowYStartPointSpinner.setValue(IkasanStudioSettings.getFlowYStartPoint());
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
        return pixelSpinner(defaultValue, IkasanStudioSettings.MINIMUM_CANVAS_DISTANCE,
                IkasanStudioSettings.MAXIMUM_CANVAS_DISTANCE);
    }

    private static JSpinner flowStartPointSpinner(int defaultValue) {
        return pixelSpinner(defaultValue, IkasanStudioSettings.MINIMUM_FLOW_START_POINT,
                IkasanStudioSettings.MAXIMUM_FLOW_START_POINT);
    }

    private static JSpinner pixelSpinner(int defaultValue, int minimum, int maximum) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(defaultValue, minimum, maximum, 1));
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "0 px"));
        return spinner;
    }

    /**
     * Adds one label+spinner row at {@code rowIndex} to {@code target}, reusing the same two GridBagConstraints
     * instances across every row (GridBagLayout copies each one when a component is added, so mutating gridy
     * between calls is the normal way to lay out a grid this way without a fresh constraints object per cell).
     */
    private static void addCanvasLayoutFieldRow(JPanel target, GridBagConstraints labelConstraints,
                                                GridBagConstraints fieldConstraints, int rowIndex,
                                                String label, JSpinner spinner) {
        labelConstraints.gridy = rowIndex;
        fieldConstraints.gridy = rowIndex;
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setLabelFor(spinner);
        target.add(fieldLabel, labelConstraints);
        target.add(spinner, fieldConstraints);
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
        flowXStartPointSpinner.setValue(IkasanStudioSettings.DEFAULT_FLOW_X_START_POINT);
        flowYStartPointSpinner.setValue(IkasanStudioSettings.DEFAULT_FLOW_Y_START_POINT);
    }
}
