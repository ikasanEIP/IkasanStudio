package org.ikasan.studio.ui.harness;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.ui.component.properties.ComponentPropertyEditRow;
import org.ikasan.studio.ui.component.properties.UnsavedPropertyChangesDialog;
import org.junit.jupiter.api.Test;

import org.ikasan.studio.ui.StudioUIUtils;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;

/**
 * Live coverage for the "Unsaved Property Changes" dialog enhancement: a details section always visible
 * (no expand/collapse) naming the changed properties with their old and new values, and a "Jump to
 * Properties" button (replacing the old plain Cancel) that switches the Studio editor to the Properties tab
 * without applying or discarding anything.
 */
public class UnsavedPropertyChangesDialogHarnessTest extends ComponentTestHarness {

    @Test
    void detailsAreVisibleImmediatelyShowingTheChangedPropertysOldAndNewValue() {
        UIUtil.invokeAndWaitIfNeeded(() -> {
            ComponentPropertiesPanel panel = new ComponentPropertiesPanel(getProject(), false);
            try {
                FlowElement broker = TestFixtures.getBroker(BASE_META_PACK);
                broker.setComponentName("My Named Broker");

                panel.updateTargetComponent(broker);

                List<ComponentPropertyEditRow> rows = panel.getComponentPropertyEditBoxList();
                ComponentPropertyEditRow row = rows.stream()
                        .filter(r -> "userImplementedClassName".equals(r.getMeta().getPropertyName()))
                        .findFirst().orElseThrow();
                String oldValue = String.valueOf(row.getComponentProperty().getValue());
                row.getOverridingInputField().setText("myEditedBrokerClassName");

                List<UnsavedPropertyChangesDialog.PropertyChangeDetail> details = invokeGetChangedPropertyDetails(panel);
                assertThat(details).anySatisfy(d -> {
                    assertThat(d.oldValueDisplay()).isEqualTo(oldValue);
                    assertThat(d.newValueDisplay()).isEqualTo("myEditedBrokerClassName");
                });

                UnsavedPropertyChangesDialog dialog = new UnsavedPropertyChangesDialog(getProject(),
                        "Unsaved Property Changes", "'My Named Broker' has unsaved changes to: User Implemented Class Name.",
                        "My Named Broker", details);
                try {
                    JComponent center = invokeCreateCenterPanel(dialog);

                    assertThat(collectLabelTexts(center)).anyMatch(text -> text.contains(oldValue));
                    assertThat(collectLabelTexts(center)).anyMatch(text -> text.contains("myEditedBrokerClassName"));
                } finally {
                    disposeDialog(dialog);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                stopAnyAttentionPulse(panel);
            }
        });
    }

    @Test
    void eachButtonActionReportsTheRightChoice() {
        UIUtil.invokeAndWaitIfNeeded(() -> {
            UnsavedPropertyChangesDialog dialog = new UnsavedPropertyChangesDialog(getProject(),
                    "Unsaved Property Changes", "'X' has unsaved changes to: Y.", "X", List.of());
            try {
                Action[] actions = invokeCreateActions(dialog);
                assertThat(actions).hasSize(3);

                silentlyInvoke(actions[0]);
                assertThat(dialog.getChoice()).isEqualTo(UnsavedPropertyChangesDialog.Choice.APPLY);

                silentlyInvoke(actions[1]);
                assertThat(dialog.getChoice()).isEqualTo(UnsavedPropertyChangesDialog.Choice.DISCARD);

                silentlyInvoke(actions[2]);
                assertThat(dialog.getChoice()).isEqualTo(UnsavedPropertyChangesDialog.Choice.JUMP_TO_PROPERTIES);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                disposeDialog(dialog);
            }
        });
    }

    @Test
    void jumpToPropertiesSwitchesTheRightTabbedPaneToThePropertiesTabEvenIfOpeningTheEditorFails() {
        UIUtil.invokeAndWaitIfNeeded(() -> {
            Project project = getProject();
            UiContext uiContext = project.getService(UiContext.class);
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.addTab("Properties", new JPanel());
            tabbedPane.addTab("Palette", new JPanel());
            tabbedPane.setSelectedIndex(UiContext.PALETTE_TAB_INDEX);
            uiContext.setRightTabbedPane(tabbedPane);

            ComponentPropertiesPanel panel = new ComponentPropertiesPanel(project, false);
            try {
                Method jump = ComponentPropertiesPanel.class.getDeclaredMethod("jumpToPropertiesForCurrentSelection");
                jump.setAccessible(true);
                jump.invoke(panel);
            } catch (Exception e) {
                // This light test fixture's fake FileEditorManager can't actually open the Studio virtual file
                // (IkasanStudioEditorServiceStateTest needs a HeavyPlatformTestCase for that) - jump switches
                // the tab first specifically so that part still lands even if opening the editor itself fails,
                // which is exactly what's being confirmed below.
            }

            assertThat(tabbedPane.getSelectedIndex()).isEqualTo(UiContext.PROPERTIES_TAB_INDEX);
        });
    }

    @SuppressWarnings("unchecked")
    private static List<UnsavedPropertyChangesDialog.PropertyChangeDetail> invokeGetChangedPropertyDetails(
            ComponentPropertiesPanel panel) throws Exception {
        Method method = ComponentPropertiesPanel.class.getDeclaredMethod("getChangedPropertyDetails");
        method.setAccessible(true);
        return (List<UnsavedPropertyChangesDialog.PropertyChangeDetail>) method.invoke(panel);
    }

    private static JComponent invokeCreateCenterPanel(UnsavedPropertyChangesDialog dialog) throws Exception {
        Method method = UnsavedPropertyChangesDialog.class.getDeclaredMethod("createCenterPanel");
        method.setAccessible(true);
        return (JComponent) method.invoke(dialog);
    }

    private static Action[] invokeCreateActions(UnsavedPropertyChangesDialog dialog) throws Exception {
        Method method = UnsavedPropertyChangesDialog.class.getDeclaredMethod("createActions");
        method.setAccessible(true);
        return (Action[]) method.invoke(dialog);
    }

    /**
     * close() needs a realised dialog peer this headless construction never shows - the choice field is set
     * before close() runs, so the assertion under test already holds even if close() itself is a no-op/throws.
     */
    private static void silentlyInvoke(Action action) {
        try {
            action.actionPerformed(null);
        } catch (Exception ignored) {
            // Expected in this headless construction - see the javadoc above.
        }
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        return field.get(target);
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

    /**
     * Editing userImplementedClassName above can flag a validation issue elsewhere on the panel, which starts a
     * real javax.swing.Timer pulsing the "Update Code" button's border (StudioUIUtils#setAttentionPulse) - this
     * test never reaches the normal refresh path that would stop it again, so stop it explicitly to avoid
     * leaking the Timer past this test.
     */
    private static void stopAnyAttentionPulse(ComponentPropertiesPanel panel) {
        try {
            JButton updateCodeButton = (JButton) getField(panel, "updateCodeButton");
            StudioUIUtils.setAttentionPulse(updateCodeButton, false);
        } catch (Exception ignored) {
            // Best-effort cleanup only.
        }
    }

    private static void disposeDialog(UnsavedPropertyChangesDialog dialog) {
        try {
            Method close = com.intellij.openapi.ui.DialogWrapper.class.getDeclaredMethod("dispose");
            close.setAccessible(true);
            close.invoke(dialog);
        } catch (Exception ignored) {
            // Best-effort cleanup only.
        }
    }

    private static List<String> collectLabelTexts(Container root) {
        List<String> texts = new ArrayList<>();
        Deque<Component> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Component c = stack.pop();
            if (c instanceof javax.swing.JLabel label && label.getText() != null) {
                texts.add(label.getText());
            }
            if (c instanceof Container container) {
                for (Component child : container.getComponents()) {
                    stack.push(child);
                }
            }
        }
        return texts;
    }
}
