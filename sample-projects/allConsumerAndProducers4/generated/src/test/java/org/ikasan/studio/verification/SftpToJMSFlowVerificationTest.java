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
        // my sftp to jms converter
        assertImplementation("org.example.acap4.sftptojmsflow.MySftpToJmsConverter", "org.ikasan.spec.component.transformation.Converter");
        // my file producer
        assertFactory("org.ikasan.studio.boot.flow.sftptojmsflow.ComponentFactorySftpToJMSFlow",
                "getMyFileProducer", "org.ikasan.spec.component.endpoint.Producer");
    }

}
