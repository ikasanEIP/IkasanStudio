package org.ikasan.studio.verification;

import org.junit.Test;

/**
 * Generated structural and interface checks for Splitter Flow.
 * Refresh explicitly; no developer completion is required.
 * Add runtime and business scenarios in user-flow-tests; these checks do not establish that coverage.
 */
public class SplitterFlowVerificationTest extends GeneratedVerificationSupport {
    @Test
    public void testFlowFactoryContract() throws Exception {
        assertFactory("org.ikasan.studio.boot.flow.splitterflow.SplitterFlow",
                "getSplitterFlow", "org.ikasan.spec.flow.Flow");
    }

    @Test
    public void testComponentContracts() throws Exception {
        // Periodic Event Initiator
        assertFactory("org.ikasan.studio.boot.flow.splitterflow.ComponentFactorySplitterFlow",
                "getPeriodicEventInitiator", "org.ikasan.spec.component.endpoint.Consumer");
        // Message Batch Generator
        assertImplementation("org.example.rab.splitterflow.MessageGenerator", "org.ikasan.spec.component.endpoint.Broker");
        // Default Split list into individuals
        // Standard split list to string
        assertImplementation("org.example.rab.splitterflow.StandardSplitListToString", "org.ikasan.spec.component.splitting.Splitter");
        // Connect to next flow
        assertFactory("org.ikasan.studio.boot.flow.splitterflow.ComponentFactorySplitterFlow",
                "getConnectToNextFlow", "org.ikasan.spec.component.endpoint.Producer");
    }

}
