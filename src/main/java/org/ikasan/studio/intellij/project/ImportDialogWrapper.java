package org.ikasan.studio.intellij.project;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.ScreenUtil;
import com.intellij.util.ui.JBUI;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;

/** Import dialogs stay with their invoking window and fit its monitor's usable bounds. */
abstract class ImportDialogWrapper extends DialogWrapper {
    private final Component parent;

    ImportDialogWrapper(Project project, Component parent) {
        super(project, parent != null ? parent : WindowManager.getInstance().getFrame(project), true, IdeModalityType.IDE);
        this.parent = parent;
    }

    private Window anchor() {
        Window window = parent instanceof Window owner ? owner : parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        return window != null ? window : getOwner();
    }

    private Rectangle initialBounds(Dimension requested) {
        Window owner = anchor();
        if (owner == null || owner.getGraphicsConfiguration() == null) return null;
        return fitBounds(requested, owner.getBounds(), ScreenUtil.getScreenRectangle(owner), JBUI.scale(12));
    }

    @Override public Dimension getInitialSize() {
        Dimension requested = super.getInitialSize();
        if (requested == null) requested = getPreferredSize();
        Rectangle bounds = initialBounds(requested);
        return bounds == null ? requested : bounds.getSize();
    }

    @Override public Point getInitialLocation() {
        Rectangle bounds = initialBounds(getInitialSize());
        return bounds == null ? super.getInitialLocation() : bounds.getLocation();
    }

    @Override public void show() {
        // Prevent a long document or previously oversized content minimum from defeating the size cap.
        Window window = getWindow();
        if (window != null) window.setMinimumSize(new Dimension(0, 0));
        super.show();
    }

    static Rectangle fitBounds(Dimension requested, Rectangle owner, Rectangle usable, int margin) {
        int insetX = Math.min(Math.max(0, margin), Math.max(0, (usable.width - 1) / 2));
        int insetY = Math.min(Math.max(0, margin), Math.max(0, (usable.height - 1) / 2));
        int width = Math.max(1, Math.min(requested.width, usable.width - 2 * insetX));
        int height = Math.max(1, Math.min(requested.height, usable.height - 2 * insetY));
        int left = usable.x + insetX;
        int top = usable.y + insetY;
        int x = Math.max(left, Math.min(owner.x + (owner.width - width) / 2, usable.x + usable.width - insetX - width));
        int y = Math.max(top, Math.min(owner.y + (owner.height - height) / 2, usable.y + usable.height - insetY - height));
        return new Rectangle(x, y, width, height);
    }
}
