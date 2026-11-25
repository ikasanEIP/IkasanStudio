package org.ikasan.studio.testing.engine;

import java.util.HashMap;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.generator.StudioGeneratorException;
import org.ikasan.studio.core.io.ComponentIO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("engine")
class GeneratorFailureInjectionTest {
    @Test
    void missingMetaPackResourceFailsExplicitly() {
        assertThrows(StudioBuildException.class, () -> ComponentIO.deserializeResource(
                "/studio/metapack/missing-failure-injection/components/library.json", Object.class));
    }

    @Test
    void missingTemplateFailsExplicitly() {
        StudioGeneratorException failure = assertThrows(StudioGeneratorException.class, () -> FreemarkerUtils.generateFromTemplate(
                "FailureInjection", "missing.ftl", new HashMap<>()));
        assertNotNull(failure.getCause());
    }

    @Test
    void brokenTemplateCannotReturnPartialOutput() {
        StudioGeneratorException failure = assertThrows(StudioGeneratorException.class, () -> FreemarkerUtils.generateFromTemplate(
                "FailureInjection", "broken.ftl", new HashMap<>()));
        assertNotNull(failure.getCause());
    }
}
