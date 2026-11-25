package org.ikasan.studio.ui.component.canvas;

import com.intellij.icons.AllIcons;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.actions.HarnessControlActions.HarnessStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HarnessToolbarStateTest {
    @Test
    void showsRunningServicesAndRestoresIdlePresentationWithoutHidingRecoveryActions() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JButton start = new JButton();
            JButton stop = new JButton();
            CanvasPanel.updateHarnessState(start, stop, List.of());
            assertThat(start.getIcon()).isSameAs(AllIcons.Actions.Execute);
            assertThat(start.getToolTipText()).isEqualTo(StudioBundle.message("tooltip.StartHarnesses"));

            CanvasPanel.updateHarnessState(start, stop, List.of("FTP", "SMTP"));
            assertThat(start.getIcon()).isNotSameAs(AllIcons.Actions.Execute);
            assertThat(start.getToolTipText()).contains("FTP, SMTP");
            assertThat(stop.getToolTipText()).contains("FTP, SMTP");
            assertThat(start.getAccessibleContext().getAccessibleDescription()).isEqualTo(start.getToolTipText());
            assertThat(start.isEnabled()).isTrue();
            assertThat(stop.isEnabled()).isTrue();
            BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try { start.getIcon().paintIcon(start, graphics, 0, 0); }
            finally { graphics.dispose(); }

            Icon ownedIcon = start.getIcon();
            HarnessStatus shared = new HarnessStatus(List.of(), List.of(StudioBundle.message("harness.smtp.sharedAvailable")));
            CanvasPanel.updateHarnessState(start, stop, shared);
            assertThat(start.getIcon()).isNotSameAs(AllIcons.Actions.Execute).isNotSameAs(ownedIcon);
            assertThat(CanvasPanel.harnessTitle(shared)).isEqualTo(StudioBundle.message("label.HarnessSharedOnly"));
            assertThat(stop.getToolTipText()).startsWith(StudioBundle.message("tooltip.NoOwnedHarnessesRunning"));
            assertThat(start.getToolTipText()).contains(StudioBundle.message("harness.smtp.sharedAvailable"));
            CanvasPanel.updateHarnessState(start, stop, List.of());
            assertThat(start.getIcon()).isSameAs(AllIcons.Actions.Execute);
            assertThat(stop.getToolTipText()).isEqualTo(StudioBundle.message("tooltip.NoOwnedHarnessesRunning"));
        });
    }
}
