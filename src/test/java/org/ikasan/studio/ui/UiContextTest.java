package org.ikasan.studio.ui;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.ui.component.canvas.CanvasPanel;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.component.palette.PaletteTabPanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
}
