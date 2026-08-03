package org.ikasan.studio.ui.component.canvas;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowRoute;
import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DesignerCanvasTest {

    @BeforeAll
    static void warmUpMetaPack() throws StudioBuildException {
        // Opening a packaged meta-pack lazily starts JVM filesystem threads. Do that before
        // IntelliJ's per-test thread-leak snapshot so the regression test stays deterministic.
        DesignerCanvas.createModuleDraft(null, BASE_META_PACK, "warm-up", "org.example");
    }

    @Test
    void moduleConfigurationDraftDoesNotMutateLiveModule() throws StudioBuildException {
        Module liveModule = Module.getDumbModuleVersion();

        Module draft = DesignerCanvas.createModuleDraft(
                liveModule, BASE_META_PACK, "my-project", "org.example.integration");

        assertThat(draft).isNotSameAs(liveModule);
        assertThat(draft.getIdentity()).isEqualTo("my-project");
        assertThat(draft.getApplicationPackageName()).isEqualTo("org.example.integration");
        assertThat(liveModule.getIdentity()).isNull();
        assertThat(liveModule.getApplicationPackageName()).isNull();
    }

    @Test
    void newModuleDefaultsFlowsToAutomaticStartup() throws StudioBuildException {
        Module placeholderModule = Module.getDumbModuleVersion();

        Module draft = DesignerCanvas.createModuleDraft(
                placeholderModule, BASE_META_PACK, "my-project", "org.example.integration");

        assertThat(draft.getPropertyValue("flowStartupType")).isEqualTo("AUTOMATIC");
        assertThat(placeholderModule.getPropertyValue("flowStartupType")).isNull();
    }


    @Test
    void gettingStartedHintProgressesWithTheFlowModel() {
        Module module = mock(Module.class);
        when(module.isInitialised()).thenReturn(true);
        when(module.getFlows()).thenReturn(List.of());
        assertThat(DesignerCanvas.getGettingStartedHint(module))
                .isEqualTo(DesignerCanvas.GettingStartedHint.NO_FLOWS);

        Flow flow = mock(Flow.class);
        FlowRoute route = mock(FlowRoute.class);
        when(module.getFlows()).thenReturn(List.of(flow));
        assertThat(DesignerCanvas.getGettingStartedHint(module))
                .isEqualTo(DesignerCanvas.GettingStartedHint.EMPTY_FLOW);

        when(flow.hasConsumer()).thenReturn(true);
        when(flow.getFlowRoute()).thenReturn(route);
        when(flow.getFlowIntegrityStatus()).thenReturn("The flow needs a producer.");
        assertThat(DesignerCanvas.getGettingStartedHint(module))
                .isEqualTo(DesignerCanvas.GettingStartedHint.ADD_COMPONENTS);

        when(flow.getFlowIntegrityStatus()).thenReturn("");
        assertThat(DesignerCanvas.getGettingStartedHint(module))
                .isEqualTo(DesignerCanvas.GettingStartedHint.READY_TO_RUN);
    }
}
