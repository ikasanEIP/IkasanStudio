package org.ikasan.studio.verification;

import org.junit.Test;

/**
 * Generated structural and interface checks for Ftp to Email Flow.
 * Refresh explicitly; no developer completion is required.
 * Add runtime and business scenarios in user-flow-tests; these checks do not establish that coverage.
 */
public class FtpToEmailFlowVerificationTest extends GeneratedVerificationSupport {
    @Test
    public void testFlowFactoryContract() throws Exception {
        assertFactory("org.ikasan.studio.boot.flow.ftptoemailflow.FtpToEmailFlow",
                "getFtpToEmailFlow", "org.ikasan.spec.flow.Flow");
    }

    @Test
    public void testComponentContracts() throws Exception {
        // my ftp consumer
        assertFactory("org.ikasan.studio.boot.flow.ftptoemailflow.ComponentFactoryFtpToEmailFlow",
                "getMyFtpConsumer", "org.ikasan.spec.component.endpoint.Consumer");
        // my ftp to email
        assertImplementation("org.example.acap4.ftptoemailflow.MyFtpToEmail", "org.ikasan.spec.component.transformation.Converter");
        // my email producer
        assertFactory("org.ikasan.studio.boot.flow.ftptoemailflow.ComponentFactoryFtpToEmailFlow",
                "getMyEmailProducer", "org.ikasan.spec.component.endpoint.Producer");
    }

}
