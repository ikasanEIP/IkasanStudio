package org.ikasan.studio.ui.harness;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertyEditRow;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;

/**
 * Live repro for a reported gap: the "Unsaved Property Changes" dialog (shown when switching selection or
 * launching with unsaved edits - ComponentPropertiesPanel#confirmSelectionChangeWithPendingEdits /
 * #preparePendingChangesForLaunch) gave no indication of which component or which property had actually
 * changed, forcing the developer to guess or cancel and go look. describePendingEditsForDialog() now names
 * both directly in the dialog text; this drives it through a real properties panel and an actual field edit
 * rather than asserting on the helper in isolation.
 */
public class UnsavedChangesDialogNamesComponentAndPropertyHarnessTest extends ComponentTestHarness {

    @Test
    void dialogTextNamesTheChangedComponentAndProperty() throws Exception {
        FlowElement broker = TestFixtures.getBroker(BASE_META_PACK);
        broker.setComponentName("My Named Broker");

        ComponentPropertiesPanel panel = new ComponentPropertiesPanel(getProject(), false);
        try {
            panel.updateTargetComponent(broker);

            List<ComponentPropertyEditRow> rows = panel.getComponentPropertyEditBoxList();
            ComponentPropertyEditRow userImplementedClassNameRow = rows.stream()
                    .filter(row -> "userImplementedClassName".equals(row.getMeta().getPropertyName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Broker's userImplementedClassName row was not found"));

            assertThat(userImplementedClassNameRow.propertyValueHasChanged())
                    .as("sanity check: nothing has been edited yet")
                    .isFalse();

            userImplementedClassNameRow.getOverridingInputField().setText("myEditedBrokerClassName");

            assertThat(userImplementedClassNameRow.propertyValueHasChanged())
                    .as("the edited row should now report a change")
                    .isTrue();
            assertThat(panel.dataHasChangedAndOKToProcess()).isTrue();

            Method describeMethod = ComponentPropertiesPanel.class.getDeclaredMethod("describePendingEditsForDialog");
            describeMethod.setAccessible(true);
            String[] componentAndProperties = (String[]) describeMethod.invoke(panel);

            assertThat(componentAndProperties[0])
                    .as("dialog should name the component being edited")
                    .isEqualTo("My Named Broker");
            assertThat(componentAndProperties[1])
                    .as("dialog should name the specific property that changed")
                    .contains(userImplementedClassNameRow.getMeta().getDisplayLabel() != null
                            ? userImplementedClassNameRow.getMeta().getDisplayLabel()
                            : "userImplementedClassName");
        } finally {
            // A validation-triggered attention pulse (StudioUIUtils#setAttentionPulse) starts a real
            // javax.swing.Timer on the "Update Code" button and stops only when re-invoked with active=false -
            // this test never reaches that normal refresh path, so stop it explicitly to avoid leaking the
            // Timer past this test (PropertiesPanel#dispose() is a no-op here, it doesn't touch this timer).
            Field updateCodeButtonField = findField(panel.getClass(), "updateCodeButton");
            updateCodeButtonField.setAccessible(true);
            JButton updateCodeButton = (JButton) updateCodeButtonField.get(panel);
            StudioUIUtils.setAttentionPulse(updateCodeButton, false);
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // keep walking up to the declaring superclass
            }
        }
        throw new NoSuchFieldException(name);
    }
}
