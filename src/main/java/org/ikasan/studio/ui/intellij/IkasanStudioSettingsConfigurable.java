package org.ikasan.studio.ui.intellij;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** Provides the Ikasan Studio settings page under Settings → Tools → Ikasan Studio. */
public class IkasanStudioSettingsConfigurable implements Configurable {

    private JCheckBox gettingStartedHintsCheckBox;
    private JCheckBox promptBeforeDeletingUserCodeCheckBox;

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

        JLabel hintsNote = new JLabel(StudioBundle.message("label.HintsNote"));
        hintsNote.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0));
        hintsPanel.add(hintsNote, BorderLayout.CENTER);

        promptBeforeDeletingUserCodeCheckBox = new JCheckBox(StudioBundle.message("checkbox.PromptBeforeDeletingUserCode"));

        JPanel userCodePanel = new JPanel(new BorderLayout(0, 4));
        userCodePanel.setBorder(BorderFactory.createTitledBorder(StudioBundle.message("label.UserCodeDeletion")));
        userCodePanel.add(promptBeforeDeletingUserCodeCheckBox, BorderLayout.NORTH);

        JLabel userCodeNote = new JLabel(StudioBundle.message("label.UserCodeDeletionNote"));
        userCodeNote.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0));
        userCodePanel.add(userCodeNote, BorderLayout.CENTER);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(hintsPanel);
        northPanel.add(userCodePanel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(northPanel, BorderLayout.NORTH);

        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        return gettingStartedHintsCheckBox.isSelected() != IkasanStudioSettings.areGettingStartedHintsEnabled()
                || promptBeforeDeletingUserCodeCheckBox.isSelected() != IkasanStudioSettings.isPromptBeforeDeletingUserCode();
    }

    @Override
    public void apply() {
        IkasanStudioSettings instance = IkasanStudioSettings.getInstance();
        IkasanStudioSettings.State state = instance != null ? instance.getState() : null;
        if (state != null) {
            state.gettingStartedHintsEnabled = gettingStartedHintsCheckBox.isSelected();
            state.promptBeforeDeletingUserCode = promptBeforeDeletingUserCodeCheckBox.isSelected();
        }
        repaintOpenCanvases();
    }

    @Override
    public void reset() {
        gettingStartedHintsCheckBox.setSelected(IkasanStudioSettings.areGettingStartedHintsEnabled());
        promptBeforeDeletingUserCodeCheckBox.setSelected(IkasanStudioSettings.isPromptBeforeDeletingUserCode());
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
