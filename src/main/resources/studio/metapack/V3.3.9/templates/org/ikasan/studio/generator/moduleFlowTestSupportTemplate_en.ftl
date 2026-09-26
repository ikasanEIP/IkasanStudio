package org.ikasan.studio.flowtests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import org.ikasan.spec.flow.Flow;
import org.ikasan.spec.flow.FlowElement;
import org.ikasan.spec.flow.FlowEvent;
import org.ikasan.spec.flow.FlowEventListener;
import org.ikasan.spec.module.Module;
import org.ikasan.testharness.flow.rule.IkasanFlowTestRule;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Developer-owned shared setup. Each call creates a NEW application, never a cached/static context. */
public abstract class ModuleFlowTestSupport {
    private volatile RuntimeException outputTextFailure;
    /**
     * Reloads shared test settings from the UTF-8 properties file into a fresh map.
     * Fails if the file is missing rather than silently using application connection defaults.
     *
     * TODO: Review module-wide connections in src/test/resources/module-test.properties.
     * Consult LOCAL_TEST_ENVIRONMENT.md for test values.
     * The whole module's Spring context is loaded. Other components may connect to external
     * services during startup even when their flows are MANUAL. Do not use production endpoints.
     * No credentials are copied from the model into the properties file.
     * For test services you start, use JUnit @Before/@After methods with reliable cleanup,
     * including partial startup failure. Do not stop services owned by another application.
     * If no other components connect during startup, no connection changes are needed.
     * Regenerating this class archives it first and preserves module-test.properties.
     */
    protected Map<String, String> moduleTestProperties() throws IOException {
        Properties loaded = new Properties();
        try (InputStream input = ModuleFlowTestSupport.class.getResourceAsStream("/module-test.properties")) {
            if (input == null) throw new IOException(
                    "Missing src/test/resources/module-test.properties in user-flow-tests; generate or restore it before testing.");
            loaded.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        }
        Map<String, String> properties = new LinkedHashMap<>();
        for (String key : loaded.stringPropertyNames()) properties.put(key, loaded.getProperty(key));
        return properties;
    }

    /** Fresh scenario overrides for each test invocation; shared settings come from module-test.properties. */
    protected Map<String, String> flowTestProperties() {
        return new LinkedHashMap<>();
    }

    /**
     * Starts a fresh application with shared settings followed by scenario overrides.
     * Enforces a random HTTP port, unique in-memory H2 database and MANUAL flow startup.
     * The caller owns the returned context and must close it with try-with-resources.
     */
    protected final ConfigurableApplicationContext openTestApplication(Map<String, String> flowProperties) throws Exception {
        outputTextFailure = null;
        Map<String, String> properties = new LinkedHashMap<>(moduleTestProperties());
        properties.putAll(flowProperties); // Flow-specific settings override shared connections.
        // Enforced isolation is scoped to this test application, never the saved Studio model.
        properties.put("server.port", "0");
        properties.put("datasource.url", "jdbc:h2:mem:flowtest_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        properties.put("ikasan.module.activator.startup.type.defaultStartupType", "MANUAL");
<#list flowNames as name>
        properties.put("ikasan.module.activator.startup.type.flowStartupTypes[${name?index}]", "${name?j_string},MANUAL");
</#list>
        List<Class<?>> sources = new ArrayList<>();
        sources.add(Class.forName("org.ikasan.studio.boot.Application"));
        Collections.addAll(sources, testConfigurationClasses());
        LocalFtpTestServer ftp = null;
        try {
            if (Boolean.parseBoolean(properties.getOrDefault("test.ftp.enabled", "false"))) {
                ftp = LocalFtpTestServer.start(properties);
<#list ftpEndpoints as endpoint>
                ftp.configure(properties, "${endpoint.name?j_string}", ${((endpoint.secure!"")?lower_case == "true")?c},
                        "${endpoint.remoteHost?j_string}", "${endpoint.remotePort?j_string}",
                        "${endpoint.username?j_string}", "${endpoint.password?j_string}", "${endpoint.directory?j_string}");
</#list>
            }
            SpringApplication application = new SpringApplication(sources.toArray(new Class<?>[0]));
            LocalFtpTestServer ownedFtp = ftp;
            if (ownedFtp != null) application.addInitializers(context -> {
                context.getBeanFactory().registerSingleton("studioLocalFtpTestServer", ownedFtp);
                // Register before application beans so the server is destroyed after the flows.
                ((org.springframework.beans.factory.support.DefaultListableBeanFactory) context.getBeanFactory())
                        .registerDisposableBean("studioLocalFtpTestServer", ownedFtp::close);
            });
            String[] arguments = properties.entrySet().stream()
                    .map(entry -> "--" + entry.getKey() + "=" + entry.getValue()).toArray(String[]::new);
            return application.run(arguments);
        } catch (Exception | Error failure) {
            if (ftp != null) try { ftp.close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
    protected Class<?>[] testConfigurationClasses() { return new Class<?>[0]; }

    // Defaults preserve older developer-owned tests that use verifyFlow/verifyScenario directly.
    protected String getFlowName() {
        throw new UnsupportedOperationException("Override getFlowName() before using runTest()");
    }
    protected void defineExpectedPath(IkasanFlowTestRule harness) {
        throw new UnsupportedOperationException("Define the expected component path");
    }
    protected String outputText(Object payload) { return String.valueOf(payload); }

    /** Explicit content conversion; false retains the original String.valueOf behaviour. */
    protected final String outputText(Object payload, boolean stringifyActualOutput) {
        try {
            return stringifyActualOutput ? OutputTextSupport.stringify(payload) : String.valueOf(payload);
        } catch (RuntimeException failure) {
            outputTextFailure = failure;
            throw failure;
        }
    }

    @FunctionalInterface
    protected interface TestScenario {
        void run(ConfigurableApplicationContext context) throws Exception;
    }
    @FunctionalInterface
    protected interface ContextBatchInput {
        void send(ConfigurableApplicationContext context,
                  IkasanFlowTestRule harness, int batch) throws Exception;
    }

    /** Fresh context per scenario, closed even if setup, delivery or assertions fail. */
    protected final void runTest(boolean configured, TestScenario scenario) throws Exception {
        assertTrue("Complete TODO 1–4, then set CONFIGURED=true in TODO 5. See user-flow-tests/README.md", configured);
        try (ConfigurableApplicationContext context = openTestApplication(flowTestProperties())) {
            scenario.run(context);
        }
    }

    /** Supplies batch 1 or 2 through the real consumer; override in the concrete test. */
    protected void supplyInput(ConfigurableApplicationContext context, IkasanFlowTestRule harness,
                               int batch) throws Exception {
        throw new UnsupportedOperationException("Override supplyInput() to provide test data");
    }

    /** Optional receiver-side assertion, called after the producer sees each expected payload. */
    protected void verifyReceivedOutput(ConfigurableApplicationContext context, int batch,
                                        String expected) throws Exception { }

    /**
     * Runs the standard scenario using the concrete test's named overrides.
     * Calls getFlowName() and defineExpectedPath(), then supplyInput() for batch 1 and batch 2.
     * For each batch, compares outputText(payload) and calls verifyReceivedOutput().
     * The shared lifecycle checks continued readiness and closes the isolated application.
     */
    protected final void runTest(boolean configured, String output,
                                 String firstExpected, String secondExpected) throws Exception {
        runTest(configured, context -> verifyFlow(context, getFlowName(), output,
                this::outputText, this::defineExpectedPath,
                (harness, batch) -> supplyInput(context, harness, batch), firstExpected, secondExpected,
                batch -> verifyReceivedOutput(context, batch, batch == 1 ? firstExpected : secondExpected)));
    }

    /** Standard two-batch scenario; custom routing/rejection tests can use the scenario overload. */
    protected final void runTest(boolean configured, String output, ContextBatchInput input,
                                 String firstExpected, String secondExpected) throws Exception {
        runTest(configured, context -> verifyFlow(context, getFlowName(), output,
                this::outputText, this::defineExpectedPath,
                (harness, batch) -> input.send(context, harness, batch), firstExpected, secondExpected));
    }

    /**
     * Concatenates UTF-8 contents when the payload is a list of files, avoiding temporary paths
     * in assertions. Other payloads use their string representation; override outputText for
     * custom conversions. File read failures fail the test rather than hiding missing content.
     */
    protected final String describeFileOutput(Object payload) {
        if (payload instanceof List<?>) {
            List<?> files = (List<?>) payload;
            if (files.stream().allMatch(item -> item instanceof File)) {
                StringBuilder contents = new StringBuilder();
                try {
                    for (Object file : files) contents.append(Files.readString(((File) file).toPath()));
                    return contents.toString();
                } catch (IOException failure) { throw new UncheckedIOException(failure); }
            }
        }
        return String.valueOf(payload);
    }

    /** Returns the actual isolated FTP home created for this context, never a guessed remote directory. */
    protected final Path localFtpDirectory(ConfigurableApplicationContext context) {
        if (context.getBeansOfType(LocalFtpTestServer.class).isEmpty()) {
            throw new IllegalStateException("Enable test.ftp.enabled=true in module-test.properties, or adapt verifyReceivedOutput for your external FTP server");
        }
        return context.getBean(LocalFtpTestServer.class).root();
    }

    /** Waits for an exact final file and UTF-8 contents; suitable for local or locally accessible server output. */
    protected final void assertFileContents(Path file, String expected) throws Exception {
        FileDeliveryAssertions.assertFileContents(file, expected, Duration.ofSeconds(10));
    }

    /** Checks exact file count and contents (including duplicates) for a final-filename glob, irrespective of order. */
    protected final void assertDeliveredFileContents(Path directory, String glob, String... expected) throws Exception {
        FileDeliveryAssertions.assertDeliveredFileContents(directory, glob, List.of(expected), Duration.ofSeconds(10));
    }

    /** Compatibility overload for observation tests generated without payload expectations. */
    protected final void runObservationTest(boolean configured, String output) throws Exception {
        runObservationTest(configured, output, List.of());
    }

    /**
     * Starts the selected self-generating flow and checks its initial payload sequence at the sink.
     * Checks RUNNING state for one second, then requires a fresh later event without restarting.
     * Retains only the expected initial samples; subsequent events are counted without storing payloads.
     * Finally stops the test flow, checks its stopped state, removes the listener and closes the context.
     */
    protected final void runObservationTest(boolean configured, String output,
            List<String> expectedInitialOutputs) throws Exception {
        List<String> expected = List.copyOf(expectedInitialOutputs);
        assertTrue("Review TODO 1–2, then set CONFIGURED=true", configured);
        try (ConfigurableApplicationContext context = openTestApplication(flowTestProperties())) {
            Module<Flow> module = context.getBean(Module.class);
            Flow flow = module.getFlow(getFlowName());
            assertNotNull("Flow must exist: " + getFlowName(), flow);
            AtomicLong delivered = new AtomicLong();
            BlockingQueue<String> initialOutputs =
                    new ArrayBlockingQueue<>(Math.max(1, expected.size()));
            FlowEventListener listener = new FlowEventListener() {
                public void beforeFlowElement(String m, String f, FlowElement e, FlowEvent event) { }
                /** Counts sink invocations and captures only the bounded initial payload sequence. */
                public void afterFlowElement(String m, String f, FlowElement e, FlowEvent event) {
                    if (output.equals(e.getComponentName())) {
                        long index = delivered.incrementAndGet();
                        if (index <= expected.size()) initialOutputs.offer(outputText(event.getPayload()));
                    }
                }
            };
            flow.addFlowListener(listener);
            try (AutoCloseable cleanup = flowCleanup(flow, listener, () -> {
                flow.stop();
                assertEquals("Stopped during test teardown", Flow.STOPPED, flow.getState());
            })) {
                flow.start();
                awaitObservedEvent(flow, delivered, 0);
                for (int index = 0; index < expected.size(); index++) {
                    assertEquals("Initial producer payload " + (index + 1), expected.get(index),
                            awaitOutputText(initialOutputs, 10));
                }
                // Keep the SAME flow running during the observation window, then require a NEW event.
                long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
                while (System.nanoTime() < until) {
                    assertEquals("Continued running", Flow.RUNNING, flow.getState());
                    Thread.sleep(20);
                }
                awaitObservedEvent(flow, delivered, delivered.get());
            }
        }
    }

    /** Surfaces asynchronous content-decoding failures on the test thread instead of reporting a missing output. */
    private String awaitOutputText(BlockingQueue<String> outputs, int seconds) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        do {
            if (outputTextFailure != null) throw outputTextFailure;
            String output = outputs.poll(25, TimeUnit.MILLISECONDS);
            if (outputTextFailure != null) throw outputTextFailure;
            if (output != null) return output;
        } while (System.nanoTime() < deadline);
        return null;
    }

    /**
     * Waits up to ten seconds for the delivery count to exceed the supplied snapshot.
     * Fails if the flow is no longer RUNNING or no new event arrives before the deadline.
     */
    private void awaitObservedEvent(Flow flow,
            AtomicLong delivered, long previous) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (delivered.get() <= previous) {
            assertEquals("Ready for generated events", Flow.RUNNING, flow.getState());
            if (System.nanoTime() >= deadline) fail("No new event reached the selected producer within 10 seconds");
            Thread.sleep(20);
        }
        assertEquals("Running after delivery", Flow.RUNNING, flow.getState());
    }

    @FunctionalInterface
    protected interface BatchInput {
        void send(IkasanFlowTestRule harness, int batch) throws Exception;
    }

    /** Common lifecycle; individual tests supply the path, inputs and meaningful expected payloads. */
    protected final void verifyFlow(ConfigurableApplicationContext context, String flowName, String output,
            Function<Object, String> describeOutput,
            Consumer<IkasanFlowTestRule> expectations,
            BatchInput input, String firstExpected, String secondExpected) throws Exception {
        verifyFlow(context, flowName, output, describeOutput, expectations, input, firstExpected, secondExpected, batch -> { });
    }

    @FunctionalInterface
    protected interface BatchVerification { void verify(int batch) throws Exception; }

    /**
     * Sends two batches through the same running flow, checking each observed payload and
     * the caller's receiver-side assertions. Between batches, checks that the flow stays
     * RUNNING and produces no unexpected output during a one-second idle window.
     * Delegates component-path verification and flow cleanup to verifyScenario.
     */
    protected final void verifyFlow(ConfigurableApplicationContext context, String flowName, String output,
            Function<Object, String> describeOutput,
            Consumer<IkasanFlowTestRule> expectations,
            BatchInput input, String firstExpected, String secondExpected, BatchVerification receiverCheck) throws Exception {
        verifyScenario(context, flowName, output, describeOutput, expectations, (harness, flow, outputs) -> {
            for (int batch = 1; batch <= 2; batch++) {
                input.send(harness, batch);
                assertEquals("Output for batch " + batch,
                        batch == 1 ? firstExpected : secondExpected,
                        awaitOutputText(outputs, 10));
                receiverCheck.verify(batch);
                assertEquals("Ready after delivery", Flow.RUNNING, flow.getState());
                assertNull("No unexpected output while idle", awaitOutputText(outputs, 1));
                assertEquals("Ready while idle", Flow.RUNNING, flow.getState());
            }
        });
    }

    @FunctionalInterface
    protected interface FlowScenario {
        void verify(IkasanFlowTestRule harness,
                    Flow flow,
                    BlockingQueue<String> outputs) throws Exception;
    }

    /**
     * Attaches the test rule and output listener, starts the flow and runs the supplied scenario.
     * The scenario supplies input and asserts deliveries, branches or deliberate absence.
     * Allows up to ten seconds afterward for component-path expectations to finish, then checks
     * that the flow remains RUNNING. Always stops the test flow and removes the output listener;
     * the caller retains ownership of the application context.
     */
    protected final void verifyScenario(ConfigurableApplicationContext context, String flowName, String output,
            Function<Object, String> describeOutput,
            Consumer<IkasanFlowTestRule> expectations,
            FlowScenario scenario) throws Exception {
        Module<Flow> module = context.getBean(Module.class);
        Flow flow = module.getFlow(flowName);
        assertNotNull("Flow must exist: " + flowName, flow);
        BlockingQueue<String> outputs = new LinkedBlockingQueue<>();
        FlowEventListener listener = new FlowEventListener() {
            public void beforeFlowElement(String m, String f, FlowElement e, FlowEvent event) { }
            public void afterFlowElement(String m, String f, FlowElement e, FlowEvent event) {
                if (output.equals(e.getComponentName())) outputs.add(describeOutput.apply(event.getPayload()));
            }
        };
        var harness = new IkasanFlowTestRule().withFlow(flow);
        expectations.accept(harness);
        flow.addFlowListener(listener);
        try (AutoCloseable cleanup = flowCleanup(flow, listener, harness::stopFlow)) {
            harness.startFlow();
            scenario.verify(harness, flow, outputs);
            // A rejected/filtered event can finish asynchronously without reaching the output listener.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (true) {
                try { harness.assertIsSatisfied(); break; }
                catch (AssertionError pending) {
                    if (System.nanoTime() >= deadline) throw pending;
                    Thread.sleep(50);
                }
            }
            assertEquals("Ready after scenario", Flow.RUNNING, flow.getState());
        }
    }

    /** Teardown failures are suppressed onto any original startup/assertion failure by try-with-resources. */
    private AutoCloseable flowCleanup(Flow flow, FlowEventListener listener, Runnable stop) {
        return () -> {
            try (AutoCloseable removal = () -> flow.removeFlowListener(listener)) {
                stop.run();
            }
        };
    }

}
