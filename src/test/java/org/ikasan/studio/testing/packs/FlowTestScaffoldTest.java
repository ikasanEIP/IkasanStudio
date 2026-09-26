package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.generator.FlowTestScaffold;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.migration.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;

class FlowTestScaffoldTest {
    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void ftpFixtureUsesPackPropertyLabels(String version) throws Exception {
        var module = ComponentIO.validatePersistedModuleJson(ModelMigration.analyse(
                Files.readString(Path.of("src/test/resources/org/ikasan/studio/populated_module.json")), version).targetJson(), "test", false);
        var flow = module.getFlows().get(0);
        flow.setConsumer(org.ikasan.studio.core.TestFixtures.getFtpConsumer(version));
        flow.getConsumer().setPropertyValue("ftps", false);
        var scaffold = FlowTestScaffold.render(module, flow,
                Files.readString(Path.of("regression-tests/migration/project/pom.xml")),
                Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml")));
        String support = scaffold.files().get(FlowTestScaffold.SUPPORT_PATH);
        assertTrue(support.contains("ftp.configure(properties"));
        assertTrue(support.contains("myflow1.ftp.consumer.remote-host"));
        assertTrue(scaffold.files().get(FlowTestScaffold.TEST_PROPERTIES_PATH).contains("# test.ftp.enabled=true"));
        assertFalse(scaffold.files().get(FlowTestScaffold.TEST_PROPERTIES_PATH).contains("secret"));
        assertTrue(scaffold.files().get("user-flow-tests/pom.xml").contains("ftpserver-core"));
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void scaffoldInheritsVersionAndMigrationPreservesModule(String version) throws Exception {
        String source = Files.readString(Path.of("src/test/resources/org/ikasan/studio/populated_module.json"));
        var plan = ModelMigration.analyse(source, version);
        var module = ComponentIO.validatePersistedModuleJson(plan.targetJson(), "test", false);
        String parent = Files.readString(Path.of("regression-tests/migration/project/pom.xml"));
        String app = Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml"));
        var scaffold = FlowTestScaffold.render(module, module.getFlows().get(0), parent, app);
        var pom = new MavenXpp3Reader().read(new StringReader(scaffold.files().get("user-flow-tests/pom.xml")));
        assertEquals("${version.ikasan}", pom.getDependencies().stream().filter(d -> d.getArtifactId().equals("ikasan-test")).findFirst().orElseThrow().getVersion());
        assertEquals(1, new MavenXpp3Reader().read(new StringReader(scaffold.rootPom())).getModules().stream().filter("user-flow-tests"::equals).count());
        assertEquals(scaffold.rootPom(), FlowTestScaffold.render(module, module.getFlows().get(0), scaffold.rootPom(), app).rootPom());
        var migrated = MigrationArtifacts.render(ModelMigration.analyse(plan.targetJson(), version.equals("V3.3.9") ? "V4.1.6" : "V3.3.9"), scaffold.rootPom());
        assertTrue(new MavenXpp3Reader().read(new StringReader(migrated.get("pom.xml"))).getModules().contains("user-flow-tests"));
        assertTrue(migrated.keySet().stream().noneMatch(p -> p.startsWith("user-flow-tests/")));
        String test = scaffold.files().get(scaffold.testPath());

        String support = scaffold.files().get(FlowTestScaffold.SUPPORT_PATH);
        assertTrue(test.contains("extends ModuleFlowTestSupport"));
        assertTrue(support.indexOf("assertTrue(\"Complete") < support.indexOf("try (ConfigurableApplicationContext"));
        assertTrue(test.contains("protected String getFlowName()"));
        assertFalse(test.contains("try (ConfigurableApplicationContext"));
        assertTrue(test.contains("runTest(CONFIGURED, PRODUCER_NAME, FIRST_EXPECTED_OUTPUT, SECOND_EXPECTED_OUTPUT)"));
        assertTrue(test.contains("protected void supplyInput("));
        assertFalse(test.contains("this::"));
        assertFalse(test.contains(" -> "));
        String properties = scaffold.files().get(FlowTestScaffold.TEST_PROPERTIES_PATH);
        assertTrue(properties.contains("${TEST_PASSWORD}"));
        assertTrue(properties.lines().allMatch(line -> line.isBlank() || line.startsWith("#")));
        assertTrue(support.contains("getResourceAsStream(\"/module-test.properties\")"));
        assertTrue(support.contains("StandardCharsets.UTF_8"));
        assertTrue(support.contains("if (input == null) throw"));
        assertFalse(test.contains("SpringApplication.run"));
        assertTrue(support.contains("flowStartupTypes[0]\", \"MyFlow1,MANUAL"));
        assertTrue(support.contains("UUID.randomUUID()"));
        assertTrue(support.contains("properties.putAll(flowProperties)"));
        assertTrue(support.contains("protected Map<String, String> flowTestProperties() {\n        return new LinkedHashMap<>();"));
        assertFalse(test.contains("Map<String, String> flowTestProperties()"));
        var local = org.ikasan.studio.core.TestFixtures.getLocalFileConsumer(version);
        module.getFlows().get(0).setConsumer(local);
        var localScaffold = FlowTestScaffold.render(module, module.getFlows().get(0), parent, app);
        String localTest = localScaffold.files().get(localScaffold.testPath());
        assertTrue(localTest.contains("new TemporaryFolder()"));
        assertTrue(localTest.contains("protected Map<String, String> flowTestProperties()"));
        assertTrue(localTest.contains("super.flowTestProperties()"));
        assertTrue(localTest.contains("Files.writeString"));
        assertTrue(support.contains("harness.assertIsSatisfied()"));
        assertTrue(localTest.contains(".repeat(2)"));
        assertTrue(localTest.contains(".scheduledConsumer(\"" + local.getIdentity() + "\")"));
        assertTrue(localTest.contains("properties.put(\"myflow1.file.consumer.filenames\""));
        module.getFlows().get(0).getFlowRoute().getChildRoutes().add(
                org.ikasan.studio.core.model.ikasan.instance.FlowRoute.flowRouteBuilder()
                        .flow(module.getFlows().get(0)).routeName("Branch").build());
        var branched = FlowTestScaffold.render(module, module.getFlows().get(0), parent, app);
        String branchedTest = branched.files().get(branched.testPath());
        assertFalse(branchedTest.contains(".repeat(2)"));
        assertTrue(branchedTest.contains("Define the expected component path for both input batches"));
        assertTrue(localTest.contains("int batch) throws Exception"));
        assertTrue(localTest.contains("harness.fireScheduledConsumer()"));
        assertTrue(localTest.contains("FIRST_EXPECTED_OUTPUT, SECOND_EXPECTED_OUTPUT"));
        assertTrue(support.contains("outputs.poll(10, TimeUnit.SECONDS)"));
        assertTrue(localTest.contains("CONFIGURED = false"));
        for (int task = 1; task <= 5; task++) assertTrue(localTest.contains("// TODO " + task + ":"));
    }
    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void jmsQueueScaffoldUsesRealInputAndReceiverChecks(String version) throws Exception {
        var module = org.ikasan.studio.core.TestFixtures.getMyFirstModuleIkasanModule(version,
                java.util.Collections.singletonList(org.ikasan.studio.core.TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerFlow(version)));
        var flow = module.getFlows().get(0);
        var consumer = org.ikasan.studio.core.model.ikasan.instance.FlowElement.flowElementBuilder()
                .componentMeta(org.ikasan.studio.core.metapack.ComponentLibrary.getIkasanComponentByKeyMandatory(version, "Spring JMS Consumer"))
                .componentName("Receive").build();
        consumer.setPropertyValue("destinationJndiName", "test.input");
        consumer.setPropertyValue("pubSubDomain", false);
        flow.setConsumer(consumer);
        var producer = org.ikasan.studio.core.model.ikasan.instance.FlowElement.flowElementBuilder()
                .componentMeta(org.ikasan.studio.core.metapack.ComponentLibrary.getIkasanComponentByKeyMandatory(version, "Spring JMS Producer"))
                .componentName("Send").build();
        producer.setPropertyValue("destinationJndiName", "test.output");
        producer.setPropertyValue("pubSubDomain", false);
        flow.getFlowRoute().getFlowElements().clear();
        flow.getFlowRoute().getFlowElements().add(producer);
        String parent = Files.readString(Path.of("regression-tests/migration/project/pom.xml"));
        String app = Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml"));
        var scaffold = FlowTestScaffold.render(module, flow, parent, app);
        String test = scaffold.files().get(scaffold.testPath());

        assertTrue(test.contains("jms.sendText"));
        assertTrue(test.contains("jms.assertText"));
        assertTrue(test.contains("protected void verifyReceivedOutput("));
        assertTrue(test.contains("runTest(CONFIGURED, PRODUCER_NAME, FIRST_EXPECTED_OUTPUT, SECOND_EXPECTED_OUTPUT)"));
        assertFalse(test.contains("this::"));
        assertFalse(test.contains(" -> "));
        assertTrue(test.contains("ModuleJmsTestConfig.class"));
        assertFalse(test.contains("removeAllMessages"));
        String helper = scaffold.files().get("user-flow-tests/src/test/java/org/ikasan/studio/flowtests/JmsFlowTestSupport.java");
        assertTrue(helper.contains((version.equals("V3.3.9") ? "javax" : "jakarta") + ".jms.Connection;"));
        assertTrue(helper.contains("consumer.receive(10000)"));
        consumer.setPropertyValue("pubSubDomain", true);
        var topic = FlowTestScaffold.render(module, flow, parent, app);
        assertFalse(topic.files().get(topic.testPath()).contains("jms.sendText"));
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void observationScaffoldIsMetadataDrivenAndConservative(String version) throws Exception {
        var flow = org.ikasan.studio.core.TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerFlow(version);
        var module = org.ikasan.studio.core.TestFixtures.getMyFirstModuleIkasanModule(version, java.util.List.of(flow));
        var converter = flow.getFlowRoute().getFlowElements().remove(0);
        String parent = Files.readString(Path.of("regression-tests/migration/project/pom.xml"));
        String app = Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml"));
        var result = FlowTestScaffold.render(module, flow, parent, app);
        String test = result.files().get(result.testPath());
        assertTrue(test.contains("testGeneratedEventsReachProducerAndFlowKeepsRunning"));
        assertTrue(test.contains("runObservationTest(CONFIGURED"));
        assertTrue(test.contains("List.of(\"Test Message 1\", \"Test Message 2\", \"Test Message 3\")"));
        assertFalse(test.contains("sendInput"));
        assertFalse(test.contains("FIRST_EXPECTED_OUTPUT"));
        assertFalse(test.contains("repeat(2)"));
        String support = result.files().get(FlowTestScaffold.SUPPORT_PATH);
        assertTrue(support.contains("awaitObservedEvent(flow, delivered, delivered.get())"));
        assertTrue(support.contains("AutoCloseable removal = () -> flow.removeFlowListener(listener)"));
        assertTrue(support.contains("if (index <= expected.size())"));
        assertTrue(support.contains("Initial producer payload"));
        assertTrue(support.contains("Stopped during test teardown"));

        // Names and implementation classes do not select the test strategy.
        var meta = flow.getConsumer().getComponentMeta();
        flow.getConsumer().setComponentMeta(meta.toBuilder().implementingClass("example.OtherSource").build());
        var renamed = FlowTestScaffold.render(module, flow, parent, app);
        assertTrue(renamed.files().get(renamed.testPath()).contains("runObservationTest"));
        flow.getConsumer().setComponentMeta(meta.toBuilder().flowTestExpectedInitialOutputs(java.util.List.of("custom\"sample", "next")).build());
        var changedSamples = FlowTestScaffold.render(module, flow, parent, app);
        assertTrue(changedSamples.files().get(changedSamples.testPath()).contains("custom\\\"sample"));
        assertFalse(changedSamples.files().get(changedSamples.testPath()).contains("Test Message 1"));
        flow.getConsumer().setComponentMeta(meta.toBuilder().flowTestExpectedInitialOutputs(java.util.List.of()).build());
        var noSamples = FlowTestScaffold.render(module, flow, parent, app);
        assertFalse(noSamples.files().get(noSamples.testPath()).contains("List.of("));
        flow.getConsumer().setComponentMeta(meta.toBuilder().flowTestInputMode(null).build());
        var noHint = FlowTestScaffold.render(module, flow, parent, app);
        assertFalse(noHint.files().get(noHint.testPath()).contains("runObservationTest"));
        flow.getConsumer().setComponentMeta(meta);

        flow.getConsumer().setPropertyValue("endpointEventProvider", "CustomProvider");
        var custom = FlowTestScaffold.render(module, flow, parent, app);
        assertFalse(custom.files().get(custom.testPath()).contains("runObservationTest"));
        flow.setConsumer(org.ikasan.studio.core.TestFixtures.getEventGeneratingConsumer(version));
        flow.getFlowRoute().getFlowElements().add(0, converter);
        var transformed = FlowTestScaffold.render(module, flow, parent, app);
        assertTrue(transformed.files().get(transformed.testPath()).contains("FIRST_EXPECTED_OUTPUT"));
        assertTrue(transformed.files().get(transformed.testPath()).contains("testFirstAndLaterDeliveryWithoutRestart"));
        flow.getFlowRoute().getFlowElements().remove(0);
        var sink = flow.getFlowRoute().getFlowElements().get(0);
        sink.setComponentMeta(sink.getComponentMeta().toBuilder().flowTestObservationOnly(false).build());
        var external = FlowTestScaffold.render(module, flow, parent, app);
        assertFalse(external.files().get(external.testPath()).contains("runObservationTest"));
    }



    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void sampleSubmissionGuidanceIsOptInAndDoesNotAssumeExistingCodeHasTheApi(String version) throws Exception {
        var flow = org.ikasan.studio.core.TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerFlow(version);
        flow.setConsumer(org.ikasan.studio.core.TestFixtures.getGenericConsumer(version));
        var module = org.ikasan.studio.core.TestFixtures.getMyFirstModuleIkasanModule(version, java.util.List.of(flow));
        String parent = Files.readString(Path.of("regression-tests/migration/project/pom.xml"));
        String app = Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml"));
        var result = FlowTestScaffold.render(module, flow, parent, app);
        String test = result.files().get(result.testPath());
        assertTrue(test.contains("fixture-input-enabled=true"));
        assertTrue(test.contains("//        .submitNow("));
        assertTrue(test.contains("throw new UnsupportedOperationException(\"Configure fixture input"));
        flow.getConsumer().setComponentMeta(flow.getConsumer().getComponentMeta().toBuilder().flowTestInputMode(null).build());
        var ordinary = FlowTestScaffold.render(module, flow, parent, app);
        assertFalse(ordinary.files().get(ordinary.testPath()).contains("submitNow"));
    }

}
