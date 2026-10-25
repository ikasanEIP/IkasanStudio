package org.ikasan.studio.verification;

import org.junit.Test;

/**
 * Generated structural and interface checks for ECG to DevNull Flow.
 * Refresh explicitly; no developer completion is required.
 * Add runtime and business scenarios in user-flow-tests; these checks do not establish that coverage.
 */
public class ECGToDevNullFlowVerificationTest extends GeneratedVerificationSupport {
    @Test
    public void testFlowFactoryContract() throws Exception {
        assertFactory("org.ikasan.studio.boot.flow.ecgtodevnullflow.ECGToDevNullFlow",
                "getECGToDevNullFlow", "org.ikasan.spec.flow.Flow");
    }

    @Test
    public void testComponentContracts() throws Exception {
        // My EGC Consumer
        assertFactory("org.ikasan.studio.boot.flow.ecgtodevnullflow.ComponentFactoryECGToDevNullFlow",
                "getMyEGCConsumer", "org.ikasan.spec.component.endpoint.Consumer");
        // dev null producer
        assertFactory("org.ikasan.studio.boot.flow.ecgtodevnullflow.ComponentFactoryECGToDevNullFlow",
                "getDevNullProducer", "org.ikasan.spec.component.endpoint.Producer");
    }

}
