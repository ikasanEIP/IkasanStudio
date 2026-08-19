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

public class DeepCopyUtilTemplateTest {

    @BeforeAll
    static void warmUpTemplateEngine() throws StudioBuildException, StudioGeneratorException {
        // See ApplicationTemplateTest for why this warm-up call exists.
        Module module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, new ArrayList<>());
        DeepCopyUtilTemplate.create(module);
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/{version}/Module/DeepCopyUtil.java
     */
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.core.TestFixtures#metaPacksToTest")
    public void test_generateDeepCopyUtilClass(String metaPackVersion) throws IOException, StudioBuildException, StudioGeneratorException {
        Module ikasanModule = TestFixtures.getMyFirstModuleIkasanModule(metaPackVersion, new ArrayList<>());
        String templateString = DeepCopyUtilTemplate.create(ikasanModule);

        assertNotNull(templateString);
        assertEquals(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(metaPackVersion, ikasanModule, DeepCopyUtilTemplate.DEEP_COPY_UTIL_CLASS_NAME + ".java"), templateString);
    }
}
