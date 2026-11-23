package org.ikasan.studio.ui;

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.HeavyPlatformTestCase;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;

public class DesignerPropertiesTabHeavyTest extends HeavyPlatformTestCase {
    public void testClickingAlreadySelectedModuleReopensPropertiesFromPalette() throws Exception {
        var context = myProject.getService(UiContext.class);
        context.setViewHandlerFactory(new org.ikasan.studio.ui.viewmodel.ViewHandlerCache(myProject));
        var module = org.ikasan.studio.core.TestFixtures.getMyFirstModuleIkasanModule(
                org.ikasan.studio.core.TestFixtures.BASE_META_PACK, java.util.List.of());
        context.setIkasanModule(module);
        var tabs = new javax.swing.JTabbedPane();
        tabs.addTab(UiContext.PROPERTIES_TAB_TITLE, new JPanel());
        tabs.addTab(UiContext.PALETTE_TAB_TITLE, new JPanel());
        tabs.setSelectedIndex(UiContext.PALETTE_TAB_INDEX);
        context.setRightTabbedPane(tabs);
        var properties = org.mockito.Mockito.mock(
                org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel.class);
        org.mockito.Mockito.when(properties.isEditingComponent(module)).thenReturn(true);
        context.setPropertiesPanel(properties);
        var canvas = new org.ikasan.studio.ui.component.canvas.DesignerCanvas(myProject);
        try {
            canvas.setSelectedComponent(module);
            canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                    0, 20, 20, 1, false, MouseEvent.BUTTON1));
            assertEquals(UiContext.PROPERTIES_TAB_INDEX, tabs.getSelectedIndex());
            org.mockito.Mockito.verify(properties, org.mockito.Mockito.never())
                    .updateTargetComponent(org.mockito.ArgumentMatchers.any());
        } finally {
            canvas.disposeCanvas();
            context.getViewHandlerFactory().clear();
            context.setPropertiesPanel(null);
        }
    }

    public void testPropertiesTabRemainsClickableAfterCopySupportIsInstalled() {
        var designer = new DesignerUI(myProject);
        Disposer.register(getTestRootDisposable(), designer);
        var tabs = designer.paletteAndProperties;
        tabs.addTab(UiContext.PALETTE_TAB_TITLE, new JPanel());
        tabs.setSelectedIndex(UiContext.PALETTE_TAB_INDEX);
        tabs.setSize(400, 500);
        tabs.doLayout();
        var bounds = tabs.getBoundsAt(0);
        int x = bounds.x + bounds.width / 2, y = bounds.y + bounds.height / 2;
        var header = tabs.getTabComponentAt(0);
        if (header != null) {
            for (var listener : header.getMouseListeners()) {
                assertNotSame("Copy support must not intercept the platform tab header",
                        StudioTextCopySupport.class, listener.getClass().getEnclosingClass());
            }
        }
        // AWT routes an uninterested header label's press to its tabbed-pane ancestor.
        tabs.dispatchEvent(new MouseEvent(tabs, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                0, x, y, 1, false, MouseEvent.BUTTON1));
        assertEquals(UiContext.PROPERTIES_TAB_INDEX, tabs.getSelectedIndex());
    }
}
