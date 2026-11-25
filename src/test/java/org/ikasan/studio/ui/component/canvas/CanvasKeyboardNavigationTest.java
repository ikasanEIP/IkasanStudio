package org.ikasan.studio.ui.component.canvas;

import org.ikasan.studio.core.TestFixtures;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;

class CanvasKeyboardNavigationTest {
    @Test void navigationOrderIncludesModuleFlowConsumerAndRouteElements() throws Exception {
        String pack = TestFixtures.BASE_META_PACK;
        FlowElement consumer = TestFixtures.getEventGeneratingConsumer(pack);
        FlowElement producer = TestFixtures.getLoggingProducer(pack);
        Flow flow = TestFixtures.getUnbuiltFlow(pack).name("A long flow name ".repeat(20)).consumer(consumer).build();
        consumer.setContainingFlow(flow);
        producer.setContainingFlow(flow); producer.setContainingFlowRoute(flow.getFlowRoute());
        flow.getFlowRoute().setFlowElements(new java.util.ArrayList<>(List.of(producer)));
        Module module = TestFixtures.getMyFirstModuleIkasanModule(pack, List.of(flow));
        assertEquals(List.of(module, flow, consumer, producer), CanvasKeyboardNavigation.elements(module));
        assertTrue(CanvasKeyboardNavigation.elements(null).isEmpty());
    }
}
