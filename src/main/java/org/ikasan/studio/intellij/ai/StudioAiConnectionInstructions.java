package org.ikasan.studio.intellij.ai;

import org.ikasan.studio.ui.StudioBundle;
import com.intellij.util.ui.JBUI;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import java.awt.Dimension;

/** Selectable, wrapping instructions with IDE action labels emphasised. */
final class StudioAiConnectionInstructions extends JTextPane {
    StudioAiConnectionInstructions(Runnable openSettings) {
        this(StudioBundle.message("ai.NativeInstructions"), StudioBundle.message("ai.NativeTab"));
        emphasizeNativeInstructions(getText(), openSettings);
    }

    StudioAiConnectionInstructions(String instructions, String accessibleName) {
        setEditable(false);
        setOpaque(false);
        setFont(UIManager.getFont("Label.font"));
        setText(instructions);
        var spacing = new SimpleAttributeSet();
        StyleConstants.setSpaceAbove(spacing, JBUI.scale(3));
        StyleConstants.setSpaceBelow(spacing, JBUI.scale(3));
        getStyledDocument().setParagraphAttributes(0, instructions.length(), spacing, false);
        setCaretPosition(0);
        getAccessibleContext().setAccessibleName(accessibleName);
    }

    private void emphasizeNativeInstructions(String instructions, Runnable openSettings) {
        var bold = new SimpleAttributeSet();
        StyleConstants.setBold(bold, true);
        for (String label : new String[] {StudioBundle.message("ai.EnableMcpServerLabel"),
                StudioBundle.message("ai.NewChatLabel"), StudioBundle.message("ai.CopyPrompt"),
                StudioBundle.message("ai.ProjectClientsLabel"), StudioBundle.message("ai.ClientsLabel"),
                StudioBundle.message("ai.AlternativelyLabel")}) {
            for (int position = instructions.indexOf(label); position >= 0;
                    position = instructions.indexOf(label, position + label.length())) {
                getStyledDocument().setCharacterAttributes(position, label.length(), bold, false);
            }
        }
        String settingsLabel = StudioBundle.message("ai.OpenMcpSettings");
        int settingsPosition = instructions.indexOf(settingsLabel);
        if (settingsPosition >= 0) {
            var settings = new javax.swing.JButton(settingsLabel);
            settings.setFont(getFont().deriveFont(java.awt.Font.BOLD));
            settings.setMargin(JBUI.insets(1, 4));
            settings.addActionListener(event -> openSettings.run());
            var componentStyle = new SimpleAttributeSet();
            StyleConstants.setComponent(componentStyle, settings);
            try {
                getStyledDocument().remove(settingsPosition, settingsLabel.length());
                // Keep the object replacement character visible as an escape.
                //noinspection UnnecessaryUnicodeEscape
                getStyledDocument().insertString(settingsPosition, "\uFFFC", componentStyle);
            } catch (javax.swing.text.BadLocationException exception) {
                throw new IllegalStateException("Cannot insert MCP settings control", exception);
            }
        }
        setCaretPosition(0);
        getAccessibleContext().setAccessibleName(StudioBundle.message("ai.NativeTab"));
    }

    @Override public Dimension getPreferredSize() {
        if (getUI() == null) return super.getPreferredSize();
        int width = getWidth() > 0 ? getWidth() : JBUI.scale(740);
        var insets = getInsets();
        View view = getUI().getRootView(this);
        view.setSize(Math.max(1, width - insets.left - insets.right), Integer.MAX_VALUE);
        return new Dimension(width, (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS)) + insets.top + insets.bottom);
    }

    @Override public Dimension getMinimumSize() {
        return new Dimension(0, getFontMetrics(getFont()).getHeight());
    }
}
