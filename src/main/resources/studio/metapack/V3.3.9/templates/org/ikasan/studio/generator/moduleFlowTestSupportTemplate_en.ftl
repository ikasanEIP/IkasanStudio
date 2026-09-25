package org.ikasan.studio.flowtests;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Developer-owned shared setup. Each call creates a NEW application, never a cached/static context. */
public abstract class ModuleFlowTestSupport {
    /**
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
    protected Map<String, String> moduleTestProperties() throws java.io.IOException {
        java.util.Properties loaded = new java.util.Properties();
        try (java.io.InputStream input = ModuleFlowTestSupport.class.getResourceAsStream("/module-test.properties")) {
            if (input == null) throw new java.io.IOException(
                    "Missing src/test/resources/module-test.properties in user-flow-tests; generate or restore it before testing.");
            loaded.load(new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8));
        }
        Map<String, String> properties = new LinkedHashMap<>();
        for (String key : loaded.stringPropertyNames()) properties.put(key, loaded.getProperty(key));
        return properties;
    }

    /** Fresh scenario overrides for each test invocation; shared settings come from module-test.properties. */
    protected Map<String, String> flowTestProperties() {
        return new LinkedHashMap<>();
    }

    // The caller owns this context: use try-with-resources so it closes on success or failure.
    protected final ConfigurableApplicationContext openTestApplication(Map<String, String> flowProperties) throws Exception {
        Map<String, String> properties = new LinkedHashMap<>(moduleTestProperties());
        properties.putAll(flowProperties); // Flow-specific settings override shared connections.
        // Enforced isolation is scoped to this test application, never the saved Studio model.
        properties.put("server.port", "0");
        properties.put("datasource.url", "jdbc:h2:mem:flowtest_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        properties.put("ikasan.module.activator.startup.type.defaultStartupType", "MANUAL");
<#list flowNames as name>
        properties.put("ikasan.module.activator.startup.type.flowStartupTypes[${name?index}]", "${name?j_string},MANUAL");
</#list>
        String[] arguments = properties.entrySet().stream()
                .map(entry -> "--" + entry.getKey() + "=" + entry.getValue()).toArray(String[]::new);
        java.util.List<Class<?>> sources = new java.util.ArrayList<>();
        sources.add(Class.forName("org.ikasan.studio.boot.Application"));
        java.util.Collections.addAll(sources, testConfigurationClasses());
        return new SpringApplication(sources.toArray(new Class<?>[0])).run(arguments);
    }
    protected Class<?>[] testConfigurationClasses() { return new Class<?>[0]; }

    // Defaults preserve older developer-owned tests that use verifyFlow/verifyScenario directly.
    protected String getFlowName() {
        throw new UnsupportedOperationException("Override getFlowName() before using runTest()");
    }
    protected void defineExpectedPath(org.ikasan.testharness.flow.rule.IkasanFlowTestRule harness) {
        throw new UnsupportedOperationException("Define the expected component path");
    }
    protected String outputText(Object payload) { return String.valueOf(payload); }

    @FunctionalInterface
    protected interface TestScenario {
        void run(ConfigurableApplicationContext context) throws Exception;
    }
    @FunctionalInterface
    protected interface ContextBatchInput {
        void send(ConfigurableApplicationContext context,
                  org.ikasan.testharness.flow.rule.IkasanFlowTestRule harness, int batch) throws Exception;
    }

    /** Fresh context per scenario, closed even if setup, delivery or assertions fail. */
    protected final void runTest(boolean configured, TestScenario scenario) throws Exception {
        org.junit.Assert.assertTrue("Complete TODO 1–4, then set CONFIGURED=true in TODO 5. See user-flow-tests/README.md", configured);
        try (ConfigurableApplicationContext context = openTestApplication(flowTestProperties())) {
            scenario.run(context);
        }
    }

    /** Standard two-batch scenario; custom routing/rejection tests can use the scenario overload. */
    protected final void runTest(boolean configured, String output, ContextBatchInput input,
                                 String firstExpected, String secondExpected) throws Exception {
        runTest(configured, context -> verifyFlow(context, getFlowName(), output,
                this::outputText, this::defineExpectedPath,
                (harness, batch) -> input.send(context, harness, batch), firstExpected, secondExpected));
    }

    /** Decode unchanged local-file consumer payloads; converted payloads use describeOutput instead. */
    protected final String describeFileOutput(Object payload) {
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
        return String.valueOf(payload);
    }

    /** Bounded observation for a self-generating source and discard sink; no payloads are retained. */
    protected final void runObservationTest(boolean configured, String output) throws Exception {
        org.junit.Assert.assertTrue("Review TODO 1–2, then set CONFIGURED=true", configured);
        try (ConfigurableApplicationContext context = openTestApplication(flowTestProperties())) {
            org.ikasan.spec.module.Module<org.ikasan.spec.flow.Flow> module = context.getBean(org.ikasan.spec.module.Module.class);
            org.ikasan.spec.flow.Flow flow = module.getFlow(getFlowName());
            org.junit.Assert.assertNotNull("Flow must exist: " + getFlowName(), flow);
            java.util.concurrent.atomic.AtomicLong delivered = new java.util.concurrent.atomic.AtomicLong();
            org.ikasan.spec.flow.FlowEventListener listener = new org.ikasan.spec.flow.FlowEventListener() {
                public void beforeFlowElement(String m, String f, org.ikasan.spec.flow.FlowElement e, org.ikasan.spec.flow.FlowEvent event) { }
                public void afterFlowElement(String m, String f, org.ikasan.spec.flow.FlowElement e, org.ikasan.spec.flow.FlowEvent event) {
                    if (output.equals(e.getComponentName())) delivered.incrementAndGet();
                }
            };
            flow.addFlowListener(listener);
            try {
                flow.start();
                awaitObservedEvent(flow, delivered, 0);
                // Keep the SAME flow running during the observation window, then require a NEW event.
                long until = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(1);
                while (System.nanoTime() < until) {
                    org.junit.Assert.assertEquals("Continued running", org.ikasan.spec.flow.Flow.RUNNING, flow.getState());
                    Thread.sleep(20);
                }
                awaitObservedEvent(flow, delivered, delivered.get());
            } finally {
                // Stopping here is bounded test teardown, never a change to module startup settings.
                try { flow.stop(); } finally { flow.removeFlowListener(listener); }
            }
        }
    }

    private void awaitObservedEvent(org.ikasan.spec.flow.Flow flow,
            java.util.concurrent.atomic.AtomicLong delivered, long previous) throws InterruptedException {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while (delivered.get() <= previous) {
            org.junit.Assert.assertEquals("Ready for generated events", org.ikasan.spec.flow.Flow.RUNNING, flow.getState());
            if (System.nanoTime() >= deadline) org.junit.Assert.fail("No new event reached the selected producer within 10 seconds");
            Thread.sleep(20);
        }
        org.junit.Assert.assertEquals("Running after delivery", org.ikasan.spec.flow.Flow.RUNNING, flow.getState());
    }

    @FunctionalInterface
    protected interface BatchInput {
        void send(org.ikasan.testharness.flow.rule.IkasanFlowTestRule harness, int batch) throws Exception;
    }

    /** Common lifecycle; individual tests supply the path, inputs and meaningful expected payloads. */
    protected final void verifyFlow(ConfigurableApplicationContext context, String flowName, String output,
            java.util.function.Function<Object, String> describeOutput,
            java.util.function.Consumer<org.ikasan.testharness.flow.rule.IkasanFlowTestRule> expectations,
            BatchInput input, String firstExpected, String secondExpected) throws Exception {
        verifyFlow(context, flowName, output, describeOutput, expectations, input, firstExpected, secondExpected, batch -> { });
    }

    @FunctionalInterface
    protected interface BatchVerification { void verify(int batch) throws Exception; }

    protected final void verifyFlow(ConfigurableApplicationContext context, String flowName, String output,
            java.util.function.Function<Object, String> describeOutput,
            java.util.function.Consumer<org.ikasan.testharness.flow.rule.IkasanFlowTestRule> expectations,
            BatchInput input, String firstExpected, String secondExpected, BatchVerification receiverCheck) throws Exception {
        verifyScenario(context, flowName, output, describeOutput, expectations, (harness, flow, outputs) -> {
            for (int batch = 1; batch <= 2; batch++) {
                input.send(harness, batch);
                org.junit.Assert.assertEquals("Output for batch " + batch,
                        batch == 1 ? firstExpected : secondExpected,
                        outputs.poll(10, java.util.concurrent.TimeUnit.SECONDS));
                receiverCheck.verify(batch);
                org.junit.Assert.assertEquals("Ready after delivery", org.ikasan.spec.flow.Flow.RUNNING, flow.getState());
                org.junit.Assert.assertNull("No unexpected output while idle", outputs.poll(1, java.util.concurrent.TimeUnit.SECONDS));
                org.junit.Assert.assertEquals("Ready while idle", org.ikasan.spec.flow.Flow.RUNNING, flow.getState());
            }
        });
    }

    @FunctionalInterface
    protected interface FlowScenario {
        void verify(org.ikasan.testharness.flow.rule.IkasanFlowTestRule harness,
                    org.ikasan.spec.flow.Flow flow,
                    java.util.concurrent.BlockingQueue<String> outputs) throws Exception;
    }

    /** For routing/rejection scenarios: send inputs and assert deliveries OR deliberate absence explicitly. */
    protected final void verifyScenario(ConfigurableApplicationContext context, String flowName, String output,
            java.util.function.Function<Object, String> describeOutput,
            java.util.function.Consumer<org.ikasan.testharness.flow.rule.IkasanFlowTestRule> expectations,
            FlowScenario scenario) throws Exception {
        org.ikasan.spec.module.Module<org.ikasan.spec.flow.Flow> module = context.getBean(org.ikasan.spec.module.Module.class);
        org.ikasan.spec.flow.Flow flow = module.getFlow(flowName);
        org.junit.Assert.assertNotNull("Flow must exist: " + flowName, flow);
        java.util.concurrent.BlockingQueue<String> outputs = new java.util.concurrent.LinkedBlockingQueue<>();
        org.ikasan.spec.flow.FlowEventListener listener = new org.ikasan.spec.flow.FlowEventListener() {
            public void beforeFlowElement(String m, String f, org.ikasan.spec.flow.FlowElement e, org.ikasan.spec.flow.FlowEvent event) { }
            public void afterFlowElement(String m, String f, org.ikasan.spec.flow.FlowElement e, org.ikasan.spec.flow.FlowEvent event) {
                if (output.equals(e.getComponentName())) outputs.add(describeOutput.apply(event.getPayload()));
            }
        };
        var harness = new org.ikasan.testharness.flow.rule.IkasanFlowTestRule().withFlow(flow);
        expectations.accept(harness);
        flow.addFlowListener(listener);
        try {
            harness.startFlow();
            scenario.verify(harness, flow, outputs);
            // A rejected/filtered event can finish asynchronously without reaching the output listener.
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
            while (true) {
                try { harness.assertIsSatisfied(); break; }
                catch (AssertionError pending) {
                    if (System.nanoTime() >= deadline) throw pending;
                    Thread.sleep(50);
                }
            }
            org.junit.Assert.assertEquals("Ready after scenario", org.ikasan.spec.flow.Flow.RUNNING, flow.getState());
        } finally {
            try { harness.stopFlow(); } finally { flow.removeFlowListener(listener); }
        }
    }

}
