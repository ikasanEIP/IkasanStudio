package org.ikasan.studio.verification;

import org.junit.Test;

/**
 * Generated structural and interface checks for ${flow.identity?j_string}.
 * Refresh explicitly; no developer completion is required.
 * Add runtime and business scenarios in user-flow-tests; these checks do not establish that coverage.
 */
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

}
