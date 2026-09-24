package org.ikasan.studio.ui;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import static org.junit.jupiter.api.Assertions.*;

class SidebarVisibilityToggleTest {
    @Test
    void hidesWithoutLosingEditsAndRestoresWidthAfterResize() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel canvas = new JPanel();
            JTabbedPane tabs = new JTabbedPane();
            JTextField field = new JTextField("unsaved edit");
            tabs.addTab("Properties", field);
            tabs.addTab("Palette", new JPanel());
            var splitter = DesignerUI.createContentSplitter(canvas, tabs);
            try {
                splitter.setSize(1200, 600);
                splitter.doLayout();
                int width = tabs.getWidth();
                var toggle = new SidebarVisibilityToggle(splitter, tabs);
                toggle.button().doClick();
                splitter.doLayout();
                assertFalse(tabs.isVisible());
                assertEquals(1200, canvas.getWidth());
                splitter.setSize(1500, 600);
                splitter.doLayout();
                toggle.button().doClick();
                splitter.doLayout();
                assertTrue(tabs.isVisible());
                assertEquals(width, tabs.getWidth(), 1);
                assertEquals("unsaved edit", field.getText());
                assertEquals(0, tabs.getSelectedIndex());
                // Repeated collapse/restore also works after the editor becomes narrower.
                toggle.button().doClick();
                splitter.setSize(400, 600);
                splitter.doLayout();
                toggle.button().doClick();
                splitter.doLayout();
                assertTrue(canvas.getWidth() >= 190);
                assertTrue(tabs.getWidth() > 0);
            } finally {
                splitter.dispose();
            }
        });
    }
}
