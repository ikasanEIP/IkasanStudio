package org.ikasan.studio.ui.component.canvas;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowRoute;
import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
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

    /**
     * Regression test: previously, with two flows both incomplete (even for different reasons), a single
     * global hint category was picked for the whole module and only the first matching flow ever got a
     * visible hint - the second flow's own issue was silently masked. getFlowHint() lets each flow be judged
     * independently, which is what fixed that.
     */
    @Test
    void flowHintIsJudgedIndependentlyPerFlow() {
        Flow needsConsumer = mock(Flow.class);
        when(needsConsumer.hasConsumer()).thenReturn(false);
        assertThat(DesignerCanvas.getFlowHint(needsConsumer)).isEqualTo(DesignerCanvas.GettingStartedHint.EMPTY_FLOW);

        Flow needsProducer = mock(Flow.class);
        when(needsProducer.hasConsumer()).thenReturn(true);
        when(needsProducer.getFlowIntegrityStatus()).thenReturn("The flow needs a producer.");
        assertThat(DesignerCanvas.getFlowHint(needsProducer)).isEqualTo(DesignerCanvas.GettingStartedHint.ADD_COMPONENTS);

        Flow complete = mock(Flow.class);
        when(complete.hasConsumer()).thenReturn(true);
        when(complete.getFlowIntegrityStatus()).thenReturn("");
        assertThat(DesignerCanvas.getFlowHint(complete)).isNull();

        assertThat(DesignerCanvas.getFlowHint(null)).isNull();
    }

    /**
     * Regression test for the multi-flow overlap bug: IkasanModuleViewHandler reserves vertical space below an
     * incomplete flow using this exact measurement (see gapAfterFlow there), so it must agree with what
     * drawHintBlock actually renders - a null hint needs no space, and a "detailed" (heading + instruction)
     * hint measures taller than heading-only, since the paint code draws both.
     */
    @Test
    void measureHintBlockHeightReflectsWhatWillActuallyBeDrawn() {
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scratch.createGraphics();
        try {
            Font baseFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

            assertThat(DesignerCanvas.measureHintBlockHeight(g, baseFont, null, true)).isZero();

            int headingOnly = DesignerCanvas.measureHintBlockHeight(
                    g, baseFont, DesignerCanvas.GettingStartedHint.ADD_COMPONENTS, false);
            int headingAndInstruction = DesignerCanvas.measureHintBlockHeight(
                    g, baseFont, DesignerCanvas.GettingStartedHint.ADD_COMPONENTS, true);

            assertThat(headingOnly).isPositive();
            assertThat(headingAndInstruction).isGreaterThan(headingOnly);
        } finally {
            g.dispose();
        }
    }
}
