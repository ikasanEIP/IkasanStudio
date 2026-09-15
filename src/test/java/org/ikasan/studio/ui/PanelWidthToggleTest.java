package org.ikasan.studio.ui;

import org.junit.jupiter.api.Test;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class PanelWidthToggleTest {
    @Test
    void fitsAndRestoresAndSwitchingTabsStartsANewToggle() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var tabs = new JTabbedPane();
            tabs.addTab("Properties", new JPanel());
            tabs.addTab("Palette", new JPanel());
            var splitter = DesignerUI.createContentSplitter(new JPanel(), tabs);
            try {
                splitter.setSize(1200, 600);
                splitter.doLayout();
                AtomicBoolean restore = new AtomicBoolean();
                var toggle = new PanelWidthToggle(splitter, tabs, restore::set);
                int original = width(splitter);
                toggle.toggle(600);
                assertEquals(600, width(splitter), 1);
                assertTrue(restore.get());
                toggle.toggle(600);
                assertEquals(original, width(splitter), 1);
                assertFalse(restore.get());

                toggle.toggle(600);
                tabs.setSelectedIndex(1);
                assertFalse(restore.get());
                toggle.toggle(250);
                assertEquals(250, width(splitter), 1);
                toggle.toggle(250);
                assertEquals(600, width(splitter), 1);
                tabs.setSelectedIndex(0);
                assertFalse(restore.get());
                toggle.toggle(450);
                assertEquals(450, width(splitter), 1);
            } finally {
                splitter.dispose();
            }
        });
    }

    @Test
    void remembersPixelsAcrossWindowResizeAndIgnoresUnlaidOutClicks() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var tabs = new JTabbedPane();
            tabs.addTab("Properties", new JPanel());
            var splitter = DesignerUI.createContentSplitter(new JPanel(), tabs);
            try {
                AtomicBoolean restore = new AtomicBoolean();
                var toggle = new PanelWidthToggle(splitter, tabs, restore::set);
                toggle.toggle(500);
                assertFalse(restore.get());
                splitter.setSize(1200, 600);
                splitter.doLayout();
                int original = width(splitter);
                toggle.toggle(500);
                splitter.setSize(1500, 600);
                splitter.doLayout();
                toggle.toggle(500);
                assertEquals(original, width(splitter), 1);
            } finally {
                splitter.dispose();
            }
        });
    }

    private static int width(com.intellij.ui.JBSplitter splitter) {
        return Math.round((splitter.getWidth() - splitter.getDividerWidth()) * (1 - splitter.getProportion()));
    }
}
