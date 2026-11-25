package org.ikasan.studio.testing.packs;

import java.util.List;
import java.util.Map;
import org.ikasan.studio.SharedResourceExtension;
import org.ikasan.studio.core.generator.IndependentH2Template;
import org.ikasan.studio.core.generator.ProjectArtifactRenderer;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SharedResourceExtension.class)
@Tag("packs")
class IndependentH2GenerationTest {
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void generatesJavaLauncherUsingProjectRuntimeInsteadOfAnH2Pom(String pack) throws Exception {
        Module module = Module.moduleBuilder().version(pack).name("H2 example").applicationPackageName("org.example")
                .port("8080").h2PortNumber("8092").h2WebPortNumber("8093").flows(List.of()).build();
        Map<String,String> artifacts = ProjectArtifactRenderer.render(module,
                "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>h2-example</artifactId><version>1</version></project>");
        assertEquals(IndependentH2Template.create(), artifacts.get(IndependentH2Template.PATH));
        assertFalse(artifacts.containsKey("generated/h2/pom.xml"));
    }
}
