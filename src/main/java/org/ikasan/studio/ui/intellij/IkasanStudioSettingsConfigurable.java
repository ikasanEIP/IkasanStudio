package org.ikasan.studio.ui.intellij;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.ikasan.studio.ui.UiContext;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** Provides the Ikasan Studio settings page under Settings → Tools → Ikasan Studio. */
public class IkasanStudioSettingsConfigurable implements Configurable {

    private JCheckBox gettingStartedHintsCheckBox;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Ikasan Studio";
    }

    @Override
    public JComponent createComponent() {
        gettingStartedHintsCheckBox = new JCheckBox("Show getting started hints");

        JPanel hintsPanel = new JPanel(new BorderLayout(0, 4));
        hintsPanel.setBorder(BorderFactory.createTitledBorder("Onboarding"));
        hintsPanel.add(gettingStartedHintsCheckBox, BorderLayout.NORTH);

        JLabel hintsNote = new JLabel(
                "<html>Shows the next step on an empty canvas or flow. Existing designs are not obscured.</html>");
        hintsNote.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0));
        hintsPanel.add(hintsNote, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(hintsPanel, BorderLayout.NORTH);

        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        return gettingStartedHintsCheckBox.isSelected()
                != IkasanStudioSettings.areGettingStartedHintsEnabled();
    }

    @Override
    public void apply() {
        IkasanStudioSettings instance = IkasanStudioSettings.getInstance();
        IkasanStudioSettings.State state = instance != null ? instance.getState() : null;
        if (state != null) {
            state.gettingStartedHintsEnabled = gettingStartedHintsCheckBox.isSelected();
        }
        repaintOpenCanvases();
    }

    @Override
    public void reset() {
        gettingStartedHintsCheckBox.setSelected(IkasanStudioSettings.areGettingStartedHintsEnabled());
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return gettingStartedHintsCheckBox;
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
