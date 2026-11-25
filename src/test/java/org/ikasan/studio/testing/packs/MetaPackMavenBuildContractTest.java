package org.ikasan.studio.testing.packs;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.ikasan.studio.core.generator.ProjectArtifactRenderer;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MetaPackMavenBuildContractTest {
    @ParameterizedTest
    @CsvSource({"V3.3.9,11", "V4.1.6,17", "V5.1.0,25"})
    void renderingUsesPackBuildDefaultsWithoutChangingTheApplicationLayout(String pack, String javaVersion) throws Exception {
        Module module = Module.moduleBuilder().version(pack).name("Build contract").applicationPackageName("org.example")
                .port("8080").h2PortNumber("8081").h2WebPortNumber("8082").flows(List.of()).build();
        Map<String,String> artifacts = ProjectArtifactRenderer.render(module,
                "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>contract</artifactId><version>1</version><modules><module>generated</module><module>user</module></modules></project>");
        Model pom = new MavenXpp3Reader().read(new StringReader(artifacts.get("pom.xml")));
        assertThat(pom.getProperties()).containsEntry("maven.compiler.source", javaVersion)
                .containsEntry("maven.compiler.target", javaVersion);
        assertThat(pom.getModules()).containsExactly("generated", "user");
        assertThat(pom.getBuild().getPlugins()).isEmpty();
        assertThat(pom.getBuild().getPluginManagement().getPlugins()).extracting(plugin ->
                plugin.getArtifactId() + ":" + plugin.getVersion()).containsExactly(
                "maven-compiler-plugin:3.8.0", "maven-surefire-plugin:3.2.5");
        assertThat(pom.getDependencyManagement().getDependencies()).anySatisfy(bom -> {
            assertThat(bom.getArtifactId()).isEqualTo("ikasan-eip-standalone-bom");
            assertThat(bom.getVersion()).isEqualTo(ComponentLibrary.getMetaPackManifest(pack).ikasanVersion());
        });
        assertThat(artifacts).containsKey("generated/src/main/java/org/ikasan/studio/boot/Application.java");
    }
}
