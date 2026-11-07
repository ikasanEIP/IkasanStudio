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

class ComponentPropertyFocusCommitTest {
    @Test void firstClickAppliesLiveNumericEditAcrossFocusTransfer() throws Exception {
        var row = new AtomicReference<ComponentPropertyEditRow>();
        var button = new JButton("Update Code");
        var updates = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            var meta = ComponentPropertyMeta.builder().propertyName("remotePort")
                    .propertyDataType(Integer.class).usageDataType("java.lang.Integer").mandatory(true).build();
            var edit = new ComponentPropertyEditRow(null, new ComponentProperty(meta, 2121), false, () -> {
                if (row.get() != null) button.setEnabled(row.get().propertyValueHasChanged() && row.get().doValidateAll().isEmpty());
            }, new HashMap<>());
            row.set(edit);
            button.addActionListener(e -> {
                assertEquals(2222, ((Number) row.get().updateValueObjectWithEnteredValues().getValue()).intValue());
                updates.incrementAndGet();
            });
            var field = edit.getOverridingInputField();
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
            var meta = ComponentPropertyMeta.builder().propertyName("remotePort")
                    .propertyDataType(Integer.class).usageDataType("java.lang.Integer").mandatory(true).build();
            var edit = new ComponentPropertyEditRow(null, new ComponentProperty(meta, 2121), false, () -> {}, new HashMap<>());
            var field = edit.getOverridingInputField();
            field.setText("invalid");
            KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(field,
                    new FocusEvent(field, FocusEvent.FOCUS_LOST, false, new JButton()));
            assertEquals("invalid", field.getText());
            assertFalse(edit.doValidateAll().isEmpty());
        });
    }
}
