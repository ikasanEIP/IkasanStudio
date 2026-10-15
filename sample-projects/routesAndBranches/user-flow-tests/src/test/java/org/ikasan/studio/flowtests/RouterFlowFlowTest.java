package org.ikasan.studio.flowtests;

import org.ikasan.studio.flowtests.support.ModuleFlowTestSupport;
import org.ikasan.studio.flowtests.support.utils.FlowTestBatch;
import java.util.List;
import java.util.Map;
import org.ikasan.studio.flowtests.support.ModuleJmsTestConfig;
import org.ikasan.studio.flowtests.support.utils.JmsFlowTestSupport;

import org.ikasan.testharness.flow.rule.IkasanFlowTestRule;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Developer-owned scenario for Router Flow.
 * 1. Choose the output producer and review the expected payloads below.
 * 2. Review the expected component path and any branch/delivery assertions.
 * 3. Define input payloads or fixtures for the batches in createInputOutputBatches().
 * 4. Review test connections in module-test.properties.
 * See user-flow-tests/README.md for details. Generated scaffolds are not completed tests.
 */
public class RouterFlowFlowTest extends ModuleFlowTestSupport {
    // Run from IntelliJ or from the project root:
    // mvn -pl user-flow-tests -am -Dtest=RouterFlowFlowTest -Dsurefire.failIfNoSpecifiedTests=false test
    private static final String FLOW_NAME = "Router Flow";
    // Review expected outputs for each producer in createInputOutputBatches; omit only deliberately unchecked producers:
    // "output1"
    // "output2"
    // "output 3"
    private static final String PRODUCER_NAME = "output1";

    @Override protected String getFlowName() { return FLOW_NAME; }

    @Override
    protected void defineExpectedPath(IkasanFlowTestRule harness) {
        // Every route below is listed once per input as an editable example, not an inferred business decision.
        // inputIndex is zero-based across all batches. Multi-recipient lists must match actual invocation order.
        // Remove or condition branches that are not visited; match producer outputs to the remaining paths.
        for (int inputIndex = 0; inputIndex < expectedInputCount(); inputIndex++) {
            int orderID = inputIndex + 1;
            harness.consumer("connect to previous flow");
            harness.multiRecipientRouter("Both routes get it");
            // Begin route "route1"
            harness.router("Either Or Router");
            // This is a single-recipient router: keep only the branch expected for this input.
            // Begin route "odd"
            if (orderID%2 != 0) {
                harness.converter("String to email 1");
                harness.producer("output1");
            }
            // End route "odd"

            // Begin route "even"
            if (orderID%2 == 0) {
                harness.converter("String to email 2");
                harness.producer("output2");
                // End route "even"
            }
            // End route "route1"

            // Begin route "route2"
            harness.converter("String to email");
            harness.producer("output 3");
            // End route "route2"
        }
    }

    // Override prepareFixtures(context) for instance fixtures after Spring starts, before the flow starts.
    // Override cleanupFixtures(context) for custom cleanup, including when fixture preparation fails.

    private static final String FIRST_BATCH_INPUT = "Item Sat Jan 01 00:00:00 GMT 2000 A=1";
    private static final String SECOND_BATCH_INPUT = "Item Sat Jan 01 00:00:00 GMT 2000 A=2";
    private static final String THIRD_BATCH_INPUT = "Item Sat Jan 01 00:00:00 GMT 2000 B=3";
    private static final String FOURTH_BATCH_INPUT = "Item Sat Jan 01 00:00:00 GMT 2000 B=4";

    // Alternative: put UTF-8 fixture files in user-flow-tests/src/test/resources/input/.
    // Replace the input constants above with these styles of declarations (do not keep both):
    // private static final String FIRST_BATCH_INPUT = readTestResource("/input/first.txt");
    // Paths are relative to src/test/resources, not your working directory. Whitespace is preserved.

    // This method provides the input values and expected output values. The general structure is:
    //
    // return List.of(
    //     batch(
    //         List.of( /* one element per input; multiple inputs need not produce multiple outputs */ ),
    //         Map.of(
    //             PRODUCER_NAME1, List.of( /* expected outputs for the above inputs */ ),
    //             PRODUCER_NAME2, List.of( /* expected outputs from a second producer, if applicable */ )
    //         )
    //     ),
    //     batch( /* repeat for every batch of tests */ )
    // );
    //
    // Expected business objects must implement equals (and matching hashCode).
    // For one timer event without business input, use triggeredBatch(Map.of(...)).
    // batch(List.of(), ...) only observes: it supplies no input and fires no timer.
    // With no output checks, use Map.of().
    // Map.of() skips output checks; Map.of(PRODUCER_NAME, List.of()) asserts no output there.
    //
    protected List<FlowTestBatch<String>> createInputOutputBatches(ConfigurableApplicationContext context) throws Exception {
        // Runs after prepareFixtures(context), so inputs may use Spring beans or runtime fixtures.
        return List.of(
                batch(
                        List.of(FIRST_BATCH_INPUT),
                        Map.ofEntries(
                                Map.entry("output1", List.of(FIRST_EXPECTED_OUTPUT)),
                                Map.entry("output 3", List.of(FIRST_EXPECTED_OUTPUT))
                        )
                ),
                batch(
                        List.of(SECOND_BATCH_INPUT),
                        Map.ofEntries(
                                Map.entry("output2", List.of(SECOND_EXPECTED_OUTPUT)),
                                Map.entry("output 3", List.of(SECOND_EXPECTED_OUTPUT))
                        )
                ),
                batch(
                        List.of(THIRD_BATCH_INPUT),
                        Map.ofEntries(
                                Map.entry("output1", List.of(THIRD_EXPECTED_OUTPUT)),
                                Map.entry("output 3", List.of(THIRD_EXPECTED_OUTPUT))
                        )
                ),
                batch(
                        List.of(FOURTH_BATCH_INPUT),
                        Map.ofEntries(
                                Map.entry("output2", List.of(FOURTH_EXPECTED_OUTPUT)),
                                Map.entry("output 3", List.of(FOURTH_EXPECTED_OUTPUT))
                        )
                )
        );
    }


    // Decode actual Payload/byte[]/File/Path/file-list/JMS TextMessage content for comparisons.
    // true decodes supported content as text; false uses toString(). Both modes compare text.
    private static final boolean DECODE_OUTPUT_CONTENT_AS_TEXT = true;

    // Set the expected text outputs for the first and second test. The test compares these values with formatOutputText(actualPayload).
    // Override formatOutputText(Object payload) to decode the actual payload, for example UTF-8 file content.
    private static final String FIRST_EXPECTED_OUTPUT = FIRST_BATCH_INPUT;
    private static final String SECOND_EXPECTED_OUTPUT = SECOND_BATCH_INPUT;
    private static final String THIRD_EXPECTED_OUTPUT = THIRD_BATCH_INPUT;
    private static final String FOURTH_EXPECTED_OUTPUT = FOURTH_BATCH_INPUT;


    // Expected business objects must implement equals (and matching hashCode).
    // Only String expectations use formatOutputText; remove this override if no text checks remain.
    @Override protected String formatOutputText(Object payload) {
        return formatOutputText(payload, DECODE_OUTPUT_CONTENT_AS_TEXT);
    }

    @Override
    protected void assertExpectedOutput(Object expected, Object actualAfterProducer) {
        Object actual = expected instanceof String
                ? formatOutputText(actualAfterProducer)
                : actualAfterProducer;

        // Set a breakpoint here to inspect expected, actual and actualAfterProducer.
        org.junit.Assert.assertEquals(expected, actual);
    }

    // Starts once, supplies every batch, checks outputs and idle readiness, then stops during teardown.
    // A single batch cannot demonstrate later delivery; use at least two for that check.
    @Test
    public void testFirstAndLaterDeliveryWithoutRestart() throws Exception {
        runBatches(this::createInputOutputBatches, this::supplyInput);
    }

    // Receiver checks receive all expected outputs; an empty list checks for no delivery.

    // ------ Standard setup and input delivery: normally no changes needed.
    @Override protected Class<?>[] testConfigurationClasses() {
        return new Class<?>[]{ModuleJmsTestConfig.class};
    }

    protected void supplyInput(ConfigurableApplicationContext context, IkasanFlowTestRule harness, String input) throws Exception {
        // Supply the configured input for this batch.
        // ModuleJmsTestConfig supplies the test connection. Configure test.jms.broker-url
        // and matching isolated flow broker/destinations in module-test.properties.
        try (JmsFlowTestSupport jms = JmsFlowTestSupport.from(context)) {
            // Text is a starting fixture; review the first processing component's required JMS message type.
            jms.sendText(context.getEnvironment().getRequiredProperty("jms.routesandbranches.routerflow.connecttopreviousflow.destination"),
                    input);
        }
    }

}
