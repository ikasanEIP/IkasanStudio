package org.ikasan.studio.flowtests;

import org.ikasan.studio.flowtests.support.ModuleFlowTestSupport;
import org.ikasan.studio.flowtests.support.utils.FlowTestBatch;
import java.util.List;
import java.util.Map;
import org.ikasan.studio.flowtests.support.utils.ScheduledEventFixture;
import org.quartz.JobExecutionContext;

import org.ikasan.testharness.flow.rule.IkasanFlowTestRule;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;
import java.util.Map;

/**
 * Developer-owned scenario for Splitter Flow.
 * 1. Choose the output producer and review the expected payloads below.
 * 2. Review the expected component path and any branch/delivery assertions.
 * 3. Define input payloads or fixtures for the batches in createInputOutputBatches().
 * 4. Review test connections in module-test.properties.
 * See user-flow-tests/README.md for details. Generated scaffolds are not completed tests.
 */
public class SplitterFlowFlowTest extends ModuleFlowTestSupport {
    // Run from IntelliJ or from the project root:
    // mvn -pl user-flow-tests -am -Dtest=SplitterFlowFlowTest -Dsurefire.failIfNoSpecifiedTests=false test
    private static final String FLOW_NAME = "Splitter Flow";
    private static final String PRODUCER_NAME = "Connect to next flow";

    @Override protected String getFlowName() { return FLOW_NAME; }

    @Override
    protected void defineExpectedPath(IkasanFlowTestRule harness) {
        // The component sequence is known; split counts depend on your fixtures and business logic.
        // Each count below is an example of ONE output item, not an inferred expectation.
        // Adjust inside the loops if counts differ by input or by an earlier splitter's item.
        // Final expected outputs alone cannot tell us the intermediate split counts.
        for (int inputIndex = 0; inputIndex < expectedInputCount(); inputIndex++) {
            harness.scheduledConsumer("Message Trigger");
            harness.broker("Message for Batch Generator");
            harness.splitter("Default Split List to individuals");
            int expectedSplitItems1 = 2;
            for (int splitItem1 = 0; splitItem1 < expectedSplitItems1; splitItem1++) {
                harness.splitter("Standard split List to String");
                int expectedSplitItems2 = 2;
                for (int splitItem2 = 0; splitItem2 < expectedSplitItems2; splitItem2++) {
                    harness.producer("Connect to next flow");
                }
            }
        }
    }

    // Override prepareFixtures(context) for instance fixtures after Spring starts, before the flow starts.
    // Override cleanupFixtures(context) for custom cleanup, including when fixture preparation fails.

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
                triggeredBatch(
                        Map.of(
                                PRODUCER_NAME, List.of(
                                        FIRST_EXPECTED_OUTPUTA,FIRST_EXPECTED_OUTPUTB,
                                        FIRST_EXPECTED_OUTPUTC, FIRST_EXPECTED_OUTPUTD)
                        )
                )
        );
    }


    // Decode actual Payload/byte[]/File/Path/file-list/JMS TextMessage content for comparisons.
    // true decodes supported content as text; false uses toString(). Both modes compare text.
    private static final boolean DECODE_OUTPUT_CONTENT_AS_TEXT = true;

    // Set the expected text outputs for the first and second test. The test compares these values with formatOutputText(actualPayload).
    // Override formatOutputText(Object payload) to decode the actual payload, for example UTF-8 file content.
    private static final String FIRST_EXPECTED_OUTPUTA = "Item Sat Jan 01 00:00:00 GMT 2000 A=0";
    private static final String FIRST_EXPECTED_OUTPUTB = "Item Sat Jan 01 00:00:00 GMT 2000 A=0";
    private static final String FIRST_EXPECTED_OUTPUTC = "Item Sat Jan 01 00:00:00 GMT 2000 B=0";
    private static final String FIRST_EXPECTED_OUTPUTD = "Item Sat Jan 01 00:00:00 GMT 2000 B=0";


    // Expected business objects must implement equals (and matching hashCode).
    // Only String expectations use formatOutputText; remove this override if no text checks remain.
    @Override protected String formatOutputText(Object payload) {
        // The default provider forwards the timer context, including our test fixture text.
        if (DECODE_OUTPUT_CONTENT_AS_TEXT && payload instanceof JobExecutionContext) {
            return ScheduledEventFixture.text((JobExecutionContext) payload);
        }
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

    protected void supplyInput(ConfigurableApplicationContext context, IkasanFlowTestRule harness, String input) throws Exception {
        // Supply the configured input for this batch.
        // Fire the real scheduled consumer now; no waiting for its cron expression.
        // This creates a Quartz timer event carrying fixture text, not a String business payload.
        // If the broker supplies its own data, prepare its source and use the trigger-only overload:
        // ScheduledEventFixture.fire(harness, "Message Trigger");
        // Use triggeredBatch(outputs) for trigger-only events; this input method is then not called.
        ScheduledEventFixture.fire(harness, "Message Trigger",
                input);
    }

}
