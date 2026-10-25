package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.generator.FlowsComponentFactoryTemplate;
import org.ikasan.studio.core.generator.ModelTemplate;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("packs")
class PayloadToMapConverterTest {
    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT"})
    void rendersRealConverterAndExportsRuntimeCompilationFixture(String pack) throws Exception {
        var meta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Payload To Map Converter");
        assertEquals("org.ikasan.filetransfer.Payload", meta.getExpectedInputTypes());
        assertEquals("java.util.Map", meta.getProducedOutputType());
        assertFalse(meta.isGeneratesUserImplementedClass());
        var flow = Flow.flowBuilder().metapackVersion(pack).name("Mapping").build();
        var element = FlowElementFactory.createFlowElement(pack, meta, flow, flow.getFlowRoute(), "Payload To Map");
        flow.getFlowRoute().getFlowElements().add(element);
        var module = Module.moduleBuilder().version(pack).name("Map Demo").applicationPackageName("org.example")
                .port("8080").h2PortNumber("8092").h2WebPortNumber("8093").flows(List.of(flow)).build();
        String factory = FlowsComponentFactoryTemplate.create("org.example", module, flow);
        assertTrue(factory.contains("new org.ikasan.component.converter.filetransfer.PayloadToMapConverter()"));
        assertFalse(factory.contains("UserImplemented"));
        var restored = org.ikasan.studio.core.io.ComponentIO.validatePersistedModuleJson(ModelTemplate.create(module), "map test", false);
        assertEquals(meta.getImplementingClass(), restored.getFlows().get(0).getFlowRoute().getFlowElements().get(0).getComponentMeta().getImplementingClass());
        assertTrue(module.getAllUniqueSortedJarDependencies().stream().anyMatch(d -> "ikasan-component-converter".equals(d.getArtifactId())));
        var manifest = ComponentLibrary.getMetaPackManifest(pack);
        Path root = Path.of("build/verification/payload-to-map", pack);
        write(root, "src/main/java/org/example/ComponentFactoryMapping.java", factory);
        write(root, "pom.xml", """
                <project><modelVersion>4.0.0</modelVersion><groupId>org.example</groupId>
                <artifactId>payload-map-probe</artifactId><version>1.0-SNAPSHOT</version>
                <properties><maven.compiler.release>%s</maven.compiler.release><project.build.sourceEncoding>UTF-8</project.build.sourceEncoding></properties>
                <dependencyManagement><dependencies><dependency><groupId>org.ikasan</groupId><artifactId>ikasan-eip-standalone-bom</artifactId><version>%s</version><type>pom</type><scope>import</scope></dependency></dependencies></dependencyManagement>
                <dependencies><dependency><groupId>org.ikasan</groupId><artifactId>ikasan-builder</artifactId></dependency>
                <dependency><groupId>org.ikasan</groupId><artifactId>ikasan-component-converter</artifactId></dependency>
                <dependency><groupId>junit</groupId><artifactId>junit</artifactId><version>4.13.2</version><scope>test</scope></dependency></dependencies>
                <build><plugins><plugin><artifactId>maven-compiler-plugin</artifactId><version>3.15.0</version></plugin>
                <plugin><artifactId>maven-surefire-plugin</artifactId><version>3.2.5</version></plugin></plugins></build></project>
                """.formatted(manifest.javaVersion(), manifest.ikasanVersion()));
        write(root, "src/test/java/org/example/PayloadMapTest.java", """
                package org.example;
                import org.junit.Test;
                import static org.junit.Assert.*;
                import org.ikasan.filetransfer.Payload;
                import java.util.*;
                public class PayloadMapTest {
                    @Test @SuppressWarnings("unchecked") public void generatedFactoryConvertsRealPayloadAndRetainsAttributePrecedence() throws Exception {
                        var converter = new ComponentFactoryMapping().getPayloadToMap();
                        Map<String,String> attributes = new HashMap<>(); attributes.put("fileName", "orders.txt");
                        Payload payload = (Payload) java.lang.reflect.Proxy.newProxyInstance(Payload.class.getClassLoader(), new Class[]{Payload.class}, (p,m,a) -> {
                            switch (m.getName()) {
                                case "getId": return "id-1";
                                case "getContent": return "orders".getBytes(java.nio.charset.Charset.defaultCharset());
                                case "getAttributeMap": return attributes;
                                default: throw new AssertionError(m.getName());
                            }
                        });
                        Map<String,Object> result = (Map<String,Object>) converter.convert(payload);
                        assertEquals("orders", result.get("content")); assertEquals("id-1", result.get("id"));
                        assertEquals("orders.txt", result.get("fileName"));
                        attributes.put("content", "attribute-content"); attributes.put("id", "attribute-id");
                        result = (Map<String,Object>) converter.convert(payload);
                        assertEquals("attribute-content", result.get("content")); assertEquals("attribute-id", result.get("id"));
                    }
                }
                """);
    }
    @ParameterizedTest @ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.0.1-SNAPSHOT"})
    void runtimeImportRecognisesTheStandardConverterWithoutRequestingAStub(String pack) throws Exception {
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var topology = json.createObjectNode().put("name", "Existing Module");
        var flow = topology.putArray("flows").addObject().put("name", "Mapping");
        var sourceMeta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Scheduled Consumer");
        var targetMeta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "Logging Producer");
        var source = json.createObjectNode().put("componentName", "Input").put("componentType", sourceMeta.getComponentType())
                .put("implementingClass", sourceMeta.getImplementingClass());
        flow.set("consumer", source);
        var elements = flow.putArray("flowElements");
        elements.add(source.deepCopy());
        elements.addObject().put("componentName", "Map Payload").put("componentType", "org.ikasan.spec.component.transformation.Converter")
                .put("implementingClass", "org.ikasan.component.converter.filetransfer.PayloadToMapConverter");
        elements.addObject().put("componentName", "Output").put("componentType", targetMeta.getComponentType())
                .put("implementingClass", targetMeta.getImplementingClass());
        var transitions = flow.putArray("transitions");
        transitions.addObject().put("from", "Input").put("to", "Map Payload").put("name", "default");
        transitions.addObject().put("from", "Map Payload").put("to", "Output").put("name", "default");
        var result = org.ikasan.studio.core.importer.IkasanRuntimeImport.convert(topology.toString(), "", pack, "org.example",
                Map.of(new org.ikasan.studio.core.importer.IkasanRuntimeImport.ComponentRef("Mapping", "Input"), "Scheduled Consumer"));
        var module = org.ikasan.studio.core.io.ComponentIO.validatePersistedModuleJson(result.studioJson(), "runtime map", false);
        var mapped = module.getFlows().get(0).getFlowRoute().getFlowElements().stream()
                .filter(element -> "Map Payload".equals(element.getComponentName())).findFirst().orElseThrow();
        assertEquals("Payload To Map Converter", mapped.getComponentMeta().getName());
        assertFalse(mapped.getComponentMeta().isGeneratesUserImplementedClass());
    }

    private static void write(Path root, String name, String contents) throws Exception {
        Path file = root.resolve(name); Files.createDirectories(file.getParent()); Files.writeString(file, contents);
    }
}
