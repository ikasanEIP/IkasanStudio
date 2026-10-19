package org.example.metapack;

import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.testkit.MetaPackContract;
import org.ikasan.studio.core.generator.ModelTemplate;
import org.ikasan.studio.testkit.MetaPackTestKit;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PackContractTest extends MetaPackContract {
    @Override protected String packId() { return "ExampleIkasan4"; }
    @Override protected Module sampleModule() throws Exception {
        Flow flow = Flow.flowBuilder().metapackVersion(packId()).name("Greeting").build();
        FlowElement consumer = element(flow, "Scheduled Consumer", "Every five seconds");
        consumer.setPropertyValue("cronExpression", "0/5 * * * * ?");
        flow.setConsumer(consumer);
        for (String name : List.of("Example Sample Text Converter", "Example Uppercase Converter", "Example Logging Producer")) {
            flow.getFlowRoute().getFlowElements().add(element(flow, name, name));
        }
        return Module.moduleBuilder().version(packId()).name("Example")
            .applicationPackageName("org.example.application").port("8080")
            .h2PortNumber("8092").h2WebPortNumber("8093").flows(List.of(flow)).build();
    }
    private FlowElement element(Flow flow, String type, String name) throws Exception {
        return FlowElementFactory.createFlowElement(packId(), ComponentLibrary.getIkasanComponentByKeyMandatory(packId(), type), flow, flow.getFlowRoute(), name);
    }
    @Test void writesSampleAndChecksCustomComponentRendering() throws Exception {
        Module sample = sampleModule();
        Files.createDirectories(Path.of("target/samples"));
        Files.writeString(Path.of("target/samples/model.json"), ModelTemplate.create(sample));
        String factory = MetaPackTestKit.render(sample).values().stream().filter(text -> text.contains("class ComponentFactory")).findFirst().orElseThrow();
        assertTrue(factory.contains("new org.example.metapack.UppercaseConverter()"));
        assertTrue(factory.contains("new org.example.metapack.LoggingProducer()"));
        Path source = Path.of("target/compiled-sample/org/example/application/ComponentFactoryGreeting.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, factory);
        var diagnostics = new java.io.ByteArrayOutputStream();
        int result = javax.tools.ToolProvider.getSystemJavaCompiler().run(null, diagnostics, diagnostics,
            "-classpath", System.getProperty("java.class.path"), "-d", "target/compiled-sample", source.toString());
        assertEquals(0, result, diagnostics.toString(java.nio.charset.StandardCharsets.UTF_8));
    }
}
