package org.ikasan.studio.intellij.project;

import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.Rectangle;

import static org.assertj.core.api.Assertions.assertThat;

class ImportDialogWrapperTest {
    @Test void oversizedPreviewFitsLaptopUsableAreaIncludingTaskbarOffset() {
        var usable = new Rectangle(0, 32, 1366, 708);
        var bounds = ImportDialogWrapper.fitBounds(new Dimension(30000, 20000), new Rectangle(0, 32, 1366, 708), usable, 12);
        assertThat(bounds).isEqualTo(new Rectangle(12, 44, 1342, 684));
        assertThat(usable.contains(bounds)).isTrue();
    }

    @Test void followsExternalMonitorWithNegativeCoordinatesInsteadOfPrimaryScreen() {
        var usable = new Rectangle(-2560, -180, 2560, 1400);
        var owner = new Rectangle(-2300, -100, 1800, 1100);
        var bounds = ImportDialogWrapper.fitBounds(new Dimension(800, 600), owner, usable, 12);
        assertThat(bounds).isEqualTo(new Rectangle(-1800, 150, 800, 600));
        assertThat(usable.contains(bounds)).isTrue();
    }

    @Test void ownerSpanningScreensAndScaledDialogStillFitTheChosenMonitor() {
        var usable = new Rectangle(1920, 0, 1280, 720);
        var owner = new Rectangle(1500, -200, 3000, 1800);
        var bounds = ImportDialogWrapper.fitBounds(new Dimension(1560, 1080), owner, usable, 24);
        assertThat(bounds).isEqualTo(new Rectangle(1944, 24, 1232, 672));
        assertThat(usable.contains(bounds)).isTrue();
    }
}
