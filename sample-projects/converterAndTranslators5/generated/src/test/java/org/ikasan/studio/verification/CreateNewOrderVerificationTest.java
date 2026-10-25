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
        // Poll for Orders
        assertFactory("org.ikasan.studio.boot.flow.createneworder.ComponentFactoryCreateNewOrder",
                "getPollForOrders", "org.ikasan.spec.component.endpoint.Consumer");
        // Create New Orders
        assertImplementation("org.ikasan.studio.demo.cat.createneworder.CreateNewOrders", "org.ikasan.spec.component.endpoint.Broker");
        // Add order number
        assertImplementation("org.ikasan.studio.demo.cat.createneworder.AddOrderNumber", "org.ikasan.spec.component.transformation.Translator");
        // Send for Processing
        assertFactory("org.ikasan.studio.boot.flow.createneworder.ComponentFactoryCreateNewOrder",
                "getSendForProcessing", "org.ikasan.spec.component.endpoint.Producer");
    }

}
