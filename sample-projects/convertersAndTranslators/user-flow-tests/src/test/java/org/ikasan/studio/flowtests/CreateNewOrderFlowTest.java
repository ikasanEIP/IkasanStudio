package org.ikasan.studio.flowtests;

import org.ikasan.studio.flowtests.support.ModuleFlowTestSupport;
import org.ikasan.studio.flowtests.support.utils.FlowTestBatch;
import java.util.List;
import java.util.Map;
import org.example.cat.domain.Order;
import org.ikasan.studio.flowtests.support.utils.ScheduledEventFixture;
import org.quartz.JobExecutionContext;

import org.ikasan.testharness.flow.rule.IkasanFlowTestRule;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Developer-owned scenario for Create New Order.
 * Complete the numbered TODOs below in order (IntelliJ's TODO view can locate them):
 * 1. Choose the output producer and review the expected payloads below.
 * 2. Review the expected component path and any branch/delivery assertions.
 * 3. Define input payloads or fixtures for the batches in createInputOutputBatches().
 * 4. Review test connections in module-test.properties.
 * Run immediately to see what works; use the TODOs to refine inputs and assertions.
 * See user-flow-tests/README.md for details. Generated scaffolds are not completed tests.
 */
public class CreateNewOrderFlowTest extends ModuleFlowTestSupport {
    // Run from IntelliJ or from the project root:
    // mvn -pl user-flow-tests -am -Dtest=CreateNewOrderFlowTest -Dsurefire.failIfNoSpecifiedTests=false test
    private static final String FLOW_NAME = "Create New Order";
    private static final String PRODUCER_NAME = "Send for Processing";

    @Override protected String getFlowName() { return FLOW_NAME; }

    @Override
    protected void defineExpectedPath(IkasanFlowTestRule harness) {
        // Define the flow per batch below. This will be executed per batch supplied.
        harness.blockStart()
                .scheduledConsumer("Poll for orders")
                .broker("Get order items")
                .translator("Add order num")
                .producer("Send for Processing")
                .repeat(expectedInputCount());
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
                                PRODUCER_NAME, List.of(FIRST_EXPECTED_OUTPUT)
                        )
                ),
                triggeredBatch(
                        Map.of(
                                PRODUCER_NAME, List.of(SECOND_EXPECTED_OUTPUT)
                        )
                )
        );
    }


    // Decode actual Payload/byte[]/File/Path/file-list/JMS TextMessage content for comparisons.
    // true decodes supported content as text; false uses toString(). Both modes compare text.
    private static final boolean DECODE_OUTPUT_CONTENT_AS_TEXT = true;

    // Populate each expected business object with the values the producer should observe.
    // Business objects must implement equals (and matching hashCode).
    // Adjust constructor calls if the class requires arguments or a factory method.
    private static final Order FIRST_EXPECTED_OUTPUT = new Order("mr-ref 2000-01-01T00:00:00Z", 0, 0);
    private static final Order SECOND_EXPECTED_OUTPUT = new Order("mr-ref 2000-01-01T00:00:00Z", 1, 1);

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
        // ScheduledEventFixture.fire(harness, "Poll for orders");
        // Use triggeredBatch(outputs) for trigger-only events; this input method is then not called.
        ScheduledEventFixture.fire(harness, "Poll for orders",
                input);
    }

}
