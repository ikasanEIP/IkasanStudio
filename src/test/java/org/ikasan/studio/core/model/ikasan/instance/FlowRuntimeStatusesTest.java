package org.ikasan.studio.core.model.ikasan.instance;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlowRuntimeStatusesTest {

    @Test
    void updateReportsChangedForANewlySeenFlow() {
        FlowRuntimeStatuses statuses = new FlowRuntimeStatuses();

        boolean changed = statuses.update("f1", "running");

        assertThat(changed).isTrue();
        assertThat(statuses.getRawState("f1")).isEqualTo("running");
    }

    @Test
    void updateReportsUnchangedWhenTheStateIsTheSameAsBefore() {
        FlowRuntimeStatuses statuses = new FlowRuntimeStatuses();
        statuses.update("f1", "running");

        boolean changed = statuses.update("f1", "running");

        assertThat(changed).isFalse();
    }

    @Test
    void updateReportsChangedWhenTheStateActuallyTransitions() {
        FlowRuntimeStatuses statuses = new FlowRuntimeStatuses();
        statuses.update("f1", "running");

        boolean changed = statuses.update("f1", "stoppedInError");

        assertThat(changed).isTrue();
        assertThat(statuses.getRawState("f1")).isEqualTo("stoppedInError");
    }

    @Test
    void getRawStateIsNullForAFlowThatHasNeverBeenPolled() {
        FlowRuntimeStatuses statuses = new FlowRuntimeStatuses();

        assertThat(statuses.getRawState("neverSeen")).isNull();
    }
}
