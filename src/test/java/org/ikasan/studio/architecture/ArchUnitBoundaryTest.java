package org.ikasan.studio.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

/** Bytecode-level dependency rules for the production architecture. */
class ArchUnitBoundaryTest {
    private static final String ROOT = "org.ikasan.studio";
    private static JavaClasses productionClasses;

    @BeforeAll
    static void importProductionClasses() {
        productionClasses = new ClassFileImporter()
                .importPath(Path.of("build/classes/java/main"));
    }

    @Test
    void coreIsIndependentOfUiIntellijAndTheIntellijPlatform() {
        classes().that().resideInAPackage("..core..")
                .should().onlyDependOnClassesThat().resideOutsideOfPackages(
                        "..ui..", "..intellij..", "com.intellij..")
                .because("core must remain usable without an IntelliJ runtime")
                .check(productionClasses);
    }

    @Test
    void domainModelIsIndependentOfPersistenceAndViewAdapters() {
        classes().that().resideInAPackage("..core.model..")
                .should().onlyDependOnClassesThat().resideOutsideOfPackages(
                        "..core.persistence..", "..ui.viewmodel..", "..ui.view..")
                .because("the version-neutral domain model owns neither storage nor presentation")
                .check(productionClasses);
    }

    @Test
    void integrationsAreIndependentOfUiAndIntellijAdapters() {
        classes().that().resideInAPackage("..integration..")
                .should().onlyDependOnClassesThat().resideOutsideOfPackages("..ui..", "..intellij..")
                .because("integration clients must be independently testable")
                .check(productionClasses);
    }

    @Test
    void platformHeavyApisRemainBehindKnownAdapters() {
        // Finite migration list: new PSI/VFS/execution dependencies must go in intellij; remove entries as
        // these existing UI classes acquire focused adapters.
        Set<String> transitionalUiAdapters = Set.of(
                "org.ikasan.studio.ui.actions.DeleteComponentAction",
                "org.ikasan.studio.ui.actions.LaunchApplicationAction",
                "org.ikasan.studio.ui.actions.LaunchH2Action",
                "org.ikasan.studio.ui.actions.OpenTestFtpFileAction",
                "org.ikasan.studio.ui.actions.SaveAction",
                "org.ikasan.studio.ui.actions.SendTestMessageAction",
                "org.ikasan.studio.ui.actions.SendTestMessagePayloadDialog",
                "org.ikasan.studio.ui.actions.ShowTestFtpDirectoryAction",
                "org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel",
                "org.ikasan.studio.ui.component.properties.ComponentPropertyEditRow",
                "org.ikasan.studio.ui.viewmodel.AbstractViewHandlerIntellij");

        classes().should(keepPlatformHeavyApisBehindAdapters(transitionalUiAdapters))
                .because("PSI, VFS and execution are IntelliJ adapter responsibilities")
                .check(productionClasses);
    }

    @Test
    void globalStaticFieldsDoNotHoldProjectSpecificState() {
        fields().that().areStatic()
                .should(notHoldProjectSpecificState())
                .because("simultaneously open projects must not share global project context")
                .check(productionClasses);
    }

    private static ArchCondition<JavaClass> keepPlatformHeavyApisBehindAdapters(Set<String> transitionalAdapters) {
        return new ArchCondition<>("keep platform-heavy APIs behind dedicated adapters") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean adapter = javaClass.getPackageName().startsWith(ROOT + ".intellij")
                        || transitionalAdapters.stream().anyMatch(name -> javaClass.getName().equals(name)
                                || javaClass.getName().startsWith(name + Character.toString(36)));
                if (adapter) return;
                javaClass.getDirectDependenciesFromSelf().stream()
                        .filter(dependency -> {
                            String target = dependency.getTargetClass().getPackageName();
                            return target.startsWith("com.intellij.psi")
                                    || target.startsWith("com.intellij.openapi.vfs")
                                    || target.startsWith("com.intellij.execution");
                        })
                        .forEach(dependency -> events.add(SimpleConditionEvent.violated(javaClass,
                                dependency.getDescription())));
            }
        };
    }

    private static ArchCondition<JavaField> notHoldProjectSpecificState() {
        return new ArchCondition<>("not hold Project, module, path or classloader state") {
            private final Set<String> forbiddenTypes = Set.of(
                    "com.intellij.openapi.project.Project",
                    "com.intellij.openapi.module.Module",
                    "com.intellij.openapi.vfs.VirtualFile",
                    "org.ikasan.studio.core.model.ikasan.instance.Module",
                    "java.nio.file.Path",
                    "java.io.File",
                    "java.lang.ClassLoader");

            @Override
            public void check(JavaField field, ConditionEvents events) {
                boolean projectSpecific = field.getType().getAllInvolvedRawTypes().stream()
                        .map(JavaClass::getName)
                        .anyMatch(forbiddenTypes::contains);
                if (projectSpecific) {
                    events.add(SimpleConditionEvent.violated(field,
                            field.getFullName() + " is global project-specific state"));
                }
            }
        };
    }
}
