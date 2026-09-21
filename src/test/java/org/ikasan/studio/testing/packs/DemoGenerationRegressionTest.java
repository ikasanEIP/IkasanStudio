package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.FlowElementFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.io.TempDir;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.Tag("packs")
class DemoGenerationRegressionTest {
    @TempDir Path temp;

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void zeroAgeIsAppliedAndFilenamePatternsRoundTripThroughProperties(String pack) throws Exception {
        var module = TestFixtures.getMyFirstModuleIkasanModule(pack, new ArrayList<>());
        var flow = TestFixtures.getUnbuiltFlow(pack).build();
        module.addFlow(flow);
        var consumer = FlowElementFactory.createFlowElement(pack,
                ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "SFTP Consumer"), flow, flow.getFlowRoute(), "Receive");
        consumer.setPropertyValue("minAge", 0L);
        flow.setConsumer(consumer);
        String factory = FlowsComponentFactoryTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, flow);
        assertTrue(factory.contains(".setMinAge(0L)"), factory);
        consumer.setPropertyValue("minAge", null);
        assertFalse(FlowsComponentFactoryTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, flow).contains(".setMinAge("));
        var local = FlowElementFactory.createFlowElement(pack,
                ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Local File Consumer"), flow, flow.getFlowRoute(), "Files");
        String pattern = "/tmp/Demo[0-9]+[.]txt";
        local.setPropertyValue("filenames", pattern);
        flow.setConsumer(local);
        var loaded = new Properties();
        loaded.load(new java.io.ByteArrayInputStream(PropertiesTemplate.create(module).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        String restored = loaded.getProperty(flow.getJavaPackageName() + ".file.consumer.filenames");
        assertEquals(pattern, restored);
        assertTrue(java.util.regex.Pattern.matches(restored, "/tmp/Demo12.txt"));
        assertFalse(java.util.regex.Pattern.matches(restored, "/tmp/Demo12Xtxt"));
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void scheduledProviderStubCompilesAndImplementsItsInterface(String pack) throws Exception {
        var meta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Scheduled Consumer")
                .getMetadata("messageProvider");
        assertTrue(meta.isProtectFromOverwrite());
        var property = new org.ikasan.studio.core.model.ikasan.instance.ComponentProperty(meta, "Samples");
        String source = FlowsUserImplementedClassPropertyTemplate.create(pack, property, "example", "Samples", "demo");
        write("org/springframework/stereotype/Component.java", "package org.springframework.stereotype; public @interface Component { String value(); }");
        write("org/quartz/JobExecutionContext.java", "package org.quartz; public interface JobExecutionContext {}");
        write("org/ikasan/component/endpoint/quartz/consumer/MessageProvider.java", "package org.ikasan.component.endpoint.quartz.consumer; public interface MessageProvider<T> { T invoke(org.quartz.JobExecutionContext context); }");
        write("example/Samples.java", source);
        compile();
        try (var loader = new URLClassLoader(new java.net.URL[]{temp.toUri().toURL()}, null)) {
            Class<?> type = loader.loadClass("example.Samples");
            assertNotNull(type.getMethod("invoke", loader.loadClass("org.quartz.JobExecutionContext")));
        }
    }

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void loggingFactoryWorksEvenWhenRegexpSetterReturnsNull(String pack) throws Exception {
        var module = TestFixtures.getMyFirstModuleIkasanModule(pack, new ArrayList<>());
        var flow = TestFixtures.getUnbuiltFlow(pack).build();
        module.addFlow(flow);
        var logger = FlowElementFactory.createFlowElement(pack,
                ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Logging Producer"), flow, flow.getFlowRoute(), "Output");
        logger.setPropertyValue("regExpPattern", "^(.+)$");
        logger.setPropertyValue("replacementText", "Priority: $1");
        flow.getFlowRoute().getFlowElements().add(logger);
        String factory = FlowsComponentFactoryTemplate.create("example", module, flow);
        int start = factory.indexOf("public org.ikasan.spec.component.endpoint.Producer getOutput()");
        assertTrue(start >= 0, factory);
        String method = factory.substring(start, factory.indexOf('}', start) + 1);
        write("org/ikasan/spec/component/endpoint/Producer.java", "package org.ikasan.spec.component.endpoint; public interface Producer {}");
        write("org/ikasan/builder/component/endpoint/LogProducerBuilder.java", """
                package org.ikasan.builder.component.endpoint;
                public class LogProducerBuilder {
                    public String pattern, replacement;
                    public LogProducerBuilder setRegExpPattern(String p) { pattern=p; return null; }
                    public LogProducerBuilder setReplacementText(String r) { replacement=r; return this; }
                    public org.ikasan.spec.component.endpoint.Producer build() {
                        if (!"^(.+)$".equals(pattern) || !"Priority: $1".equals(replacement)) throw new AssertionError();
                        return new org.ikasan.spec.component.endpoint.Producer() {};
                    }
                }
                """);
        write("example/Factory.java", "package example; public class Factory { "
                + "static class Builders { Builders getComponentBuilder() { return this; } "
                + "org.ikasan.builder.component.endpoint.LogProducerBuilder logProducer() { return new org.ikasan.builder.component.endpoint.LogProducerBuilder(); }} "
                + "Builders builderFactory = new Builders(); " + method + " }");
        compile();
        try (var loader = new URLClassLoader(new java.net.URL[]{temp.toUri().toURL()}, null)) {
            var type = loader.loadClass("example.Factory");
            assertNotNull(type.getMethod("getOutput").invoke(type.getConstructor().newInstance()));
        }
    }

    private void write(String relative, String content) throws Exception {
        Path file = temp.resolve(relative); Files.createDirectories(file.getParent()); Files.writeString(file, content);
    }

    private void compile() throws Exception {
        try (var files = Files.walk(temp)) {
            var args = new ArrayList<String>(); args.add("-d"); args.add(temp.toString());
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> args.add(p.toString()));
            args.add(0, "javac");
            Path output = temp.resolve("javac.txt");
            var process = new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(output.toFile()).start();
            if (!process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly(); fail("javac timed out");
            }
            assertEquals(0, process.exitValue(), Files.readString(output));
        }
    }
}
