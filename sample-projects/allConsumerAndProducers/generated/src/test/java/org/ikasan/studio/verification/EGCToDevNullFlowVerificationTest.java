package org.ikasan.studio.verification;

import org.junit.Test;

/**
 * Generated structural and interface checks for EGC To DevNull Flow.
 * Refresh explicitly; no developer completion is required.
 * Add runtime and business scenarios in user-flow-tests; these checks do not establish that coverage.
 */
public class EGCToDevNullFlowVerificationTest extends GeneratedVerificationSupport {
    @Test
    public void testFlowFactoryContract() throws Exception {
        assertFactory("org.ikasan.studio.boot.flow.egctodevnullflow.EGCToDevNullFlow",
                "getEGCToDevNullFlow", "org.ikasan.spec.flow.Flow");
    }

    @Test
    public void testComponentContracts() throws Exception {
        // my egc
        assertFactory("org.ikasan.studio.boot.flow.egctodevnullflow.ComponentFactoryEGCToDevNullFlow",
                "getMyEgc", "org.ikasan.spec.component.endpoint.Consumer");
        // dev null producer
        assertFactory("org.ikasan.studio.boot.flow.egctodevnullflow.ComponentFactoryEGCToDevNullFlow",
                "getDevNullProducer", "org.ikasan.spec.component.endpoint.Producer");
    }

}
