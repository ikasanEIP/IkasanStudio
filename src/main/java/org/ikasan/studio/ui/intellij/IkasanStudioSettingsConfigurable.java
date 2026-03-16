package org.ikasan.studio.ui.intellij;

import com.intellij.openapi.options.Configurable;
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

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Ikasan Studio";
    }

    @Override
    public JComponent createComponent() {
        dockedButton  = new JRadioButton("Docked (default) — shares space with the editor; drag-and-drop works reliably");
        slidingButton = new JRadioButton("Sliding — overlays the editor, but drag-and-drop may collapse the panel mid-drag");

        ButtonGroup group = new ButtonGroup();
        group.add(slidingButton);
        group.add(dockedButton);

        JLabel label = new JLabel("Tool window display mode:");
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        JPanel radioPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        radioPanel.add(dockedButton);
        radioPanel.add(slidingButton);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(label, BorderLayout.NORTH);
        panel.add(radioPanel, BorderLayout.CENTER);

        JLabel note = new JLabel("<html><i>Changes apply the next time the Ikasan Studio panel is opened.</i></html>");
        note.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        panel.add(note, BorderLayout.SOUTH);

        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        return dockedButton.isSelected() != IkasanStudioSettings.isDockedModeEnabled();
    }

    @Override
    public void apply() {
        IkasanStudioSettings instance = IkasanStudioSettings.getInstance();
        IkasanStudioSettings.State s = instance != null ? instance.getState() : null;
        if (s != null) {
            s.dockedMode = dockedButton.isSelected();
        }
    }

    @Override
    public void reset() {
        boolean docked = IkasanStudioSettings.isDockedModeEnabled();
        dockedButton.setSelected(docked);
        slidingButton.setSelected(!docked);
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return dockedButton;
    }
}
