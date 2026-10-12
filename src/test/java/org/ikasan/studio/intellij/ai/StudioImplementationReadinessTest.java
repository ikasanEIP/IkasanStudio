package org.ikasan.studio.intellij.ai;

import org.ikasan.studio.core.ai.ImplementationReadiness;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudioImplementationReadinessTest {
    @Test
    void resolvesExplicitTargetEvenWhenComponentIsNamedFlowAndRejectsStaleFindings() {
        Module module = mock(Module.class);
        Flow flow = mock(Flow.class);
        FlowElement component = mock(FlowElement.class);
        when(module.getFlows()).thenReturn(List.of(flow));
        when(flow.getIdentity()).thenReturn("Example");
        when(component.getIdentity()).thenReturn("Flow");
        when(flow.ftlGetConsumerAndFlowElements()).thenReturn(List.of(component));
        assertSame(component, StudioImplementationReadiness.resolveTarget(module, finding(ImplementationReadiness.Target.COMPONENT, "Flow")));
        assertSame(flow, StudioImplementationReadiness.resolveTarget(module, finding(ImplementationReadiness.Target.FLOW, "Flow")));
        assertSame(module, StudioImplementationReadiness.resolveTarget(module, finding(ImplementationReadiness.Target.MODULE, "Module")));
        assertNull(StudioImplementationReadiness.resolveTarget(module, finding(ImplementationReadiness.Target.COMPONENT, "Removed")));
        assertNull(StudioImplementationReadiness.resolveTarget(null, finding(ImplementationReadiness.Target.COMPONENT, "Flow")));
    }

    private ImplementationReadiness.Finding finding(ImplementationReadiness.Target target, String component) {
        return new ImplementationReadiness.Finding("Example", component, "generated/src/main/model/model.json", 1,
                "INVALID_PROPERTY", "Invalid recipient", target, "toRecipient", "bad.@example.com");
    }
}
