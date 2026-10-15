package org.ikasan.studio.verification;

import org.junit.Test;

/**
 * Generated structural and interface checks for move from Java Object to XML to String flow.
 * Refresh explicitly; no developer completion is required.
 * Add runtime and business scenarios in user-flow-tests; these checks do not establish that coverage.
 */
public class MoveFromJavaObjectToXMLToStringFlowVerificationTest extends GeneratedVerificationSupport {
    @Test
    public void testFlowFactoryContract() throws Exception {
        assertFactory("org.ikasan.studio.boot.flow.movefromjavaobjecttoxmltostringflow.MoveFromJavaObjectToXMLToStringFlow",
                "getMoveFromJavaObjectToXMLToStringFlow", "org.ikasan.spec.flow.Flow");
    }

    @Test
    public void testComponentContracts() throws Exception {
        // my order consumer
        assertFactory("org.ikasan.studio.boot.flow.movefromjavaobjecttoxmltostringflow.ComponentFactoryMoveFromJavaObjectToXMLToStringFlow",
                "getMyOrderConsumer", "org.ikasan.spec.component.endpoint.Consumer");
        // From JMS Obj to Java Obj
        // Java Obj to String
        assertFactory("org.ikasan.studio.boot.flow.movefromjavaobjecttoxmltostringflow.ComponentFactoryMoveFromJavaObjectToXMLToStringFlow",
                "getJavaObjToString", "org.ikasan.spec.component.transformation.Converter");
        // String to email
        assertImplementation("org.example.cat4.movefromjavaobjecttoxmltostringflow.StringToEmail", "org.ikasan.spec.component.transformation.Converter");
        // Sent to email
        assertFactory("org.ikasan.studio.boot.flow.movefromjavaobjecttoxmltostringflow.ComponentFactoryMoveFromJavaObjectToXMLToStringFlow",
                "getSentToEmail", "org.ikasan.spec.component.endpoint.Producer");
    }

}
