package org.ikasan.studio.flowtests;

import org.ikasan.studio.flowtests.support.ModuleFlowTestSupport;

import org.junit.Test;

import java.util.List;

/**
 * Developer-owned observation test for ECG to DevNull Flow.
 * The source generates its own input; the sink discards it. No external output is expected.
 * This checks the declared initial payload sequence and continued running, not external delivery.
 */
public class ECGToDevNullFlowFlowTest extends ModuleFlowTestSupport {
    // mvn -pl user-flow-tests -am -Dtest=ECGToDevNullFlowFlowTest -Dsurefire.failIfNoSpecifiedTests=false test

    private static final String FLOW_NAME = "ECG to DevNull Flow";
    private static final String PRODUCER_NAME = "dev null producer";

    @Override protected String getFlowName() { return FLOW_NAME; }

    // Override prepareFixtures(context) for instance fixtures after Spring starts, before the flow starts.
    // Override cleanupFixtures(context) for custom cleanup, including when fixture preparation fails.

    @Test
    public void testGeneratedEventsReachProducerAndFlowKeepsRunning() throws Exception {
        // runObservationTest checks:
        // - The real consumer generates events, without injected fixture input.
        // - Events reach and complete the named producer.
        // - Supplied expected payloads match in order provided (the actual payload comes from the afterFlowElement of the producer)
        // - The flow stays RUNNING throughout a one-second observation window.
        // - A fresh later event reaches the producer without restarting the flow.
        // - Teardown stops the flow and confirms its STOPPED state.
        // It does not check external delivery, intermediate component order or recovery behaviour.
        runObservationTest(PRODUCER_NAME,
                // Expected messages from the built-in event provider.
                List.of("Test Message 1", "Test Message 2", "Test Message 3"));
    }
}
