package org.ikasan.studio.ui.component.canvas;

import org.ikasan.studio.core.TestFixtures;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CanvasKeyboardNavigationTest {
    @Test void navigationOrderIncludesModuleFlowConsumerAndRouteElements() throws Exception {
        String pack = TestFixtures.BASE_META_PACK;
        var consumer = TestFixtures.getEventGeneratingConsumer(pack);
        var producer = TestFixtures.getLoggingProducer(pack);
        var flow = TestFixtures.getUnbuiltFlow(pack).name("A long flow name ".repeat(20)).consumer(consumer).build();
        consumer.setContainingFlow(flow);
        producer.setContainingFlow(flow); producer.setContainingFlowRoute(flow.getFlowRoute());
        flow.getFlowRoute().setFlowElements(new java.util.ArrayList<>(List.of(producer)));
        var module = TestFixtures.getMyFirstModuleIkasanModule(pack, List.of(flow));
        assertEquals(List.of(module, flow, consumer, producer), CanvasKeyboardNavigation.elements(module));
        assertTrue(CanvasKeyboardNavigation.elements(null).isEmpty());
    }
}
