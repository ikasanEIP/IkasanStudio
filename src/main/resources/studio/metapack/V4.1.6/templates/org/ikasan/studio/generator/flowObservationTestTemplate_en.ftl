package org.ikasan.studio.flowtests;

import org.ikasan.studio.flowtests.support.ModuleFlowTestSupport;

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
    // TODO 2: Review the observation below, then set TEST_REVIEWED=true and run this test.
    // mvn -pl user-flow-tests -am -Dtest=${className} -Dsurefire.failIfNoSpecifiedTests=false test
    private static final boolean TEST_REVIEWED = false;

    private static final String FLOW_NAME = "${flowName?j_string}";
    @Override protected String getFlowName() { return FLOW_NAME; }

    // Override prepareFixtures(context) for instance fixtures after Spring starts, before the flow starts.
    // Override cleanupFixtures(context) for custom cleanup, including when fixture preparation fails.

    @Test
    public void testGeneratedEventsReachProducerAndFlowKeepsRunning() throws Exception {
        runObservationTest(TEST_REVIEWED, "${producers[0]?j_string}"<#if expectedInitialOutputs?has_content>,
                // Expected messages from the built-in event provider.
                List.of(<#list expectedInitialOutputs as output>"${output?j_string}"<#sep>, </#sep></#list>)</#if>);
    }
}
