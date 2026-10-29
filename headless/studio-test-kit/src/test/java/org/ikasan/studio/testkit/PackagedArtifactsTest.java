package org.ikasan.studio.testkit;

import org.ikasan.studio.core.generator.ApplicationTemplate;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PackagedArtifactsTest {
    @Test
    void consumesGeneratorAndPacksAsJarsWithoutIntellij() throws Exception {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.intellij.openapi.application.ApplicationManager"));
        assertTrue(ApplicationTemplate.class.getProtectionDomain().getCodeSource().getLocation().getPath().endsWith(".jar"));
        var expected = Boolean.getBoolean("studio.includeSnapshotPacks")
                ? List.of("V3.3.9", "V4.1.6", "V5.1.0", "V5.0.1-SNAPSHOT") : List.of("V3.3.9", "V4.1.6", "V5.1.0");
        assertEquals(new java.util.HashSet<>(expected), new java.util.HashSet<>(ComponentLibrary.getMetapackList()));
        for (String version : expected) {
            var resource = getClass().getClassLoader().getResource("studio/metapack/" + version + "/metapack.json");
            assertNotNull(resource);
            assertEquals("jar", resource.getProtocol());
            String artifact = version.startsWith("V3") ? "studio-pack-v3" : version.startsWith("V4") ? "studio-pack-v4" : version.equals("V5.1.0") ? "studio-pack-v5-release" : "studio-pack-v5";
            var manifest = ComponentLibrary.getMetaPackManifest(version);
            assertTrue(resource.toString().contains(artifact + "-" + manifest.packVersion() + ".jar!"),
                    "Pack resources must come from their individually versioned artifact: " + resource);
            var sample = Module.moduleBuilder().version(version).name("Example")
                    .applicationPackageName("org.example").port("8080").h2PortNumber("8092")
                    .h2WebPortNumber("8093").flows(List.of()).build();
            var artifacts = MetaPackTestKit.render(sample);
            assertTrue(artifacts.get("Application.java").contains("class Application"));
            assertEquals(version, MetaPackTestKit.readModel(artifacts.get("model.json"), "jar sample").getVersion());
        }
    }
}
