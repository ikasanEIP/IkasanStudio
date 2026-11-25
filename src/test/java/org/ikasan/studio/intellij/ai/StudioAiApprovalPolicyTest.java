package org.ikasan.studio.intellij.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.ai.*;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowUserImplementedElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudioAiApprovalPolicyTest {
    @Test void checksTransientProtectedPropertyFlagsInLiveModelEvenForUnchangedFlows() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Module empty = TestFixtures.getMyFirstModuleIkasanModule("V4.1.6", List.of());
        Module live = ModelProposal.prepare(LiveModelSnapshot.capture(empty), mapper.readTree("""
                [{"type":"addFlow","flow":"existing"},
                 {"type":"addComponent","flow":"existing","key":"Event Generating Consumer","name":"Input",
                 "properties":{"endpointEventProvider":"MinuteEventProvider"}},
                 {"type":"addComponent","flow":"existing","key":"Dev Null Producer","name":"Discard"}]
                """)).draft();
        ComponentProperty property = live.getFlows().get(0).getConsumer().getProperty("endpointEventProvider");
        property.setOverwriteEnabled(false);
        Module proposed = ModelProposal.prepare(LiveModelSnapshot.capture(live), mapper.readTree(
                "[{\"type\":\"addFlow\",\"flow\":\"unrelated\"}]")).draft();
        assertThat(StudioAiService.requiresUserCodeReview(live, proposed)).isFalse();
        property.setOverwriteEnabled(true);
        assertThat(StudioAiService.requiresUserCodeReview(live, proposed)).isTrue();
        property.setOverwriteEnabled(false);
        proposed.getFlows().get(0).getConsumer().getProperty("endpointEventProvider").setOverwriteEnabled(true);
        assertThat(StudioAiService.requiresUserCodeReview(live, proposed)).isTrue();
    }

    @Test void implementationClassOverwriteAlwaysNeedsReview() {
        Module module = mock(Module.class);
        Flow flow = mock(Flow.class);
        FlowUserImplementedElement implementation = mock(FlowUserImplementedElement.class);
        when(module.getFlows()).thenReturn(List.of(flow));
        when(flow.getFlowElementsNoExternalEndPoints()).thenReturn(List.of(implementation));
        assertThat(StudioAiService.requiresUserCodeReview(module)).isFalse();
        when(implementation.isOverwriteEnabled()).thenReturn(true);
        assertThat(StudioAiService.requiresUserCodeReview(module)).isTrue();
    }
}
