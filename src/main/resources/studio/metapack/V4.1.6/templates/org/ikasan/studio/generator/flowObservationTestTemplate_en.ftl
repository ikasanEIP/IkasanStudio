package org.ikasan.studio.flowtests;

import org.junit.Test;
<#if expectedInitialOutputs?has_content>

import java.util.List;
</#if>

/**
 * Developer-owned observation test for ${flowName?j_string}.
 * The source generates its own input; the sink discards it. No external output is expected.
 * This checks the declared initial payload sequence and continued running, not external delivery.
 */
public class ${className} extends ModuleFlowTestSupport {
    // TODO 1: Review src/test/resources/module-test.properties for other module startup connections.
    // TODO 2: Review the observation below, then set CONFIGURED=true and run this test.
    // mvn -pl user-flow-tests -am -Dtest=${className} -Dsurefire.failIfNoSpecifiedTests=false test
    private static final boolean CONFIGURED = false;

    @Override protected String getFlowName() { return "${flowName?j_string}"; }

    @Test(timeout = 60000)
    public void testGeneratedEventsReachProducerAndFlowKeepsRunning() throws Exception {
        runObservationTest(CONFIGURED, "${producers[0]?j_string}"<#if expectedInitialOutputs?has_content>,
                // Expected messages from the built-in event provider.
                List.of(<#list expectedInitialOutputs as output>"${output?j_string}"<#sep>, </#sep></#list>)</#if>);
    }
}
