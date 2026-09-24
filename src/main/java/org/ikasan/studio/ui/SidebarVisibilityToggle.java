package org.ikasan.studio.ui;

import com.intellij.ui.JBSplitter;
import com.intellij.util.ui.JBUI;

import javax.swing.JButton;
import javax.swing.JComponent;

/** Editor-local visibility; hiding never disposes the form or commits pending edits. */
final class SidebarVisibilityToggle {
    private final JBSplitter splitter;
    private final JComponent sidebar;
    private final JButton button = new JButton();
    private int previousWidth;

    SidebarVisibilityToggle(JBSplitter splitter, JComponent sidebar) {
        this.splitter = splitter;
        this.sidebar = sidebar;
        button.addActionListener(event -> toggle());
        updateLabel();
    }

    JButton button() { return button; }

    private void toggle() {
        int available = splitter.getWidth() - splitter.getDividerWidth();
        if (available <= 0) return;
        if (sidebar.isVisible()) {
            previousWidth = Math.round(available * (1 - splitter.getProportion()));
            sidebar.setVisible(false);
        } else {
            // Preserve room for the canvas if the editor became smaller while hidden.
            int width = Math.min(previousWidth, available - Math.min(JBUI.scale(320), available / 2));
            splitter.setProportion(1 - (float) Math.max(0, width) / available);
            sidebar.setVisible(true);
        }
        updateLabel();
        splitter.revalidate();
        splitter.repaint();
        button.requestFocusInWindow();
    }

    private void updateLabel() {
        String action = sidebar.isVisible() ? "hide" : "show";
        button.setText(StudioBundle.message("designer.sidebar." + action));
        button.setToolTipText(StudioBundle.message("designer.sidebar." + action + ".tooltip"));
        button.getAccessibleContext().setAccessibleName(button.getToolTipText());
    }
}
