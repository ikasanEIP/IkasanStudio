package org.ikasan.studio.verification;

import org.junit.Test;

/**
 * Generated structural and interface checks for Transform The Order.
 * Refresh explicitly; no developer completion is required.
 * Add runtime and business scenarios in user-flow-tests; these checks do not establish that coverage.
 */
public class TransformTheOrderVerificationTest extends GeneratedVerificationSupport {
    @Test
    public void testFlowFactoryContract() throws Exception {
        assertFactory("org.ikasan.studio.boot.flow.transformtheorder.TransformTheOrder",
                "getTransformTheOrder", "org.ikasan.spec.flow.Flow");
    }

    @Test
    public void testComponentContracts() throws Exception {
        // Order consumer
        assertFactory("org.ikasan.studio.boot.flow.transformtheorder.ComponentFactoryTransformTheOrder",
                "getOrderConsumer", "org.ikasan.spec.component.endpoint.Consumer");
        // Order consumerDebug
        assertImplementation("org.example.cat.transformtheorder.TransformTheOrderOrderConsumerDebug", "org.ikasan.spec.component.filter.Filter");
        // Jms to OP order
        // XML converter
        assertFactory("org.ikasan.studio.boot.flow.transformtheorder.ComponentFactoryTransformTheOrder",
                "getXMLConverter", "org.ikasan.spec.component.transformation.Converter");
        // Add to Email
        assertImplementation("org.example.cat.transformtheorder.StringToEmailConverter", "org.ikasan.spec.component.transformation.Converter");
        // Email to warehouse
        assertFactory("org.ikasan.studio.boot.flow.transformtheorder.ComponentFactoryTransformTheOrder",
                "getEmailToWarehouse", "org.ikasan.spec.component.endpoint.Producer");
    }

}
