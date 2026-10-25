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
 * Developer-owned scenario for JMS to SFTP Flow.
 * Complete the numbered TODOs below in order (IntelliJ's TODO view can locate them):
 * 1. Choose the output producer and review the expected payloads below.
 * 2. Review the expected component path and any branch/delivery assertions.
 * 3. Define input payloads or fixtures for the batches in createInputOutputBatches().
 * 4. Review test connections in module-test.properties.
 * Run immediately to see what works; use the TODOs to refine inputs and assertions.
 * See user-flow-tests/README.md for details. Generated scaffolds are not completed tests.
 */
public class JMSToSFTPFlowFlowTest extends ModuleFlowTestSupport {
    // Run from IntelliJ or from the project root:
    // mvn -pl user-flow-tests -am -Dtest=JMSToSFTPFlowFlowTest -Dsurefire.failIfNoSpecifiedTests=false test
    private static final String FLOW_NAME = "JMS to SFTP Flow";
    private static final String PRODUCER_NAME = "my sftp producer";

    @Override protected String getFlowName() { return FLOW_NAME; }

    @Override
    protected void defineExpectedPath(IkasanFlowTestRule harness) {
        // Define the flow per batch below. This will be executed per batch supplied.
        harness.blockStart()
                .consumer("my JSM consumer")
                .converter("my jms to ftp converter")
                .producer("my sftp producer")
                .repeat(expectedInputCount());
    }

    // Override prepareFixtures(context) for instance fixtures after Spring starts, before the flow starts.
    // Override cleanupFixtures(context) for custom cleanup, including when fixture preparation fails.

    private static final String FIRST_BATCH_INPUT = "first expected payload";
    private static final String SECOND_BATCH_INPUT = "second expected payload";

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
                        Map.of(
                                PRODUCER_NAME, List.of(readTestResource(FIRST_EXPECTED_OUTPUT_RESOURCE))
                        )
                ),
                batch(
                        List.of(SECOND_BATCH_INPUT),
                        Map.of(
                                PRODUCER_NAME, List.of(readTestResource(SECOND_EXPECTED_OUTPUT_RESOURCE))
                        )
                )
        );
    }


    // Decode actual Payload/byte[]/File/Path/file-list/JMS TextMessage content for comparisons.
    // true decodes supported content as text; false uses toString(). Both modes compare text.
    private static final boolean DECODE_OUTPUT_CONTENT_AS_TEXT = true;

    // Review the sample expected UTF-8 output files under src/test/resources/<flow>/<producer>/.
    // Replace spaces in the directory names with underscores, as for input resources.
    // Keep expected-output fixtures independent of input fixtures; transformations may change the contents.
    // These resource names do not dictate the producer's delivered filenames.
    private static final String FIRST_EXPECTED_OUTPUT_RESOURCE = "/" + noSpaces(FLOW_NAME)
            + "/" + noSpaces(PRODUCER_NAME) + "/first.txt";
    private static final String SECOND_EXPECTED_OUTPUT_RESOURCE = "/" + noSpaces(FLOW_NAME)
            + "/" + noSpaces(PRODUCER_NAME) + "/second.txt";

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
        runBatches(this::createInputOutputBatches, this::supplyInput,
                (context, batchNumber, definition) -> {
                    if (definition.producers().contains(PRODUCER_NAME))
                        verifyReceivedOutput(context, batchNumber, definition.expectedTexts(PRODUCER_NAME));
                });
    }

    // Receiver checks receive all expected outputs; an empty list checks for no delivery.

    @Override
    protected void verifyReceivedOutput(ConfigurableApplicationContext context, int batch, List<String> expected) throws Exception {
        // Check the output artifacts created by the producer.
        // The local test SFTP server owns a separate directory for this producer.
        assertDeliveredBatchContents(localSftpDirectory(context, FLOW_NAME, PRODUCER_NAME), "*", batch,
                expected.toArray(new String[0]));
    }

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
            jms.sendText(context.getEnvironment().getRequiredProperty("jms.allconsumersandproducers5.jmstosftpflow.myjsmconsumer.destination"),
                    input);
        }
    }

}
