package org.ikasan.studio.packs;

import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.ikasan.studio.core.generator.FlowTestScaffold;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.maven.IkasanPomModel;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.StringReader;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Verifies real archetype module boundaries rather than a consolidated compilation POM. */
class ReactorFixtureTest {
    @TempDir Path temporary;
    private static final Path ARCHETYPE = Path.of("ikasan-studio-ancillary/ikasan-studio-project-archetype/src/main/resources/archetype-resources");

    @Test void exportsArchetypeReactorWithGeneratedFlowTests() throws Exception {
        var source = RuntimeTransportFixtureTest.render("smtp-generated");
        var module = ComponentIO.deserializeModuleInstanceString(source.get("generated/src/main/model/model.json"), "V5 reactor fixture");
        var rootModel = new MavenXpp3Reader().read(new StringReader(archetype("pom.xml")));
        var rootPom = new IkasanPomModel(rootModel);
        rootPom.applyBuildContract(ComponentLibrary.getMetaPackManifest(DevelopmentFixtureTest.PACK));
        for (var dependency : module.getAllUniqueSortedJarDependencies()) {
            var managed = dependency.clone();
            if ("org.ikasan".equals(managed.getGroupId())) managed.setVersion(null);
            rootPom.checkIfDependancyAlreadyExists(managed);
        }
        rootModel.addModule("user-flow-tests");
        String parent = rootPom.getModelAsString();
        String application = archetype("generated/pom.xml");
        var scaffold = FlowTestScaffold.render(module, module.getFlows().get(0), parent, application);
        var files = new LinkedHashMap<String, String>();
        files.put("pom.xml", parent);
        files.put("user/pom.xml", archetype("user/pom.xml"));
        files.put("generated/pom.xml", application);
        files.put("user-flow-tests/pom.xml", scaffold.files().get("user-flow-tests/pom.xml"));
        for (var file : source.entrySet()) {
            String path = file.getKey();
            if (path.startsWith("src/main/java/org/example/")) files.put("user/" + path, file.getValue());
            else if (path.startsWith("src/main/")) files.put("generated/" + path, file.getValue());
            else if (path.startsWith("src/test/")) files.put("user-flow-tests/" + path, file.getValue());
            else if (path.startsWith("generated/")) files.put(path, file.getValue());
        }
        assertEquals("25", rootPom.getProperty("maven.compiler.source"));
        assertTrue(files.get("user-flow-tests/pom.xml").contains("<artifactId>generated</artifactId>"));
        assertTrue(files.containsKey("user/src/main/java/org/example/transport/transport/TransportConverter.java"));
        assertFalse(files.containsKey("generated/src/main/java/org/example/transport/transport/TransportConverter.java"));
        Path destination = Path.of(System.getProperty("studio.v5.reactor", temporary.toString()));
        for (var file : files.entrySet()) {
            Path target = destination.resolve(file.getKey());
            Files.createDirectories(target.getParent()); Files.writeString(target, file.getValue());
        }
    }

    private String archetype(String path) throws Exception {
        return Files.readString(ARCHETYPE.resolve(path)).replace("${groupId}", "org.example.v5review")
                .replace("${rootArtifactId}", "v5-reactor").replace("${artifactId}", "v5-reactor")
                .replace("${version}", "1.0-SNAPSHOT");
    }
}
