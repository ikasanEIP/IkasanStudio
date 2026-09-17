package org.ikasan.studio.intellij.ai;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class StudioAiConnectionStatusTest {
    @Test void unverifiedNativeConnectionExplainsSetupThenNewChat() {
        assertThat(StudioAiConnectionStatus.nextStep(true, null, null, true, false)).isNull();
        assertThat(StudioAiConnectionStatus.nextStep(true, null, null, true, true)).isEqualTo("ai.NextNewChat");
    }
    @Test void manualRouteUsesClientNeutralGuidance() {
        assertThat(StudioAiConnectionStatus.nextStep(true, null, null, false, false)).isEqualTo("ai.NextConfigureManual");
        assertThat(StudioAiConnectionStatus.nextStep(true, null, null, false, true)).isEqualTo("ai.NextPasteManual");
    }
    @Test void successfulReadIsEvidenceForEitherTransport() {
        for (String transport : new String[]{"native", "adapter"})
            assertThat(StudioAiConnectionStatus.nextStep(true, null, transport, true, false)).isEqualTo("ai.NextVerified");
    }
    @Test void currentReadinessOverridesEarlierSuccess() {
        assertThat(StudioAiConnectionStatus.nextStep(true, "Pending edits", "native", true, true)).isEqualTo("ai.NextModel");
        assertThat(StudioAiConnectionStatus.nextStep(false, null, "native", true, true)).isEqualTo("ai.NextStopped");
    }
}
