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
        assertTrue(ComponentLibrary.getMetapackList().containsAll(List.of("V3.3.9", "V4.1.6")));
        for (String version : List.of("V3.3.9", "V4.1.6")) {
            var resource = getClass().getClassLoader().getResource("studio/metapack/" + version + "/metapack.json");
            assertNotNull(resource);
            assertEquals("jar", resource.getProtocol());
            String artifact = version.startsWith("V3") ? "studio-pack-v3" : "studio-pack-v4";
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
