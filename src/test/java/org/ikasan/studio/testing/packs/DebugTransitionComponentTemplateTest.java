package org.ikasan.studio.testing.packs;

import java.io.IOException;
import java.util.ArrayList;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("packs")
public class DebugTransitionComponentTemplateTest {

    @BeforeAll
    static void warmUpTemplateEngine() throws StudioBuildException, StudioGeneratorException {
        // See ApplicationTemplateTest for why this warm-up call exists.
        Module module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, new ArrayList<>());
        DebugTransitionComponentTemplate.create(module);
    }

    /**
     * Expected output is recorded in the pack-specific {@code Module/DebugTransitionComponent.java} test fixture.
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    public void test_generateDebugTransitionComponentClass(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module ikasanModule = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        String templateString = DebugTransitionComponentTemplate.create(ikasanModule);

        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, ikasanModule, DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_CLASS_NAME + ".java"), templateString);
    }
}
