package org.ikasan.studio.flowtests.support;

import org.ikasan.studio.flowtests.support.utils.FlowTestSupportFingerprint;
import org.ikasan.studio.flowtests.support.utils.LocalSftpTestServer;
import org.ikasan.studio.flowtests.support.utils.LocalFtpTestServer;
import org.ikasan.studio.flowtests.support.utils.FileDeliveryAssertions;
import org.ikasan.studio.flowtests.support.utils.OutputTextSupport;
import org.ikasan.studio.flowtests.support.utils.LocalSmtpTestServer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import java.util.Arrays;
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
import java.util.Set;
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
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
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
    private int deliveryTimeoutSeconds = 10;
    private final TemporaryFolder ftpTestDirectory = TemporaryFolder.builder().assureDeletion().build();

    private static final String SUPPORT_MODEL_SHA256 = "${supportFingerprint}";
    private static final Set<String> SUPPORT_MODEL_FIELDS = Set.of(
            <#list supportModelFields as field>"${field?j_string}"<#sep>, </#sep></#list>);

    /** Positive per-delivery wait, shared by producer, file, JMS and path assertions. */
    public static int deliveryTimeoutSeconds(String value) {
        try {
            int seconds = Integer.parseInt(value);
            if (seconds > 0) return seconds;
        } catch (NumberFormatException invalid) { /* Report the setting, not a parser failure. */ }
        throw new IllegalArgumentException("test.delivery.timeout-seconds must be a positive whole number");
    }

    /** Allow startup/cleanup plus multiple sequential delivery waits without a fixed 60-second cap. */
    @Rule
    public TestRule scenarioRules() throws IOException {
        // Do not call scenario overrides here: they can allocate temporary directories/services.
        Map<String, String> properties = moduleTestProperties();
        int seconds = deliveryTimeoutSeconds(properties.getOrDefault("test.delivery.timeout-seconds", "10"));
        // JUnit removes files after the test/context cleanup, and reports deletion failures.
        return RuleChain.outerRule(ftpTestDirectory).around(Timeout.seconds(60L + 10L * seconds));
    }
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
        FlowTestSupportFingerprint.verify(SUPPORT_MODEL_SHA256, SUPPORT_MODEL_FIELDS);
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

    /**
     * Replaces literal spaces with underscores in a resource-directory segment.
     * Does not change runtime names, trim text or sanitise other filesystem characters.
     * @param name non-null flow/component name
     * @return the name with spaces replaced
     */
    protected static String noSpaces(String name) {
        return name.replace(' ', '_');
    }

    /**
     * Reads a classpath fixture as UTF-8 text, preserving whitespace and line endings.
     * @param resourcePath path relative to {@code src/test/resources}; leading slash optional
     * @return the complete fixture text, suitable for expected business payloads
     * @throws IllegalArgumentException if the resource does not exist
     * @throws UncheckedIOException if reading fails
     */
    protected static String readTestResource(String resourcePath) {
        String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        try (InputStream input = ModuleFlowTestSupport.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalArgumentException(
                    "Missing test resource " + path + "; add it under user-flow-tests/src/test/resources");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException("Cannot read test resource " + path, failure);
        }
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
        deliveryTimeoutSeconds = deliveryTimeoutSeconds(properties.getOrDefault("test.delivery.timeout-seconds", "10"));
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
        LocalSftpTestServer sftp = null;
        LocalFtpTestServer ftp = null;
        LocalSmtpTestServer smtp = null;
        ConfigurableApplicationContext startedContext = null;
        try {
            if (Boolean.parseBoolean(properties.getOrDefault("test.ftp.enabled", "false"))) {
                ftp = LocalFtpTestServer.start(properties, ftpTestDirectory.newFolder().toPath());
<#list ftpEndpoints as endpoint>
                ftp.configure(properties, "${endpoint.name?j_string}", ${((endpoint.secure!"")?lower_case == "true")?c},
                        "${endpoint.remoteHost?j_string}", "${endpoint.remotePort?j_string}",
                        "${endpoint.username?j_string}", "${endpoint.password?j_string}", "${endpoint.directory?j_string}");
</#list>
            }
            if (Boolean.parseBoolean(properties.getOrDefault("test.smtp.enabled", "false"))) {
                smtp = LocalSmtpTestServer.start();
            }
            if (Boolean.parseBoolean(properties.getOrDefault("test.sftp.enabled", "false"))) {
                sftp = LocalSftpTestServer.start(properties, ftpTestDirectory.newFolder().toPath());
            }
            SpringApplication application = new SpringApplication(sources.toArray(new Class<?>[0]));
            LocalSftpTestServer ownedSftp = sftp;
            if (ownedSftp != null) application.addInitializers(context -> {
                context.getBeanFactory().registerSingleton("studioLocalSftpTestServer", ownedSftp);
                ((DefaultListableBeanFactory) context.getBeanFactory())
                        .registerDisposableBean("studioLocalSftpTestServer", ownedSftp::close);
            });
            LocalFtpTestServer ownedFtp = ftp;
            if (ownedFtp != null) application.addInitializers(context -> {
                context.getBeanFactory().registerSingleton("studioLocalFtpTestServer", ownedFtp);
                // Register before application beans so the server is destroyed after the flows.
                ((DefaultListableBeanFactory) context.getBeanFactory())
                        .registerDisposableBean("studioLocalFtpTestServer", ownedFtp::close);
            });
            LocalSmtpTestServer ownedSmtp = smtp;
            if (ownedSmtp != null) application.addInitializers(context -> {
                context.getBeanFactory().registerSingleton("studioLocalSmtpTestServer", ownedSmtp);
                ((DefaultListableBeanFactory) context.getBeanFactory())
                        .registerDisposableBean("studioLocalSmtpTestServer", ownedSmtp::close);
            });
            String[] arguments = properties.entrySet().stream()
                    .map(entry -> "--" + entry.getKey() + "=" + entry.getValue()).toArray(String[]::new);
            application.addInitializers(context -> {
                for (String key : properties.keySet()) {
                    try { context.getEnvironment().getProperty(key); }
                    catch (IllegalArgumentException invalidReference) {
                        throw new IllegalStateException("Review module-test.properties or scenario overrides: property '"
                                + key + "' refers to a missing or invalid application property. "
                                + "Component renames can change property keys; compare with generated application.properties.", invalidReference);
                    }
                }
            });
            startedContext = application.run(arguments);
            if (ftp != null) {
                Module<Flow> module = startedContext.getBean(Module.class);
<#list ftpEndpoints as endpoint>
<#if endpoint.consumer == "true">
                ftp.configureConsumer(module.getFlow("${endpoint.flow?j_string}")
                        .getFlowElement("${endpoint.component?j_string}").getFlowComponent(), properties);
</#if>
</#list>
            }
            if (sftp != null) {
                Module<Flow> module = startedContext.getBean(Module.class);
<#list sftpEndpoints as endpoint>
                sftp.configure(module.getFlow("${endpoint.flow?j_string}")
                        .getFlowElement("${endpoint.component?j_string}").getFlowComponent(),
                        "${endpoint.flow?j_string}", "${endpoint.component?j_string}", ${endpoint.consumer}, properties);
</#list>
            }
            if (smtp != null) {
                Module<Flow> module = startedContext.getBean(Module.class);
<#list smtpEndpoints as endpoint>
                smtp.configure(module.getFlow("${endpoint.flow?j_string}")
                        .getFlowElement("${endpoint.component?j_string}").getFlowComponent());
</#list>
            }
            return startedContext;
        } catch (Exception | Error failure) {
            if (startedContext != null) try { startedContext.close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            if (smtp != null) try { smtp.close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            if (sftp != null) try { sftp.close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            if (ftp != null) try { ftp.close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
    /** Override to add scenario-specific Spring configuration classes to each fresh application. */
    protected Class<?>[] testConfigurationClasses() { return new Class<?>[0]; }

    // Defaults preserve older developer-owned tests that use verifyFlow/verifyScenario directly.
    /** Returns the exact model flow name, including spaces; resource-path normalisation does not apply here. */
    protected String getFlowName() {
        throw new UnsupportedOperationException("Override getFlowName() before using runTest()");
    }
    /**
     * Defines component invocation expectations before flow startup, including both input batches.
     * This checks the journey only; use {@link #verifyReceivedOutput} for actual receiver delivery.
     */
    protected void defineExpectedPath(IkasanFlowTestRule harness) {
        throw new UnsupportedOperationException("Define the expected component path");
    }
    /** Override to extract stable business content from the observed producer payload. */
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

    /** Business scenario executed within one owned application context; exceptions fail the test. */
    @FunctionalInterface
    protected interface TestScenario {
        void run(ConfigurableApplicationContext context) throws Exception;
    }
    /** Supplies batch 1 or 2 through the real consumer using the same context and test rule. */
    @FunctionalInterface
    protected interface ContextBatchInput {
        void send(ConfigurableApplicationContext context,
                  IkasanFlowTestRule harness, int batch) throws Exception;
    }

    /** Fresh context per scenario, closed even if setup, delivery or assertions fail. */
    protected final void runTest(boolean configured, TestScenario scenario) throws Exception {
        assertTrue("Complete TODO 1–4, then set TEST_REVIEWED=true in TODO 5. See user-flow-tests/README.md", configured);
        try (ConfigurableApplicationContext context = openTestApplication(flowTestProperties())) {
            scenario.run(context);
        }
    }

    /**
     * Supplies batch 1 or 2 through the real consumer; override in the concrete test.
     * Called after startup on the same flow for both batches. Scheduled inputs must be fully
     * prepared before firing the consumer; do not restart the flow or reset application beans.
     * @param context current scenario's application context
     * @param harness rule attached to the already-running flow
     * @param batch one-based batch number (1 or 2)
     * @throws Exception if fixture preparation or sending fails
     */
    protected void supplyInput(ConfigurableApplicationContext context, IkasanFlowTestRule harness,
                               int batch) throws Exception {
        throw new UnsupportedOperationException("Override supplyInput() to provide test data");
    }

    /**
     * Optional business delivery assertion after the selected producer completes and its payload matches.
     * Override to inspect physical files, captured mail, broker messages or downstream state.
     * The default does nothing: component invocation alone does not prove receiver delivery.
     * @param context current scenario's application context
     * @param batch completed batch number (1 or 2)
     * @param expected expected text for this batch, not cumulative previous batches
     * @throws Exception if receiver access or verification fails
     */
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

    /** Returns the test FTP server's directory inside the JUnit temporary folder. */
    protected final Path localFtpDirectory(ConfigurableApplicationContext context) {
        if (context.getBeansOfType(LocalFtpTestServer.class).isEmpty()) {
            throw new IllegalStateException("Enable test.ftp.enabled=true in module-test.properties, or adapt verifyReceivedOutput for your external FTP server");
        }
        return context.getBean(LocalFtpTestServer.class).root();
    }

    /** Returns one endpoint's directory on the local test SFTP server, retained across batches. */
    protected final Path localSftpDirectory(ConfigurableApplicationContext context, String flow, String component) {
        if (context.getBeansOfType(LocalSftpTestServer.class).isEmpty())
            throw new IllegalStateException("Enable test.sftp.enabled=true in module-test.properties");
        return context.getBean(LocalSftpTestServer.class).directory(flow, component);
    }

    /** Returns the owned SMTP inbox; external servers need their own receiver-side assertions. */
    protected final LocalSmtpTestServer localSmtpServer(ConfigurableApplicationContext context) {
        if (context.getBeansOfType(LocalSmtpTestServer.class).isEmpty())
            throw new IllegalStateException("Enable test.smtp.enabled=true in module-test.properties");
        return context.getBean(LocalSmtpTestServer.class);
    }

    /** Returns the context's positive {@code test.delivery.timeout-seconds}, defaulting to ten seconds. */
    protected final Duration deliveryTimeout(ConfigurableApplicationContext context) {
        return Duration.ofSeconds(deliveryTimeoutSeconds(context.getEnvironment()
                .getProperty("test.delivery.timeout-seconds", "10")));
    }

    /** Waits for an exact final file and UTF-8 contents; suitable for local or locally accessible server output. */
    protected final void assertFileContents(Path file, String expected) throws Exception {
        FileDeliveryAssertions.assertFileContents(file, expected, Duration.ofSeconds(deliveryTimeoutSeconds));
    }

    /** Checks exact file count and contents (including duplicates) for a final-filename glob, irrespective of order. */
    protected final void assertDeliveredFileContents(Path directory, String glob, String... expected) throws Exception {
        FileDeliveryAssertions.assertDeliveredFileContents(directory, glob, List.of(expected), Duration.ofSeconds(deliveryTimeoutSeconds));
    }

    /**
     * Compares a physical file with an independent UTF-8 classpath fixture using the shared timeout.
     * @param file local delivered file; resource basenames do not dictate this filename
     * @param resource expected-content path relative to {@code src/test/resources}
     * @throws Exception if the resource cannot be read or delivery cannot be inspected
     * @throws AssertionError if the delivered file does not match before the deadline
     */
    protected final void assertFileMatchesResource(Path file, String resource) throws Exception {
        String expected = readTestResource(resource);
        try { assertFileContents(file, expected); }
        catch (AssertionError failure) {
            throw new AssertionError("Expected resource " + resource + ": " + failure.getMessage(), failure);
        }
    }

    /**
     * Checks cumulative file count and contents against independent classpath fixtures.
     * Resource basenames do not constrain delivered filenames; the glob selects final output files.
     * @param directory fresh local output directory, or downloaded remote output
     * @param glob final-file glob, for example {@code *.xml}
     * @param resources expected UTF-8 resource paths; repeat entries for expected duplicate contents
     * @throws Exception on resource or filesystem errors
     * @throws AssertionError if count/contents differ after the shared timeout
     */
    protected final void assertDeliveredFileResources(Path directory, String glob, String... resources) throws Exception {
        String[] expected = new String[resources.length];
        for (int i = 0; i < resources.length; i++) expected[i] = readTestResource(resources[i]);
        try { assertDeliveredFileContents(directory, glob, expected); }
        catch (AssertionError failure) {
            throw new AssertionError("Expected resources " + Arrays.toString(resources)
                    + ": " + failure.getMessage(), failure);
        }
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
        assertTrue("Review TODO 1–2, then set TEST_REVIEWED=true", configured);
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
                            awaitOutputText(initialOutputs, deliveryTimeoutSeconds));
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
     * Waits up to the configured delivery timeout for the delivery count to exceed the supplied snapshot.
     * Fails if the flow is no longer RUNNING or no new event arrives before the deadline.
     */
    private void awaitObservedEvent(Flow flow,
            AtomicLong delivered, long previous) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(deliveryTimeoutSeconds);
        while (delivered.get() <= previous) {
            assertEquals("Ready for generated events", Flow.RUNNING, flow.getState());
            if (System.nanoTime() >= deadline) fail("No new event reached the selected producer within " + deliveryTimeoutSeconds + " seconds");
            Thread.sleep(20);
        }
        assertEquals("Running after delivery", Flow.RUNNING, flow.getState());
    }

    /** Sends one numbered batch using the running flow; must not restart or reset its consumer. */
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

    /** Receiver-side checks after each observed batch; throw an assertion failure on missing delivery. */
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
                String actual = awaitOutputText(outputs, deliveryTimeoutSeconds);
                assertNotNull("No output observed after producer '" + output + "' in flow '" + flowName
                        + "' for batch " + batch + " within " + deliveryTimeoutSeconds + " seconds. Flow state: "
                        + flow.getState() + ". Check earlier flow errors, consumer filename/minimum-age filters and endpoint connections.", actual);
                assertEquals("Output for batch " + batch, batch == 1 ? firstExpected : secondExpected, actual);
                receiverCheck.verify(batch);
                assertEquals("Ready after delivery", Flow.RUNNING, flow.getState());
                assertNull("No unexpected output while idle", awaitOutputText(outputs, 1));
                assertEquals("Ready while idle", Flow.RUNNING, flow.getState());
            }
        });
    }

    /** Custom scenario with a started flow and a queue of text observations after the selected producer. */
    @FunctionalInterface
    protected interface FlowScenario {
        void verify(IkasanFlowTestRule harness,
                    Flow flow,
                    BlockingQueue<String> outputs) throws Exception;
    }

    /**
     * Attaches the test rule and output listener, starts the flow and runs the supplied scenario.
     * The scenario supplies input and asserts deliveries, branches or deliberate absence.
     * Allows the configured delivery timeout afterward for component-path expectations to finish, then checks
     * that the flow remains RUNNING. Always stops the test flow and removes the output listener;
     * the caller retains ownership of the application context.
     */
    protected final void verifyScenario(ConfigurableApplicationContext context, String flowName, String output,
            Function<Object, String> describeOutput,
            Consumer<IkasanFlowTestRule> expectations,
            FlowScenario scenario) throws Exception {
        deliveryTimeoutSeconds = deliveryTimeoutSeconds(context.getEnvironment()
                .getProperty("test.delivery.timeout-seconds", "10"));
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
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(deliveryTimeoutSeconds);
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
