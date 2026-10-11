<#macro expectedRoute route indent>
<#list route.steps as step>
${indent}harness.${step.method}("${step.name?j_string}");
</#list>
<#if route.children?has_content>
${indent}// TODO 2: Every route is listed once as a starting example; adjust for this inputIndex.
<#if !route.multi>
${indent}// This is a single-recipient router: keep only the branch expected for this input.
</#if>
<#list route.children as child>
${indent}// Begin route "${child.name?j_string}"
<@expectedRoute route=child indent=indent />
${indent}// End route "${child.name?j_string}"
</#list>
</#if>
</#macro>
<#assign importInputType = objectMessageInput && objectInputType != "java.io.Serializable" && objectInputType?contains(".") && !["ModuleFlowTestSupport", "FlowTestBatch", "List", "Map", "ScheduledEventFixture", "JobExecutionContext", "FtpInputFixture", "ModuleJmsTestConfig", "JmsFlowTestSupport", "IkasanFlowTestRule", "Test", "ConfigurableApplicationContext", "FileInputFixture", "Rule", "TemporaryFolder", "String", "Object", "Class", "Exception", "UnsupportedOperationException", className]?seq_contains(objectInputType?keep_after_last("."))>
<#assign inputTypeName = importInputType?then(objectInputType?keep_after_last("."), objectInputType)>
<#assign objectOutput = objectOutputType?has_content && !fileDelivery && !smtpDelivery && producers?size == 1>
<#assign importOutputType = objectOutput && !["ModuleFlowTestSupport", "FlowTestBatch", "List", "Map", "ScheduledEventFixture", "JobExecutionContext", "FtpInputFixture", "ModuleJmsTestConfig", "JmsFlowTestSupport", "IkasanFlowTestRule", "Test", "ConfigurableApplicationContext", "FileInputFixture", "Rule", "TemporaryFolder", "String", "Object", "Class", "Exception", "UnsupportedOperationException", className]?seq_contains(objectOutputType?keep_after_last(".")) && !(importInputType && objectInputType != objectOutputType && inputTypeName == objectOutputType?keep_after_last("."))>
<#assign outputTypeName = importOutputType?then(objectOutputType?keep_after_last("."), objectOutputType)>
package org.ikasan.studio.flowtests;

import org.ikasan.studio.flowtests.support.ModuleFlowTestSupport;
import org.ikasan.studio.flowtests.support.utils.FlowTestBatch;
import java.util.List;
import java.util.Map;
<#if importOutputType && !(importInputType && objectInputType == objectOutputType)>
import ${objectOutputType};
</#if>
<#if importInputType>
import ${objectInputType};
</#if>
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
import org.springframework.context.ConfigurableApplicationContext;
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
 * 1. Choose the output producer and review the expected payloads below.
 * 2. Review the expected component path and any branch/delivery assertions.
 * 3. Define input payloads or fixtures for the batches in createInputOutputBatches().
 * 4. Review test connections in module-test.properties.
 * Run immediately to see what works; use the TODOs to refine inputs and assertions.
 * See user-flow-tests/README.md for details. Generated scaffolds are not completed tests.
 */
public class ${className} extends ModuleFlowTestSupport {
    // Run from IntelliJ or from the project root:
    // mvn -pl user-flow-tests -am -Dtest=${className} -Dsurefire.failIfNoSpecifiedTests=false test
    private static final String FLOW_NAME = "${flowName?j_string}";
    // TODO 1: Confirm this is the producer to observe, then set the expected payloads below.
<#if producers?size != 1>
    // Review expected outputs for each producer in createInputOutputBatches; omit only deliberately unchecked producers:
<#list producers as producer>
    // "${producer?j_string}"
</#list>
</#if>
    private static final String PRODUCER_NAME = <#if producers?size gt 0>"${producers[0]?j_string}"<#else>"REPLACE: producer name"</#if>;

    @Override protected String getFlowName() { return FLOW_NAME; }

    @Override
    protected void defineExpectedPath(IkasanFlowTestRule harness) {
        // TODO 2: Review the expected journey through the flow. This is separate from payload checks.
<#if automaticPath>
        // Define the flow per batch below. This will be executed per batch supplied.
        harness.blockStart()
                .<#if scheduled>scheduledConsumer<#else>consumer</#if>("${consumerName?j_string}")
<#list expectedPath as step>
                .${step.method}("${step.name?j_string}")
</#list>
                .repeat(expectedInputCount());
<#elseif routerPath?has_content>
        // Every route below is listed once per input as an editable example, not an inferred business decision.
        // inputIndex is zero-based across all batches. Multi-recipient lists must match actual invocation order.
        // Remove or condition branches that are not visited; match producer outputs to the remaining paths.
        for (int inputIndex = 0; inputIndex < expectedInputCount(); inputIndex++) {
            harness.<#if scheduled>scheduledConsumer<#else>consumer</#if>("${consumerName?j_string}");
<@expectedRoute route=routerPath indent="            " />
        }
<#elseif splitterPath || filterPath>
<#if splitterPath>
        // The component sequence is known; split counts depend on your fixtures and business logic.
        // Each count below is an example of ONE output item, not an inferred expectation.
        // Adjust inside the loops if counts differ by input or by an earlier splitter's item.
        // Final expected outputs alone cannot tell us the intermediate split counts.
</#if>
<#if filterPath>
        // The component sequence is known; filter decisions depend on your inputs and business logic.
        // The true values below are editable examples, not inferred filter outcomes.
        // Set each decision for inputIndex (zero-based across all batches).
        // A rejected event visits its filter, but none of the components inside the following if block.
        // After a splitter, decisions can also depend on the enclosing splitItem index.
        // Match producer expectations to accepted events; an empty producer list asserts no output.
</#if>
        for (int inputIndex = 0; inputIndex < expectedInputCount(); inputIndex++) {
            harness.<#if scheduled>scheduledConsumer<#else>consumer</#if>("${consumerName?j_string}");
<#list splitSteps as step>
${step.indent}harness.${step.method}("${step.name?j_string}");
<#if step.splitter>
${step.indent}// TODO 2: Set the expected number of items from this invocation of "${step.name?j_string}".
${step.indent}int expectedSplitItems${step.index} = 1;
${step.indent}for (int splitItem${step.index} = 0; splitItem${step.index} < expectedSplitItems${step.index}; splitItem${step.index}++) {
<#elseif step.filterDecision>
${step.indent}// TODO 2: Set whether this invocation of "${step.name?j_string}" passes the event.
${step.indent}boolean expectedFilterPasses${step.index} = true;
${step.indent}if (expectedFilterPasses${step.index}) {
</#if>
</#list>
<#list splitClosings as indent>
${indent}}
</#list>
        }
<#else>
        // This flow needs scenario-specific expectations (routing, splitting, filtering or exclusions).
        // List the actual component invocations for all inputs in execution order using:
        // harness.consumer/scheduledConsumer, converter, translator, broker, filter, splitter,
        // router, multiRecipientRouter, sequencer and producer (each takes the component name).
        // Register scheduledConsumer("${consumerName?j_string}") for a scheduled input.
        // Available component names:
<#list componentNames as name>
        // "${name?j_string}"
</#list>
        // Add assertions in the test for every intended branch and stored exclusion; then remove this guard.
        throw new UnsupportedOperationException("Define the expected component path for all input batches");
</#if>
    }

    // Override prepareFixtures(context) for instance fixtures after Spring starts, before the flow starts.
    // Override cleanupFixtures(context) for custom cleanup, including when fixture preparation fails.

<#if objectMessageInput>
    // Construct a serializable business object for each batch (expected type: ${objectInputType?j_string}).
<#elseif triggerOnly>
    // TODO 3: Prepare the broker data in prepareFixtures if needed; each triggeredBatch fires once.
<#elseif ftpInput || sftpInput || isolatedFiles>
    // TODO 3: Review the sample input files under user-flow-tests/src/test/resources (or change the paths).
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
    // TODO 3: Set the data supplied for each batch; expected outputs below are independent assertions.
    private static final String FIRST_BATCH_INPUT = "first expected payload";
    private static final String SECOND_BATCH_INPUT = "second expected payload";

    // Alternative: put UTF-8 fixture files in user-flow-tests/src/test/resources/input/.
    // Replace the input constants above with these styles of declarations (do not keep both):
    // private static final String FIRST_BATCH_INPUT = readTestResource("/input/first.txt");
    // Paths are relative to src/test/resources, not your working directory. Whitespace is preserved.
</#if>

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
    protected List<FlowTestBatch<<#if objectMessageInput>java.io.Serializable<#else>String</#if>>> createInputOutputBatches(ConfigurableApplicationContext context) throws Exception {
        // Runs after prepareFixtures(context), so inputs may use Spring beans or runtime fixtures.
<#if objectMessageInput>
<#if objectInputType != "java.io.Serializable">
        // TODO 3: Populate each ${objectInputType?j_string} below with values for its expected output.
        // Adjust the constructor calls if this type requires arguments or a factory method.
<#else>
        // TODO 3: Replace null with populated Serializable business objects for each expected output.
        java.io.Serializable firstInputPayload = null;
        java.io.Serializable secondInputPayload = null;
</#if>
</#if>
        return List.of(
                <#if triggerOnly>triggeredBatch(<#else>batch(
                        List.of(<#if objectMessageInput><#if objectInputType != "java.io.Serializable">new ${inputTypeName}()<#else>java.util.Objects.requireNonNull(firstInputPayload, "Update createInputOutputBatches: populate firstInputPayload with a ${objectInputType?j_string} fixture")</#if><#elseif ftpInput || sftpInput || isolatedFiles>FIRST_BATCH_INPUT_FILENAME<#else>FIRST_BATCH_INPUT</#if>),</#if>
<#if producers?size gt 1>
                        Map.ofEntries(
<#list producers as producer>
                                Map.entry("${producer?j_string}", List.of(FIRST_EXPECTED_OUTPUT))<#sep>,</#sep>
</#list>
                        )
<#else>
                        Map.of(
                                PRODUCER_NAME, List.of(<#if fileDelivery || smtpDelivery>readTestResource(FIRST_EXPECTED_OUTPUT_RESOURCE)<#else>FIRST_EXPECTED_OUTPUT</#if>)
                        )
</#if>
                ),
                <#if triggerOnly>triggeredBatch(<#else>batch(
                        List.of(<#if objectMessageInput><#if objectInputType != "java.io.Serializable">new ${inputTypeName}()<#else>java.util.Objects.requireNonNull(secondInputPayload, "Update createInputOutputBatches: populate secondInputPayload with a ${objectInputType?j_string} fixture")</#if><#elseif ftpInput || sftpInput || isolatedFiles>SECOND_BATCH_INPUT_FILENAME<#else>SECOND_BATCH_INPUT</#if>),</#if>
<#if producers?size gt 1>
                        Map.ofEntries(
<#list producers as producer>
                                Map.entry("${producer?j_string}", List.of(SECOND_EXPECTED_OUTPUT))<#sep>,</#sep>
</#list>
                        )
<#else>
                        Map.of(
                                PRODUCER_NAME, List.of(<#if fileDelivery || smtpDelivery>readTestResource(SECOND_EXPECTED_OUTPUT_RESOURCE)<#else>SECOND_EXPECTED_OUTPUT</#if>)
                        )
</#if>
                )
        );
    }

<#if isolatedFiles>
    @Rule
    public TemporaryFolder inputDirectory = TemporaryFolder.builder().assureDeletion().build();
    private java.nio.file.Path previousInputFile;
</#if>

<#if localFile>
    @Override
    protected Map<String, String> flowTestProperties() {
        Map<String, String> properties = super.flowTestProperties();
<#if isolatedFiles>
        // JUnit creates and cleans this temporary input directory. No changes are needed here.
        properties.put("${filenameKey?j_string}",
                inputDirectory.getRoot().toPath().toAbsolutePath().toString().replace('\\', '/') + "/.*");
<#elseif localFile>
        // Set this consumer's filenames in Studio and regenerate so a temporary directory can be supplied.
        throw new UnsupportedOperationException("Configure isolated filenames before running this test");
</#if>
<#if !localFile || isolatedFiles>
        return properties;
</#if>
    }
</#if>
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
<#if objectOutput>
    // Populate each expected business object with the values the producer should observe.
    // Business objects must implement equals (and matching hashCode).
    // Adjust constructor calls if the class requires arguments or a factory method.
    private static final ${outputTypeName} FIRST_EXPECTED_OUTPUT = new ${outputTypeName}();
    private static final ${outputTypeName} SECOND_EXPECTED_OUTPUT = new ${outputTypeName}();
<#else>
    // Set the expected text outputs for the first and second test. The test compares these values with formatOutputText(actualPayload).
    // Override formatOutputText(Object payload) to decode the actual payload, for example UTF-8 file content.
    private static final String FIRST_EXPECTED_OUTPUT = "first expected payload";
    private static final String SECOND_EXPECTED_OUTPUT = "second expected payload";

</#if>
</#if>

    // Expected business objects must implement equals (and matching hashCode).
    // Only String expectations use formatOutputText; remove this override if no text checks remain.
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
    protected void assertExpectedOutput(Object expected, Object actualAfterProducer) {
        Object actual = expected instanceof String
                ? formatOutputText(actualAfterProducer)
                : actualAfterProducer;

        // Set a breakpoint here to inspect expected, actual and actualAfterProducer.
        org.junit.Assert.assertEquals(expected, actual);
    }

    // TODO 4: Review shared connections in src/test/resources/module-test.properties.
    // Starts once, supplies every batch, checks outputs and idle readiness, then stops during teardown.
    // A single batch cannot demonstrate later delivery; use at least two for that check.
    @Test
    public void testFirstAndLaterDeliveryWithoutRestart() throws Exception {
<#if fileDelivery || smtpDelivery || (jmsConsumer && jmsOutputKey?has_content && !objectOutput)>
        runBatches(this::createInputOutputBatches, this::supplyInput,
                (context, batchNumber, definition) -> {
                    if (definition.producers().contains(PRODUCER_NAME))
                        verifyReceivedOutput(context, batchNumber, definition.expectedTexts(PRODUCER_NAME));
                });
<#else>
        runBatches(this::createInputOutputBatches, this::supplyInput);
</#if>
    }

    // Receiver checks receive all expected outputs; an empty list checks for no delivery.
<#if jmsConsumer && jmsOutputKey?has_content && !objectOutput>

    @Override
    protected void verifyReceivedOutput(ConfigurableApplicationContext context, int batch, List<String> expected) throws Exception {
        // Read the actual isolated output queue; keep input and output queues distinct.
        try (JmsFlowTestSupport jms = JmsFlowTestSupport.from(context)) {
            jms.assertTexts(context.getEnvironment().getRequiredProperty("${jmsOutputKey?j_string}"), expected);
        }
    }
</#if>
<#if smtpDelivery>
    @Override
    protected void verifyReceivedOutput(ConfigurableApplicationContext context, int batch, List<String> expected) throws Exception {
        // Verify actual SMTP delivery, not just the email producer invocation.
        // Set a breakpoint on the assertion below; inspect localSmtpServer(context).receivedMessages()
        // for the actual recipients, subjects and bodies.
<#if smtpRecipientCount gt 0>
        // Expect one mailbox copy per configured recipient. Review if recipient settings change.
        localSmtpServer(context).assertBatchBodies(
                expected.stream().flatMap(body -> java.util.Collections.nCopies(${smtpRecipientCount}, body).stream())
                        .collect(java.util.stream.Collectors.toList()), deliveryTimeout(context));
<#else>
        // Review the recipient settings: the mailbox count could not be inferred safely.
        // Supply one expected body per mailbox copy; repeat each body for multiple recipients.
        localSmtpServer(context).assertBatchBodies(expected, deliveryTimeout(context));
</#if>
    }
</#if>
<#if fileDelivery && !(jmsConsumer && jmsOutputKey?has_content && !objectOutput)>

    @Override
    protected void verifyReceivedOutput(ConfigurableApplicationContext context, int batch, List<String> expected) throws Exception {
        // Check the output artifacts created by the producer.
<#if sftpFileDelivery>
        // The local test SFTP server owns a separate directory for this producer.
        assertDeliveredBatchContents(localSftpDirectory(context, FLOW_NAME, PRODUCER_NAME), "*", batch,
                expected.toArray(new String[0]));
<#elseif ftpFileDelivery>
        // The FTP unit test server is enabled by test.ftp.enabled in test properties. The property
        // test.delivery.timeout-seconds allows timeout configuration.
        // Refine the "*" glob to resemble the expected filename(s)
        assertDeliveredBatchContents(localFtpDirectory(context), "*", batch,
                expected.toArray(new String[0]));
        // For a known filename/overwrite: assertFileContents(localFtpDirectory(context).resolve("result.txt"),
        //         expected);
<#else>
        // Use the test server's LOCAL output directory, or download remote files to a temporary directory.
        // A remote SFTP path is not a local filesystem path. These helpers do not connect to SFTP.
        // var directory = Path.of("REPLACE: local test output directory");
        // assertDeliveredBatchContents(directory, "*", batch,
        //         expected);
        throw new UnsupportedOperationException("Implement verifyReceivedOutput to check physical file delivery");
</#if>
    }
</#if>

    // ------ Standard setup and input delivery: normally no changes needed.
<#if !jmsConsumer && !sampleSubmission && !isolatedFiles && !scheduledContext && !ftpInput && !sftpInput>
    // This consumer needs custom input preparation; complete the guard below before running.
</#if>
<#if jmsConsumer>
    @Override protected Class<?>[] testConfigurationClasses() {
        return new Class<?>[]{ModuleJmsTestConfig.class};
    }
</#if>

    protected void supplyInput(ConfigurableApplicationContext context, IkasanFlowTestRule harness, <#if objectMessageInput>java.io.Serializable<#else>String</#if> input) throws Exception {
        // Supply the configured input for this batch.
<#if jmsConsumer>
        // ModuleJmsTestConfig supplies the test connection. Configure test.jms.broker-url
        // and matching isolated flow broker/destinations in module-test.properties.
        try (JmsFlowTestSupport jms = JmsFlowTestSupport.from(context)) {
<#if objectMessageInput>
            jms.sendObject(context.getEnvironment().getRequiredProperty("${jmsInputKey?j_string}"), input);
<#else>
            // Text is a starting fixture; review the first processing component's required JMS message type.
            jms.sendText(context.getEnvironment().getRequiredProperty("${jmsInputKey?j_string}"),
                    input);
</#if>
        }
<#elseif sampleSubmission>
        // The following property has been set in module-test.properties so that the ${sampleConsumerClass} does not
        // automatically pole on its configured time based schedule, but instead is triggered by the test
        // studio.sample-consumer.${sampleConsumerClass}.fixture-input-enabled=true
        context.getBean(${sampleConsumerClass}.class)
                .submitNow(input);
<#elseif isolatedFiles>
        // Copy the resource's original bytes using its basename; remove only earlier batch fixtures.
        if (previousInputFile != null) java.nio.file.Files.deleteIfExists(previousInputFile);
        previousInputFile = FileInputFixture.copyResource(inputDirectory.getRoot().toPath(), ".*", input);
        harness.fireScheduledConsumer();
<#elseif scheduledContext>
        // Fire the real scheduled consumer now; no waiting for its cron expression.
        // This creates a Quartz timer event carrying fixture text, not a String business payload.
        // If the broker supplies its own data, prepare its source and use the trigger-only overload:
        // ScheduledEventFixture.fire(harness, "${consumerName?j_string}");
        // Use triggeredBatch(outputs) for trigger-only events; this input method is then not called.
        ScheduledEventFixture.fire(harness, "${consumerName?j_string}",
                input);
<#elseif scheduled>
        // Create the files/provider data here BEFORE firing.
        // Account for filename filters, minimum file age and duplicate detection when applicable.
        prepareInputBatch(context, input);
        harness.fireScheduledConsumer();
<#else>
        // Use input as the batch content.
        // Send through the real consumer API (for example JMS), or supply a controllable test provider.
        // Self-generating sources must supply deterministic batches without busy loops or restarting.
        throw new UnsupportedOperationException("Supply input batch " + input + " for ${consumerName?j_string}");
</#if>
    }
<#if scheduled && !isolatedFiles && !scheduledContext>

    private void prepareInputBatch(ConfigurableApplicationContext context, String input) throws Exception {
<#if sftpInput>
        // Copy the fixture into this consumer's directory on the local test SFTP server.
        // Enable test.sftp.enabled in module-test.properties; JUnit owns directory cleanup.
        FileInputFixture.copyResource(localSftpDirectory(context, FLOW_NAME, CONSUMER_NAME),
                context.getEnvironment().getRequiredProperty("${sftpInputPatternKey?j_string}"),
                input);
<#elseif ftpInput>
        // Enable test.ftp.enabled in module-test.properties.
        // Files are created in the test FTP server's JUnit temporary directory.
        // The classpath resource is streamed unchanged; only its basename is used at the destination.
        FtpInputFixture.copyResource(localFtpDirectory(context),
                context.getEnvironment().getRequiredProperty("${ftpInputPatternKey?j_string}"),
                input);
<#else>
        // Use input as the file/provider content.
        // Replace this guard with real input preparation for the scheduled consumer.
        throw new UnsupportedOperationException("Prepare input batch " + input + " before scanning");
</#if>
    }
</#if>

}
