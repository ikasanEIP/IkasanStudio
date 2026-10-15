package org.ikasan.studio.verification;

import org.junit.Test;

/**
 * Generated structural and interface checks for Create New Order.
 * Refresh explicitly; no developer completion is required.
 * Add runtime and business scenarios in user-flow-tests; these checks do not establish that coverage.
 */
public class CreateNewOrderVerificationTest extends GeneratedVerificationSupport {
    @Test
    public void testFlowFactoryContract() throws Exception {
        assertFactory("org.ikasan.studio.boot.flow.createneworder.CreateNewOrder",
                "getCreateNewOrder", "org.ikasan.spec.flow.Flow");
    }

    @Test
    public void testComponentContracts() throws Exception {
        // Poll for orders
        assertFactory("org.ikasan.studio.boot.flow.createneworder.ComponentFactoryCreateNewOrder",
                "getPollForOrders", "org.ikasan.spec.component.endpoint.Consumer");
        // Get order items
        assertImplementation("org.example.cat.createneworder.ArtificallyCreateOrder", "org.ikasan.spec.component.endpoint.Broker");
        // Add order num
        assertImplementation("org.example.cat.createneworder.AddOrderNumber", "org.ikasan.spec.component.transformation.Translator");
        // Send for Processing
        assertFactory("org.ikasan.studio.boot.flow.createneworder.ComponentFactoryCreateNewOrder",
                "getSendForProcessing", "org.ikasan.spec.component.endpoint.Producer");
    }

}
