package org.ikasan.studio.examples;

import org.ikasan.spec.flow.Flow;
import org.ikasan.spec.flow.FlowElement;
import org.ikasan.spec.flow.FlowEvent;
import org.ikasan.spec.flow.FlowEventListener;
import org.ikasan.testharness.flow.rule.IkasanFlowTestRule;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Adapt names and expected payloads to a real, isolated application flow; see the README. */
public final class ScheduledFlowExample {
    private ScheduledFlowExample() { }

    public static void verifyTwoDeliveries(Flow flow, String source, String converter,
                                          String producer, String first, String second) throws Exception {
        var outputs = new LinkedBlockingQueue<String>();
        FlowEventListener listener = new FlowEventListener() {
            public void beforeFlowElement(String module, String name, FlowElement element, FlowEvent event) { }
            public void afterFlowElement(String module, String name, FlowElement element, FlowEvent event) {
                if (producer.equals(element.getComponentName())) outputs.add(String.valueOf(event.getPayload()));
            }
        };
        var harness = new IkasanFlowTestRule();
        harness.withFlow(flow).blockStart().scheduledConsumer(source).converter(converter)
                .producer(producer).blockEnd().repeat(2);
        flow.addFlowListener(listener);
        try {
            harness.startFlow();
            harness.fireScheduledConsumer();
            assertEquals("First delivery", first, outputs.poll(10, TimeUnit.SECONDS));
            assertEquals(Flow.RUNNING, flow.getState());
            // No reset or restart: verify readiness while idle, then another delivery.
            assertNull("No unsolicited delivery", outputs.poll(1, TimeUnit.SECONDS));
            assertEquals(Flow.RUNNING, flow.getState());
            harness.fireScheduledConsumer();
            assertEquals("Later delivery", second, outputs.poll(10, TimeUnit.SECONDS));
            assertEquals(Flow.RUNNING, flow.getState());
            // Our capture callback can run before the harness observer for the same event.
            org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10))
                    .untilAsserted(harness::assertIsSatisfied);
        } finally {
            // This is an isolated test instance, never the developer's running module.
            harness.stopFlow();
            flow.removeFlowListener(listener);
        }
    }
}
