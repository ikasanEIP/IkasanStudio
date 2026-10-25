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
        var reverseMeta = ComponentLibrary.getIkasanComponentByKeyMandatory(pack, "JMS Map Message To Payload Converter");
        var reverse = FlowElementFactory.createFlowElement(pack, reverseMeta, flow, flow.getFlowRoute(), "Map To Payload");
        flow.getFlowRoute().getFlowElements().add(reverse);
        assertEquals((pack.equals("V3.3.9") ? "javax" : "jakarta") + ".jms.MapMessage", reverseMeta.getExpectedInputTypes());
        assertEquals("org.ikasan.filetransfer.Payload", reverseMeta.getProducedOutputType());
        var module = Module.moduleBuilder().version(pack).name("Map Demo").applicationPackageName("org.example")
                .port("8080").h2PortNumber("8092").h2WebPortNumber("8093").flows(List.of(flow)).build();
        String defaultsFactory = FlowsComponentFactoryTemplate.create("org.example", module, flow);
        assertTrue(defaultsFactory.contains("setContentAttributeName(\"content\")"));
        assertTrue(defaultsFactory.contains("setIdAttributeName(\"fileName\")"));
        assertTrue(defaultsFactory.contains("setFileNameAttributeName(\"fileName\")"));
        reverse.setPropertyValue("contentAttributeName", "content");
        reverse.setPropertyValue("idAttributeName", "fileName");
        reverse.setPropertyValue("fileNameAttributeName", "fileName");
        String properties = org.ikasan.studio.core.generator.PropertiesTemplate.create(module);
        assertTrue(properties.contains("mapping.maptopayload.map.converter.contentAttributeName=content"));
        assertTrue(properties.contains("mapping.maptopayload.map.converter.idAttributeName=fileName"));
        assertTrue(properties.contains("mapping.maptopayload.map.converter.fileNameAttributeName=fileName"));
        String factory = FlowsComponentFactoryTemplate.create("org.example", module, flow);
        assertTrue(factory.contains("new org.ikasan.component.converter.filetransfer.PayloadToMapConverter()"));
        assertFalse(factory.contains("UserImplemented"));
        assertTrue(factory.contains("new org.ikasan.component.converter.filetransfer.MapMessageToPayloadConverter()"));
        assertTrue(factory.contains("component.getConfiguration().setContentAttributeName("));
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
        write(root, "src/test/java/org/example/MapMessagePayloadTest.java", """
                package org.example;
                import org.junit.Test;
                import static org.junit.Assert.*;
                import org.ikasan.filetransfer.Payload;
                import org.ikasan.component.converter.filetransfer.MapMessageToPayloadConverter;
                import java.util.*;
                import JMS_NAMESPACE.jms.MapMessage;
                public class MapMessagePayloadTest {
                    private MapMessageToPayloadConverter converter(String content, String id, String file) throws Exception {
                        var factory = new ComponentFactoryMapping();
                        for (var field : ComponentFactoryMapping.class.getDeclaredFields()) {
                            String name = field.getName().toLowerCase(java.util.Locale.ROOT);
                            if (field.getType() != String.class) continue;
                            field.setAccessible(true);
                            if (name.endsWith("contentattributename")) field.set(factory, content);
                            if (name.endsWith("idattributename")) field.set(factory, id);
                            if (name.endsWith("filenameattributename")) field.set(factory, file);
                        }
                        return (MapMessageToPayloadConverter) factory.getMapToPayload();
                    }
                    private MapMessage message(Map<String,Object> values) {
                        return (MapMessage) java.lang.reflect.Proxy.newProxyInstance(MapMessage.class.getClassLoader(), new Class[]{MapMessage.class}, (p,m,a) -> {
                            if (m.getName().equals("getString") || m.getName().equals("getObject")) return values.get(a[0]);
                            throw new AssertionError(m.getName());
                        });
                    }
                    @Test public void defaultsDecodeTextAndCopySupportedMetadata() throws Exception {
                        var converter = converter("content", "fileName", "fileName");
                        var values = new HashMap<String,Object>(); values.put("content", "orders"); values.put("fileName", "orders.txt");
                        values.put("referencePayload", "ref"); values.put("relativePath", "folder");
                        Payload result = converter.convert(message(values));
                        assertEquals("orders.txt", result.getId());
                        assertArrayEquals("orders".getBytes(java.nio.charset.Charset.defaultCharset()), result.getContent());
                        assertEquals("orders.txt", result.getAttribute("fileName"));
                        assertEquals("ref", result.getAttribute("referencePayload"));
                        assertEquals("folder", result.getAttribute("relativePath"));
                        assertNotNull(converter.getConfiguredResourceId());
                    }
                    @Test public void customFieldsPreserveBinaryContentAndRejectMissingOrUnsupportedContent() throws Exception {
                        var converter = converter("body", "externalId", "outputName");
                        var values = new HashMap<String,Object>(); values.put("body", new byte[]{0, -1, 42});
                        values.put("externalId", "id-7"); values.put("outputName", "result.bin");
                        Payload result = converter.convert(message(values));
                        assertEquals("id-7", result.getId()); assertEquals("result.bin", result.getAttribute("fileName"));
                        assertArrayEquals(new byte[]{0, -1, 42}, result.getContent());
                        values.put("body", 123L);
                        assertThrows(org.ikasan.spec.component.transformation.TransformationException.class, () -> converter.convert(message(values)));
                        values.remove("body");
                        assertThrows(org.ikasan.spec.component.transformation.TransformationException.class, () -> converter.convert(message(values)));
                    }
                }
                """.replace("JMS_NAMESPACE", pack.equals("V3.3.9") ? "javax" : "jakarta"));
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
        elements.addObject().put("componentName", "Unmap Payload").put("componentType", "org.ikasan.spec.component.transformation.Converter")
                .put("implementingClass", "org.ikasan.component.converter.filetransfer.MapMessageToPayloadConverter")
                .put("configurationId", "map-config");
        var transitions = flow.putArray("transitions");
        transitions.addObject().put("from", "Input").put("to", "Map Payload").put("name", "default");
        transitions.addObject().put("from", "Map Payload").put("to", "Unmap Payload").put("name", "default");
        transitions.addObject().put("from", "Unmap Payload").put("to", "Output").put("name", "default");
        var result = org.ikasan.studio.core.importer.IkasanRuntimeImport.convert(topology.toString(), "[{\"configurationId\":\"map-config\",\"implementingClass\":\"org.ikasan.component.converter.filetransfer.MapMessageToPayloadConverterConfiguration\",\"parameters\":[{\"name\":\"idAttributeName\",\"value\":\"id\"}]}]", pack, "org.example",
                Map.of(new org.ikasan.studio.core.importer.IkasanRuntimeImport.ComponentRef("Mapping", "Input"), "Scheduled Consumer"));
        var module = org.ikasan.studio.core.io.ComponentIO.validatePersistedModuleJson(result.studioJson(), "runtime map", false);
        var mapped = module.getFlows().get(0).getFlowRoute().getFlowElements().stream()
                .filter(element -> "Map Payload".equals(element.getComponentName())).findFirst().orElseThrow();
        assertEquals("Payload To Map Converter", mapped.getComponentMeta().getName());
        assertFalse(mapped.getComponentMeta().isGeneratesUserImplementedClass());
        var reverse = module.getFlows().get(0).getFlowRoute().getFlowElements().stream()
                .filter(e -> "Unmap Payload".equals(e.getComponentName())).findFirst().orElseThrow();
        assertEquals("JMS Map Message To Payload Converter", reverse.getComponentMeta().getName());
        assertEquals("id", reverse.getPropertyValue("idAttributeName"));
    }

    private static void write(Path root, String name, String contents) throws Exception {
        Path file = root.resolve(name); Files.createDirectories(file.getParent()); Files.writeString(file, contents);
    }
}
