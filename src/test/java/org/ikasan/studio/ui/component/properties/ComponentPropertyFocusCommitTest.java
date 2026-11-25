package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusEvent;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;

class ComponentPropertyFocusCommitTest {
    @Test void firstClickAppliesLiveNumericEditAcrossFocusTransfer() throws Exception {
        AtomicReference<ComponentPropertyEditRow> row = new AtomicReference<>();
        JButton button = new JButton("Update Code");
        AtomicInteger updates = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            ComponentPropertyMeta meta = ComponentPropertyMeta.builder().propertyName("remotePort")
                    .propertyDataType(Integer.class).usageDataType("java.lang.Integer").mandatory(true).build();
            ComponentPropertyEditRow edit = new ComponentPropertyEditRow(null, new ComponentProperty(meta, 2121), false, () -> {
                if (row.get() != null) button.setEnabled(row.get().propertyValueHasChanged() && row.get().doValidateAll().isEmpty());
            }, new HashMap<>());
            row.set(edit);
            button.addActionListener(e -> {
                assertEquals(2222, ((Number) row.get().updateValueObjectWithEnteredValues().getValue()).intValue());
                updates.incrementAndGet();
            });
            JFormattedTextField field = edit.getOverridingInputField();
            field.setText("2222");
            assertTrue(button.isEnabled());
            // A mouse press arms the button before the edited field's focus-loss processing completes.
            button.getModel().setArmed(true);
            button.getModel().setPressed(true);
            KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(field,
                    new FocusEvent(field, FocusEvent.FOCUS_LOST, false, button));
        });
        SwingUtilities.invokeAndWait(() -> button.getModel().setPressed(false));
        assertEquals(1, updates.get(), "Focus transfer must not consume the first Update Code click");
    }

    @Test void invalidNumericTextRemainsVisibleAndInvalidAfterFocusTransfer() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ComponentPropertyMeta meta = ComponentPropertyMeta.builder().propertyName("remotePort")
                    .propertyDataType(Integer.class).usageDataType("java.lang.Integer").mandatory(true).build();
            ComponentPropertyEditRow edit = new ComponentPropertyEditRow(null, new ComponentProperty(meta, 2121), false, () -> {}, new HashMap<>());
            JFormattedTextField field = edit.getOverridingInputField();
            field.setText("invalid");
            KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(field,
                    new FocusEvent(field, FocusEvent.FOCUS_LOST, false, new JButton()));
            assertEquals("invalid", field.getText());
            assertFalse(edit.doValidateAll().isEmpty());
        });
    }
}
