package org.ikasan.studio.flowtests;

import org.ikasan.spec.flow.Flow;
import org.ikasan.spec.flow.FlowElement;
import org.ikasan.spec.flow.FlowEvent;
import org.ikasan.spec.flow.FlowEventListener;
import org.ikasan.spec.module.Module;
import org.ikasan.testharness.flow.rule.IkasanFlowTestRule;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/**
 * Developer-owned scenario for ${flowName?j_string}.
 * Complete the numbered tasks below in order (IntelliJ's TODO view can locate them):
 * 1. Review test connections in openTestApplication().
 * 2. Supply first and later input in sendInput().
 * 3. Choose the output producer and a stable payload representation.
 * 4. Review the expected component path and any branch/delivery assertions.
 * 5. Enable the completed scenario and run it.
 * See user-flow-tests/README.md for details. Generated scaffolds are not completed tests.
 */
public class ${className} {
    // TODO 5: After completing tasks 1–4, set CONFIGURED=true and run from the project root:
    // mvn -pl user-flow-tests -am -Dtest=${className} -Dsurefire.failIfNoSpecifiedTests=false test
    // Success requires first delivery, idle readiness and later delivery without restarting.
    private static final boolean CONFIGURED = false;
    private static final String FLOW = "${flowName?j_string}";
    // TODO 3: Review OUTPUT, describeOutput(), and set the two expected output values below.
    // A single producer is selected for you; otherwise choose one of the names listed here.
    // Compare meaningful data (for example file contents or order IDs), not object identity strings.
    // Available producer names (cover additional router outputs in task 4):
<#list producers as producer>
    // "${producer?j_string}"
</#list>
    private static final String OUTPUT = <#if producers?size == 1>"${producers[0]?j_string}"<#else>"REPLACE: producer name"</#if>;

    // These expected values must match describeOutput() for the two inputs in task 2.
    private static final String FIRST_EXPECTED = "REPLACE: first expected payload";
    private static final String SECOND_EXPECTED = "REPLACE: second expected payload";

<#if isolatedFiles>
    @org.junit.Rule
    public org.junit.rules.TemporaryFolder inputDirectory = new org.junit.rules.TemporaryFolder();
</#if>

    @Test(timeout = 60000)
    public void deliversFirstAndLaterInputWithoutRestart() throws Exception {
        assertTrue("Complete TODO 1–4, then set CONFIGURED=true in TODO 5. See user-flow-tests/README.md", CONFIGURED);
        try (ConfigurableApplicationContext context = openTestApplication()) {
            Module<Flow> module = context.getBean(Module.class);
            Flow flow = module.getFlow(FLOW);
            assertNotNull("Flow must exist: " + FLOW, flow);
            BlockingQueue<String> outputs = new LinkedBlockingQueue<>();
            FlowEventListener listener = new FlowEventListener() {
                public void beforeFlowElement(String m, String f, FlowElement e, FlowEvent event) { }
                public void afterFlowElement(String m, String f, FlowElement e, FlowEvent event) {
                    if (OUTPUT.equals(e.getComponentName())) outputs.add(describeOutput(event.getPayload()));
                }
            };
            IkasanFlowTestRule harness = new IkasanFlowTestRule().withFlow(flow);
            configureExpectedPath(harness);
            flow.addFlowListener(listener);
            try {
                harness.startFlow();
                sendInput(context, harness, 1);
                assertEquals("First output", FIRST_EXPECTED, outputs.poll(10, TimeUnit.SECONDS));
                assertEquals("Ready after first delivery", Flow.RUNNING, flow.getState());
                assertNull("No unexpected output while idle", outputs.poll(1, TimeUnit.SECONDS));
                assertEquals("Ready while idle", Flow.RUNNING, flow.getState());
                sendInput(context, harness, 2);
                assertEquals("Later output", SECOND_EXPECTED, outputs.poll(10, TimeUnit.SECONDS));
                assertEquals("Ready after later delivery", Flow.RUNNING, flow.getState());
                assertNull("No unexpected output after later delivery", outputs.poll(1, TimeUnit.SECONDS));
                harness.assertIsSatisfied(); // Checks actual component invocations against task 4.
                assertEquals("Ready after verification", Flow.RUNNING, flow.getState());
            } finally {
                try { harness.stopFlow(); } finally { flow.removeFlowListener(listener); }
            }
        }
    }

    private String describeOutput(Object payload) {
        // Task 3: Review how your output is compared. Text is compared directly.
<#if localFile>
        // For an unchanged local-file payload, compare UTF-8 file contents instead of temporary paths.
        if (payload instanceof java.util.List<?>) {
            java.util.List<?> files = (java.util.List<?>) payload;
            if (files.stream().allMatch(item -> item instanceof java.io.File)) {
                StringBuilder contents = new StringBuilder();
                try {
                    for (Object file : files) contents.append(java.nio.file.Files.readString(((java.io.File) file).toPath()));
                    return contents.toString();
                } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
            }
        }
</#if>
        // For byte arrays or custom objects, replace this with meaningful decoding/field extraction.
        return String.valueOf(payload);
    }

    private void configureExpectedPath(IkasanFlowTestRule harness) {
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

    private ConfigurableApplicationContext openTestApplication() throws Exception {
        // TODO 1: Review test isolation.
<#if isolatedFiles>
        // This test uses a temporary input directory and an in-memory database.
<#elseif localFile>
        // Set this consumer's filenames in Studio and regenerate the module/test so its filename
        // property can be overridden for an isolated temporary directory. Do not scan real input.
<#else>
        // Add test-only connection overrides here for this flow's transports (JMS, FTP/SFTP, mail etc.).
        // Consult LOCAL_TEST_ENVIRONMENT.md and generated application.properties for values and keys.
        // Arrange cleanup for any test services you start, including when assertions fail.
</#if>
        // All flows start in MANUAL mode; the test starts only this flow.
        // The whole module's Spring context is loaded, so check whether other
        // components connect to external services during application startup.
<#if isolatedFiles>
        // If none do, no changes are needed here.
</#if>
        return SpringApplication.run(Class.forName("org.ikasan.studio.boot.Application"),
<#if isolatedFiles>
                "--${filenameKey?j_string}=" + inputDirectory.getRoot().getAbsolutePath().replace('\\', '/') + "/batch-.*[.]txt",
</#if>
                "--server.port=0",
                "--datasource.url=jdbc:h2:mem:flowtest_" + java.util.UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
                "--ikasan.module.activator.startup.type.defaultStartupType=MANUAL"<#list flowNames as name>,
                "--ikasan.module.activator.startup.type.flowStartupTypes[${name?index}]=${name?j_string},MANUAL"</#list>);
    }

    private void sendInput(ConfigurableApplicationContext context, IkasanFlowTestRule harness, int batch) throws Exception {
        // TODO 2: Supply the data for batch 1 and batch 2. Keep the same flow running between them.
<#if isolatedFiles>
        // File creation and scanning are supplied for you. Replace these sample contents with your input.
        // Each scan should see only this batch's file; earlier test files are removed before batch 2.
        try (java.nio.file.DirectoryStream<java.nio.file.Path> previous =
                     java.nio.file.Files.newDirectoryStream(inputDirectory.getRoot().toPath(), "batch-*.txt")) {
            for (java.nio.file.Path file : previous) java.nio.file.Files.delete(file);
        }
        String contents = batch == 1 ? "REPLACE: first input" : "REPLACE: second input";
        java.nio.file.Files.writeString(inputDirectory.getRoot().toPath().resolve("batch-" + batch + ".txt"), contents);
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
<#if scheduled && !isolatedFiles>

    private void prepareInputBatch(ConfigurableApplicationContext context, int batch) throws Exception {
        // Task 2: Replace this guard with real input preparation for the scheduled consumer.
        throw new UnsupportedOperationException("Prepare input batch " + batch + " before scanning");
    }
</#if>
}
