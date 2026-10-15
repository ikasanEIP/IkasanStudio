package org.ikasan.studio.verification;

import org.junit.Test;

/**
 * Generated structural and interface checks for Generic To FTP Flow.
 * Refresh explicitly; no developer completion is required.
 * Add runtime and business scenarios in user-flow-tests; these checks do not establish that coverage.
 */
public class GenericToFTPFlowVerificationTest extends GeneratedVerificationSupport {
    @Test
    public void testFlowFactoryContract() throws Exception {
        assertFactory("org.ikasan.studio.boot.flow.generictoftpflow.GenericToFTPFlow",
                "getGenericToFTPFlow", "org.ikasan.spec.flow.Flow");
    }

    @Test
    public void testComponentContracts() throws Exception {
        // my generic consumer
        assertImplementation("org.example.acap.generictoftpflow.MyGenericConsumer", "org.ikasan.spec.component.endpoint.Consumer");
        // my gen to ftp converter
        assertImplementation("org.example.acap.generictoftpflow.MyGenToFtpConverter", "org.ikasan.spec.component.transformation.Converter");
        // my ftp producer
        assertFactory("org.ikasan.studio.boot.flow.generictoftpflow.ComponentFactoryGenericToFTPFlow",
                "getMyFtpProducer", "org.ikasan.spec.component.endpoint.Producer");
    }

}
