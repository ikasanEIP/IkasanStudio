package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StudioInjectControllerTemplateTest {

    @BeforeAll
    static void warmUpTemplateEngine() throws StudioBuildException, StudioGeneratorException {
        // See ApplicationTemplateTest for why this warm-up call exists.
        Module module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, new ArrayList<>());
        StudioInjectControllerTemplate.create(module);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void exportLocalFileTestController(String version) throws Exception {
        var module = TestFixtures.getMyFirstModuleIkasanModule(version, new ArrayList<>());
        String generated = StudioInjectControllerTemplate.create(module);
        org.junit.jupiter.api.Assertions.assertTrue(generated.contains("ScheduledComponent && !selectedLocalFiles"));
        org.junit.jupiter.api.Assertions.assertTrue(generated.contains("invokeInTransaction(flow, event)"));
        String export = System.getenv("STUDIO_INJECT_EXPORT");
        if (export != null) {
            var directory = java.nio.file.Path.of(export, version);
            java.nio.file.Files.createDirectories(directory);
            java.nio.file.Files.writeString(directory.resolve("StudioInjectController.java"), generated);
        }
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/StudioInjectController.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void test_generateStudioInjectControllerClass(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module ikasanModule = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        String templateString = StudioInjectControllerTemplate.create(ikasanModule);

        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, ikasanModule, StudioInjectControllerTemplate.STUDIO_INJECT_CONTROLLER_CLASS_NAME + ".java"), templateString);
    }
}
