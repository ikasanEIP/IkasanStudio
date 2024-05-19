package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.ui.StudioUIUtils;

import javax.swing.*;
import java.awt.*;

public class ComponentDescription extends JPanel {
    private JTextPane paletteHelpTextArea;
    public ComponentDescription() {
        JPanel helpDescriptionHeaderPanel = new JPanel();
        JLabel descriptionLabel = new JLabel("Description");
        descriptionLabel.setFont(new Font(getFont().getFontName(), Font.BOLD, getFont().getSize()));
        descriptionLabel.setForeground(StudioUIUtils.IKASAN_ORANGE);
        helpDescriptionHeaderPanel.add(descriptionLabel);

        JPanel paletteHelpBodyPanel = new JPanel(new BorderLayout());
        paletteHelpTextArea = new JTextPane();
        paletteHelpTextArea.setContentType("text/html");
        paletteHelpBodyPanel.add(paletteHelpTextArea, BorderLayout.CENTER);
        setLayout(new BorderLayout());
        add(helpDescriptionHeaderPanel, BorderLayout.NORTH);
        add(paletteHelpBodyPanel, BorderLayout.CENTER);
    }

    public void setText(String text) {
        paletteHelpTextArea.setText(text);
    }
}
