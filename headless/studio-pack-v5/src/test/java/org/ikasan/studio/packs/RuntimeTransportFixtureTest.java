package org.ikasan.studio.packs;

import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exports real, isolated transport scenarios. Maven/JDK 26 executes them separately. */
class RuntimeTransportFixtureTest {
    @TempDir Path temporary;
    @Test void exportsLocalTransportScenarios() throws Exception {
        Path root = Path.of(System.getProperty("studio.v5.transports", temporary.toString()));
        var scenarios = List.of("jms", "jms-object", "smtp", "smtp-generated", "smtp-configured", "ftp-input", "ftp-output", "sftp-input", "sftp-output");
        for (String scenario : scenarios) {
            var files = render(scenario);
            assertTrue(files.get("src/test/java/org/ikasan/studio/flowtests/TransportTest.java").contains("\"third\""));
            for (var file : files.entrySet()) {
                Path target = root.resolve(scenario).resolve(file.getKey());
                Files.createDirectories(target.getParent()); Files.writeString(target, file.getValue());
            }
        }
        Files.writeString(root.resolve("pom.xml"), "<project><modelVersion>4.0.0</modelVersion>"
                + "<groupId>org.ikasan.studio.probe</groupId><artifactId>transports</artifactId>"
                + "<version>1</version><packaging>pom</packaging><modules>"
                + scenarios.stream().map(s -> "<module>" + s + "</module>").reduce("", String::concat)
                + "</modules></project>");
    }

    static Map<String,String> render(String scenario) throws Exception {
        return render(scenario, flow -> { });
    }

    @FunctionalInterface interface FlowConfiguration { void accept(Flow flow) throws Exception; }

    static Map<String,String> render(String scenario, FlowConfiguration configure) throws Exception {
        boolean input = scenario.endsWith("input"), jms = scenario.startsWith("jms"), smtp = scenario.startsWith("smtp");
        boolean objectMessage = scenario.equals("jms-object");
        boolean configuredEmail = scenario.equals("smtp-configured");
        boolean generatedEmail = scenario.equals("smtp-generated") || configuredEmail;
        String protocol = scenario.startsWith("sftp") ? "sftp" : "ftp";
        String consumerKind = jms ? "Spring JMS Consumer" : input ? protocol.toUpperCase() + " Consumer" : "Generic Consumer";
        String producerKind = jms ? "Spring JMS Producer" : smtp ? "Email Producer" : input ? "Dev Null Producer" : protocol.toUpperCase() + " Producer";
        var consumer = DevelopmentFixtureTest.element(consumerKind, "Input");
        var producer = DevelopmentFixtureTest.element(producerKind, "Output");
        // Without destination JNDI properties, Ikasan creates literal queue names.
        if (jms) {
            consumer.setPropertyValue("destinationJndiName", "input");
            producer.setPropertyValue("destinationJndiName", "output");
        } else if (input) {
            consumer.setPropertyValue("filenamePattern", ".*\\.txt");
            consumer.setPropertyValue("sourceDirectory", "/input");
            consumer.setPropertyValue("username", "test");
        } else {
            consumer.setPropertyValue("userImplementedClassName", "InputFeed");
            if (smtp) {
                producer.setPropertyValue("toRecipients", List.of("one@example.test", "two@example.test"));
                producer.setPropertyValue("from", "sender@example.test");
                if (generatedEmail) producer.setPropertyValue("mailSubject", "Model subject");
            } else producer.setPropertyValue("username", "test");
        }
        var flow = Flow.flowBuilder().metapackVersion(DevelopmentFixtureTest.PACK).name("Transport").consumer(consumer).build();
        var elements = new ArrayList<FlowElement>();
        if (objectMessage) elements.add(DevelopmentFixtureTest.element("JMS Object Message To Object Converter", "Unwrap"));
        if (!input) {
            var converter = DevelopmentFixtureTest.element(generatedEmail ? "Email Converter" : "Converter", "Convert");
            converter.setPropertyValue("userImplementedClassName", "TransportConverter");
            if (generatedEmail) converter.setPropertyValue("fromType", "java.lang.String");
            if (configuredEmail) converter.setPropertyValue("configuration", "MailSettings");
            elements.add(converter);
        }
        elements.add(producer);
        flow.setFlowRoute(FlowRoute.flowRouteBuilder().flow(flow).flowElements(elements).build());
        configure.accept(flow);
        var module = Module.moduleBuilder().version(DevelopmentFixtureTest.PACK).name("TransportProbe")
                .applicationPackageName("org.example.transport").port("8080").h2PortNumber("8092")
                .h2WebPortNumber("8093").flows(List.of(flow)).build();
        module.setPropertyValue("flowStartupType", "MANUAL");
        var files = DevelopmentFixtureTest.render(module);
        files.put("pom.xml", files.get("pom.xml").replace("<artifactId>ikasan5-review</artifactId>", "<artifactId>transport-" + scenario + "</artifactId>"));
        String pkg = GeneratorUtils.getUserImplementedClassesPackageName(module, flow);
        if (!input && !jms) DevelopmentFixtureTest.putJava(files, pkg, "InputFeed",
                FlowsUserImplementedComponentTemplate.create(pkg, module, flow, consumer));
        if (!input) {
            String inputType = jms && !objectMessage ? "jakarta.jms.TextMessage" : "String";
            String outputType = jms ? "String" : smtp ? "org.ikasan.component.endpoint.email.producer.DefaultEmailPayload" : "org.ikasan.filetransfer.component.DefaultPayload";
            String body = objectMessage ? "return value;" : jms ? "try { return value.getText(); } catch (jakarta.jms.JMSException e) { throw new RuntimeException(e); }"
                    : smtp ? "var result = new " + outputType + "(); result.setEmailBody(value); return result;"
                    : "var result = new " + outputType + "(value, value.getBytes(java.nio.charset.StandardCharsets.UTF_8));"
                    + "result.setAttribute(org.ikasan.filetransfer.FilePayloadAttributeNames.FILE_NAME, value + \".txt\"); return result;";
            DevelopmentFixtureTest.putJava(files, pkg, "TransportConverter", "package " + pkg + ";\n"
                    + "@org.springframework.stereotype.Component(\"" + pkg + ".TransportConverter\")\n"
                    + "public class TransportConverter implements org.ikasan.spec.component.transformation.Converter<" + inputType + "," + outputType + "> {\n"
                    + "public " + outputType + " convert(" + inputType + " value) { " + body + " }\n}");
        }
        if (generatedEmail) {
            // Exercise the real generated business stub, not the fixture's hand-written converter.
            DevelopmentFixtureTest.putJava(files, pkg, "TransportConverter",
                    FlowsUserImplementedComponentTemplate.create(pkg, module, flow, elements.get(0)));
        }
        if (configuredEmail) {
            String configuration = FlowsUserImplementedClassPropertyTemplate.create(DevelopmentFixtureTest.PACK,
                    elements.get(0).getProperty("configuration"), pkg, "MailSettings", "transport.mail-settings");
            configuration = configuration.substring(0, configuration.lastIndexOf('}')) + """
                    private String prefix = "UNBOUND";
                    private int copies;
                    private java.util.List<String> tags = new java.util.ArrayList<>();
                    private java.util.Map<String, String> labels = new java.util.HashMap<>();
                    public String getPrefix() { return prefix; }
                    public void setPrefix(String value) { prefix = value; }
                    public int getCopies() { return copies; }
                    public void setCopies(int value) { copies = value; }
                    public java.util.List<String> getTags() { return tags; }
                    public void setTags(java.util.List<String> value) { tags = value; }
                    public java.util.Map<String, String> getLabels() { return labels; }
                    public void setLabels(java.util.Map<String, String> value) { labels = value; }
                    }
                    """;
            DevelopmentFixtureTest.putJava(files, pkg, "MailSettings", configuration);
            String converterPath = "src/main/java/" + pkg.replace('.', '/') + "/TransportConverter.java";
            String converter = files.get(converterPath);
            assertTrue(converter.contains("emailPayload.setEmailBody(payload.toString());"));
            converter = converter.replace("emailPayload.setEmailBody(payload.toString());", """
                    emailPayload.setEmailBody(settings.getPrefix() + String.join("/", settings.getTags()) + ":"
                        + settings.getLabels().get("suffix") + ":" + payload.repeat(settings.getCopies()));
                    """);
            files.put(converterPath, converter.substring(0, converter.lastIndexOf('}'))
                    + "private MailSettings settings; public void setConfiguration(MailSettings value) { settings = value; }\n}\n");
            files.compute("src/main/resources/application.properties", (key, value) -> value
                    + "\ntransport.mail-settings.prefix=model-\ntransport.mail-settings.copies=1\n"
                    + "transport.mail-settings.tags[0]=base\ntransport.mail-settings.labels.suffix=model\n");
            files.compute("src/test/resources/module-test.properties", (key, value) -> value
                    + "\ntransport.mail-settings.prefix=env-\ntransport.mail-settings.copies=2\n"
                    + "transport.mail-settings.tags[0]=review\ntransport.mail-settings.tags[1]=v5\n"
                    + "transport.mail-settings.labels.suffix=tail\n");
        }
        files.keySet().removeIf(p -> p.startsWith("src/test/java/org/ikasan/studio/flowtests/") && !p.contains("/support/"));
        String directory = protocol.equals("sftp") ? "localSftpDirectory(context, \"Transport\", \"" + (input ? "Input" : "Output") + "\")" : "localFtpDirectory(context)";
        String send = jms ? "client." + (objectMessage ? "sendObject" : "sendText") + "(\"input\", value);"
                : input ? "FileInputFixture.copyResource(" + directory + ", \".*\\\\.txt\", \"/input/\" + value + \".txt\"); harness.fireScheduledConsumer();"
                : "context.getBean(" + pkg + ".InputFeed.class).submitNow(value);";
        String receive = jms ? "client.assertTexts(\"output\", batch.expectedTexts(\"Output\"));"
                : smtp ? "String body = batch.expectedTexts(\"Output\").get(0); localSmtpServer(context).assertBatchBodies(List.of(body, body), deliveryTimeout(context));"
                : input ? "// Producer observation already checked the downloaded content."
                : "assertDeliveredBatchContents(" + directory + ", \"*.txt\", number, batch.expectedTexts(\"Output\").toArray(String[]::new));";
        if (generatedEmail) receive += """
                var module = context.getBean(org.ikasan.spec.module.Module.class);
                var flow = (org.ikasan.spec.flow.Flow) module.getFlow("Transport");
                org.ikasan.component.endpoint.email.producer.EmailProducer email =
                        org.springframework.test.util.AopTestUtils.getUltimateTargetObject(flow.getFlowElement("Output").getFlowComponent());
                org.junit.Assert.assertEquals("environment-sender@example.test", email.getConfiguration().getMailFrom());
                for (var message : localSmtpServer(context).receivedMessages()) {
                    org.junit.Assert.assertEquals("Environment subject", message.getSubject());
                    var recipients = java.util.Arrays.stream(message.getAllRecipients())
                            .map(Object::toString).collect(java.util.stream.Collectors.toSet());
                    org.junit.Assert.assertEquals(java.util.Set.of("review-one@example.test", "review-two@example.test"), recipients);
                }
                """;
        String config = jms ? "private JmsFlowTestSupport client;\nprivate org.apache.activemq.broker.BrokerService broker;\n"
                + "@Override protected void prepareFixtures(org.springframework.context.ConfigurableApplicationContext context) throws Exception { broker = new org.apache.activemq.broker.BrokerService(); broker.setBrokerName(\"v5-probe\"); broker.setPersistent(false); broker.setUseJmx(false); broker.setUseShutdownHook(false); broker.start(); client = JmsFlowTestSupport.from(context); }\n"
                + "@Override protected void cleanupFixtures(org.springframework.context.ConfigurableApplicationContext context) throws Exception { try { if (client != null) client.close(); } finally { if (broker != null) { broker.stop(); broker.waitUntilStopped(); } } }\n"
                + "@Override protected Class<?>[] testConfigurationClasses() { return new Class<?>[]{org.ikasan.studio.flowtests.support.ModuleJmsTestConfig.class}; }" : "";
        if (!jms) {
            String serverType = smtp ? "LocalSmtpTestServer" : protocol.equals("sftp") ? "LocalSftpTestServer" : "LocalFtpTestServer";
            String bean = "studio" + serverType;
            config = "private int transportPort;\n"
                    + "@Override protected void prepareFixtures(org.springframework.context.ConfigurableApplicationContext context) { transportPort = context.getBean(\"" + bean + "\", " + serverType + ".class).port(); }\n";
        }
        String cleanupCheck = jms
                ? "org.junit.Assert.assertNull(\"Owned broker stopped after teardown\", org.apache.activemq.broker.BrokerRegistry.getInstance().lookup(\"v5-probe\"));"
                : "try (var socket = new java.net.Socket()) { socket.connect(new java.net.InetSocketAddress(\"127.0.0.1\", transportPort), 500); org.junit.Assert.fail(\"Local transport listener still open after teardown\"); } catch (java.net.ConnectException expected) { /* Listener closed. */ }";
        files.put("src/test/java/org/ikasan/studio/flowtests/TransportTest.java", """
                package org.ikasan.studio.flowtests;
                import java.util.*;
                import org.junit.Test;
                import org.ikasan.studio.flowtests.support.ModuleFlowTestSupport;
                import org.ikasan.studio.flowtests.support.utils.*;
                import org.ikasan.testharness.flow.rule.IkasanFlowTestRule;
                public class TransportTest extends ModuleFlowTestSupport {
                    @Override protected String getFlowName() { return "Transport"; }
                    @Override protected void defineExpectedPath(IkasanFlowTestRule harness) {
                        for (int i = 0; i < expectedInputCount(); i++) {
                """ + (input ? "harness.scheduledConsumer(\"Input\");" : "harness.consumer(\"Input\");")
                + (objectMessage ? "harness.converter(\"Unwrap\");" : "")
                + (input ? "" : "harness.converter(\"Convert\");") + "harness.producer(\"Output\");\n}}\n" + config
                + (smtp ? "@Override protected String formatOutputText(Object payload) { return ((org.ikasan.component.endpoint.email.producer.EmailPayload) payload).getEmailBody(); }\n" : "@Override protected String formatOutputText(Object payload) { return formatOutputText(payload, true); }\n")
                + """
                    @Test public void firstIdleAndLaterDelivery() throws Exception {
                        // runBatches checks RUNNING during each idle window; it never restarts the flow.
                        runBatches(context -> List.of(
                            batch(List.of("first"), Map.of("Output", List.of("first"))),
                            batch(List.of("later"), Map.of("Output", List.of("later"))),
                            batch(List.of("third"), Map.of("Output", List.of("third")))),
                """ + "(context, harness, value) -> { " + send + " },\n(context, number, batch) -> { " + receive + "\n});\n" + cleanupCheck + "\n}\n}\n");
        for (String value : List.of("first", "later", "third")) files.put("src/test/resources/input/" + value + ".txt", value);
        String settings = jms ? "test.jms.broker-url=vm://v5-probe?create=false\n"
                : "test." + (smtp ? "smtp" : protocol) + ".enabled=true\n";
        files.compute("src/test/resources/module-test.properties", (key, value) -> value.replaceAll("(?m)^test\\.(smtp|ftp|sftp)\\.enabled=.*\\R?", "") + "\n" + settings);
        if (generatedEmail) {
            String prefix = "transport.output.email.producer.";
            files.compute("src/test/resources/module-test.properties", (key, value) -> value
                    + "\n" + prefix + "toRecipients=[\"review-one@example.test\",\"review-two@example.test\"]\n"
                    + prefix + "from=environment-sender@example.test\n"
                    + prefix + "mailSubject=Environment subject\n");
            assertTrue(files.get("src/main/resources/application.properties").contains(prefix + "mailSubject=Model subject"));
            assertTrue(files.get("src/main/resources/application.properties").contains("one@example.test"));
        }
        if (configuredEmail) {
            files.compute("src/test/java/org/ikasan/studio/flowtests/TransportTest.java", (key, value) -> {
                for (String inputValue : List.of("first", "later", "third")) {
                    value = value.replace("Map.of(\"Output\", List.of(\"" + inputValue + "\"))",
                            "Map.of(\"Output\", List.of(\"env-review/v5:tail:" + inputValue.repeat(2) + "\"))");
                }
                return value;
            });
        }
        return files;
    }
}
