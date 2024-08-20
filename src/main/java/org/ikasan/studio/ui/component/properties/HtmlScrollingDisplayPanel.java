package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.ui.Styling;

import javax.swing.*;
import java.awt.*;

/**
 * Used to display html context e.g. the description for a component
 */
public class HtmlScrollingDisplayPanel extends JPanel {
    private final JTextPane paletteHelpTextArea;

    /**
     * Create HtmlScrollingDisplayPanel
     *
     * @param title to be displayed or null if no title.
     * @param dimension of the initial text area, or null if to be controlled by container.
     */
    public HtmlScrollingDisplayPanel(String title, Dimension dimension) {
        super();
        setLayout(new BorderLayout());
        JPanel helpDescriptionHeaderPanel = new JPanel();
        helpDescriptionHeaderPanel.setBorder(null);
        if (title != null) {
            JLabel descriptionLabel = new JLabel(title);
            descriptionLabel.setFont(new Font(getFont().getFontName(), Font.BOLD, getFont().getSize()));
            descriptionLabel.setForeground(Styling.IKASAN_ORANGE);
            helpDescriptionHeaderPanel.add(descriptionLabel);
            add(helpDescriptionHeaderPanel, BorderLayout.NORTH);
        }
        paletteHelpTextArea = new JTextPane();
        paletteHelpTextArea.setBorder(null);
        paletteHelpTextArea.setContentType("text/html");

        if (dimension != null) {
            paletteHelpTextArea.setPreferredSize(dimension);
        }
        JScrollPane helpTextScrollPane = new JScrollPane(paletteHelpTextArea);
        helpTextScrollPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        add(helpTextScrollPane, BorderLayout.CENTER);
    }

    public void setText(String text) {
        // By saving the caret position, once the text is added we can present the scroll box as it was.
        int caretPosition = paletteHelpTextArea.getCaretPosition();
        paletteHelpTextArea.setText(text);
        paletteHelpTextArea.setCaretPosition(caretPosition);
    }
}
