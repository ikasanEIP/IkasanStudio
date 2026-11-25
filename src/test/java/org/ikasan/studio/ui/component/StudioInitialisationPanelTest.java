package org.ikasan.studio.ui.component;

import com.intellij.ui.components.labels.LinkLabel;
import org.ikasan.studio.ui.StudioBundle;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.concurrent.atomic.AtomicInteger;

class StudioInitialisationPanelTest {
    @Test void repairIsDiscoverableOnFailureAndHiddenDuringLoading() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            StudioInitialisationPanel panel = new StudioInitialisationPanel(() -> { }, index -> { }, () -> { }, () -> { });
            LinkLabel<?> repair = findRepair(panel);
            assertThat(repair).isNotNull();
            assertThat(repair.isVisible()).isFalse();
            panel.showFailure("A pack revision is unavailable", List.of(1));
            assertThat(repair.isVisible()).isTrue();
            panel.showReadingProject();
            assertThat(repair.isVisible()).isFalse();
            panel.showFailure("The model needs review", List.of());
            assertThat(repair.isVisible()).isTrue();
            panel.showLoadingComponents();
            assertThat(repair.isVisible()).isFalse();
        });
    }

    @Test void waitingScreenOffersAnExplicitMavenReloadAndHidesItAfterImport() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger reloads = new java.util.concurrent.atomic.AtomicInteger();
            StudioInitialisationPanel panel = new StudioInitialisationPanel(() -> { }, index -> { }, () -> { }, reloads::incrementAndGet);
            LinkLabel<?> link = findLink(panel, StudioBundle.message("initialisation.ReloadMaven"));
            assertThat(link).isNotNull();
            assertThat(link.isVisible()).isFalse();
            panel.showWaitingForProjectImport("No registered content roots");
            assertThat(link.isVisible()).isTrue();
            link.doClick();
            assertThat(reloads.get()).isEqualTo(1);
            panel.showReadingProject();
            assertThat(link.isVisible()).isFalse();
        });
    }

    @Test void revisionUpdateOffersUpdateActionAndRestoresRepairActionForFailures() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger reviews = new AtomicInteger();
            StudioInitialisationPanel panel = new StudioInitialisationPanel(() -> { }, index -> { }, reviews::incrementAndGet, () -> { });
            panel.showFailure("Old failure", List.of(1));
            panel.showUpdateAvailable("Templates have changed");
            LinkLabel<?> update = findLink(panel, StudioBundle.message("modelUpdate.Link"));
            assertThat(update).isNotNull();
            assertThat(update.isVisible()).isTrue();
            assertThat(findLink(panel, StudioBundle.message("label.Retry")).isVisible()).isFalse();
            assertThat(findLink(panel, StudioBundle.message("label.RestoreModelBackup", 1)).getParent().isVisible()).isFalse();
            update.doClick();
            assertThat(reviews.get()).isEqualTo(1);
            panel.showFailure("Invalid model", List.of());
            assertThat(findRepair(panel).isVisible()).isTrue();
        });
    }

    private static LinkLabel<?> findRepair(Container parent) {
        return findLink(parent, StudioBundle.message("modelRepair.Link"));
    }

    private static LinkLabel<?> findLink(Container parent, String text) {
        for (Component child : parent.getComponents()) {
            if (child instanceof LinkLabel<?> link && text.equals(link.getText())) return link;
            if (child instanceof Container container) {
                LinkLabel<?> result = findLink(container, text);
                if (result != null) return result;
            }
        }
        return null;
    }
}
