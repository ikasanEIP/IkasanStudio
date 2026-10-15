package org.ikasan.studio.verification;

import org.junit.Test;

/**
 * Generated structural and interface checks for create an order flow.
 * Refresh explicitly; no developer completion is required.
 * Add runtime and business scenarios in user-flow-tests; these checks do not establish that coverage.
 */
public class CreateAnOrderFlowVerificationTest extends GeneratedVerificationSupport {
    @Test
    public void testFlowFactoryContract() throws Exception {
        assertFactory("org.ikasan.studio.boot.flow.createanorderflow.CreateAnOrderFlow",
                "getCreateAnOrderFlow", "org.ikasan.spec.flow.Flow");
    }

    @Test
    public void testComponentContracts() throws Exception {
        // at a recurring
        assertFactory("org.ikasan.studio.boot.flow.createanorderflow.ComponentFactoryCreateAnOrderFlow",
                "getAtARecurring", "org.ikasan.spec.component.endpoint.Consumer");
        // Artifically Create order
        assertImplementation("org.example.cat4.createanorderflow.ArtificallyCreateOrder", "org.ikasan.spec.component.endpoint.Broker");
        // Artifically Create orderDebug
        assertImplementation("org.example.cat4.createanorderflow.Flow0CreateAnOrderArtificallyCreateOrderDebug", "org.ikasan.spec.component.filter.Filter");
        // Translator add order num
        assertImplementation("org.example.cat4.createanorderflow.TranslatorAddOrderNum", "org.ikasan.spec.component.transformation.Translator");
        // My order generator
        assertFactory("org.ikasan.studio.boot.flow.createanorderflow.ComponentFactoryCreateAnOrderFlow",
                "getMyOrderGenerator", "org.ikasan.spec.component.endpoint.Producer");
    }

}
