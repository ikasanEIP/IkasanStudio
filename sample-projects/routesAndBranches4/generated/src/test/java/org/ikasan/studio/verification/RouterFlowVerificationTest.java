package org.ikasan.studio.verification;

import org.junit.Test;

/**
 * Generated structural and interface checks for Router flow.
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
        // Connect to previous flow
        assertFactory("org.ikasan.studio.boot.flow.routerflow.ComponentFactoryRouterFlow",
                "getConnectToPreviousFlow", "org.ikasan.spec.component.endpoint.Consumer");
        // Both routes get it
        assertImplementation("org.example.cat4.routerflow.BouthRoutesGetIt", "org.ikasan.spec.component.routing.MultiRecipientRouter");
        // route1
        // Either Or Router
        assertImplementation("org.example.cat4.routerflow.EitherOrRouter", "org.ikasan.spec.component.routing.SingleRecipientRouter");
        // odd
        // String to email 1
        assertImplementation("org.example.cat4.routerflow.StringToEmail1", "org.ikasan.spec.component.transformation.Converter");
        // output 1
        assertFactory("org.ikasan.studio.boot.flow.routerflow.ComponentFactoryRouterFlow",
                "getOutput1", "org.ikasan.spec.component.endpoint.Producer");
        // even
        // String to email 2
        assertImplementation("org.example.cat4.routerflow.StringToEmail2", "org.ikasan.spec.component.transformation.Converter");
        // output 2
        assertFactory("org.ikasan.studio.boot.flow.routerflow.ComponentFactoryRouterFlow",
                "getOutput2", "org.ikasan.spec.component.endpoint.Producer");
        // route2
        // String to email3
        assertImplementation("org.example.cat4.routerflow.StringToEmail", "org.ikasan.spec.component.transformation.Converter");
        // output 3
        assertFactory("org.ikasan.studio.boot.flow.routerflow.ComponentFactoryRouterFlow",
                "getOutput3", "org.ikasan.spec.component.endpoint.Producer");
    }

}
