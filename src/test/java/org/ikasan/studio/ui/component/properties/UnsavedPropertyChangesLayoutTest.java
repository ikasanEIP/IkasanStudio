package org.ikasan.studio.ui.component.properties;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class UnsavedPropertyChangesLayoutTest {
    @Test
    void usesTheSameBoundedLayoutAndPreservesLongAndLiteralValues() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String oldValue = "java.lang.String";
            String newValue = "java.util.List<java.lang.String>";
            var content = UnsavedPropertyChangesDialog.buildContent("Unsaved edits for Broker", "MessageGenerator",
                    List.of(new UnsavedPropertyChangesDialog.PropertyChangeDetail("Output type", oldValue, newValue),
                            new UnsavedPropertyChangesDialog.PropertyChangeDetail("Description", "old", "Long text & <tags> ".repeat(100))));
            assertThat(content.getPreferredSize().width).isLessThan(com.intellij.util.ui.JBUI.scale(700));
            assertThat(content.getPreferredSize().height).isLessThanOrEqualTo(com.intellij.util.ui.JBUI.scale(320));
            content.setSize(content.getPreferredSize());
            for (int i = 0; i < 3; i++) layout(content);
            List<JTextArea> values = new ArrayList<>();
            collect(content, values);
            assertThat(values).extracting(JTextArea::getText).contains(oldValue, newValue, "Output type");
            JTextArea saved = values.stream().filter(t -> t.getText().equals(oldValue)).findFirst().orElseThrow();
            JTextArea edited = values.stream().filter(t -> t.getText().equals(newValue)).findFirst().orElseThrow();
            assertThat(saved.getX()).isEqualTo(edited.getX());
            assertThat(edited.getY()).isGreaterThan(saved.getY());
            assertThat(values).allMatch(t -> !t.isEditable() && t.getLineWrap());
        });
    }
    private static void layout(Container parent) {
        parent.doLayout();
        for (Component c : parent.getComponents()) if (c instanceof Container child) layout(child);
    }
    private static void collect(Container parent, List<JTextArea> values) {
        for (Component c : parent.getComponents()) {
            if (c instanceof JTextArea text) values.add(text);
            if (c instanceof Container child) collect(child, values);
        }
    }
}
