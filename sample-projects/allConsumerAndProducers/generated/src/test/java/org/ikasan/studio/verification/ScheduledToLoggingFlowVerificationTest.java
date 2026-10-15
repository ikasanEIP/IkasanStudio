package org.ikasan.studio.verification;

import org.junit.Test;

/**
 * Generated structural and interface checks for Scheduled to Logging Flow.
 * Refresh explicitly; no developer completion is required.
 * Add runtime and business scenarios in user-flow-tests; these checks do not establish that coverage.
 */
public class ScheduledToLoggingFlowVerificationTest extends GeneratedVerificationSupport {
    @Test
    public void testFlowFactoryContract() throws Exception {
        assertFactory("org.ikasan.studio.boot.flow.scheduledtologgingflow.ScheduledToLoggingFlow",
                "getScheduledToLoggingFlow", "org.ikasan.spec.flow.Flow");
    }

    @Test
    public void testComponentContracts() throws Exception {
        // my scheduled consumer
        assertFactory("org.ikasan.studio.boot.flow.scheduledtologgingflow.ComponentFactoryScheduledToLoggingFlow",
                "getMyScheduledConsumer", "org.ikasan.spec.component.endpoint.Consumer");
        // my logging producer
        assertFactory("org.ikasan.studio.boot.flow.scheduledtologgingflow.ComponentFactoryScheduledToLoggingFlow",
                "getMyLoggingProducer", "org.ikasan.spec.component.endpoint.Producer");
    }

}
