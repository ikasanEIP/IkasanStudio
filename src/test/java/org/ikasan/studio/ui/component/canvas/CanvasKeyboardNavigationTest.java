package org.ikasan.studio.ui.component.canvas;

import java.util.ArrayList;
import java.util.List;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CanvasKeyboardNavigationTest {
    @Test void navigationOrderIncludesModuleFlowConsumerAndRouteElements() throws Exception {
        String pack = TestFixtures.BASE_META_PACK;
        FlowElement consumer = TestFixtures.getEventGeneratingConsumer(pack);
        FlowElement producer = TestFixtures.getLoggingProducer(pack);
        Flow flow = TestFixtures.getUnbuiltFlow(pack).name("A long flow name ".repeat(20)).consumer(consumer).build();
        consumer.setContainingFlow(flow);
        producer.setContainingFlow(flow); producer.setContainingFlowRoute(flow.getFlowRoute());
        flow.getFlowRoute().setFlowElements(new ArrayList<>(List.of(producer)));
        Module module = TestFixtures.getMyFirstModuleIkasanModule(pack, List.of(flow));
        assertEquals(List.of(module, flow, consumer, producer), CanvasKeyboardNavigation.elements(module));
        assertTrue(CanvasKeyboardNavigation.elements(null).isEmpty());
    }
}
