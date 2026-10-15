package org.ikasan.studio.verification;

import org.junit.Test;

/**
 * Generated structural and interface checks for Router Flow.
 * Refresh explicitly; no developer completion is required.
 * Add runtime and business scenarios in user-flow-tests; these checks do not establish that coverage.
 */
public class RouterFlowVerificationTest extends GeneratedVerificationSupport {
    @Test
    public void testFlowFactoryContract() throws Exception {
        assertFactory("org.ikasan.studio.boot.flow.routerflow.RouterFlow",
                "getRouterFlow", "org.ikasan.spec.flow.Flow");
    }

    @Test
    public void testComponentContracts() throws Exception {
        // connect to previous flow
        assertFactory("org.ikasan.studio.boot.flow.routerflow.ComponentFactoryRouterFlow",
                "getConnectToPreviousFlow", "org.ikasan.spec.component.endpoint.Consumer");
        // Both routes get it
        assertImplementation("org.example.rab.routerflow.MultiRecipient", "org.ikasan.spec.component.routing.MultiRecipientRouter");
        // route1
        // Either Or Router
        assertImplementation("org.example.rab.routerflow.EitherOrRouter", "org.ikasan.spec.component.routing.SingleRecipientRouter");
        // odd
        // String to email 1
        assertImplementation("org.example.rab.routerflow.StringToEmail1", "org.ikasan.spec.component.transformation.Converter");
        // output1
        assertFactory("org.ikasan.studio.boot.flow.routerflow.ComponentFactoryRouterFlow",
                "getOutput1", "org.ikasan.spec.component.endpoint.Producer");
        // even
        // String to email 2
        assertImplementation("org.example.rab.routerflow.StringToEmail2", "org.ikasan.spec.component.transformation.Converter");
        // output2
        assertFactory("org.ikasan.studio.boot.flow.routerflow.ComponentFactoryRouterFlow",
                "getOutput2", "org.ikasan.spec.component.endpoint.Producer");
        // route2
        // String to email
        assertImplementation("org.example.rab.routerflow.StringToEmai", "org.ikasan.spec.component.transformation.Converter");
        // output 3
        assertFactory("org.ikasan.studio.boot.flow.routerflow.ComponentFactoryRouterFlow",
                "getOutput3", "org.ikasan.spec.component.endpoint.Producer");
    }

}
