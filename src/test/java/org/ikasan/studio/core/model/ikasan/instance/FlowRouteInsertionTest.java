package org.ikasan.studio.core.model.ikasan.instance;

import org.ikasan.studio.SharedResourceExtension;
import org.ikasan.studio.core.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.ArrayList;
import java.util.List;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SharedResourceExtension.class)
class FlowRouteInsertionTest {
    @Test
    void producerDroppedBeforeConverterBecomesTerminal() throws Exception {
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK);
        FlowElement producer = TestFixtures.getLoggingProducer(BASE_META_PACK);
        FlowRoute route = FlowRoute.flowRouteBuilder().flow(TestFixtures.getUnbuiltFlow(BASE_META_PACK).build()).flowElements(new ArrayList<>(List.of(converter))).build();
        route.insertFlowElement(0, producer);
        assertEquals(List.of(converter, producer), route.getFlowElements());
        assertSame(route, producer.getContainingFlowRoute());
    }

    @Test
    void converterDroppedAfterProducerIsInsertedBeforeIt() throws Exception {
        FlowElement converter = TestFixtures.getCustomConverter(BASE_META_PACK);
        FlowElement producer = TestFixtures.getLoggingProducer(BASE_META_PACK);
        FlowRoute route = FlowRoute.flowRouteBuilder().flow(TestFixtures.getUnbuiltFlow(BASE_META_PACK).build()).flowElements(new ArrayList<>(List.of(producer))).build();
        route.insertFlowElement(1, converter);
        assertEquals(List.of(converter, producer), route.getFlowElements());
    }
}
