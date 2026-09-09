package org.ikasan.studio.testing.engine;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@org.junit.jupiter.api.Tag("engine")
class EngineTestBoundaryTest {
    @Test
    void engineTestsDoNotDependOnPackFixturesOrIntellij() {
        var tests = new ClassFileImporter().importPackages("org.ikasan.studio.testing.engine");
        noClasses().should().dependOnClassesThat().resideInAnyPackage(
                "org.ikasan.studio.testing.packs..", "org.ikasan.studio.ui..",
                "org.ikasan.studio.intellij..", "com.intellij..").check(tests);
        noClasses().should().dependOnClassesThat()
                .haveFullyQualifiedName("org.ikasan.studio.core.TestFixtures").check(tests);
    }
}
