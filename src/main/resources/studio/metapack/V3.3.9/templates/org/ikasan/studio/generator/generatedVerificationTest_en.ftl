package org.ikasan.studio.verification;

import org.junit.Test;
import org.junit.Assume;

/** Generated baseline for ${flow.identity?j_string}. Refresh explicitly; no developer completion is required. */
public class ${flow.javaClassName}VerificationTest extends GeneratedVerificationSupport {
    @Test
    public void testFlowFactoryContract() throws Exception {
        assertFactory("org.ikasan.studio.boot.flow.${flow.javaPackageName}.${flow.javaClassName}",
                "get${flow.javaClassName}", "org.ikasan.spec.flow.Flow");
    }

    @Test
    public void testComponentContracts() throws Exception {
<#list components as component>
        // ${component.name?replace("\n", " ")?replace("\r", " ")}
<#if component.factory == "true">
        assertFactory("org.ikasan.studio.boot.flow.${flow.javaPackageName}.ComponentFactory${flow.javaClassName}",
                "get${component.javaName}", "${component.contract?j_string}");
</#if>
<#if component.implementation?has_content>
        assertImplementation("${component.implementation?j_string}", "${component.implementationContract?j_string}");
</#if>
</#list>
    }

    @Test
    public void testRuntimeBehaviourNotVerified() {
        Assume.assumeTrue("NOT VERIFIED: automatic runtime scenario unavailable; delivery, routing, exclusions and business behaviour require functional tests", false);
    }
}
