package org.ikasan.studio.ui.component.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The "Regenerate User-Implemented Class?" confirmation used to name the affected class as one line,
 * "flowName: ClassName.java" - reformatted (on request) into two aligned, labelled lines naming the flow and
 * the component's type, so a developer scanning several affected classes at a glance can tell which flow and
 * which kind of component each belongs to.
 */
class AffectedClassDescriptionFormatTest {

    @Test
    void namesTheFlowAndComponentTypeOnAlignedLines() {
        String description = ComponentPropertiesPanel.formatAffectedClassDescription(
                "flow 0 create an order", "Broker", "ArtificallyCreateOrder");

        assertThat(description).isEqualTo(
                "Flow:               flow 0 create an order\n" +
                "Component (Broker): ArtificallyCreateOrder");
    }

    @Test
    void doesNotAppendAJavaFileExtension() {
        String description = ComponentPropertiesPanel.formatAffectedClassDescription(
                "MyFlow", "Consumer", "MyGenericConsumer");

        assertThat(description).doesNotContain(".java");
    }

    /**
     * A longer component type (e.g. "Local File Consumer") pushes the "Component (...):" label past "Flow:"'s
     * own width - both lines' values must still start at the same column, not just when "Flow:" happens to be
     * the longer label.
     */
    @Test
    void alignsBothValueColumnsWhicheverLabelIsLonger() {
        String description = ComponentPropertiesPanel.formatAffectedClassDescription(
                "MyFlow", "Local File Consumer", "MyLocalFileConsumer");

        String[] lines = description.split("\n");
        assertThat(lines).hasSize(2);
        int flowValueStart = lines[0].indexOf("MyFlow");
        int componentValueStart = lines[1].indexOf("MyLocalFileConsumer");
        assertThat(flowValueStart).isEqualTo(componentValueStart);
    }
}
