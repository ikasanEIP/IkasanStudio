package org.ikasan.studio.ui;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.ui.component.canvas.CanvasPanel;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.component.palette.PaletteTabPanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesTabPanel;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.settings.IkasanStudioSettings;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UiContextTest {

    @Test
    void closingOwningDesignerClearsUiReferencesButRetainsModelState() {
        UiContext context = new UiContext(mock(Project.class));
        DesignerUI owningDesigner = mock(DesignerUI.class);
        DesignerUI staleDesigner = mock(DesignerUI.class);
        DesignerCanvas canvas = mock(DesignerCanvas.class);
        CanvasPanel canvasPanel = mock(CanvasPanel.class);
        ComponentPropertiesPanel propertiesPanel = mock(ComponentPropertiesPanel.class);
        PaletteTabPanel palettePanel = mock(PaletteTabPanel.class);

        context.setDesignerUI(owningDesigner);
        context.setDesignerCanvas(canvas);
        context.setCanvasPanel(canvasPanel);
        context.setPropertiesPanel(propertiesPanel);
        context.setPalettePanel(palettePanel);

        context.clearDesignerUI(staleDesigner);
        assertThat(context.getDesignerCanvas()).isSameAs(canvas);

        context.clearDesignerUI(owningDesigner);
        assertThat(context.getDesignerCanvas()).isNull();
        assertThat(context.getCanvasPanel()).isNull();
        assertThat(context.getPropertiesPanel()).isNull();
        assertThat(context.getPalettePanel()).isNull();
    }

    @Test
    void deletionResetsPropertiesToModuleAndSelectsPalette() {
        UiContext context = new UiContext(mock(Project.class));
        Module module = mock(Module.class);
        DesignerCanvas canvas = mock(DesignerCanvas.class);
        ComponentPropertiesTabPanel propertiesTab = mock(ComponentPropertiesTabPanel.class);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(UiContext.PROPERTIES_TAB_TITLE, new JPanel());
        tabs.addTab(UiContext.PALETTE_TAB_TITLE, new JPanel());
        when(module.isInitialised()).thenReturn(true);

        context.setIkasanModule(module);
        context.setDesignerCanvas(canvas);
        context.setPropertiesTabPanel(propertiesTab);
        context.setRightTabbedPane(tabs);
        context.resetSelectionAfterDeletion();

        assertThat(context.getSelectedComponent()).isSameAs(module);
        assertThat(tabs.getSelectedIndex()).isEqualTo(UiContext.PALETTE_TAB_INDEX);
        verify(canvas).setSelectedComponent(module);
        verify(propertiesTab).updateTargetComponent(module);
    }

    @Test
    void componentDistancePreservesExistingDecoratorClearanceGeometry() {
        int existingDistance = IkasanStudioSettings.DEFAULT_COMPONENT_DISTANCE;
        assertThat(UiContext.componentSpacingAfterTrailingDecoration(existingDistance, 0)).isEqualTo(30);
        assertThat(UiContext.componentSpacingAfterTrailingDecoration(existingDistance, 10)).isEqualTo(30);
        assertThat(UiContext.componentSpacingAfterTrailingDecoration(existingDistance, 40)).isEqualTo(55);
    }
}
