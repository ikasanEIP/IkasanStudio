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
        // Message Trigger
        assertFactory("org.ikasan.studio.boot.flow.splitterflow.ComponentFactorySplitterFlow",
                "getMessageTrigger", "org.ikasan.spec.component.endpoint.Consumer");
        // Message for Batch Generator
        assertImplementation("org.ikasan.studio.demo.cat.splitterflow.MessageForBatchGenerator", "org.ikasan.spec.component.endpoint.Broker");
        // Default Split List to Individual
        // Standard Splitter List to String
        assertImplementation("org.ikasan.studio.demo.cat.splitterflow.StandardSplitterListToString", "org.ikasan.spec.component.splitting.Splitter");
        // Connect to next flow
        assertFactory("org.ikasan.studio.boot.flow.splitterflow.ComponentFactorySplitterFlow",
                "getConnectToNextFlow", "org.ikasan.spec.component.endpoint.Producer");
    }

}
