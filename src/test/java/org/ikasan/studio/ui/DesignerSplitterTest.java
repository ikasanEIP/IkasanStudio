package org.ikasan.studio.ui;

import com.intellij.ui.scale.JBUIScale;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesignerSplitterTest {
    @Test
    void sidebarRetainsItsWidthWhenCanvasResizesAtDifferentScales() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            float originalScale = JBUIScale.scale(1f);
            try {
                for (float scale : new float[]{1f, 1.5f, 2f}) {
                    JBUIScale.setUserScaleFactorForTest(scale);
                    JPanel canvas = new JPanel();
                    JPanel sidebar = new JPanel();
                    var splitter = DesignerUI.createContentSplitter(canvas, sidebar);
                    splitter.setSize(1000, 600);
                    splitter.doLayout();
                    int sidebarWidth = sidebar.getWidth();
                    int canvasWidth = canvas.getWidth();
                    assertTrue(sidebarWidth > 0);

                    splitter.setSize(1400, 600);
                    splitter.doLayout();
                    assertEquals(sidebarWidth, sidebar.getWidth(), 1);
                    assertEquals(canvasWidth + 400, canvas.getWidth(), 1);

                    splitter.setSize(800, 600);
                    splitter.doLayout();
                    assertEquals(sidebarWidth, sidebar.getWidth(), 1);
                    assertEquals(canvasWidth - 200, canvas.getWidth(), 1);
                    splitter.dispose();
                }
            } finally {
                JBUIScale.setUserScaleFactorForTest(originalScale);
            }
        });
    }
}
