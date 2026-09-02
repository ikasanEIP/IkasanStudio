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
    void resetButtonRestoresBothCurrentCanvasLayoutDefaults() {
        IkasanStudioSettingsConfigurable configurable = new IkasanStudioSettingsConfigurable();
        JComponent settings = configurable.createComponent();
        assertThat(settings).isNotNull();
        List<JSpinner> spinners = descendantsOfType(settings, JSpinner.class);
        assertThat(spinners).hasSize(2);

        spinners.get(0).setValue(80);
        spinners.get(1).setValue(90);
        configurable.resetCanvasDistancesToDefaults();

        assertThat(spinners.get(0).getValue()).isEqualTo(IkasanStudioSettings.DEFAULT_COMPONENT_DISTANCE);
        assertThat(spinners.get(1).getValue()).isEqualTo(IkasanStudioSettings.DEFAULT_FLOW_DISTANCE);
        configurable.disposeUIResources();
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
