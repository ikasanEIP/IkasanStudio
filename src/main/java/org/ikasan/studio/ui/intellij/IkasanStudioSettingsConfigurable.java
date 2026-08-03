package org.ikasan.studio.ui.intellij;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.ikasan.studio.ui.UiContext;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Provides the Ikasan Studio settings page under File → Settings → Tools → Ikasan Studio.
 * Changes take effect the next time the tool window is opened (a restart is not required,
 * but any already-open tool window will keep its current mode for the session).
 */
public class IkasanStudioSettingsConfigurable implements Configurable {

    private JRadioButton slidingButton;
    private JRadioButton dockedButton;
    private JCheckBox gettingStartedHintsCheckBox;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Ikasan Studio";
    }

    @Override
    public JComponent createComponent() {
        dockedButton  = new JRadioButton("Docked (default) — shares space with the editor; drag-and-drop works reliably");
        slidingButton = new JRadioButton("Sliding — overlays the editor, but drag-and-drop may collapse the panel mid-drag");
        gettingStartedHintsCheckBox = new JCheckBox("Show getting started hints");

        ButtonGroup group = new ButtonGroup();
        group.add(slidingButton);
        group.add(dockedButton);

        JLabel label = new JLabel("Tool window display mode:");
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        JPanel radioPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        radioPanel.add(dockedButton);
        radioPanel.add(slidingButton);

        JPanel hintsPanel = new JPanel(new BorderLayout(0, 4));
        hintsPanel.setBorder(BorderFactory.createTitledBorder("Onboarding"));
        hintsPanel.add(gettingStartedHintsCheckBox, BorderLayout.NORTH);
        JLabel hintsNote = new JLabel("<html>Shows the next step on an empty canvas or flow. Existing designs are not obscured.</html>");
        hintsNote.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0));
        hintsPanel.add(hintsNote, BorderLayout.CENTER);

        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        JPanel toolWindowPanel = new JPanel(new BorderLayout());
        toolWindowPanel.add(label, BorderLayout.NORTH);
        toolWindowPanel.add(radioPanel, BorderLayout.CENTER);
        toolWindowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsPanel.add(toolWindowPanel);
        settingsPanel.add(Box.createVerticalStrut(12));
        settingsPanel.add(hintsPanel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(settingsPanel, BorderLayout.NORTH);

        JLabel note = new JLabel("<html><i>Display-mode changes apply when the Ikasan Studio panel is next opened. Hint changes apply immediately.</i></html>");
        note.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        panel.add(note, BorderLayout.SOUTH);

        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        return dockedButton.isSelected() != IkasanStudioSettings.isDockedModeEnabled()
                || gettingStartedHintsCheckBox.isSelected() != IkasanStudioSettings.areGettingStartedHintsEnabled();
    }

    @Override
    public void apply() {
        IkasanStudioSettings instance = IkasanStudioSettings.getInstance();
        IkasanStudioSettings.State s = instance != null ? instance.getState() : null;
        if (s != null) {
            s.dockedMode = dockedButton.isSelected();
            s.gettingStartedHintsEnabled = gettingStartedHintsCheckBox.isSelected();
        }
        repaintOpenCanvases();
    }

    @Override
    public void reset() {
        boolean docked = IkasanStudioSettings.isDockedModeEnabled();
        dockedButton.setSelected(docked);
        slidingButton.setSelected(!docked);
        gettingStartedHintsCheckBox.setSelected(IkasanStudioSettings.areGettingStartedHintsEnabled());
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return dockedButton;
    }

    private void repaintOpenCanvases() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            UiContext context = project.getService(UiContext.class);
            if (context != null && context.getDesignerCanvas() != null) {
                context.getDesignerCanvas().repaint();
            }
        }
    }
}
