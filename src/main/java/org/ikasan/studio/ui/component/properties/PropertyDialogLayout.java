package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import javax.swing.*;
import java.awt.*;

/** Shared sizing and native text controls for property confirmation dialogs. */
final class PropertyDialogLayout {
    private PropertyDialogLayout() {}

    static JComponent wrap(JPanel details, JComponent footer) {
        JPanel topAligned = new JPanel(new BorderLayout());
        topAligned.add(details, BorderLayout.NORTH);
        JBScrollPane scroll = new JBScrollPane(topAligned);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(JBUI.size(560, Math.min(320, details.getPreferredSize().height + 12)));
        JPanel content = new JPanel(new BorderLayout(JBUI.scale(12), JBUI.scale(12)));
        JLabel warning = new JBLabel(Messages.getWarningIcon());
        warning.setVerticalAlignment(SwingConstants.TOP);
        content.add(warning, BorderLayout.WEST);
        content.add(scroll, BorderLayout.CENTER);
        if (footer != null) content.add(footer, BorderLayout.SOUTH);
        return content;
    }

    static void addRow(JPanel panel, GridBagConstraints c, String label, String value) {
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.insets = JBUI.insets(3, 0, 3, 12);
        panel.add(new JBLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.insets = JBUI.insets(3, 0);
        panel.add(wrappedText(value, 340), c);
    }

    static JTextArea wrappedText(String text, int width) {
        JTextArea area = new JTextArea(text);
        area.setFont(UIManager.getFont("Label.font"));
        area.setForeground(UIManager.getColor("Label.foreground"));
        area.setEditable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setSize(JBUI.scale(width), Short.MAX_VALUE);
        return area;
    }
}
