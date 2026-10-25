package org.ikasan.studio.flowtests;

import org.ikasan.studio.flowtests.support.ModuleFlowTestSupport;
import org.ikasan.studio.flowtests.support.utils.FlowTestBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ikasan.studio.flowtests.support.utils.FtpInputFixture;

import org.ikasan.testharness.flow.rule.IkasanFlowTestRule;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Developer-owned scenario for Payload To JMS Map Demo.
 * Complete the numbered TODOs below in order (IntelliJ's TODO view can locate them):
 * 1. Choose the output producer and review the expected payloads below.
 * 2. Review the expected component path and any branch/delivery assertions.
 * 3. Define input payloads or fixtures for the batches in createInputOutputBatches().
 * 4. Review test connections in module-test.properties.
 * Run immediately to see what works; use the TODOs to refine inputs and assertions.
 * See user-flow-tests/README.md for details. Generated scaffolds are not completed tests.
 */
public class PayloadToJMSMapDemoFlowTest extends ModuleFlowTestSupport {
    // Run from IntelliJ or from the project root:
    // mvn -pl user-flow-tests -am -Dtest=PayloadToJMSMapDemoFlowTest -Dsurefire.failIfNoSpecifiedTests=false test
    private static final String FLOW_NAME = "Payload To JMS Map Demo";
    private static final String CONSUMER_NAME = "Read Demo Files";
    private static final String PRODUCER_NAME = "Send Map Message";

    @Override protected String getFlowName() { return FLOW_NAME; }

    @Override
    protected void defineExpectedPath(IkasanFlowTestRule harness) {
        // Define the flow per batch below. This will be executed per batch supplied.
        harness.blockStart()
                .scheduledConsumer("Read Demo Files")
                .converter("Payload To Map")
                .producer("Send Map Message")
                .repeat(expectedInputCount());
    }

    // Input String values are classpath resource filenames, not the file contents.
    // Each resource is copied unchanged to the JUnit test input directory using only its filename.
    // Use distinct filenames; the test directory is configured automatically.
    // Filenames must match the FTP/SFTP consumer's filenamePattern.
    // Resource directory names replace spaces with underscores; runtime names remain unchanged.
    private static final String FIRST_BATCH_INPUT_FILENAME = "/" + noSpaces(FLOW_NAME)
            + "/" + noSpaces(CONSUMER_NAME) + "/payload-map-demo-first.txt";
    private static final String SECOND_BATCH_INPUT_FILENAME = "/" + noSpaces(FLOW_NAME)
            + "/" + noSpaces(CONSUMER_NAME) + "/payload-map-demo-second.txt";
    // If needed for an assertion: readTestResource(FIRST_BATCH_INPUT_FILENAME) reads its UTF-8 text.

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
                        List.of(FIRST_BATCH_INPUT_FILENAME),
                        Map.of(
                                PRODUCER_NAME, List.of(FIRST_EXPECTED_OUTPUT)
                        )
                ),
                batch(
                        List.of(SECOND_BATCH_INPUT_FILENAME),
                        Map.of(
                                PRODUCER_NAME, List.of(SECOND_EXPECTED_OUTPUT)
                        )
                )
        );
    }

    // Map.equals compares keys and values without depending on HashMap iteration order.
    // The FTP payload ID is a String derived from filename and file timestamp, not a fixed integer.
    private static final Map<String, Object> FIRST_EXPECTED_OUTPUT = Map.of(
            "fileName", "payload-map-demo-first.txt",
            "content", "first expected payload");
    private static final Map<String, Object> SECOND_EXPECTED_OUTPUT = Map.of(
            "fileName", "payload-map-demo-second.txt",
            "content", "second expected payload");

    @Override
    protected void assertExpectedOutput(Object expected, Object actualAfterProducer) {
        org.junit.Assert.assertTrue("Expected a Map at Send Map Message", actualAfterProducer instanceof Map);
        // Ignore only the variable FTP ID; keep the original observed map unchanged for debugging.
        Map<?, ?> actual = new HashMap<>((Map<?, ?>) actualAfterProducer);
        // ID is non-deterministic so don't bother to test
        actual.remove("id");

        // Useful breakpoint: expected, actual and actualAfterProducer are visible together.
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
        // Create the files/provider data here BEFORE firing.
        // Account for filename filters, minimum file age and duplicate detection when applicable.
        prepareInputBatch(context, input);
        harness.fireScheduledConsumer();
    }

    private void prepareInputBatch(ConfigurableApplicationContext context, String input) throws Exception {
        // Enable test.ftp.enabled in module-test.properties.
        // Files are created in the test FTP server's JUnit temporary directory.
        // The classpath resource is streamed unchanged; only its basename is used at the destination.
        FtpInputFixture.copyResource(localFtpDirectory(context),
                context.getEnvironment().getRequiredProperty("payloadtojmsmapdemo.ftp.consumer.filename-pattern"),
                input);
    }

}
