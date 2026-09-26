package org.ikasan.studio.core.generator;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Renders developer-owned tests once; normal generation never owns these files. */
public final class FlowTestScaffold {
    public static final String TEST_PROPERTIES_PATH = "user-flow-tests/src/test/resources/module-test.properties";
    public static final String SUPPORT_PATH = "user-flow-tests/src/test/java/org/ikasan/studio/flowtests/ModuleFlowTestSupport.java";
    private FlowTestScaffold() { }
    public record Scaffold(String rootPom, String testPath, Map<String, String> files) { }

    public static Scaffold render(Module module, Flow flow, String rootPom, String applicationPom) throws Exception {
        if (flow.getConsumer() == null) throw new IllegalArgumentException("Add a consumer before generating a flow test.");
        Model parent = new MavenXpp3Reader().read(new StringReader(rootPom));
        Model application = new MavenXpp3Reader().read(new StringReader(applicationPom));
        if (!"pom".equals(parent.getPackaging()) || parent.getGroupId() == null || parent.getVersion() == null
                || parent.getProperties().getProperty("version.ikasan") == null) {
            throw new IllegalArgumentException("Expected a Studio parent POM with groupId, version and version.ikasan.");
        }
        String className = flow.getJavaClassName() + "FlowTest";
        if (!className.matches("[A-Za-z_$][A-Za-z0-9_$]*")) throw new IllegalArgumentException("Invalid flow Java class name.");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("className", className);
        values.put("flowName", flow.getIdentity());
        java.util.Set<String> propertyKeys = new java.util.TreeSet<>();
        for (var moduleFlow : module.getFlows()) for (var component : moduleFlow.getFlowElementsNoExternalEndPoints()) {
            for (var property : component.getComponentProperties().values()) {
                String label = property.getMeta().getPropertyConfigFileLabel();
                if (label != null && !label.isBlank() && !property.valueNotSet()) {
                    propertyKeys.add(org.ikasan.studio.core.StudioBuildUtils.substitutePlaceholderInLowerCase(module, moduleFlow, component, label));
                }
            }
        }
        java.util.Set<String> sampleConsumerClasses = new java.util.TreeSet<>();
        for (var moduleFlow : module.getFlows()) {
            var consumer = moduleFlow.getConsumer();
            if (consumer == null || !"sample-submission".equals(consumer.getComponentMeta().getFlowTestInputMode())) continue;
            String implementationClass = consumer.getPropertyValueAsString("userImplementedClassName");
            if (implementationClass != null && !implementationClass.isBlank()) {
                sampleConsumerClasses.add(implementationClass.contains(".") ? implementationClass
                        : GeneratorUtils.getUserImplementedClassesPackageName(module, moduleFlow) + "." + implementationClass);
            }
        }
        values.put("sampleConsumerClasses", sampleConsumerClasses);
        // The capability flag defines the connection contract; labels remain pack-owned.
        java.util.List<Map<String, String>> ftpEndpoints = new java.util.ArrayList<>();
        for (var moduleFlow : module.getFlows()) for (var component : moduleFlow.getFlowElementsNoExternalEndPoints()) {
            if (!component.getComponentMeta().supportsTestFtpServer()) continue;
            Map<String, String> endpoint = new LinkedHashMap<>();
            endpoint.put("name", moduleFlow.getIdentity() + " / " + component.getIdentity());
            endpoint.put("secure", component.getPropertyValueAsString("ftps"));
            for (String name : java.util.List.of("remoteHost", "remotePort", "username", "password",
                    component.getComponentMeta().isProducer() ? "outputDirectory" : "sourceDirectory")) {
                var property = component.getProperty(name);
                String label = property == null ? null : property.getMeta().getPropertyConfigFileLabel();
                endpoint.put(name.endsWith("Directory") ? "directory" : name,
                        label == null || label.isBlank() || property.valueNotSet() ? "" :
                        org.ikasan.studio.core.StudioBuildUtils.substitutePlaceholderInLowerCase(module, moduleFlow, component, label));
            }
            ftpEndpoints.add(endpoint);
        }
        values.put("ftpEndpoints", ftpEndpoints);
        values.put("modulePropertyKeys", propertyKeys);
        values.put("flowNames", module.getFlows().stream().map(Flow::getIdentity).toList());
        values.put("consumerName", flow.getConsumer().getIdentity());
        values.put("localFile", flow.getConsumer().getComponentMeta().isLocalFileConsumer());
        var filenameProperty = flow.getConsumer().getProperty("filenames");
        boolean isolatedFiles = flow.getConsumer().getComponentMeta().isLocalFileConsumer()
                && filenameProperty != null && !filenameProperty.valueNotSet()
                && filenameProperty.getMeta().getPropertyConfigFileLabel() != null;
        values.put("isolatedFiles", isolatedFiles);
        values.put("filenameKey", isolatedFiles ? org.ikasan.studio.core.StudioBuildUtils.substitutePlaceholderInLowerCase(
                module, flow, flow.getConsumer(), filenameProperty.getMeta().getPropertyConfigFileLabel()) : "");
        Map<String, String> methods = Map.of("Converter", "converter", "Translator", "translator", "Broker", "broker", "Producer", "producer");
        java.util.List<Map<String, String>> executionPath = new java.util.ArrayList<>();
        boolean automaticPath = flow.getFlowRoute().getChildRoutes().isEmpty() && flow.getExceptionResolver() == null;
        for (var element : flow.getFlowRoute().getFlowElements()) {
            String type = element.getComponentMeta().getComponentType();
            String method = type == null ? null : methods.get(type.substring(type.lastIndexOf('.') + 1));
            if (method == null) automaticPath = false;
            else executionPath.add(Map.of("method", method, "name", element.getIdentity()));
        }
        automaticPath &= !executionPath.isEmpty() && "producer".equals(executionPath.get(executionPath.size() - 1).get("method"));
        var inputMeta = flow.getConsumer().getComponentMeta();
        boolean observationOnly = automaticPath && flow.getFlowRoute().getFlowElements().size() == 1
                && "self-generating".equals(inputMeta.getFlowTestInputMode())
                && inputMeta.getFlowTestInputModeInvalidatedByProperties().stream().allMatch(name ->
                    flow.getConsumer().getProperty(name) == null || flow.getConsumer().getProperty(name).valueNotSet())
                && flow.getFlowRoute().getFlowElements().get(0).getComponentMeta().isFlowTestObservationOnly();
        String implementation = flow.getConsumer().getPropertyValueAsString("userImplementedClassName");
        values.put("sampleSubmission", "sample-submission".equals(inputMeta.getFlowTestInputMode())
                && implementation != null && !implementation.isBlank());
        values.put("sampleConsumerClass", implementation == null ? "" :
                (implementation.contains(".") ? implementation : GeneratorUtils.getUserImplementedClassesPackageName(module, flow) + "." + implementation));
        values.put("expectedInitialOutputs", inputMeta.getFlowTestExpectedInitialOutputs());
        values.put("automaticPath", automaticPath);
        values.put("expectedPath", executionPath);
        values.put("componentNames", flow.getFlowElementsNoExternalEndPoints().stream().map(FlowElement::getIdentity).toList());
        var destination = flow.getConsumer().getProperty("destinationJndiName");
        boolean jmsConsumer = "org.ikasan.component.endpoint.jms.spring.consumer.JmsContainerConsumer".equals(
                flow.getConsumer().getComponentMeta().getImplementingClass())
                && !"true".equalsIgnoreCase(flow.getConsumer().getPropertyValueAsString("pubSubDomain"))
                && destination != null && !destination.valueNotSet()
                && destination.getMeta().getPropertyConfigFileLabel() != null;
        var jmsOutputs = flow.getFlowElementsNoExternalEndPoints().stream().filter(e -> e.getComponentMeta().isProducer()).toList();
        values.put("fileDelivery", jmsOutputs.size() == 1 && jmsOutputs.get(0).getComponentMeta().isFlowTestFileDelivery());
        values.put("ftpFileDelivery", jmsOutputs.size() == 1 && jmsOutputs.get(0).getComponentMeta().supportsTestFtpServer());
        String jmsOutputKey = "";
        if (jmsOutputs.size() == 1) {
            var output = jmsOutputs.get(0);
            var outputDestination = output.getProperty("destinationJndiName");
            if ("org.ikasan.component.endpoint.jms.spring.producer.ArjunaJmsTemplateProducer".equals(output.getComponentMeta().getImplementingClass())
                    && !"true".equalsIgnoreCase(output.getPropertyValueAsString("pubSubDomain"))
                    && outputDestination != null && !outputDestination.valueNotSet()
                    && outputDestination.getMeta().getPropertyConfigFileLabel() != null) {
                jmsOutputKey = org.ikasan.studio.core.StudioBuildUtils.substitutePlaceholderInLowerCase(
                        module, flow, output, outputDestination.getMeta().getPropertyConfigFileLabel());
            }
        }
        values.put("jmsOutputKey", jmsOutputKey);
        values.put("jmsConsumer", jmsConsumer);
        values.put("jmsInputKey", jmsConsumer ? org.ikasan.studio.core.StudioBuildUtils.substitutePlaceholderInLowerCase(
                module, flow, flow.getConsumer(), destination.getMeta().getPropertyConfigFileLabel()) : "");
        values.put("scheduled", flow.getConsumer().getComponentMeta().isTimeEventConsumer());
        values.put("producers", flow.getFlowElementsNoExternalEndPoints().stream()
                .filter(e -> e.getComponentMeta().isProducer()).map(FlowElement::getIdentity).toList());
        values.put("groupId", parent.getGroupId());
        values.put("artifactId", parent.getArtifactId());
        values.put("version", parent.getVersion());
        values.put("applicationGroupId", application.getGroupId() == null ? parent.getGroupId() : application.getGroupId());
        values.put("applicationArtifactId", application.getArtifactId());
        values.put("applicationVersion", application.getVersion() == null ? parent.getVersion() : application.getVersion());
        String path = "user-flow-tests/src/test/java/org/ikasan/studio/flowtests/" + className + ".java";
        Map<String, String> files = new LinkedHashMap<>();
        if (jmsConsumer) files.put("user-flow-tests/src/test/java/org/ikasan/studio/flowtests/ModuleJmsTestConfig.java",
                FreemarkerUtils.generateFromTemplate(module.getMetaVersion(), "moduleJmsTestConfigTemplate_en.ftl", values));
        if (jmsConsumer) files.put("user-flow-tests/src/test/java/org/ikasan/studio/flowtests/JmsFlowTestSupport.java",
                FreemarkerUtils.generateFromTemplate(module.getMetaVersion(), "jmsFlowTestSupportTemplate_en.ftl", values));
        files.put(TEST_PROPERTIES_PATH, FreemarkerUtils.generateFromTemplate(module.getMetaVersion(), "moduleFlowTestPropertiesTemplate_en.ftl", values));
        files.put("user-flow-tests/src/test/java/org/ikasan/studio/flowtests/LocalFtpTestServer.java",
                FreemarkerUtils.generateFromTemplate(module.getMetaVersion(), "localFtpTestServerTemplate_en.ftl", values));
        files.put("user-flow-tests/src/test/java/org/ikasan/studio/flowtests/FileDeliveryAssertions.java",
                FreemarkerUtils.generateFromTemplate(module.getMetaVersion(), "fileDeliveryAssertionsTemplate_en.ftl", values));
        files.put("user-flow-tests/src/test/java/org/ikasan/studio/flowtests/OutputTextSupport.java",
                FreemarkerUtils.generateFromTemplate(module.getMetaVersion(), "outputTextSupportTemplate_en.ftl", values));
        files.put(SUPPORT_PATH, FreemarkerUtils.generateFromTemplate(module.getMetaVersion(), "moduleFlowTestSupportTemplate_en.ftl", values));
        files.put(path, FreemarkerUtils.generateFromTemplate(module.getMetaVersion(), observationOnly ? "flowObservationTestTemplate_en.ftl" : "flowTestTemplate_en.ftl", values));
        files.put("user-flow-tests/pom.xml", FreemarkerUtils.generateFromTemplate(module.getMetaVersion(), "flowTestPomTemplate_en.ftl", values));
        files.put("user-flow-tests/README.md", """
                # Flow tests

                These are developer-owned scaffolds using the actual generated application.
                Scenario tests use IkasanFlowTestRule; continuous-source observation tests use a bounded counting listener.
                They deliberately FAIL before starting services until you complete the test scenario.
                Direct self-generating-source/discard-sink observation tests have only TODO 1–2: review settings, then enable.
                They check any meta-pack initial payload sequence, count later events, and stop the test flow only during teardown.
                Other scenarios follow TODO 1–5 in the Java test: isolate settings, supply two input batches, select output and expected results,
                review component-path expectations, then enable and run. Set CONFIGURED only after completing the first four tasks.
                Configure shared test connections in src/test/resources/module-test.properties (UTF-8).
                For plain FTP endpoints, set test.ftp.enabled=true to start a disposable loopback FTP server per test.
                This overrides all module FTP connections and directories; inspect/seed context.getBean(LocalFtpTestServer.class).root().
                The server and temporary files are cleaned up with the context. SFTP/FTPS require separate services.
                The Generate Flow Test dialog can enable local FTP and regenerate shared setup, archiving existing files.
                Unchecked leaves settings unchanged. Selecting FTP preserves unrelated properties and custom credentials.
                Missing FTP/MINA dependencies are added to an existing test POM with a backup; other contents are preserved.
                ModuleFlowTestSupport.java loads that file and enforces isolated H2 and test-only startup settings.
                Missing sample-consumer fixture-input defaults are added with a backup; explicit values are preserved.
                Set a fixture-input flag false to retain polling across regeneration; adapt the input scenario accordingly.
                Absent settings retain application defaults; review them before starting the whole module context.
                Each scenario opens and closes a fresh context; contexts and service state are not shared across tests.
                Standard tests use runTest with named supplyInput and optional verifyReceivedOutput overrides.
                verifyFlow supplies the common lifecycle and two-delivery checks; verifyScenario supports
                deliberate rejection/branch scenarios with explicit expectations and output/absence assertions.
                For JMS queue scaffolds configure test.jms.broker-url and the flow's matching isolated broker/destinations
                in module-test.properties. ModuleJmsTestConfig uses ActiveMQ; adapt it for another provider.
                Test configuration classes can be supplied through testConfigurationClasses(); retrieve sample beans
                from the context explicitly (these tests do not use SpringRunner field injection).
                Flow-specific input overrides remain in the individual test. The support file is created if missing,
                otherwise preserved unless you explicitly select archive and regenerate shared setup.
                Review shared setup when flows are added or renamed; merge custom settings from its backup after regeneration.
                Consult ../LOCAL_TEST_ENVIRONMENT.md for test services. Never point tests at production systems.
                Test-only MANUAL startup does not change the model or normal application startup settings.

                Run from the project root: `mvn -pl user-flow-tests -am test`.
                For one test: `mvn -pl user-flow-tests -am -Dtest=YourFlowTest -Dsurefire.failIfNoSpecifiedTests=false test`.
                STRINGIFY_ACTUAL_OUTPUT enables content comparison for Ikasan Payload, bytes, files/paths/file lists and JMS TextMessage.
                It uses UTF-8, rejects unsupported types and never acknowledges/consumes JMS messages; override outputText for other formats.
                Set it false to retain String.valueOf. Receiver delivery assertions remain separate.
                FIRST_BATCH_INPUT and SECOND_BATCH_INPUT define scenario data independently of expected outputs.
                Shared assertFileContents / assertDeliveredFileContents wait for exact UTF-8 file delivery and contents.
                Isolated FTP tests check all accumulated output files using localFtpDirectory(context): one new file per batch.
                Adapt assertions for overwrites/checksums; unknown remote receiver locations retain explicit guards.
                For SFTP/remote FTP, inspect the test server filesystem or download files first; these helpers do not access remote paths.
                Assert actual delivery at external receivers as well as observed flow payloads. For routers, assert
                every expected branch and absence of unwanted deliveries. For exclusions, assert stored exclusions
                and delivery of subsequent valid input. Component invocation alone proves none of these.
                Scheduled tests explicitly trigger the consumer; separately verify real scheduling and pacing.
                Isolated test flows stop during teardown; production/demo flows must stay running between batches.

                Migration updates the inherited version.ikasan dependency. Studio never regenerates these tests.
                Run before and after migration; review framework API changes if a future major version needs them.
                Keep assertions unchanged when comparing behaviour. Add application-specific timeouts and reliable
                cleanup for any local services you start. A stuck JVM is a test/shutdown failure, not a passing run.
                """);
        String updated = rootPom;
        if (!parent.getModules().contains("user-flow-tests")) {
            parent.addModule("user-flow-tests");
            StringWriter writer = new StringWriter();
            new MavenXpp3Writer().write(writer, parent);
            updated = writer.toString();
        }
        return new Scaffold(updated, path, java.util.Collections.unmodifiableMap(files));
    }
}
