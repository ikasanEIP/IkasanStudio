package org.ikasan.studio.intellij.settings;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IkasanStudioSettingsTest {
    @Test
    void newSettingsPreserveTheExistingCanvasDistances() {
        IkasanStudioSettings.State state = new IkasanStudioSettings.State();
        assertThat(state.componentDistance).isEqualTo(30);
        assertThat(state.flowDistance).isEqualTo(20);
    }

    @Test
    void invalidCanvasDistancesFallBackToCurrentDefaults() {
        assertThat(IkasanStudioSettings.normaliseCanvasDistance(-1, 30)).isEqualTo(30);
        assertThat(IkasanStudioSettings.normaliseCanvasDistance(251, 20)).isEqualTo(20);
        assertThat(IkasanStudioSettings.normaliseCanvasDistance(0, 30)).isZero();
        assertThat(IkasanStudioSettings.normaliseCanvasDistance(250, 20)).isEqualTo(250);
    }
}
