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
    void sameLengthPortEditsPulseAndRestoringSavedValuesStopsAttention() throws Exception {
        var module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, java.util.List.of());
        module.setPort("12450");
        module.setH2DbPortNumber("12451");
        module.setH2WebPortNumber("12452");
        module.setPropertyValue(ComponentPropertyMeta.USE_EMBEDDED_H2, true);
        Project project = mock(Project.class);
        var context = mock(UiContext.class);
        when(project.getService(UiContext.class)).thenReturn(context);
        when(context.getIkasanModule()).thenReturn(module);
        SwingUtilities.invokeAndWait(() -> {
            var panel = new ComponentPropertiesPanel(project, false);
            try {
                panel.updateTargetComponent(module);
                for (String key : java.util.List.of("port", "h2DbPortNumber", "h2WebPortNumber")) {
                    var row = panel.getComponentPropertyEditBoxList().stream()
                            .filter(candidate -> key.equals(candidate.getPropertyKey())).findFirst().orElseThrow();
                    var field = row.getOverridingInputField();
                    String original = field.getText();
                    field.select(3, 4);
                    field.replaceSelection("6");
                    assertEquals(original.length(), field.getText().length());
                    assertTrue(panel.dataHasChangedAndOKToProcess(), key);
                    assertTrue(panel.updateCodeButton.isEnabled(), key + ": " + panel.updateCodeButton.getToolTipText());
                    assertNull(panel.updateCodeButton.getToolTipText(), "Valid port edits must not report a validation error");
                    assertNotNull(panel.updateCodeButton.getClientProperty("ikasan.studio.attentionPulseTimer"), key);
                    field.setText(original);
                    assertFalse(panel.dataHasChangedAndOKToProcess());
                    assertNull(panel.updateCodeButton.getClientProperty("ikasan.studio.attentionPulseTimer"));
                }
                assertEquals("12450", module.getPort());
                assertEquals("12451", module.getH2PortNumber());
                assertEquals("12452", module.getH2WebPortNumber());
            } finally { panel.dispose(); }
            assertNull(panel.updateCodeButton.getClientProperty("ikasan.studio.attentionPulseTimer"));
        });
    }

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
