package org.ikasan.studio.ui;

import com.intellij.ui.JBSplitter;
import com.intellij.util.ui.JBUI;

import javax.swing.JTabbedPane;
import java.util.function.Consumer;

/** Temporary fit/restore state belongs to the current sidebar tab, not the project model. */
final class PanelWidthToggle {
    private final JBSplitter splitter;
    private final Consumer<Boolean> restoreAvailable;
    private Integer previousWidth;

    PanelWidthToggle(JBSplitter splitter, JTabbedPane tabs, Consumer<Boolean> restoreAvailable) {
        this.splitter = splitter;
        this.restoreAvailable = restoreAvailable;
        tabs.addChangeListener(event -> reset());
    }

    void toggle(int preferredWidth) {
        int available = splitter.getWidth() - splitter.getDividerWidth();
        if (available <= 0) return;
        int target;
        if (previousWidth == null) {
            previousWidth = Math.round(available * (1.0f - splitter.getProportion()));
            int canvasReserve = Math.min(JBUI.scale(320), available / 2);
            target = Math.min(preferredWidth, available - canvasReserve);
        } else {
            target = previousWidth;
            previousWidth = null;
        }
        // A window may have become smaller since the original width was remembered.
        target = Math.max(0, Math.min(target, available));
        splitter.setProportion(1.0f - (float) target / available);
        restoreAvailable.accept(previousWidth != null);
    }

    void reset() {
        previousWidth = null;
        restoreAvailable.accept(false);
    }
}
