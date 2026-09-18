package org.ikasan.studio.intellij.ai;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorNotificationPanel;
import org.ikasan.studio.ui.StudioBundle;

/** Editor-owned view of project-owned inbox state; switching or closing tabs cannot dismiss a proposal. */
public final class StudioAiProposalBanner extends EditorNotificationPanel {
    private final StudioAiProposalInboxService inbox;
    private final Timer pulse = new Timer(50, event -> repaint());
    private boolean disposed;
    private long pulseStarted;
    private static final Color ACCENT = new JBColor(new Color(225, 153, 25), new Color(255, 190, 65));

    public StudioAiProposalBanner(Project project, Disposable owner) {
        pulse.setCoalesce(true);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) updatePulse();
        });
        Disposer.register(owner, () -> {
            disposed = true;
            pulse.stop();
        });
        inbox = project.getService(StudioAiProposalInboxService.class);
        createActionLabel(StudioBundle.message("ai.BannerReview"), inbox::reviewPending);
        createActionLabel(StudioBundle.message("ai.BannerDismiss"), inbox::dismissPending);
        project.getMessageBus().connect(owner).subscribe(StudioAiProposalInboxService.CHANGED, this::refresh);
        refresh();
    }

    private void updatePulse() {
        if (!disposed && isShowing()) {
            if (!pulse.isRunning()) {
                pulseStarted = System.nanoTime();
                pulse.start();
            }
        } else {
            pulse.stop();
        }
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (!isVisible() || disposed) return;
        // One smooth cycle every 2.4 seconds. Only the background changes; labels stay readable.
        double phase = (System.nanoTime() - pulseStarted) / 2_400_000_000.0 * Math.PI * 2;
        float alpha = (float) (0.12 + 0.12 * (1 - Math.cos(phase)) / 2);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setColor(ACCENT);
            g.setComposite(AlphaComposite.SrcOver.derive(alpha));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setComposite(AlphaComposite.SrcOver);
            g.fillRect(0, 0, com.intellij.util.ui.JBUI.scale(4), getHeight());
        } finally {
            g.dispose();
        }
    }

    private void refresh() {
        var path = inbox.getPendingProposal();
        setText(path == null ? "" : StudioBundle.message("ai.BannerReady", path.getFileName().toString()));
        setVisible(path != null);
        updatePulse();
        revalidate();
        repaint();
    }
}
