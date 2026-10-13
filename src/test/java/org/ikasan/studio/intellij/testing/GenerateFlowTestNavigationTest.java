package org.ikasan.studio.intellij.testing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GenerateFlowTestNavigationTest {
    @TempDir Path root;
    @Test void refreshOpensPreservedScenarioInsteadOfFirstSupportFile() throws Exception {
        Path support = Files.createFile(root.resolve("ModuleJmsTestConfig.java"));
        Path test = Files.createFile(root.resolve("RouterFlowFlowTest.java"));
        assertEquals(test, GenerateFlowTestAction.preferredTestToOpen(root,
                List.of(test.getFileName().toString()), List.of(support)));
        assertEquals(test, GenerateFlowTestAction.preferredTestToOpen(root,
                List.of(test.getFileName().toString()), List.of()));
    }
    @Test void multipleFlowsPreferNewScenarioOverPreservedScenarioAndSupport() throws Exception {
        Path support = Files.createFile(root.resolve("ModuleJmsTestConfig.java"));
        Files.createFile(root.resolve("ExistingTest.java"));
        Path fresh = Files.createFile(root.resolve("NewTest.java"));
        assertEquals(fresh, GenerateFlowTestAction.preferredTestToOpen(root,
                List.of("ExistingTest.java", "NewTest.java"), List.of(support, fresh)));
    }
    @Test void missingScenarioFallsBackToExistingWrittenFileOrNothing() throws Exception {
        Path properties = Files.createFile(root.resolve("module-test.properties"));
        assertEquals(properties, GenerateFlowTestAction.preferredTestToOpen(root,
                List.of("MissingTest.java"), List.of(properties)));
        assertNull(GenerateFlowTestAction.preferredTestToOpen(root, List.of("MissingTest.java"), List.of()));
    }
}
