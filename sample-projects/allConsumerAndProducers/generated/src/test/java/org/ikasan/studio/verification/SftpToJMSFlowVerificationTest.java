package org.ikasan.studio.verification;

import org.junit.Test;

/**
 * Generated structural and interface checks for Sftp to JMS Flow.
 * Refresh explicitly; no developer completion is required.
 * Add runtime and business scenarios in user-flow-tests; these checks do not establish that coverage.
 */
public class SftpToJMSFlowVerificationTest extends GeneratedVerificationSupport {
    @Test
    public void testFlowFactoryContract() throws Exception {
        assertFactory("org.ikasan.studio.boot.flow.sftptojmsflow.SftpToJMSFlow",
                "getSftpToJMSFlow", "org.ikasan.spec.flow.Flow");
    }

    @Test
    public void testComponentContracts() throws Exception {
        // my sftp consumer
        assertFactory("org.ikasan.studio.boot.flow.sftptojmsflow.ComponentFactorySftpToJMSFlow",
                "getMySftpConsumer", "org.ikasan.spec.component.endpoint.Consumer");
        // my converter
        assertImplementation("org.example.acap.sftptojmsflow.MyConverter", "org.ikasan.spec.component.transformation.Converter");
        // my JMS producer
        assertFactory("org.ikasan.studio.boot.flow.sftptojmsflow.ComponentFactorySftpToJMSFlow",
                "getMyJMSProducer", "org.ikasan.spec.component.endpoint.Producer");
    }

}
