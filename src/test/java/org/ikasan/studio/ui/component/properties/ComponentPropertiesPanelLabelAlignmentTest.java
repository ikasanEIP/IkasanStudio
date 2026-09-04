package org.ikasan.studio.ui.component.properties;

import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;

/**
 * SFTP Producer's Mandatory Properties mixes flat rows (componentName, remoteHost, outputDirectory) with
 * mandatorySectionHeading sub-panels ("Provide password or" holding password; "Provide key and hosts" holding
 * privateKeyFilename and knownHostFilename) - see ComponentPropertiesPanel#populatePropertiesEditorPanel.
 * -
 * Two separate defects made the sub-panels' fields not line up with the flat rows', both reported by the user
 * with annotated screenshots:
 * <ol>
 *   <li>each sub-panel went in at gridx=0 with the default gridwidth of 1, confining a whole-row panel to the
 *       label column - so it could never reach the panel's right edge, and (having weightx=1) it inflated that
 *       label column to its own width, pushing every flat row's field far right;</li>
 *   <li>each sub-panel is its own GridBagLayout, so its label column was sized only to its own widest label -
 *       "Password" alone gave a narrower column than "Private Key Filename"/"Known Hosts Filename" next to it.</li>
 * </ol>
 * Fixed by gridwidth=REMAINDER and ComponentPropertiesPanel#alignPropertyLabelColumnWidths respectively. This
 * reproduces the real panel structure - outer GridBagLayout, flat rows, and titled sub-panels spanning the full
 * row - without needing a full ComponentPropertiesPanel/Project to construct.
 */
class ComponentPropertiesPanelLabelAlignmentTest {

    /** Wide enough that the field column has real space to stretch into, like the actual dialog. */
    private static final int PANEL_WIDTH = 460;

    /**
     * A grouped field can only stretch as far as its own box's inner edge, so its right edge stays this much
     * short of a flat row's at most - the titled border's own line and padding, not a layout defect.
     */
    private static final int MAX_GROUP_BORDER_RIGHT_INSET = 6;

    private static ComponentMeta sftpProducerMeta;

    @BeforeAll
    static void loadSftpProducerMeta() throws Exception {
        sftpProducerMeta = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "SFTP Producer");
    }

    @Test
    void everyMandatoryFieldSharesTheSameLeftAndRightEdgeWhicheverPanelItLivesIn() {
        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(sftpProducerMeta)
                .componentName("My SFTP Producer")
                .build();
        Map<String, ComponentPropertyEditRow> sharedRowMap = new HashMap<>();

        JBPanel<?> mandatoryPanel = new JBPanel<>(new GridBagLayout());
        GridBagConstraints rowGc = new GridBagConstraints();
        rowGc.fill = GridBagConstraints.HORIZONTAL;
        rowGc.insets = JBUI.insets(3, 4);

        // Mirrors populatePropertiesEditorPanel's own mandatoryHeadingGc, including the gridwidth fix.
        GridBagConstraints headingGc = new GridBagConstraints();
        headingGc.fill = GridBagConstraints.HORIZONTAL;
        headingGc.insets = JBUI.insets(3, 0);
        headingGc.gridx = 0;
        headingGc.gridwidth = GridBagConstraints.REMAINDER;
        headingGc.weightx = 1;

        int gridy = 0;
        List<ComponentPropertyEditRow> allRows = new ArrayList<>();
        List<ComponentPropertiesPanel.LabelAlignmentGroup> groups = new ArrayList<>();

        // Flat rows, added straight into the section panel - no container indent of their own.
        List<ComponentPropertyEditRow> flatRows = new ArrayList<>();
        for (String propertyName : List.of("componentName", "remoteHost")) {
            ComponentPropertyEditRow row = rowFor(flowElement, propertyName, sharedRowMap);
            addFlatRow(mandatoryPanel, row, rowGc, gridy++);
            flatRows.add(row);
        }

        // "Provide password or" - a single-property heading sub-panel, its own independent GridBagLayout.
        List<ComponentPropertyEditRow> passwordGroup = List.of(rowFor(flowElement, "password", sharedRowMap));
        gridy = addHeadingGroup(mandatoryPanel, "Provide password or", passwordGroup, headingGc, rowGc, gridy, groups);

        // "Provide key and hosts" - a two-property heading sub-panel, likewise independent.
        List<ComponentPropertyEditRow> keyGroup = List.of(
                rowFor(flowElement, "privateKeyFilename", sharedRowMap),
                rowFor(flowElement, "knownHostFilename", sharedRowMap));
        gridy = addHeadingGroup(mandatoryPanel, "Provide key and hosts", keyGroup, headingGc, rowGc, gridy, groups);

        // Flat row below the groups.
        ComponentPropertyEditRow outputDirectoryRow = rowFor(flowElement, "outputDirectory", sharedRowMap);
        addFlatRow(mandatoryPanel, outputDirectoryRow, rowGc, gridy);
        flatRows.add(outputDirectoryRow);

        groups.add(new ComponentPropertiesPanel.LabelAlignmentGroup(flatRows, 0));
        allRows.addAll(flatRows);
        allRows.addAll(passwordGroup);
        allRows.addAll(keyGroup);

        ComponentPropertiesPanel.alignPropertyLabelColumnWidths(groups);
        layoutAt(mandatoryPanel, PANEL_WIDTH);

        int[] reference = fieldBoundsIn(flatRows.get(0), mandatoryPanel);
        assertThat(reference[0]).isGreaterThan(0);
        assertThat(reference[1]).isGreaterThan(reference[0]);
        for (ComponentPropertyEditRow row : allRows) {
            int[] bounds = fieldBoundsIn(row, mandatoryPanel);
            assertThat(bounds[0])
                    .as("left edge of '%s' field", row.getMeta().getPropertyName())
                    .isEqualTo(reference[0]);
            // The right edge of a grouped field stops at its own box's border rather than the section edge - that
            // remaining gap is exactly the titled border's right inset, nothing more.
            assertThat(reference[1] - bounds[1])
                    .as("right edge of '%s' field, relative to a flat row's", row.getMeta().getPropertyName())
                    .isBetween(0, MAX_GROUP_BORDER_RIGHT_INSET);
        }
    }

    private static ComponentPropertyEditRow rowFor(FlowElement flowElement, String propertyName,
                                                     Map<String, ComponentPropertyEditRow> sharedRowMap) {
        ComponentProperty existing = flowElement.getProperty(propertyName);
        ComponentProperty property = existing != null ? existing
                : new ComponentProperty(flowElement.getComponentMeta().getMetadata(propertyName));
        return new ComponentPropertyEditRow(null, property, false, () -> { }, sharedRowMap);
    }

    /**
     * Reproduces ComponentPropertiesPanel#addLabelAndParamInput's row layout: label at gridx=0, field at gridx=2
     * (gridx=1 holds optional aux widgets - a cron builder button and the like - which none of these fields have).
     */
    private static void addFlatRow(JBPanel<?> panel, ComponentPropertyEditRow row, GridBagConstraints gc, int gridy) {
        gc.gridwidth = 1;
        gc.weightx = 0.0;
        gc.gridx = 0;
        gc.gridy = gridy;
        panel.add(row.getPropertyTitleField(), gc);
        gc.gridx = 2;
        gc.weightx = 1.0;
        panel.add(row.getInputField().getFirstFocusComponent(), gc);
    }

    /** Reproduces ComponentPropertiesPanel#flushMandatoryHeadingGroup, titled border and indent included. */
    private static int addHeadingGroup(JBPanel<?> parent, String heading, List<ComponentPropertyEditRow> rows,
                                       GridBagConstraints headingGc, GridBagConstraints rowGc, int gridy,
                                       List<ComponentPropertiesPanel.LabelAlignmentGroup> groups) {
        JBPanel<?> headingPanel = new JBPanel<>(new GridBagLayout());
        int innerGridy = 0;
        for (ComponentPropertyEditRow row : rows) {
            addFlatRow(headingPanel, row, rowGc, innerGridy++);
        }
        headingPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), heading, TitledBorder.LEFT, TitledBorder.TOP));
        headingGc.gridy = gridy;
        parent.add(headingPanel, headingGc);
        int indent = headingGc.insets.left + headingPanel.getBorder().getBorderInsets(headingPanel).left;
        groups.add(new ComponentPropertiesPanel.LabelAlignmentGroup(rows, indent));
        return gridy + 1;
    }

    /** @return {leftEdge, rightEdge} of this row's input field, in {@code ancestor}'s coordinate space. */
    private static int[] fieldBoundsIn(ComponentPropertyEditRow row, Container ancestor) {
        Component field = row.getInputField().getFirstFocusComponent();
        assertThat(field).as("'%s' has a focus component to measure", row.getMeta().getPropertyName()).isNotNull();
        int left = 0;
        for (Component current = field; current != null && current != ancestor; current = current.getParent()) {
            left += current.getX();
        }
        return new int[]{left, left + field.getWidth()};
    }

    private static void layoutAt(Container container, int width) {
        container.setSize(width, container.getPreferredSize().height);
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container childContainer) {
                childContainer.doLayout();
                for (Component grandChild : childContainer.getComponents()) {
                    if (grandChild instanceof Container grandChildContainer) {
                        grandChildContainer.doLayout();
                    }
                }
            }
        }
    }
}
