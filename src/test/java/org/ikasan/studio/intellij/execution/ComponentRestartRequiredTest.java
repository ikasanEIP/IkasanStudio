package org.ikasan.studio.intellij.execution;

import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class ComponentRestartRequiredTest {
    @Test void ordinaryComponentChangesAreTrackedOnlyWhileRunningAndClearOnStop() {
        UiContext context = new UiContext();
        FlowElement component = mock(FlowElement.class);
        when(component.getIdentity()).thenReturn("Filter");
        IkasanDebugSessionService.recordRestartRequired(context, component, false);
        assertThat(context.hasRestartPendingElements()).isFalse();
        IkasanDebugSessionService.recordRestartRequired(context, component, true);
        assertThat(context.isRestartPending(UiContext.restartPendingKey(component))).isTrue();
        assertThat(new UiContext().hasRestartPendingElements()).isFalse();
        context.clearRestartPendingElements();
        assertThat(context.hasRestartPendingElements()).isFalse();
    }

    @Test void moduleChangesMarkAllAffectedFlowsAndTheirComponents() {
        UiContext context = new UiContext();
        Module module = mock(Module.class);
        Flow flow = mock(Flow.class);
        FlowElement component = mock(FlowElement.class);
        when(flow.getIdentity()).thenReturn("Flow");
        when(component.getIdentity()).thenReturn("Consumer");
        when(component.getContainingFlow()).thenReturn(flow);
        when(flow.getFlowElementsNoExternalEndPoints()).thenReturn(List.of(component));
        when(module.getFlows()).thenReturn(List.of(flow));
        IkasanDebugSessionService.recordRestartRequired(context, module, true);
        assertThat(context.isRestartPending(UiContext.restartPendingKey(flow))).isTrue();
        assertThat(context.isRestartPending(UiContext.restartPendingKey(component))).isTrue();
    }
}
