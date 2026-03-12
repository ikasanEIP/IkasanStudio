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

public class ApplicationTemplateTest {

    @BeforeAll
    static void warmUpTemplateEngine() throws StudioBuildException, StudioGeneratorException {
        // Process a template here, before ThreadLeakTrackerExtension.beforeEach() captures the
        // per-test thread baseline. Classloader I/O during template loading lazily starts system
        // daemon threads (NIO EPoll, Java2D Disposer). Running a generation here ensures those
        // threads are already alive when beforeEach snapshots, so afterEach won't flag them.
        Module module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, new ArrayList<>());
        ApplicationTemplate.create(module);
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/Application.java
     * @throws IOException if the template cant be generated
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void test_generateApplicationClass(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module ikasanModule = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        String templateString = ApplicationTemplate.create(ikasanModule);

        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, ikasanModule, ApplicationTemplate.APPLICATION_CLASS_NAME + ".java"), templateString);
    }
}