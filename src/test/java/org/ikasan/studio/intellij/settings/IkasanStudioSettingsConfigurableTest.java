package org.ikasan.studio.intellij.settings;

import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IkasanStudioSettingsConfigurableTest {
    @Test
    void explanatoryNotesUseIntellijResponsiveWrapping() {
        IkasanStudioSettingsConfigurable configurable = new IkasanStudioSettingsConfigurable();
        JComponent settings = configurable.createComponent();
        assertThat(settings).isNotNull();

        // Settings uses this width for its horizontal viewport. An unwrapped label
        // therefore creates a scrollbar even when its minimum width is small.
        assertThat(settings.getPreferredSize().width).isLessThan(800);
        configurable.disposeUIResources();
    }

    @Test
    void resetButtonRestoresAllFourCurrentCanvasLayoutDefaults() {
        IkasanStudioSettingsConfigurable configurable = new IkasanStudioSettingsConfigurable();
        JComponent settings = configurable.createComponent();
        assertThat(settings).isNotNull();
        List<JSpinner> spinners = descendantsOfType(settings, JSpinner.class);
        assertThat(spinners).hasSize(4);

        for (JSpinner spinner : spinners) {
            spinner.setValue(1);
        }
        configurable.resetCanvasDistancesToDefaults();

        assertThat(spinners.get(0).getValue()).isEqualTo(IkasanStudioSettings.DEFAULT_COMPONENT_DISTANCE);
        assertThat(spinners.get(1).getValue()).isEqualTo(IkasanStudioSettings.DEFAULT_FLOW_DISTANCE);
        assertThat(spinners.get(2).getValue()).isEqualTo(IkasanStudioSettings.DEFAULT_FLOW_X_START_POINT);
        assertThat(spinners.get(3).getValue()).isEqualTo(IkasanStudioSettings.DEFAULT_FLOW_Y_START_POINT);
        configurable.disposeUIResources();
    }

    /**
     * The whole point of the GridBagLayout rewrite: every field should start at the same x regardless of its
     * own label's length (e.g. "Distance between components:" vs the much shorter "Distance between flows:"),
     * matching the aligned look of IntelliJ's own settings pages rather than each row packing its field
     * immediately after its own label.
     */
    @Test
    void canvasLayoutFieldsAlignIntoOneColumn() {
        IkasanStudioSettingsConfigurable configurable = new IkasanStudioSettingsConfigurable();
        JComponent settings = configurable.createComponent();
        assertThat(settings).isNotNull();
        // This panel is never added to a window, so nothing has ever assigned its descendants real bounds -
        // validate() alone was observed not to cascade in that state, so lay out every container in the tree
        // explicitly, top-down, using each one's own preferred size.
        layoutRecursively(settings);

        List<JSpinner> spinners = descendantsOfType(settings, JSpinner.class);
        assertThat(spinners).hasSize(4);
        // All four spinners share the same immediate parent panel, so their x is directly comparable.
        int expectedX = spinners.get(0).getX();
        assertThat(expectedX).isGreaterThan(0);
        assertThat(spinners).allSatisfy(spinner -> assertThat(spinner.getX()).isEqualTo(expectedX));
        configurable.disposeUIResources();
    }


    @Test
    void settingsNotesStayWithinTheirContainersAtNarrowAndWideEditorWidths() {
        for (int width : List.of(420, 1100)) {
            IkasanStudioSettingsConfigurable configurable = new IkasanStudioSettingsConfigurable();
            JComponent settings = configurable.createComponent();
            settings.setSize(width, settings.getPreferredSize().height);
            layoutUsingAssignedSizes(settings);

            List<javax.swing.JLabel> wrappingNotes = descendantsOfType(settings, javax.swing.JLabel.class).stream()
                    .filter(label -> label.getText() != null && label.getText().startsWith("<html>"))
                    .toList();
            assertThat(wrappingNotes).hasSizeGreaterThanOrEqualTo(7);
            assertThat(wrappingNotes).allSatisfy(label ->
                    assertThat(label.getWidth()).isLessThanOrEqualTo(label.getParent().getWidth()));
            configurable.disposeUIResources();
        }
    }

    private static void layoutUsingAssignedSizes(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container childContainer) {
                layoutUsingAssignedSizes(childContainer);
            }
        }
    }

    private static void layoutRecursively(Container container) {
        container.setSize(container.getPreferredSize());
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container childContainer) {
                layoutRecursively(childContainer);
            }
        }
    }

    private static <T extends Component> List<T> descendantsOfType(Container root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                matches.add(type.cast(component));
            }
            if (component instanceof Container container) {
                matches.addAll(descendantsOfType(container, type));
            }
        }
        return matches;
    }
}
