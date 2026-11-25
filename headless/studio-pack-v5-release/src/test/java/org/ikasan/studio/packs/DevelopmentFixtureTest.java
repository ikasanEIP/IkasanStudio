package org.ikasan.studio.packs;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.maven.IkasanPomModel;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.StringReader;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.ikasan.studio.core.generator.FlowTestScaffold.Scaffold;
import org.ikasan.studio.core.metapack.model.MetaPackManifest;
import org.ikasan.studio.core.metapack.model.MetaPackManifest.BomImport;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;

/** Rendering is portable; Maven compilation is a separate, explicit Java 25 development gate. */
class DevelopmentFixtureTest {
    static final String PACK = "V5.1.0";
    @TempDir Path temporary;

    @Test void rendersApplicationAgainstSnapshotBom() throws Exception {
        FlowElement consumer = element("Event Generating Consumer", "Input");
        FlowElement producer = element("Dev Null Producer", "Output");
        Flow flow = Flow.flowBuilder().metapackVersion(PACK).name("Example Flow").consumer(consumer).build();
        flow.setFlowRoute(FlowRoute.flowRouteBuilder().flow(flow).flowElements(new ArrayList<>(List.of(producer))).build());
        Module module = Module.moduleBuilder().version(PACK).name("Ikasan5Review")
                .applicationPackageName("org.example").port("8080").h2PortNumber("8092")
                .h2WebPortNumber("8093").flows(List.of(flow)).build();
        Map<String,String> files = render(module);
        try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream("/application/console.java.txt"))) {
            files.put("src/test/java/org/ikasan/studio/flowtests/BlueConsoleTest.java",
                    new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        }
        Path destination = Path.of(System.getProperty("studio.v5.fixture", temporary.toString()));
        for (Entry<String,String> file : files.entrySet()) {
            Path target = destination.resolve(file.getKey()); Files.createDirectories(target.getParent());
            Files.writeString(target, file.getValue());
        }
    }

    static Map<String, String> render(Module module) throws Exception {
        MetaPackManifest manifest = ComponentLibrary.getMetaPackManifest(PACK);
        assertEquals("25", manifest.javaVersion());
        assertEquals(2, manifest.compatibilityOverrides().size());
        assertTrue(manifest.compatibilityOverrides().stream().allMatch(override ->
                "org.apache.activemq".equals(override.groupId()) && "6.3.3".equals(override.version())),
                "V5 keeps its runtime BOM and adds only the verified ActiveMQ security upgrade");
        Model model = new Model();
        model.setModelVersion("4.0.0"); model.setGroupId("org.ikasan.studio.probe");
        model.setArtifactId("ikasan5-review"); model.setVersion("1.0-SNAPSHOT");
        IkasanPomModel pom = new IkasanPomModel(model);
        pom.addProperty("version.ikasan", manifest.ikasanVersion());
        pom.addProperty("maven.compiler.release", manifest.javaVersion());
        pom.addProperty("project.build.sourceEncoding", "UTF-8");
        for (BomImport bom : manifest.dependencyManagement()) pom.addOrUpdateBomImport(bom.groupId(), bom.artifactId(), bom.version());
        for (Dependency dependency : module.getAllUniqueSortedJarDependencies()) pom.checkIfDependancyAlreadyExists(dependency.clone());
        Build build = new org.apache.maven.model.Build();
        Plugin compiler = new org.apache.maven.model.Plugin();
        compiler.setArtifactId("maven-compiler-plugin"); compiler.setVersion("3.15.0"); build.addPlugin(compiler);
        model.setBuild(build);
        Map<String, String> files = new LinkedHashMap<>();
        // IkasanPomModel updates its supplied Model; use its serializer to preserve all changes.
        files.put("pom.xml", pom.getModelAsString());
        putJava(files, Generator.STUDIO_BOOT_PACKAGE, "Application", ApplicationTemplate.create(module));
        putJava(files, Generator.STUDIO_BOOT_PACKAGE, "ModuleConfig", ModuleConfigTemplate.create(module));
        putJava(files, Generator.STUDIO_BOOT_PACKAGE, "StudioInjectController", StudioInjectControllerTemplate.create(module));
        for (Flow flow : module.getFlows()) {
            String pkg = Generator.STUDIO_FLOW_PACKAGE + "." + flow.getJavaPackageName();
            putJava(files, pkg, flow.getJavaClassName(), FlowTemplate.create(pkg, module, flow));
            putJava(files, pkg, FlowsComponentFactoryTemplate.COMPONENT_FACTORY_CLASS_NAME + flow.getJavaClassName(),
                    FlowsComponentFactoryTemplate.create(pkg, module, flow));
        }
        files.put("src/main/resources/application.properties", PropertiesTemplate.create(module));
        files.put("generated/src/main/model/model.json", ModelTemplate.create(module));
        files.put("src/main/resources/studio-flow-test-wiring.json", ModuleTestWiring.create(module));
        String parent = "<project><modelVersion>4.0.0</modelVersion><groupId>org.ikasan.studio.probe</groupId>"
                + "<artifactId>parent</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging>"
                + "<properties><version.ikasan>5.1.0</version.ikasan></properties></project>";
        String application = "<project><modelVersion>4.0.0</modelVersion><groupId>org.ikasan.studio.probe</groupId>"
                + "<artifactId>probe-application</artifactId><version>1.0-SNAPSHOT</version></project>";
        for (Flow flow : module.getFlows()) {
            Scaffold scaffold = FlowTestScaffold.render(module, flow, parent, application);
            Model tests = new MavenXpp3Reader().read(new StringReader(scaffold.files().get("user-flow-tests/pom.xml")));
            if (build.getPlugins().size() == 1)
                for (Plugin plugin : tests.getBuild().getPlugins()) build.addPlugin(plugin.clone());
            for (Dependency dependency : tests.getDependencies()) {
                if (!"probe-application".equals(dependency.getArtifactId())) pom.checkIfDependancyAlreadyExists(dependency.clone());
            }
            for (Entry<String,String> file : scaffold.files().entrySet()) {
                if (file.getKey().startsWith("user-flow-tests/src/"))
                    files.put(file.getKey().substring("user-flow-tests/".length()), file.getValue());
            }
        }
        files.computeIfPresent("src/test/resources/module-test.properties", (path, text) -> text + "\nserver.address=127.0.0.1\n");
        files.put("pom.xml", pom.getModelAsString());
        assertTrue(files.values().stream().noneMatch(code -> code.contains("javax.annotation.Resource")),
                "Spring 7 injection must use Jakarta annotations");
        assertTrue(files.get("src/main/java/org/ikasan/studio/boot/ModuleConfig.java").contains("jakarta.annotation.Resource"));
        assertTrue(files.get("pom.xml").contains("5.1.0"));
        assertFalse(files.get("pom.xml").contains("6.2.4"));
        return files;
    }

    static FlowElement element(String key, String name) throws Exception {
        FlowElement result = FlowElement.flowElementBuilder().componentMeta(ComponentLibrary.getIkasanComponentByKeyMandatory(PACK, key))
                .componentName(name).build();
        result.defaultUnsetMandatoryProperties();
        return result;
    }
    static void putJava(Map<String, String> files, String pkg, String name, String code) {
        files.put("src/main/java/" + pkg.replace('.', '/') + "/" + name + ".java", code);
    }
}
