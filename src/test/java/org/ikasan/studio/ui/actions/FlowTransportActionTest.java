package org.ikasan.studio.ui.actions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.ui.actions.FlowTransportAction.*;

class FlowTransportActionTest {

    @Test
    void runningFlowCanBeStoppedOrPaused() {
        assertThat(isEnabledFor(STOP, "running")).isTrue();
        assertThat(isEnabledFor(PAUSE, "running")).isTrue();
        assertThat(isEnabledFor(START, "running")).isFalse();
        assertThat(isEnabledFor(START_PAUSE, "running")).isFalse();
    }

    @Test
    void pausedFlowCanBeStoppedOrStarted() {
        assertThat(isEnabledFor(STOP, "paused")).isTrue();
        assertThat(isEnabledFor(START, "paused")).isTrue();
        assertThat(isEnabledFor(PAUSE, "paused")).isFalse();
        assertThat(isEnabledFor(START_PAUSE, "paused")).isFalse();
    }

    @Test
    void stoppedFlowCanBeStartedOrStartedPaused() {
        assertThat(isEnabledFor(START, "stopped")).isTrue();
        assertThat(isEnabledFor(START_PAUSE, "stopped")).isTrue();
        assertThat(isEnabledFor(STOP, "stopped")).isFalse();
        assertThat(isEnabledFor(PAUSE, "stopped")).isFalse();
    }

    @Test
    void flowStoppedInErrorCanBeStartedOrStartedPausedJustLikeAPlainStoppedFlow() {
        assertThat(isEnabledFor(START, "stoppedInError")).isTrue();
        assertThat(isEnabledFor(START_PAUSE, "stoppedInError")).isTrue();
        assertThat(isEnabledFor(STOP, "stoppedInError")).isFalse();
        assertThat(isEnabledFor(PAUSE, "stoppedInError")).isFalse();
    }

    @Test
    void everyActionIsDisabledForATransitionalOrUnknownState() {
        for (FlowTransportAction action : values()) {
            assertThat(isEnabledFor(action, "recovering")).isFalse();
            assertThat(isEnabledFor(action, null)).isFalse();
        }
    }

    @Test
    void startResolvesToResumeOnlyWhenTheFlowIsCurrentlyPaused() {
        assertThat(START.resolveWireValue("paused")).isEqualTo("resume");
        assertThat(START.resolveWireValue("stopped")).isEqualTo("start");
        assertThat(START.resolveWireValue("stoppedInError")).isEqualTo("start");
        assertThat(START.resolveWireValue(null)).isEqualTo("start");
    }

    @Test
    void onlyStartIsEverContextSensitive() {
        for (FlowTransportAction action : values()) {
            if (action != START) {
                assertThat(action.resolveWireValue("paused")).isEqualTo(action.getWireValue());
            }
        }
    }
}
