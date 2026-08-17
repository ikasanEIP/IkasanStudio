package org.ikasan.studio.ui.viewmodel;

import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IkasanFlowViewHandlerTest {
    @Test
    void recordingIndicatorIsShownOnlyWhenFlowRecordingIsEnabled() {
        Flow flow = mock(Flow.class);

        assertFalse(IkasanFlowViewHandler.isRecording(flow));
        when(flow.getPropertyValue("isRecording")).thenReturn(false, true, "true");
        assertFalse(IkasanFlowViewHandler.isRecording(flow));
        assertTrue(IkasanFlowViewHandler.isRecording(flow));
        assertTrue(IkasanFlowViewHandler.isRecording(flow));
    }
}
