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
    // TODO 3: Review OUTPUT and set the two expected output values below.
    // A single producer is selected for you; otherwise choose one of the names listed here.
    // Compare meaningful data (for example file contents or order IDs), not object identity strings.
    // Available producer names (cover additional router outputs in task 4):
<#list producers as producer>
    // "${producer?j_string}"
</#list>
    private static final String OUTPUT = <#if producers?size == 1>"${producers[0]?j_string}"<#else>"REPLACE: producer name"</#if>;

    // These expected values must match outputText() for the two inputs in task 2.
    // The shared default uses String.valueOf(payload); override for custom objects or byte arrays.
    private static final String FIRST_EXPECTED = "REPLACE: first expected payload";
    private static final String SECOND_EXPECTED = "REPLACE: second expected payload";

<#if isolatedFiles>
    @Rule
    public TemporaryFolder inputDirectory = new TemporaryFolder();
</#if>

    // TODO 1: Review shared connections in src/test/resources/module-test.properties.
    // Shared setup supplies a fresh context and isolated H2; all flows initially start MANUAL.
    @Test(timeout = 60000)
    public void testFirstAndLaterDeliveryWithoutRestart() throws Exception {
        runTest(CONFIGURED, OUTPUT, FIRST_EXPECTED, SECOND_EXPECTED);
    }

<#if jmsConsumer>
    @Override protected Class<?>[] testConfigurationClasses() {
        return new Class<?>[]{ModuleJmsTestConfig.class};
    }
</#if>

<#if localFile>
    @Override protected String outputText(Object payload) {
        // Task 3: For unchanged local-file payloads, compare UTF-8 contents, not temporary paths.
        // Adapt this if your flow converts files to another payload type.
        return describeFileOutput(payload);
    }
</#if>

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
        // TODO 2: Supply the data for batch 1 and batch 2. Keep the same flow running between them.
<#if jmsConsumer>
        // ModuleJmsTestConfig supplies the test connection. Configure test.jms.broker-url
        // and matching isolated flow broker/destinations in module-test.properties.
        try (JmsFlowTestSupport jms = JmsFlowTestSupport.from(context)) {
            jms.sendText(context.getEnvironment().getRequiredProperty("${jmsInputKey?j_string}"),
                    batch == 1 ? "REPLACE: first input" : "REPLACE: second input");
        }
<#elseif isolatedFiles>
        // File creation and scanning are supplied for you. Replace these sample contents with your input.
        // Each scan should see only this batch's file; earlier test files are removed before batch 2.
        try (DirectoryStream<Path> previous =
                     Files.newDirectoryStream(inputDirectory.getRoot().toPath(), "batch-*.txt")) {
            for (Path file : previous) Files.delete(file);
        }
        String contents = batch == 1 ? "REPLACE: first input" : "REPLACE: second input";
        Files.writeString(inputDirectory.getRoot().toPath().resolve("batch-" + batch + ".txt"), contents);
        harness.fireScheduledConsumer();
<#elseif scheduled>
        // Create the files/provider data here BEFORE firing. Scanning does not create input.
        // Account for filename filters, minimum file age and duplicate detection when applicable.
        prepareInputBatch(context, batch);
        harness.fireScheduledConsumer();
<#else>
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
<#if scheduled && !isolatedFiles>

    private void prepareInputBatch(ConfigurableApplicationContext context, int batch) throws Exception {
        // Task 2: Replace this guard with real input preparation for the scheduled consumer.
        throw new UnsupportedOperationException("Prepare input batch " + batch + " before scanning");
    }
</#if>
}
