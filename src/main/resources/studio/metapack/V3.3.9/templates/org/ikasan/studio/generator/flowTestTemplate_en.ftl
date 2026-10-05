package org.ikasan.studio.flowtests;

import org.ikasan.studio.flowtests.support.ModuleFlowTestSupport;
<#if scheduledContext>
import org.ikasan.studio.flowtests.support.utils.ScheduledEventFixture;
import org.quartz.JobExecutionContext;
</#if>
<#if ftpInput>
import org.ikasan.studio.flowtests.support.utils.FtpInputFixture;
</#if>
<#if jmsConsumer>
import org.ikasan.studio.flowtests.support.ModuleJmsTestConfig;
import org.ikasan.studio.flowtests.support.utils.JmsFlowTestSupport;
</#if>

import org.ikasan.testharness.flow.rule.IkasanFlowTestRule;
import org.junit.Test;
<#if fileDelivery || smtpDelivery>
import static org.junit.Assert.assertTrue;
</#if>
import org.springframework.context.ConfigurableApplicationContext;
<#if localFile>
import java.util.Map;
</#if>
<#if isolatedFiles || sftpInput>
import org.ikasan.studio.flowtests.support.utils.FileInputFixture;
</#if>
<#if isolatedFiles>
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

</#if>

/**
 * Developer-owned scenario for ${flowName?j_string}.
 * Complete the numbered TODOs below in order (IntelliJ's TODO view can locate them):
 * 1. Supply first and later input in supplyInput().
 * 2. Choose the output producer and a stable payload representation.
 * 3. Review the expected component path and any branch/delivery assertions.
 * 4. Review test connections in module-test.properties.
 * 5. Enable the completed scenario and run it.
 * See user-flow-tests/README.md for details. Generated scaffolds are not completed tests.
 */
public class ${className} extends ModuleFlowTestSupport {
    // TODO 5: After completing TODOs 1–4, set TEST_REVIEWED=true and run from the project root:
    // mvn -pl user-flow-tests -am -Dtest=${className} -Dsurefire.failIfNoSpecifiedTests=false test
    private static final boolean TEST_REVIEWED = false;
    private static final String FLOW_NAME = "${flowName?j_string}";
    @Override protected String getFlowName() { return FLOW_NAME; }

    // Override prepareFixtures(context) for instance fixtures after Spring starts, before the flow starts.
    // Override cleanupFixtures(context) for custom cleanup, including when fixture preparation fails.

<#if jmsConsumer>
    @Override protected Class<?>[] testConfigurationClasses() {
        return new Class<?>[]{ModuleJmsTestConfig.class};
    }
</#if>

<#if ftpInput || sftpInput || isolatedFiles>
    // TODO 1: Review the sample input files under user-flow-tests/src/test/resources (or change the paths).
    // Each resource is copied unchanged to the JUnit test input directory using only its filename.
    // Use distinct filenames; the test directory is configured automatically.
<#if ftpInput || sftpInput>
    // Filenames must match the FTP/SFTP consumer's filenamePattern.
</#if>
    private static final String CONSUMER_NAME = "${consumerName?j_string}";
    // Resource directory names replace spaces with underscores; runtime names remain unchanged.
    private static final String FIRST_BATCH_INPUT_FILENAME = "/" + noSpaces(FLOW_NAME)
            + "/" + noSpaces(CONSUMER_NAME) + "/first.txt";
    private static final String SECOND_BATCH_INPUT_FILENAME = "/" + noSpaces(FLOW_NAME)
            + "/" + noSpaces(CONSUMER_NAME) + "/second.txt";
    // If needed for an assertion: readTestResource(FIRST_BATCH_INPUT_FILENAME) reads its UTF-8 text.
<#else>
    // TODO 1: Set the data supplied for each batch; expected outputs below are independent assertions.
    private static final String FIRST_BATCH_INPUT = "first expected payload";
    private static final String SECOND_BATCH_INPUT = "second expected payload";

    // Alternative: put UTF-8 fixture files in user-flow-tests/src/test/resources/input/.
    // Replace the two input constants above with these declarations (do not keep both):
    // private static final String FIRST_BATCH_INPUT = readTestResource("/input/first.txt");
    // private static final String SECOND_BATCH_INPUT = readTestResource("/input/second.txt");
    // Paths are relative to src/test/resources, not your working directory. Whitespace is preserved.
</#if>

<#if isolatedFiles>
    @Rule
    public TemporaryFolder inputDirectory = TemporaryFolder.builder().assureDeletion().build();
</#if>

<#if localFile>
    @Override
    protected Map<String, String> flowTestProperties() {
        Map<String, String> properties = super.flowTestProperties();
<#if isolatedFiles>
        // JUnit creates and cleans this temporary input directory. No changes are needed here.
        properties.put("${filenameKey?j_string}",
                FileInputFixture.localFilenames(inputDirectory.getRoot().toPath(),
                        FIRST_BATCH_INPUT_FILENAME, SECOND_BATCH_INPUT_FILENAME));
<#elseif localFile>
        // Set this consumer's filenames in Studio and regenerate so a temporary directory can be supplied.
        throw new UnsupportedOperationException("Configure isolated filenames before running this test");
</#if>
<#if !localFile || isolatedFiles>
        return properties;
</#if>
    }
</#if>
    @Override
    protected void supplyInput(ConfigurableApplicationContext context, IkasanFlowTestRule harness, int batch) throws Exception {
        // Supply the configured input for this batch.
<#if jmsConsumer>
        // ModuleJmsTestConfig supplies the test connection. Configure test.jms.broker-url
        // and matching isolated flow broker/destinations in module-test.properties.
        try (JmsFlowTestSupport jms = JmsFlowTestSupport.from(context)) {
            jms.sendText(context.getEnvironment().getRequiredProperty("${jmsInputKey?j_string}"),
                    batch == 1 ? FIRST_BATCH_INPUT : SECOND_BATCH_INPUT);
        }
<#elseif sampleSubmission>
        // The following property has been set in module-test.properties so that the ${sampleConsumerClass} does not
        // automatically pole on its configured time based schedule, but instead is triggered by the test
        // studio.sample-consumer.${sampleConsumerClass}.fixture-input-enabled=true
        context.getBean(${sampleConsumerClass}.class)
                .submitNow(batch == 1 ? FIRST_BATCH_INPUT : SECOND_BATCH_INPUT);
<#elseif isolatedFiles>
        // Copy the resource's original bytes using its basename; remove only earlier batch fixtures.
        FileInputFixture.prepareLocalBatch(inputDirectory.getRoot().toPath(), batch,
                FIRST_BATCH_INPUT_FILENAME, SECOND_BATCH_INPUT_FILENAME);
        harness.fireScheduledConsumer();
<#elseif scheduledContext>
        // Fire the real scheduled consumer now; no waiting for its cron expression.
        // This creates a Quartz timer event carrying fixture text, not a String business payload.
        // If the broker supplies its own data, prepare its source and use the trigger-only overload:
        // ScheduledEventFixture.fire(harness, "${consumerName?j_string}");
        // Then remove the unused batch input constants; compare the broker output below.
        ScheduledEventFixture.fire(harness, "${consumerName?j_string}",
                batch == 1 ? FIRST_BATCH_INPUT : SECOND_BATCH_INPUT);
<#elseif scheduled>
        // Create the files/provider data here BEFORE firing. Scanning does not create input.
        // Account for filename filters, minimum file age and duplicate detection when applicable.
        prepareInputBatch(context, batch);
        harness.fireScheduledConsumer();
<#else>
        // Use batch == 1 ? FIRST_BATCH_INPUT : SECOND_BATCH_INPUT as the batch content.
        // Send through the real consumer API (for example JMS), or supply a controllable test provider.
        // Self-generating sources must supply deterministic batches without busy loops or restarting.
        throw new UnsupportedOperationException("Supply input batch " + batch + " for ${consumerName?j_string}");
</#if>
    }
<#if scheduled && !isolatedFiles && !scheduledContext>

    private void prepareInputBatch(ConfigurableApplicationContext context, int batch) throws Exception {
<#if sftpInput>
        // Copy the fixture into this consumer's directory on the local test SFTP server.
        // Enable test.sftp.enabled in module-test.properties; JUnit owns directory cleanup.
        FileInputFixture.copyResource(localSftpDirectory(context, FLOW_NAME, CONSUMER_NAME),
                context.getEnvironment().getRequiredProperty("${sftpInputPatternKey?j_string}"),
                batch == 1 ? FIRST_BATCH_INPUT_FILENAME : SECOND_BATCH_INPUT_FILENAME);
<#elseif ftpInput>
        // Enable test.ftp.enabled in module-test.properties.
        // Files are created in the test FTP server's JUnit temporary directory.
        // The classpath resource is streamed unchanged; only its basename is used at the destination.
        FtpInputFixture.copyResource(localFtpDirectory(context),
                context.getEnvironment().getRequiredProperty("${ftpInputPatternKey?j_string}"),
                batch == 1 ? FIRST_BATCH_INPUT_FILENAME : SECOND_BATCH_INPUT_FILENAME);
<#else>
        // Use batch == 1 ? FIRST_BATCH_INPUT : SECOND_BATCH_INPUT as the file/provider content.
        // Replace this guard with real input preparation for the scheduled consumer.
        throw new UnsupportedOperationException("Prepare input batch " + batch + " before scanning");
</#if>
    }
</#if>

    // TODO 2: Confirm this is the producer to observe, then set the expected payloads below.
<#if producers?size != 1>
    // Choose one of these producer names; cover other router outputs in the path and delivery assertions:
<#list producers as producer>
    // "${producer?j_string}"
</#list>
</#if>
    private static final String PRODUCER_NAME = <#if producers?size == 1>"${producers[0]?j_string}"<#else>"REPLACE: producer name"</#if>;

    // Decode actual Payload/byte[]/File/Path/file-list/JMS TextMessage content for comparisons.
    // true decodes supported content as text; false uses toString(). Both modes compare text.
    private static final boolean DECODE_OUTPUT_CONTENT_AS_TEXT = true;

<#if fileDelivery || smtpDelivery>
    // Review the sample expected UTF-8 output files under src/test/resources/<flow>/<producer>/.
    // Replace spaces in the directory names with underscores, as for input resources.
    // These resource names do not dictate the producer's delivered filenames.
    private static final String FIRST_EXPECTED_OUTPUT_RESOURCE = "/" + noSpaces(FLOW_NAME)
            + "/" + noSpaces(PRODUCER_NAME) + "/first.txt";
    private static final String SECOND_EXPECTED_OUTPUT_RESOURCE = "/" + noSpaces(FLOW_NAME)
            + "/" + noSpaces(PRODUCER_NAME) + "/second.txt";
<#else>
    // Set the expected text outputs for the first and second test. The test compares these values with formatOutputText(actualPayload).
    // Override formatOutputText(Object payload) to decode the actual payload, for example UTF-8 file content.
    private static final String FIRST_EXPECTED_OUTPUT = "REPLACE: first expected payload";
    private static final String SECOND_EXPECTED_OUTPUT = "REPLACE: second expected payload";

</#if>

    // To check business-object fields instead of text, override assertOutput(actualAfterProducer, batch) and
    // remove the formatOutputText method below.
    @Override protected String formatOutputText(Object payload) {
<#if scheduledContext>
        // The default provider forwards the timer context, including our test fixture text.
        if (DECODE_OUTPUT_CONTENT_AS_TEXT && payload instanceof JobExecutionContext) {
            return ScheduledEventFixture.text((JobExecutionContext) payload);
        }
</#if>
        return formatOutputText(payload, DECODE_OUTPUT_CONTENT_AS_TEXT);
    }


    @Override
    protected void defineExpectedPath(IkasanFlowTestRule harness) {
        // TODO 3: Review the expected journey through the flow. This is separate from payload checks.
<#if automaticPath>
        // Define the flow per batch below. This will be executed per batch supplied.
        harness.blockStart()
                .<#if scheduled>scheduledConsumer<#else>consumer</#if>("${consumerName?j_string}")
<#list expectedPath as step>
                .${step.method}("${step.name?j_string}")
</#list>
                .repeat(2);
<#else>
        // This flow needs scenario-specific expectations (routing, splitting, filtering or exclusions).
        // List the actual component invocations for BOTH inputs in execution order using:
        // harness.consumer/scheduledConsumer, converter, translator, broker, filter, splitter,
        // router, multiRecipientRouter, sequencer and producer (each takes the component name).
        // Register scheduledConsumer("${consumerName?j_string}") for a scheduled input.
        // Available component names:
<#list componentNames as name>
        // "${name?j_string}"
</#list>
        // Add assertions in the test for every intended branch and stored exclusion; then remove this guard.
        throw new UnsupportedOperationException("Define the expected component path for both input batches");
</#if>
    }

<#if jmsConsumer && jmsOutputKey?has_content>

    @Override
    protected void verifyReceivedOutput(ConfigurableApplicationContext context, int batch, String expected) throws Exception {
        // Read the actual isolated output queue; keep input and output queues distinct.
        try (JmsFlowTestSupport jms = JmsFlowTestSupport.from(context)) {
            jms.assertText(context.getEnvironment().getRequiredProperty("${jmsOutputKey?j_string}"), expected);
        }
    }
</#if>
<#if smtpDelivery>
    @Override
    protected void verifyReceivedOutput(ConfigurableApplicationContext context, int batch, String expected) throws Exception {
        // Verify actual SMTP delivery, not just the email producer invocation.
        // The default expects one mailbox delivery per batch; adapt for multiple recipients or attachments.
        localSmtpServer(context).assertBody(batch, expected, deliveryTimeout(context));
    }
</#if>
<#if fileDelivery && !(jmsConsumer && jmsOutputKey?has_content)>

    @Override
    protected void verifyReceivedOutput(ConfigurableApplicationContext context, int batch, String expected) throws Exception {
        // Check the output received from the producer.
<#if sftpFileDelivery>
        // The local test SFTP server owns a separate directory for this producer.
        assertDeliveredFileResources(localSftpDirectory(context, FLOW_NAME, PRODUCER_NAME), "*", batch,
                batch == 1 ? FIRST_EXPECTED_OUTPUT_RESOURCE : SECOND_EXPECTED_OUTPUT_RESOURCE);
<#elseif ftpFileDelivery>
        // The FTP unit test server is enabled by test.ftp.enabled in test properties. The property
        // test.delivery.timeout-seconds allows timeout configuration.
        // Refine the "*" glob to resemble the expected filename(s)
        // Supply only this batch's expected resources; earlier deliveries are tracked internally.
        assertDeliveredFileResources(localFtpDirectory(context), "*", batch,
                batch == 1 ? FIRST_EXPECTED_OUTPUT_RESOURCE : SECOND_EXPECTED_OUTPUT_RESOURCE);
        // For a known filename/overwrite: assertFileMatchesResource(localFtpDirectory(context).resolve("result.txt"),
        //         batch == 1 ? FIRST_EXPECTED_OUTPUT_RESOURCE : SECOND_EXPECTED_OUTPUT_RESOURCE);
<#else>
        // Use the test server's LOCAL output directory, or download remote files to a temporary directory.
        // A remote SFTP path is not a local filesystem path. These helpers do not connect to SFTP.
        // var directory = Path.of("REPLACE: local test output directory");
        // assertDeliveredFileResources(directory, "*", batch,
        //         batch == 1 ? FIRST_EXPECTED_OUTPUT_RESOURCE : SECOND_EXPECTED_OUTPUT_RESOURCE);
        throw new UnsupportedOperationException("Implement verifyReceivedOutput to check physical file delivery");
</#if>
    }
</#if>

    // TODO 4: Review shared connections in src/test/resources/module-test.properties.
    // Shared setup supplies a fresh context and isolated H2; all flows initially start MANUAL.
    @Test
    public void testFirstAndLaterDeliveryWithoutRestart() throws Exception {
        // If you override assertOutput(actualAfterProducer, batch), replace the call below with:
        // runTest(TEST_REVIEWED, PRODUCER_NAME);
<#if fileDelivery || smtpDelivery>
        assertTrue("Complete TODOs 1–4, then set TEST_REVIEWED=true", TEST_REVIEWED);
        runTest(TEST_REVIEWED, PRODUCER_NAME, readTestResource(FIRST_EXPECTED_OUTPUT_RESOURCE),
                readTestResource(SECOND_EXPECTED_OUTPUT_RESOURCE));
<#else>
        runTest(TEST_REVIEWED, PRODUCER_NAME, FIRST_EXPECTED_OUTPUT, SECOND_EXPECTED_OUTPUT);
</#if>
    }

}
