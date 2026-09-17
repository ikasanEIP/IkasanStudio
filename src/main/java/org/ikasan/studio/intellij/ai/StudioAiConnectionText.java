package org.ikasan.studio.intellij.ai;

import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import java.awt.Dimension;
import javax.swing.text.View;

/** Measures wrapped guidance at a useful width before the dialog has been laid out. */
final class StudioAiConnectionText extends JBTextArea {
    StudioAiConnectionText(String text) { super(text); }

    @Override public Dimension getPreferredSize() {
        if (!getLineWrap() || getUI() == null) return super.getPreferredSize();
        int width = getWidth() > 0 ? getWidth() : JBUI.scale(740);
        var insets = getInsets();
        View view = getUI().getRootView(this);
        view.setSize(Math.max(1, width - insets.left - insets.right), Integer.MAX_VALUE);
        int height = (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS)) + insets.top + insets.bottom;
        return new Dimension(width, height);
    }

    @Override public Dimension getMinimumSize() {
        // Swing otherwise measures wrapping at an uninitialised width and can demand a screen-high dialog.
        return new Dimension(0, getFontMetrics(getFont()).getHeight());
    }
}
