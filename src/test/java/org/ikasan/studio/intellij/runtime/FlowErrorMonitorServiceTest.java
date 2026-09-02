package org.ikasan.studio.intellij.runtime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlowErrorMonitorServiceTest {
    @Test
    void pollsOnlyWhenMonitoringIsEnabledAndModuleProcessIsRunning() {
        assertThat(FlowErrorMonitorService.shouldPoll(true, true)).isTrue();
        assertThat(FlowErrorMonitorService.shouldPoll(true, false)).isFalse();
        assertThat(FlowErrorMonitorService.shouldPoll(false, true)).isFalse();
        assertThat(FlowErrorMonitorService.shouldPoll(false, false)).isFalse();
    }
}
