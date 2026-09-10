package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.ui.StudioBundle;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class AffectedClassDescriptionFormatTest {
    @Test
    void completeDialogContentIsBoundedAndKeepsAlignedVisibleDetails() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var backup = new JCheckBox("Backup existing files", true);
            var content = RegenerateUserClassDialog.buildContent(
                    StudioBundle.message("message.ConfirmRegenerateUserImplementedClassNamed", "toType"),
                    List.of(new RegenerateUserClassDialog.AffectedClass("Filters Flow", "Broker", "MessageGenerator")), backup);
            assertThat(content.getPreferredSize().width).isLessThan(com.intellij.util.ui.JBUI.scale(700));
            content.setSize(content.getPreferredSize());
            for (int i = 0; i < 3; i++) layout(content);
            List<Component> all = new ArrayList<>();
            collect(content, all);
            var text = all.stream().filter(JTextArea.class::isInstance).map(JTextArea.class::cast).toList();
            assertThat(text).extracting(JTextArea::getText).contains("Filters Flow", "MessageGenerator");
            assertThat(text.get(0).getText()).contains("This will affect:", "toType");
            var flow = text.stream().filter(t -> t.getText().equals("Filters Flow")).findFirst().orElseThrow();
            var clazz = text.stream().filter(t -> t.getText().equals("MessageGenerator")).findFirst().orElseThrow();
            assertThat(flow.getX()).isEqualTo(clazz.getX());
            assertThat(flow.getWidth()).isPositive();
            assertThat(flow.getHeight()).isPositive();
            assertThat(clazz.getY()).isGreaterThan(flow.getY());
            assertThat(backup.isSelected()).isTrue();
            assertThat(all.stream().filter(JLabel.class::isInstance).map(JLabel.class::cast).map(JLabel::getText).toList())
                    .contains("Flow:", "Component (Broker):", "Continue?");
        });
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) if (child instanceof Container container) layout(container);
    }
    private static void collect(Container parent, List<Component> all) {
        for (Component child : parent.getComponents()) {
            all.add(child);
            if (child instanceof Container container) collect(container, all);
        }
    }
}
