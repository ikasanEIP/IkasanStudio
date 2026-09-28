package org.ikasan.studio.flowtests;

import org.ikasan.studio.flowtests.support.ModuleFlowTestSupport;
<#if ftpInput>
import org.ikasan.studio.flowtests.support.FtpInputFixture;
</#if>
<#if jmsConsumer>
import org.ikasan.studio.flowtests.support.ModuleJmsTestConfig;
import org.ikasan.studio.flowtests.support.JmsFlowTestSupport;
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
<#if isolatedFiles>
import org.ikasan.studio.flowtests.support.FileInputFixture;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

</#if>

/**
 * Developer-owned scenario for ${flowName?j_string}.
 * Complete the numbered tasks below in order (IntelliJ's TODO view can locate them):
 * 1. Review test connections in module-test.properties.
 * 2. Supply first and later input in supplyInput().
 * 3. Choose the output producer and a stable payload representation.
 * 4. Review the expected component path and any branch/delivery assertions.
 * 5. Enable the completed scenario and run it.
 * See user-flow-tests/README.md for details. Generated scaffolds are not completed tests.
 */
public class ${className} extends ModuleFlowTestSupport {
    // TODO 5: After completing tasks 1–4, set TEST_REVIEWED=true and run from the project root:
    // mvn -pl user-flow-tests -am -Dtest=${className} -Dsurefire.failIfNoSpecifiedTests=false test
    // Success requires first delivery, idle readiness and later delivery without restarting.
    private static final boolean TEST_REVIEWED = false;
    private static final String FLOW_NAME = "${flowName?j_string}";
    @Override protected String getFlowName() { return FLOW_NAME; }

<#if ftpInput || isolatedFiles>
    // TODO 2: Review the sample input files under user-flow-tests/src/test/resources (or change the paths).
    // Each resource is copied unchanged to the JUnit test input directory using only its filename.
    // Use distinct filenames; the test directory is configured automatically.
<#if ftpInput>
    // Filenames must match the FTP consumer's filenamePattern.
</#if>
    private static final String CONSUMER_NAME = "${consumerName?j_string}";
    // Resource directory names replace spaces with underscores; runtime names remain unchanged.
    private static final String FIRST_BATCH_INPUT_FILENAME = "/" + noSpaces(FLOW_NAME)
            + "/" + noSpaces(CONSUMER_NAME) + "/first.txt";
    private static final String SECOND_BATCH_INPUT_FILENAME = "/" + noSpaces(FLOW_NAME)
            + "/" + noSpaces(CONSUMER_NAME) + "/second.txt";
    // If needed for an assertion: readTestResource(FIRST_BATCH_INPUT_FILENAME) reads its UTF-8 text.
<#else>
    // TODO 2: Set the data supplied for each batch; expected outputs below are independent assertions.
    private static final String FIRST_BATCH_INPUT = "REPLACE: first input";
    private static final String SECOND_BATCH_INPUT = "REPLACE: second input";

    // Alternative: put UTF-8 fixture files in user-flow-tests/src/test/resources/input/.
    // Replace the two input constants above with these declarations (do not keep both):
    // private static final String FIRST_BATCH_INPUT = readTestResource("/input/first.txt");
    // private static final String SECOND_BATCH_INPUT = readTestResource("/input/second.txt");
    // Paths are relative to src/test/resources, not your working directory. Whitespace is preserved.
</#if>

    // TODO 3: Confirm this is the producer to observe, then set the expected payloads below.
<#if producers?size != 1>
    // Choose one of these producer names; cover other router outputs in task 4:
<#list producers as producer>
    // "${producer?j_string}"
</#list>
</#if>
    private static final String PRODUCER_NAME = <#if producers?size == 1>"${producers[0]?j_string}"<#else>"REPLACE: producer name"</#if>;

    // Decode actual Payload/byte[]/File/Path/file-list/JMS TextMessage content for comparisons.
    // UTF-8 is used for bytes/files; false retains String.valueOf. Override outputText for other formats.
    private static final boolean STRINGIFY_ACTUAL_OUTPUT = true;

<#if fileDelivery || smtpDelivery>
    // Review the sample expected UTF-8 output files under src/test/resources/<flow>/<producer>/.
    // Replace spaces in the directory names with underscores, as for input resources.
    // These resource names do not dictate the producer's delivered filenames.
    private static final String FIRST_EXPECTED_OUTPUT_RESOURCE = "/" + noSpaces(FLOW_NAME)
            + "/" + noSpaces(PRODUCER_NAME) + "/first.txt";
    private static final String SECOND_EXPECTED_OUTPUT_RESOURCE = "/" + noSpaces(FLOW_NAME)
            + "/" + noSpaces(PRODUCER_NAME) + "/second.txt";
<#else>
    // Set the expected text outputs for the first and second test. The test compares these values with outputText(actualPayload).
    // Override outputText(Object payload) to decode the actual payload, for example UTF-8 file content.
    private static final String FIRST_EXPECTED_OUTPUT = "REPLACE: first expected payload";
    private static final String SECOND_EXPECTED_OUTPUT = "REPLACE: second expected payload";

</#if>

<#if isolatedFiles>
    @Rule
    public TemporaryFolder inputDirectory = TemporaryFolder.builder().assureDeletion().build();
</#if>

    // TODO 1: Review shared connections in src/test/resources/module-test.properties.
    // Shared setup supplies a fresh context and isolated H2; all flows initially start MANUAL.
    @Test
    public void testFirstAndLaterDeliveryWithoutRestart() throws Exception {
<#if fileDelivery || smtpDelivery>
        assertTrue("Complete tasks 1–4, then set TEST_REVIEWED=true", TEST_REVIEWED);
        runTest(TEST_REVIEWED, PRODUCER_NAME, readTestResource(FIRST_EXPECTED_OUTPUT_RESOURCE),
                readTestResource(SECOND_EXPECTED_OUTPUT_RESOURCE));
<#else>
        runTest(TEST_REVIEWED, PRODUCER_NAME, FIRST_EXPECTED_OUTPUT, SECOND_EXPECTED_OUTPUT);
</#if>
    }

<#if jmsConsumer>
    @Override protected Class<?>[] testConfigurationClasses() {
        return new Class<?>[]{ModuleJmsTestConfig.class};
    }
</#if>

    @Override protected String outputText(Object payload) {
        return outputText(payload, STRINGIFY_ACTUAL_OUTPUT);
    }


    @Override
    protected void defineExpectedPath(IkasanFlowTestRule harness) {
        // TODO 4: Review the expected journey through the flow. This is separate from payload checks.
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
        // TODO 2: Supply the data for batch 1 and batch 2.
<#if jmsConsumer>
        // ModuleJmsTestConfig supplies the test connection. Configure test.jms.broker-url
        // and matching isolated flow broker/destinations in module-test.properties.
        try (JmsFlowTestSupport jms = JmsFlowTestSupport.from(context)) {
            jms.sendText(context.getEnvironment().getRequiredProperty("${jmsInputKey?j_string}"),
                    batch == 1 ? FIRST_BATCH_INPUT : SECOND_BATCH_INPUT);
        }
<#elseif sampleSubmission>
        // The following property has been set in module-test.properties so that the ${sampleConsumerClass} does not
        // Automatically pole on its configured time based schedule, but instead is triggered by the test
        // Remove the property or set it to false if this is now required.
        // studio.sample-consumer.${sampleConsumerClass}.fixture-input-enabled=true
        context.getBean(${sampleConsumerClass}.class)
                .submitNow(batch == 1 ? FIRST_BATCH_INPUT : SECOND_BATCH_INPUT);
<#elseif isolatedFiles>
        // Copy the resource's original bytes using its basename; remove only earlier batch fixtures.
        FileInputFixture.prepareLocalBatch(inputDirectory.getRoot().toPath(), batch,
                FIRST_BATCH_INPUT_FILENAME, SECOND_BATCH_INPUT_FILENAME);
        harness.fireScheduledConsumer();
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
        // Task 4: Check the output of the producer.
<#if ftpFileDelivery>
        // The FTP unit test server is enabled by test.ftp.enabled in test properties. The property
        // test.delivery.timeout-seconds allows timeout configuration.
        // Refine the "*" glob to resemble the expected filename(s)
        // When sending multiple files, append the expected output as a new array element so we can check both files
        assertDeliveredFileResources(localFtpDirectory(context), "*", batch == 1
                ? new String[]{FIRST_EXPECTED_OUTPUT_RESOURCE}
                : new String[]{FIRST_EXPECTED_OUTPUT_RESOURCE, SECOND_EXPECTED_OUTPUT_RESOURCE});
        // For a known filename/overwrite: assertFileMatchesResource(localFtpDirectory(context).resolve("result.txt"),
        //         batch == 1 ? FIRST_EXPECTED_OUTPUT_RESOURCE : SECOND_EXPECTED_OUTPUT_RESOURCE);
<#else>
        // Use the test server's LOCAL output directory, or download remote files to a temporary directory.
        // A remote SFTP path is not a local filesystem path. These helpers do not connect to SFTP.
        // var directory = Path.of("REPLACE: local test output directory");
        // assertDeliveredFileResources(directory, "*", batch == 1
        //         ? new String[]{FIRST_EXPECTED_OUTPUT_RESOURCE}
        //         : new String[]{FIRST_EXPECTED_OUTPUT_RESOURCE, SECOND_EXPECTED_OUTPUT_RESOURCE});
        throw new UnsupportedOperationException("Configure physical file delivery assertions in task 4");
</#if>
    }
</#if>
<#if scheduled && !isolatedFiles>

    private void prepareInputBatch(ConfigurableApplicationContext context, int batch) throws Exception {
<#if ftpInput>
        // Enable test.ftp.enabled in module-test.properties.
        // Files are created in the test FTP server's JUnit temporary directory.
        // The classpath resource is streamed unchanged; only its basename is used at the destination.
        FtpInputFixture.copyResource(localFtpDirectory(context),
                context.getEnvironment().getRequiredProperty("${ftpInputPatternKey?j_string}"),
                batch == 1 ? FIRST_BATCH_INPUT_FILENAME : SECOND_BATCH_INPUT_FILENAME);
<#else>
        // Use batch == 1 ? FIRST_BATCH_INPUT : SECOND_BATCH_INPUT as the file/provider content.
        // Task 2: Replace this guard with real input preparation for the scheduled consumer.
        throw new UnsupportedOperationException("Prepare input batch " + batch + " before scanning");
</#if>
    }
</#if>
}
