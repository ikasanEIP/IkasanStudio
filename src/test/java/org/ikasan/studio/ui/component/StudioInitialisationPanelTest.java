package org.ikasan.studio.ui.component;

import com.intellij.ui.components.labels.LinkLabel;
import java.awt.Component;
import java.awt.Container;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import java.util.Arrays;
import org.ikasan.studio.ui.StudioBundle;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
            AtomicInteger reloads = new AtomicInteger();
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
            assertThat(findLink(panel, StudioBundle.message("label.Retry")))
                    .isNotNull().matches(link -> !link.isVisible());
            assertThat(findLink(panel, StudioBundle.message("label.RestoreModelBackup", 1)))
                    .isNotNull().matches(link -> !link.getParent().isVisible());
            update.doClick();
            assertThat(reviews.get()).isEqualTo(1);
            panel.showFailure("Invalid model", List.of());
            assertThat(findRepair(panel).isVisible()).isTrue();
        });
    }

    @Test void setupIsExplicitAndUnavailableForUnsupportedProjects() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger setups = new AtomicInteger();
            StudioInitialisationPanel panel = new StudioInitialisationPanel(() -> { }, index -> { }, () -> { }, () -> { });
            panel.setSetupAction(setups::incrementAndGet);
            JButton setup = Arrays.stream(panel.getComponents())
                    .filter(child -> child instanceof JButton)
                    .map(child -> (JButton) child).findFirst().orElseThrow();
            assertThat(setup.isVisible()).isFalse();
            panel.showSetupAvailable("Simple Maven project", true);
            assertThat(setup.isVisible()).isTrue();
            assertThat(setup.isEnabled()).isTrue();
            assertThat(findRepair(panel).isVisible()).isFalse();
            setup.doClick();
            assertThat(setups.get()).isEqualTo(1);
            panel.showSetupAvailable("Custom layout", false);
            assertThat(setup.isEnabled()).isFalse();
            panel.showReadingProject();
            assertThat(setup.isVisible()).isFalse();
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
