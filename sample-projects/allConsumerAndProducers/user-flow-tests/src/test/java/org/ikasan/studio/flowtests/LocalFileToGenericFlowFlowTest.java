package org.ikasan.studio.flowtests;

import org.ikasan.studio.flowtests.support.ModuleFlowTestSupport;
import org.ikasan.studio.flowtests.support.utils.FlowTestBatch;
import java.util.List;
import java.util.Map;

import org.ikasan.testharness.flow.rule.IkasanFlowTestRule;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.ikasan.studio.flowtests.support.utils.FileInputFixture;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;


/**
 * Developer-owned scenario for Local File to Generic Flow.
 * 1. Choose the output producer and review the expected payloads below.
 * 2. Review the expected component path and any branch/delivery assertions.
 * 3. Define input payloads or fixtures for the batches in createInputOutputBatches().
 * 4. Review test connections in module-test.properties.
 * See user-flow-tests/README.md for details. Generated scaffolds are not completed tests.
 */
public class LocalFileToGenericFlowFlowTest extends ModuleFlowTestSupport {
    // Run from IntelliJ or from the project root:
    // mvn -pl user-flow-tests -am -Dtest=LocalFileToGenericFlowFlowTest -Dsurefire.failIfNoSpecifiedTests=false test
    private static final String FLOW_NAME = "Local File to Generic Flow";
    private static final String CONSUMER_NAME = "my local consumer";
    private static final String PRODUCER_NAME = "my generic producer";

    @Override protected String getFlowName() { return FLOW_NAME; }

    @Override
    protected void defineExpectedPath(IkasanFlowTestRule harness) {
        // Define the flow per batch below. This will be executed per batch supplied.
        harness.blockStart()
                .scheduledConsumer("my local consumer")
                .producer("my generic producer")
                .repeat(expectedInputCount());
    }

    // Override prepareFixtures(context) for instance fixtures after Spring starts, before the flow starts.
    // Override cleanupFixtures(context) for custom cleanup, including when fixture preparation fails.

    // Input String values are classpath resource filenames, not the file contents.
    // Each resource's bytes are copied unchanged, with a studio-input- filename prefix for safe matching.
    // Use distinct filenames; the test directory is configured automatically.
    // Resource directory names replace spaces with underscores; runtime names remain unchanged.
    private static final String FIRST_BATCH_INPUT_FILENAME = "/" + noSpaces(FLOW_NAME)
            + "/" + noSpaces(CONSUMER_NAME) + "/first.txt";
    private static final String SECOND_BATCH_INPUT_FILENAME = "/" + noSpaces(FLOW_NAME)
            + "/" + noSpaces(CONSUMER_NAME) + "/second.txt";
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
                                PRODUCER_NAME, List.of(readTestResource(FIRST_EXPECTED_OUTPUT_RESOURCE))
                        )
                ),
                batch(
                        List.of(SECOND_BATCH_INPUT_FILENAME),
                        Map.of(
                                PRODUCER_NAME, List.of(readTestResource(SECOND_EXPECTED_OUTPUT_RESOURCE))
                        )
                )
        );
    }

    @Rule
    public TemporaryFolder inputDirectory = TemporaryFolder.builder().assureDeletion().build();
    private java.nio.file.Path previousInputFile;

    @Override
    protected Map<String, String> flowTestProperties() {
        Map<String, String> properties = super.flowTestProperties();
        // JUnit creates and cleans this temporary input directory. No changes are needed here.
        properties.put("localfiletogenericflow.file.consumer.filenames",
                FileInputFixture.localFixtureFilenames(inputDirectory.getRoot().toPath()));
        return properties;
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
        runBatches(this::createInputOutputBatches, this::supplyInput);
    }

    // Receiver checks receive all expected outputs; an empty list checks for no delivery.

    // ------ Standard setup and input delivery: normally no changes needed.

    protected void supplyInput(ConfigurableApplicationContext context, IkasanFlowTestRule harness, String input) throws Exception {
        // Supply the configured input for this batch.
        // Publish original bytes with the reserved local-fixture prefix; remove only earlier batch fixtures.
        if (previousInputFile != null) java.nio.file.Files.deleteIfExists(previousInputFile);
        previousInputFile = FileInputFixture.copyLocalResource(inputDirectory.getRoot().toPath(), input);
        harness.fireScheduledConsumer();
    }

}
