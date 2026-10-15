package org.ikasan.studio.verification;

import org.junit.Test;

/**
 * Generated structural and interface checks for Local File to Generic Flow.
 * Refresh explicitly; no developer completion is required.
 * Add runtime and business scenarios in user-flow-tests; these checks do not establish that coverage.
 */
public class LocalFileToGenericFlowVerificationTest extends GeneratedVerificationSupport {
    @Test
    public void testFlowFactoryContract() throws Exception {
        assertFactory("org.ikasan.studio.boot.flow.localfiletogenericflow.LocalFileToGenericFlow",
                "getLocalFileToGenericFlow", "org.ikasan.spec.flow.Flow");
    }

    @Test
    public void testComponentContracts() throws Exception {
        // my local consumer
        assertFactory("org.ikasan.studio.boot.flow.localfiletogenericflow.ComponentFactoryLocalFileToGenericFlow",
                "getMyLocalConsumer", "org.ikasan.spec.component.endpoint.Consumer");
        // my generic producer
        assertImplementation("org.example.acap.localfiletogenericflow.MyGenericProducer", "org.ikasan.spec.component.endpoint.Producer");
    }

}
