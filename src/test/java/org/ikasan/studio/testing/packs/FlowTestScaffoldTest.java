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
    void scheduledTimerFixtureIsSelectedByMetadataAndDisabledForCustomProviders(String version) throws Exception {
        var flow = org.ikasan.studio.core.TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerFlow(version);
        var module = org.ikasan.studio.core.TestFixtures.getMyFirstModuleIkasanModule(version, java.util.List.of(flow));
        flow.setConsumer(org.ikasan.studio.core.TestFixtures.getScheduledConsumer(version));
        flow.getConsumer().setPropertyValue("messageProvider", "");
        String parent = Files.readString(Path.of("regression-tests/migration/project/pom.xml"));
        String app = Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml"));
        var result = FlowTestScaffold.render(module, flow, parent, app);
        String code = result.files().get(result.testPath());
        assertTrue(code.contains("ScheduledEventFixture.fire(harness,"));
        assertTrue(code.contains("ScheduledEventFixture.text((JobExecutionContext) payload)"));
        assertFalse(code.contains("prepareInputBatch"));
        assertFalse(code.contains("UnsupportedOperationException"));
        assertTrue(result.files().containsKey("user-flow-tests/src/test/java/org/ikasan/studio/flowtests/support/utils/ScheduledEventFixture.java"));
        var meta = flow.getConsumer().getComponentMeta();
        flow.getConsumer().setComponentMeta(meta.toBuilder().implementingClass("example.Timer").build());
        result = FlowTestScaffold.render(module, flow, parent, app);
        assertTrue(result.files().get(result.testPath()).contains("ScheduledEventFixture.fire(harness,"));
        flow.getConsumer().setComponentMeta(meta);
        flow.getConsumer().setPropertyValue("messageProvider", "example.CustomProvider");
        result = FlowTestScaffold.render(module, flow, parent, app);
        assertFalse(result.files().get(result.testPath()).contains("ScheduledEventFixture.fire(harness,"));
        assertTrue(result.files().get(result.testPath()).contains("prepareInputBatch(context, batch)"));
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void jmsPropertiesReferenceCommonEmbeddedConsumerBroker(String version) throws Exception {
        var module = ComponentIO.validatePersistedModuleJson(ModelMigration.analyse(
                Files.readString(Path.of("src/test/resources/org/ikasan/studio/populated_module.json")), version).targetJson(), "test", false);
        var flow = module.getFlows().get(0);
        flow.setConsumer(org.ikasan.studio.core.TestFixtures.getSpringJmsConsumer(version));
        flow.getConsumer().setPropertyValue("connectionFactoryJndiPropertyProviderUrl", "vm://embedded-broker?broker.persistent=false");
        var scaffold = FlowTestScaffold.render(module, flow,
                Files.readString(Path.of("regression-tests/migration/project/pom.xml")),
                Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml")));
        var properties = new java.util.Properties();
        properties.load(new StringReader(scaffold.files().get(FlowTestScaffold.TEST_PROPERTIES_PATH)));
        String key = org.ikasan.studio.core.StudioBuildUtils.substitutePlaceholderInLowerCase(module, flow, flow.getConsumer(),
                flow.getConsumer().getProperty("connectionFactoryJndiPropertyProviderUrl").getMeta().getPropertyConfigFileLabel());
        assertNull(properties.getProperty("test.jms.broker-url"));
        assertTrue(scaffold.files().get(org.ikasan.studio.core.generator.ModuleTestWiring.PATH).contains(key));
        flow.getConsumer().setPropertyValue("connectionFactoryJndiPropertyProviderUrl", "tcp://external:61616");
        scaffold = FlowTestScaffold.render(module, flow,
                Files.readString(Path.of("regression-tests/migration/project/pom.xml")),
                Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml")));
        properties.clear();
        properties.load(new StringReader(scaffold.files().get(FlowTestScaffold.TEST_PROPERTIES_PATH)));
        assertNull(properties.getProperty("test.jms.broker-url"));
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void smtpProducerGetsLocalInboxAndResourceExpectations(String version) throws Exception {
        var module = ComponentIO.validatePersistedModuleJson(ModelMigration.analyse(
                Files.readString(Path.of("src/test/resources/org/ikasan/studio/populated_module.json")), version).targetJson(), "test", false);
        var flow = module.getFlows().get(0);
        flow.setConsumer(org.ikasan.studio.core.TestFixtures.getFtpConsumer(version));
        flow.getFlowRoute().getFlowElements().clear();
        flow.getFlowRoute().getFlowElements().add(org.ikasan.studio.core.TestFixtures.getEmailProducer(version));
        var scaffold = FlowTestScaffold.render(module, flow,
                Files.readString(Path.of("regression-tests/migration/project/pom.xml")),
                Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml")));
        String code = scaffold.files().get(scaffold.testPath());
        assertTrue(code.contains("FIRST_EXPECTED_OUTPUT_RESOURCE"));
        assertTrue(code.contains("localSmtpServer(context).assertBody(batch, expected, deliveryTimeout(context))"));
        String support = scaffold.files().get(FlowTestScaffold.SUPPORT_PATH);
        assertTrue(support.contains("LocalSmtpTestServer.start()"));
        assertTrue(scaffold.files().get(org.ikasan.studio.core.generator.ModuleTestWiring.PATH).contains("My Email Producer"));
        assertTrue(support.contains("ownedSmtp::close"));
        String pom = scaffold.files().get("user-flow-tests/pom.xml");
        assertTrue(pom.contains("<artifactId>greenmail</artifactId><version>" + "1.6.15"));
        assertTrue(pom.contains("<groupId>com.sun.mail</groupId><artifactId>jakarta.mail</artifactId><version>1.6.8</version><scope>test</scope>"));
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void debugFilterKeepsLinearPathButOrdinaryFiltersRequireReview(String version) throws Exception {
        var module = ComponentIO.validatePersistedModuleJson(ModelMigration.analyse(
                Files.readString(Path.of("src/test/resources/org/ikasan/studio/populated_module.json")), version).targetJson(), "test", false);
        var flow = module.getFlows().get(0);
        flow.setConsumer(org.ikasan.studio.core.TestFixtures.getFtpConsumer(version));
        var debug = org.ikasan.studio.core.TestFixtures.getDebugTransition(version);
        debug.setPropertyValue("componentName", "Input debug");
        flow.getFlowRoute().getFlowElements().add(0, debug);
        String rootPom = Files.readString(Path.of("regression-tests/migration/project/pom.xml"));
        String generatedPom = Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml"));
        var scaffold = FlowTestScaffold.render(module, flow, rootPom, generatedPom);
        String code = scaffold.files().get(scaffold.testPath());
        assertTrue(code.contains(".scheduledConsumer("));
        assertTrue(scaffold.files().get(FlowTestScaffold.SUPPORT_PATH).contains("ftp.configureConsumer(module.getFlow("));
        assertTrue(scaffold.files().get(FlowTestScaffold.SUPPORT_PATH).contains("No output observed after producer"));
        assertTrue(scaffold.files().get(FlowTestScaffold.TEST_PROPERTIES_PATH).contains("test.ftp.consumer.min-age-seconds=0"));
        assertTrue(code.contains("FtpInputFixture.copyResource(localFtpDirectory(context)"));
        assertTrue(code.contains("batch == 1 ? FIRST_BATCH_INPUT_FILENAME : SECOND_BATCH_INPUT_FILENAME"));
        assertFalse(code.contains("test.ftp.input.filename.batch"));
        assertFalse(code.contains("Prepare input batch "));
        assertTrue(scaffold.files().containsKey("user-flow-tests/src/test/java/org/ikasan/studio/flowtests/support/utils/FtpInputFixture.java"));
        assertTrue(code.contains(".filter(\"Input debug\")"));
        assertTrue(code.indexOf(".filter(\"Input debug\")") < code.indexOf(".converter("));
        assertTrue(code.contains(".repeat(2)"));
        assertFalse(code.contains("Define the expected component path for both input batches"));
        flow.getFlowRoute().getFlowElements().set(0, org.ikasan.studio.core.TestFixtures.getMessageFilter(version));
        var filtered = FlowTestScaffold.render(module, flow, rootPom, generatedPom);
        assertTrue(filtered.files().get(filtered.testPath()).contains("Define the expected component path for both input batches"));
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void fileProducersOfferPhysicalDeliveryChecksAndNamedInputs(String version) throws Exception {
        var module = ComponentIO.validatePersistedModuleJson(ModelMigration.analyse(
                Files.readString(Path.of("src/test/resources/org/ikasan/studio/populated_module.json")), version).targetJson(), "test", false);
        var flow = module.getFlows().get(0);
        flow.getFlowRoute().getFlowElements().clear();
        flow.getFlowRoute().getFlowElements().add(org.ikasan.studio.core.TestFixtures.getFtpProducer(version));
        var scaffold = FlowTestScaffold.render(module, flow,
                Files.readString(Path.of("regression-tests/migration/project/pom.xml")),
                Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml")));
        String test = scaffold.files().get(scaffold.testPath());
        assertTrue(test.contains("private static final String FIRST_BATCH_INPUT"));
        assertTrue(test.contains("private static final boolean DECODE_OUTPUT_CONTENT_AS_TEXT = true"));
        assertTrue(test.contains("return formatOutputText(payload, DECODE_OUTPUT_CONTENT_AS_TEXT)"));
        assertTrue(scaffold.files().containsKey("user-flow-tests/src/test/java/org/ikasan/studio/flowtests/support/utils/OutputTextSupport.java"));
        assertTrue(test.contains("private static final String SECOND_BATCH_INPUT"));
        assertTrue(test.contains("batch == 1 ? FIRST_BATCH_INPUT : SECOND_BATCH_INPUT"));
        assertTrue(test.contains("assertDeliveredFileResources(localFtpDirectory(context)"));
        assertTrue(test.contains("FIRST_EXPECTED_OUTPUT_RESOURCE = \"/\" + noSpaces(FLOW_NAME)"));
        assertTrue(test.contains("noSpaces(PRODUCER_NAME) + \"/first.txt\""));
        assertTrue(test.contains("noSpaces(PRODUCER_NAME) + \"/second.txt\""));
        assertTrue(test.contains("readTestResource(SECOND_EXPECTED_OUTPUT_RESOURCE)"));
        assertFalse(test.contains("FIRST_EXPECTED_OUTPUT = readTestResource"));
        assertTrue(scaffold.files().get(FlowTestScaffold.SUPPORT_PATH).contains("context.getBean(LocalFtpTestServer.class).root()"));
        assertTrue(scaffold.files().containsKey("user-flow-tests/src/test/java/org/ikasan/studio/flowtests/support/utils/FileDeliveryAssertions.java"));
        flow.getFlowRoute().getFlowElements().clear();
        flow.getFlowRoute().getFlowElements().add(org.ikasan.studio.core.TestFixtures.getSftpProducer(version));
        var sftp = FlowTestScaffold.render(module, flow,
                Files.readString(Path.of("regression-tests/migration/project/pom.xml")),
                Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml")));
        assertTrue(sftp.files().get(sftp.testPath()).contains("assertDeliveredFileResources(localSftpDirectory(context, FLOW_NAME, PRODUCER_NAME)"));
    }

    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void sftpConsumerUsesResourcesAndOwnedServer(String version) throws Exception {
        var module = ComponentIO.validatePersistedModuleJson(ModelMigration.analyse(
                Files.readString(Path.of("src/test/resources/org/ikasan/studio/populated_module.json")), version).targetJson(), "test", false);
        var flow = module.getFlows().get(0);
        flow.setConsumer(org.ikasan.studio.core.TestFixtures.getSftpConsumer(version));
        var scaffold = FlowTestScaffold.render(module, flow,
                Files.readString(Path.of("regression-tests/migration/project/pom.xml")),
                Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml")));
        String code = scaffold.files().get(scaffold.testPath());
        assertTrue(code.contains("FIRST_BATCH_INPUT_FILENAME"));
        assertTrue(code.contains("FileInputFixture.copyResource(localSftpDirectory(context, FLOW_NAME, CONSUMER_NAME)"));
        assertFalse(code.contains("throw new UnsupportedOperationException(\"Prepare input batch"));
        String support = scaffold.files().get(FlowTestScaffold.SUPPORT_PATH);
        assertTrue(support.contains("sftp.configure(module.getFlow("));
        assertTrue(support.contains("registerDisposableBean(\"studioLocalSftpTestServer\", ownedSftp::close)"));
        assertTrue(scaffold.files().get("user-flow-tests/pom.xml").contains("sshd-sftp"));
        assertEquals("first expected payload", scaffold.files().get("user-flow-tests/src/test/resources/"
                + flow.getIdentity().replace(' ', '_') + "/" + flow.getConsumer().getIdentity().replace(' ', '_') + "/first.txt"));
    }

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
        assertTrue(scaffold.files().get(org.ikasan.studio.core.generator.ModuleTestWiring.PATH).contains("myflow1.ftp.consumer.remote-host"));
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
        assertTrue(test.contains("runTest(TEST_REVIEWED, PRODUCER_NAME, FIRST_EXPECTED_OUTPUT, SECOND_EXPECTED_OUTPUT)"));
        assertTrue(test.contains("protected void supplyInput("));
        assertTrue(test.contains("FIRST_BATCH_INPUT = readTestResource(\"/input/first.txt\")"));
        assertTrue(test.contains("SECOND_BATCH_INPUT = readTestResource(\"/input/second.txt\")"));
        assertTrue(support.contains("protected static String readTestResource(String resourcePath)"));
        assertTrue(support.contains("new String(input.readAllBytes(), StandardCharsets.UTF_8)"));

        assertFalse(test.contains("this::"));
        assertFalse(test.contains(" -> "));
        String properties = scaffold.files().get(FlowTestScaffold.TEST_PROPERTIES_PATH);
        assertTrue(properties.contains("${TEST_PASSWORD}"));
        assertTrue(properties.contains("test.delivery.timeout-seconds=10"));
        assertTrue(properties.lines().allMatch(line -> line.isBlank() || line.startsWith("#")
                || line.equals("test.delivery.timeout-seconds=10") || line.equals("test.smtp.enabled=false") || line.equals("test.ftp.consumer.min-age-seconds=0") || line.startsWith("test.sftp.")));
        assertTrue(support.contains("getResourceAsStream(\"/module-test.properties\")"));
        assertTrue(support.contains("StandardCharsets.UTF_8"));
        assertTrue(support.contains("if (input == null) throw"));
        assertFalse(test.contains("SpringApplication.run"));
        assertTrue(support.contains("flowIndex++"));
        assertTrue(scaffold.files().get(org.ikasan.studio.core.generator.ModuleTestWiring.PATH).contains("MyFlow1"));
        assertTrue(support.contains("UUID.randomUUID()"));
        assertTrue(support.contains("properties.putAll(flowProperties)"));
        assertTrue(support.contains("protected Map<String, String> flowTestProperties() {\n        return new LinkedHashMap<>();"));
        assertFalse(test.contains("Map<String, String> flowTestProperties()"));
        var local = org.ikasan.studio.core.TestFixtures.getLocalFileConsumer(version);
        module.getFlows().get(0).setConsumer(local);
        var localScaffold = FlowTestScaffold.render(module, module.getFlows().get(0), parent, app);
        String localTest = localScaffold.files().get(localScaffold.testPath());
        String fixtureDirectory = "user-flow-tests/src/test/resources/"
                + module.getFlows().get(0).getIdentity().replace(' ', '_') + "/" + local.getIdentity().replace(' ', '_') + "/";
        assertEquals("first expected payload", localScaffold.files().get(fixtureDirectory + "first.txt"));
        assertEquals("second expected payload", localScaffold.files().get(fixtureDirectory + "second.txt"));
        assertTrue(localTest.contains("TemporaryFolder.builder().assureDeletion().build()"));
        assertTrue(localTest.contains("protected Map<String, String> flowTestProperties()"));
        assertTrue(localTest.contains("super.flowTestProperties()"));
        assertTrue(localTest.contains("FileInputFixture.prepareLocalBatch("));
        assertTrue(localTest.contains("FileInputFixture.localFilenames("));
        assertTrue(localTest.contains("noSpaces(CONSUMER_NAME)"));
        assertFalse(localTest.contains("FIRST_BATCH_INPUT ="));
        assertTrue(localScaffold.files().containsKey("user-flow-tests/src/test/java/org/ikasan/studio/flowtests/support/utils/FileInputFixture.java"));
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
        assertTrue(support.contains("awaitOutputText(outputs, deliveryTimeoutSeconds)"));
        assertTrue(localTest.contains("TEST_REVIEWED = false"));
        assertFalse(localTest.contains("@Test(timeout ="));
        assertTrue(support.contains("test.delivery.timeout-seconds"));
        assertTrue(support.contains("Timeout.seconds(60L + 10L * seconds)"));
        assertTrue(support.contains("TemporaryFolder.builder().assureDeletion().build()"));
        assertTrue(support.contains("RuleChain.outerRule(ftpTestDirectory)"));
        assertTrue(support.contains("LocalFtpTestServer.start(properties, ftpTestDirectory.newFolder().toPath())"));
        assertTrue(support.contains("Duration.ofSeconds(deliveryTimeoutSeconds)"));
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
        assertTrue(test.contains("runTest(TEST_REVIEWED, PRODUCER_NAME, FIRST_EXPECTED_OUTPUT, SECOND_EXPECTED_OUTPUT)"));
        assertFalse(test.contains("this::"));
        assertFalse(test.contains(" -> "));
        assertTrue(test.contains("ModuleJmsTestConfig.class"));
        assertFalse(test.contains("removeAllMessages"));
        String helper = scaffold.files().get("user-flow-tests/src/test/java/org/ikasan/studio/flowtests/support/utils/JmsFlowTestSupport.java");
        assertTrue(helper.contains((version.equals("V3.3.9") ? "javax" : "jakarta") + ".jms.Connection;"));
        assertTrue(helper.contains("consumer.receive(deliveryTimeoutMillis)"));
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
        assertTrue(test.contains("runObservationTest(TEST_REVIEWED"));
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
    void sampleSubmissionDefaultsAreDrivenByMetadata(String version) throws Exception {
        var flow = org.ikasan.studio.core.TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerFlow(version);
        flow.setConsumer(org.ikasan.studio.core.TestFixtures.getGenericConsumer(version));
        var module = org.ikasan.studio.core.TestFixtures.getMyFirstModuleIkasanModule(version, java.util.List.of(flow));
        String parent = Files.readString(Path.of("regression-tests/migration/project/pom.xml"));
        String app = Files.readString(Path.of("regression-tests/migration/project/generated/pom.xml"));
        var result = FlowTestScaffold.render(module, flow, parent, app);
        String test = result.files().get(result.testPath());
        assertTrue(test.contains("fixture-input-enabled=true"));
        assertTrue(test.contains("                .submitNow(batch == 1 ? FIRST_BATCH_INPUT : SECOND_BATCH_INPUT)"));
        assertFalse(test.contains("throw new UnsupportedOperationException(\"Configure fixture input"));
        assertTrue(result.files().get(org.ikasan.studio.core.generator.ModuleTestWiring.PATH).contains("sampleConsumerClasses"));
        flow.getConsumer().setComponentMeta(flow.getConsumer().getComponentMeta().toBuilder().flowTestInputMode(null).build());
        var ordinary = FlowTestScaffold.render(module, flow, parent, app);
        assertFalse(ordinary.files().get(ordinary.testPath()).contains("submitNow"));
    }

}
