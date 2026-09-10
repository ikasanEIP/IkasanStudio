package org.ikasan.studio.ui.component.properties;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ComponentInputEnterTest {
    private static void pressEnter(JFormattedTextField field) {
        Object binding = field.getInputMap().get(KeyStroke.getKeyStroke("ENTER"));
        Action action = field.getActionMap().get(binding);
        assertNotNull(action);
        action.actionPerformed(new ActionEvent(field, ActionEvent.ACTION_PERFORMED, ""));
    }

    @Test
    void enterInvokesExistingUpdateActionOnceForTextAndNumericEdits() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (JFormattedTextField field : new JFormattedTextField[]{
                    new JFormattedTextField(), new JFormattedTextField(NumberFormat.getIntegerInstance())}) {
                var updates = new AtomicInteger();
                var button = new JButton();
                button.addActionListener(e -> {
                    assertSame(button, e.getSource());
                    assertEquals("42", field.getText());
                    updates.incrementAndGet();
                });
                new ComponentInput(field).applyOnEnter(button);
                field.setText("42");
                pressEnter(field);
                assertEquals(1, updates.get());
            }
        });
    }

    @Test
    void disabledUpdateAndReadOnlyFieldsDoNotSubmit() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var field = new JFormattedTextField();
            var button = new JButton();
            var updates = new AtomicInteger();
            button.addActionListener(e -> updates.incrementAndGet());
            new ComponentInput(field).applyOnEnter(button);
            field.setText("changed");
            button.setEnabled(false);
            pressEnter(field);
            button.setEnabled(true);
            field.setEditable(false);
            pressEnter(field);
            assertEquals(0, updates.get());
        });
    }

    @Test
    void popupWithoutUpdateButtonRetainsItsDefaultEnterBehavior() {
        var field = new JFormattedTextField();
        Object originalBinding = field.getInputMap().get(KeyStroke.getKeyStroke("ENTER"));
        new ComponentInput(field).applyOnEnter(null);
        assertEquals(0, field.getActionListeners().length);
        assertEquals(originalBinding, field.getInputMap().get(KeyStroke.getKeyStroke("ENTER")));
    }
}
