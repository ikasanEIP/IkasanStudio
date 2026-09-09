package org.ikasan.studio.testing.engine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeadlessRuntimeTest {
    @Test
    void generatorRunsWithoutIntellijOrOfficialPacksOnItsClasspath() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.intellij.openapi.application.ApplicationManager"));
        assertNull(getClass().getClassLoader().getResource("studio/metapack/V3.3.9/metapack.json"));
        assertNull(getClass().getClassLoader().getResource("studio/metapack/V4.1.6/metapack.json"));
    }
}
