package org.ikasan.studio.flowtests;

import org.ikasan.testharness.flow.rule.IkasanFlowTestRule;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
<#if localFile>
import java.util.Map;
</#if>
<#if isolatedFiles>
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
    // TODO 5: After completing tasks 1–4, set CONFIGURED=true and run from the project root:
    // mvn -pl user-flow-tests -am -Dtest=${className} -Dsurefire.failIfNoSpecifiedTests=false test
    // Success requires first delivery, idle readiness and later delivery without restarting.
    private static final boolean CONFIGURED = false;
    @Override protected String getFlowName() { return "${flowName?j_string}"; }

    // TODO 2: Set the data supplied for each batch; expected outputs below are independent assertions.
    private static final String FIRST_BATCH_INPUT = "REPLACE: first input";
    private static final String SECOND_BATCH_INPUT = "REPLACE: second input";

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

    // Set the expected text outputs for the first and second test. The test compares these values with outputText(actualPayload).
    // Override outputText(Object payload) to decode the actual payload, for example UTF-8 file content.
    private static final String FIRST_EXPECTED_OUTPUT = "REPLACE: first expected payload";
    private static final String SECOND_EXPECTED_OUTPUT = "REPLACE: second expected payload";

<#if isolatedFiles>
    @Rule
    public TemporaryFolder inputDirectory = new TemporaryFolder();
</#if>

    // TODO 1: Review shared connections in src/test/resources/module-test.properties.
    // Shared setup supplies a fresh context and isolated H2; all flows initially start MANUAL.
    @Test(timeout = 60000)
    public void testFirstAndLaterDeliveryWithoutRestart() throws Exception {
        runTest(CONFIGURED, PRODUCER_NAME, FIRST_EXPECTED_OUTPUT, SECOND_EXPECTED_OUTPUT);
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
        // Generated for ONE event per batch, following the same path twice (two batches).
        // If you change the number of events or introduce rejection/branching, update this sequence.
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
        // For external producers also assert the received file/message. Invocation alone is not delivery.
    }

<#if localFile>
    @Override
    protected Map<String, String> flowTestProperties() {
        Map<String, String> properties = super.flowTestProperties();
<#if isolatedFiles>
        // JUnit creates and cleans this temporary input directory. No changes are needed here.
        properties.put("${filenameKey?j_string}",
                inputDirectory.getRoot().getAbsolutePath().replace('\\', '/') + "/batch-.*[.]txt");
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
        // module-test.properties enables fixture input by default, disabling automatic polling:
        // studio.sample-consumer.${sampleConsumerClass}.fixture-input-enabled=true
        // Remove that setting (or set false) only when testing automatic polling; adapt this input accordingly.
        context.getBean(${sampleConsumerClass}.class)
                .submitNow(batch == 1 ? FIRST_BATCH_INPUT : SECOND_BATCH_INPUT);
<#elseif isolatedFiles>
        // File creation and scanning are supplied for you. Replace these sample contents with your input.
        // Each scan should see only this batch's file; earlier test files are removed before batch 2.
        try (DirectoryStream<Path> previous =
                     Files.newDirectoryStream(inputDirectory.getRoot().toPath(), "batch-*.txt")) {
            for (Path file : previous) Files.delete(file);
        }
        String contents = batch == 1 ? FIRST_BATCH_INPUT : SECOND_BATCH_INPUT;
        Files.writeString(inputDirectory.getRoot().toPath().resolve("batch-" + batch + ".txt"), contents);
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
<#if fileDelivery && !(jmsConsumer && jmsOutputKey?has_content)>

    @Override
    protected void verifyReceivedOutput(ConfigurableApplicationContext context, int batch, String expected) throws Exception {
        // Task 4: Check the physical files, separately from the payload observed at the producer.
<#if ftpFileDelivery>
        // The isolated FTP server exposes the directory it created; enable test.ftp.enabled in test properties.
        // Default: one new file per batch, preserving the first file. Check ALL files, including temporary leftovers.
        // Adapt the glob/expected values for checksums, multiple files or intentional overwrites.
        assertDeliveredFileContents(localFtpDirectory(context), "*", batch == 1
                ? new String[]{FIRST_EXPECTED_OUTPUT}
                : new String[]{FIRST_EXPECTED_OUTPUT, SECOND_EXPECTED_OUTPUT});
        // For a known filename/overwrite instead: assertFileContents(localFtpDirectory(context).resolve("result.txt"), expected);
<#else>
        // Use the test server's LOCAL output directory, or download remote files to a temporary directory.
        // A remote SFTP path is not a local filesystem path. These helpers do not connect to SFTP.
        // var directory = Path.of("REPLACE: local test output directory");
        // assertDeliveredFileContents(directory, "*", batch == 1
        //         ? new String[]{FIRST_EXPECTED_OUTPUT}
        //         : new String[]{FIRST_EXPECTED_OUTPUT, SECOND_EXPECTED_OUTPUT});
        throw new UnsupportedOperationException("Configure physical file delivery assertions in task 4");
</#if>
    }
</#if>
<#if scheduled && !isolatedFiles>

    private void prepareInputBatch(ConfigurableApplicationContext context, int batch) throws Exception {
        // Use batch == 1 ? FIRST_BATCH_INPUT : SECOND_BATCH_INPUT as the file/provider content.
        // Task 2: Replace this guard with real input preparation for the scheduled consumer.
        throw new UnsupportedOperationException("Prepare input batch " + batch + " before scanning");
    }
</#if>
}
