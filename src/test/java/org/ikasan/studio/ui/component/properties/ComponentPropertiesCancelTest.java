package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.ui.UiContext;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComponentPropertiesCancelTest {
    @Test
    void cancelRestoresInvalidEditsWithoutChangingTheSelectedModel() throws Exception {
        var component = TestFixtures.getDevNullProducer(TestFixtures.BASE_META_PACK);
        String savedName = component.getComponentName();
        Project project = mock(Project.class);
        when(project.getService(UiContext.class)).thenReturn(mock(UiContext.class));
        SwingUtilities.invokeAndWait(() -> {
            var panel = new ComponentPropertiesPanel(project, false);
            try {
                panel.updateTargetComponent(component);
                JButton cancel = java.util.Arrays.stream(panel.footerPanel.getComponents())
                        .filter(c -> c instanceof JButton b && "Cancel".equals(b.getText()))
                        .map(JButton.class::cast).findFirst().orElseThrow();
                assertFalse(cancel.isEnabled());
                var name = panel.getComponentPropertyEditBoxList().stream()
                        .filter(row -> "componentName".equals(row.getPropertyKey())).findFirst().orElseThrow();
                name.getOverridingInputField().setText("");
                assertTrue(cancel.isEnabled(), "invalid edits must still be cancellable");
                assertFalse(panel.updateCodeButton.isEnabled());
                cancel.doClick(0);
                assertSame(component, panel.getSelectedComponent());
                assertEquals(savedName, component.getComponentName());
                var restored = panel.getComponentPropertyEditBoxList().stream()
                        .filter(row -> "componentName".equals(row.getPropertyKey())).findFirst().orElseThrow();
                assertEquals(savedName, restored.getOverridingInputField().getText());
                assertFalse(panel.dataHasChangedAndOKToProcess());
                assertFalse(cancel.isEnabled());
                assertFalse(panel.updateCodeButton.isEnabled());
            } finally {
                panel.dispose();
            }
        });
    }

    @Test
    void clearingOptionalValuesIsAnUncommittedEdit() {
        var property = new ComponentProperty(ComponentPropertyMeta.builder().propertyName("optional").build(), "saved");
        var row = new ComponentPropertyEditRow(null, property, false, () -> {}, new HashMap<>());
        row.resetDataEntryComponentsWithNewValues();
        row.clearValue();
        assertTrue(row.propertyValueHasChanged());
        assertEquals("saved", property.getValue());
        row.resetDataEntryComponentsWithNewValues();
        assertEquals("saved", row.getOverridingInputField().getText());
        assertFalse(row.propertyValueHasChanged());
    }
}
