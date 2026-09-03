package org.ikasan.studio.ui.actions;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;

class ToggleTestHarnessesActionTest {
    @Test
    void availableForFtpConsumerOrProducer() throws Exception {
        assertThat(ToggleTestHarnessesAction.hasHarnesses(moduleWith(TestFixtures.getFtpConsumer(BASE_META_PACK)))).isTrue();
        assertThat(ToggleTestHarnessesAction.hasHarnesses(moduleWith(TestFixtures.getFtpProducer(BASE_META_PACK)))).isTrue();
    }

    @Test
    void availableForEmailEndpoint() throws Exception {
        FlowElement emailProducer = FlowElement.flowElementBuilder()
                .componentMeta(ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Email Producer"))
                .componentName("mail")
                .build();

        assertThat(ToggleTestHarnessesAction.hasHarnesses(moduleWith(emailProducer))).isTrue();
    }

    @Test
    void unavailableWithoutSupportedEndpoints() throws Exception {
        assertThat(ToggleTestHarnessesAction.hasHarnesses(moduleWith(TestFixtures.getDevNullProducer(BASE_META_PACK)))).isFalse();
        assertThat(ToggleTestHarnessesAction.hasHarnesses(null)).isFalse();
    }

    private static Module moduleWith(FlowElement element) throws StudioBuildException {
        Flow flow = new Flow(BASE_META_PACK);
        element.setContainingFlow(flow);
        element.setContainingFlowRoute(flow.getFlowRoute());
        if (element.getComponentMeta().isConsumer()) {
            flow.setConsumer(element);
        } else {
            flow.getFlowRoute().getFlowElements().add(element);
        }
        return TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK, List.of(flow));
    }
}
