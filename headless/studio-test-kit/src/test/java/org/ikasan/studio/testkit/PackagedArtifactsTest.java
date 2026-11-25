package org.ikasan.studio.testkit;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.ikasan.studio.core.generator.ApplicationTemplate;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.MetaPackManifest;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PackagedArtifactsTest {
    @Test
    void consumesGeneratorAndPacksAsJarsWithoutIntellij() throws Exception {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.intellij.openapi.application.ApplicationManager"));
        assertTrue(ApplicationTemplate.class.getProtectionDomain().getCodeSource().getLocation().getPath().endsWith(".jar"));
        List<String> expected = List.of("V3.3.9", "V4.1.6", "V5.1.0");
        assertEquals(new HashSet<>(expected), new HashSet<>(ComponentLibrary.getMetapackList()));
        for (String version : expected) {
            URL resource = getClass().getClassLoader().getResource("studio/metapack/" + version + "/metapack.json");
            assertNotNull(resource);
            assertEquals("jar", resource.getProtocol());
            String artifact = version.startsWith("V3") ? "studio-pack-v3" : version.startsWith("V4") ? "studio-pack-v4" : "studio-pack-v5-release";
            MetaPackManifest manifest = ComponentLibrary.getMetaPackManifest(version);
            assertTrue(resource.toString().contains(artifact + "-" + manifest.packVersion() + ".jar!"),
                    "Pack resources must come from their individually versioned artifact: " + resource);
            Module sample = Module.moduleBuilder().version(version).name("Example")
                    .applicationPackageName("org.example").port("8080").h2PortNumber("8092")
                    .h2WebPortNumber("8093").flows(List.of()).build();
            Map<String,String> artifacts = MetaPackTestKit.render(sample);
            assertTrue(artifacts.get("Application.java").contains("class Application"));
            assertEquals(version, MetaPackTestKit.readModel(artifacts.get("model.json"), "jar sample").getVersion());
        }
    }
}
