package org.ikasan.studio.verification;

import org.junit.Test;

/**
 * Generated structural and interface checks for JMS to SFTP flow.
 * Refresh explicitly; no developer completion is required.
 * Add runtime and business scenarios in user-flow-tests; these checks do not establish that coverage.
 */
public class JMSToSFTPFlowVerificationTest extends GeneratedVerificationSupport {
    @Test
    public void testFlowFactoryContract() throws Exception {
        assertFactory("org.ikasan.studio.boot.flow.jmstosftpflow.JMSToSFTPFlow",
                "getJMSToSFTPFlow", "org.ikasan.spec.flow.Flow");
    }

    @Test
    public void testComponentContracts() throws Exception {
        // my Spring consumer
        assertFactory("org.ikasan.studio.boot.flow.jmstosftpflow.ComponentFactoryJMSToSFTPFlow",
                "getMySpringConsumer", "org.ikasan.spec.component.endpoint.Consumer");
        // my converter
        assertImplementation("org.example.acap.jmstosftpflow.MyConverter", "org.ikasan.spec.component.transformation.Converter");
        // my sftp producer
        assertFactory("org.ikasan.studio.boot.flow.jmstosftpflow.ComponentFactoryJMSToSFTPFlow",
                "getMySftpProducer", "org.ikasan.spec.component.endpoint.Producer");
    }

}
