package org.ikasan.studio.testing.engine;

import org.ikasan.studio.core.generator.*;

import org.junit.jupiter.api.Test;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.StudioBuildException;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.Tag("engine")
class GeneratorFailureInjectionTest {
    @Test
    void missingMetaPackResourceFailsExplicitly() {
        assertThrows(StudioBuildException.class, () -> ComponentIO.deserializeResource(
                "/studio/metapack/missing-failure-injection/components/library.json", Object.class));
    }

    @Test
    void missingTemplateFailsExplicitly() {
        var failure = assertThrows(StudioGeneratorException.class, () -> FreemarkerUtils.generateFromTemplate(
                "FailureInjection", "missing.ftl", new HashMap<>()));
        assertNotNull(failure.getCause());
    }

    @Test
    void brokenTemplateCannotReturnPartialOutput() {
        var failure = assertThrows(StudioGeneratorException.class, () -> FreemarkerUtils.generateFromTemplate(
                "FailureInjection", "broken.ftl", new HashMap<>()));
        assertNotNull(failure.getCause());
    }
}
