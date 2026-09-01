package org.ikasan.studio.core.model.ikasan.instance;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlowErrorStatesTest {

    @Test
    void flaggingAPreviouslyUnknownFlowReportsItAsNew() {
        FlowErrorStates errorStates = new FlowErrorStates();

        boolean wasNew = errorStates.flag("f1", new FlowErrorStates.ErrorInfo("boom", 1L));

        assertThat(wasNew).isTrue();
        assertThat(errorStates.isFlagged("f1")).isTrue();
        assertThat(errorStates.getError("f1").summary()).isEqualTo("boom");
        assertThat(errorStates.hasAnyFlagged()).isTrue();
    }

    @Test
    void reFlaggingAnAlreadyFlaggedFlowReportsItAsNotNew() {
        FlowErrorStates errorStates = new FlowErrorStates();
        errorStates.flag("f1", new FlowErrorStates.ErrorInfo("boom", 1L));

        boolean wasNew = errorStates.flag("f1", new FlowErrorStates.ErrorInfo("boom again", 2L));

        assertThat(wasNew).isFalse();
    }

    @Test
    void clearingAFlaggedFlowReportsThatStateActuallyChanged() {
        FlowErrorStates errorStates = new FlowErrorStates();
        errorStates.flag("f1", new FlowErrorStates.ErrorInfo("boom", 1L));

        boolean wasFlagged = errorStates.clear("f1");

        assertThat(wasFlagged).isTrue();
        assertThat(errorStates.isFlagged("f1")).isFalse();
        assertThat(errorStates.hasAnyFlagged()).isFalse();
    }

    @Test
    void clearingAFlowThatWasNeverFlaggedIsANoOp() {
        FlowErrorStates errorStates = new FlowErrorStates();

        boolean wasFlagged = errorStates.clear("f1");

        assertThat(wasFlagged).isFalse();
    }

    @Test
    void tracksMultipleFlaggedFlowsIndependently() {
        FlowErrorStates errorStates = new FlowErrorStates();
        errorStates.flag("f1", new FlowErrorStates.ErrorInfo("boom", 1L));
        errorStates.flag("f2", new FlowErrorStates.ErrorInfo("bang", 2L));

        errorStates.clear("f1");

        assertThat(errorStates.isFlagged("f1")).isFalse();
        assertThat(errorStates.isFlagged("f2")).isTrue();
        assertThat(errorStates.flaggedFlowNames()).containsExactly("f2");
    }
}
