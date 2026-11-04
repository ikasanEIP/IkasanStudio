package org.ikasan.studio.core.importer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.StringReader;
import java.nio.file.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class SourceModuleImportTest {
    @TempDir Path temporary;
    private Path source() { return temporary.resolve("original"); }
    private Path target() { return temporary.resolve("studio"); }
    private void write(Path root, String relative, String text) throws Exception {
        Path file = root.resolve(relative); Files.createDirectories(file.getParent()); Files.writeString(file, text);
    }
    private void fixture() throws Exception {
        write(target(), "pom.xml", "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>studio</artifactId><version>1</version></project>");
        write(target(), "generated/src/main/java/studio/ComponentFactory.java", """
                package studio;
                class ComponentFactory {
                    @jakarta.annotation.Resource org.customer.Bespoke renamedBean;
                    @javax.annotation.Resource(name="explicitBean") org.customer.Other other;
                    @javax.annotation.Resource org.ikasan.builder.BuilderFactory builderFactory;
                }
                """);
        write(source(), "src/main/java/org/customer/Bespoke.java", "package org.customer;\r\n@org.springframework.context.annotation.PropertySource(\"classpath:bespoke.properties\")\r\npublic class Bespoke { Helper helper; }\r\n");
        write(source(), "src/main/java/org/customer/Helper.java", "package org.customer; public class Helper { }");
        write(source(), "src/main/java/org/customer/Other.java", "package org.customer; public class Other { }");
        write(source(), "src/main/java/org/customer/Unrelated.java", "package org.customer; public class Unrelated { }");
        write(source(), "src/main/java/org/customer/OriginalConfig.java", """
                package org.customer;
                @org.springframework.context.annotation.Configuration
                class OriginalConfig {
                    @org.springframework.context.annotation.Bean Bespoke oldBeanName() { return new Bespoke(); }
                }
                """);
        write(source(), "src/main/java/org/customer/Application.java", "package org.customer; @org.springframework.boot.autoconfigure.SpringBootApplication class Application { }");
        write(source(), "src/main/resources/application-dev.properties", "secret=${CUSTOM_SECRET}\n");
        write(source(), "src/main/resources/bespoke.properties", "custom.value=preserved\n");
        write(source(), "config/customer.properties", "custom.endpoint=https://customer\n");
        write(source(), "target/src/main/java/Fake.java", "not Java");
        write(source(), "src/test/resources/config/application-prod.properties", "test.only=should-not-import");
        write(source(), "pom.xml", """
                <project><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>original</artifactId><version>1</version>
                <properties><custom.version>2.3.4</custom.version></properties><dependencies>
                <dependency><groupId>customer</groupId><artifactId>custom-client</artifactId><version>${custom.version}</version>
                  <exclusions><exclusion><groupId>old.xml</groupId><artifactId>parser</artifactId></exclusion></exclusions>
                </dependency>
                <dependency><groupId>customer</groupId><artifactId>inherited</artifactId></dependency>
                <dependency><groupId>org.ikasan</groupId><artifactId>ikasan-parent</artifactId><version>old</version></dependency>
                </dependencies></project>
                """);
    }
    private SourceModuleImport.Analysis analyse() throws Exception { return SourceModuleImport.analyse(source(), target(), () -> { }); }

    @Test void recommendsExactImplementationsAndHelpersButRequiresReviewOfOriginalWiring() throws Exception {
        fixture(); var analysis = analyse();
        assertThat(analysis.files().stream().filter(SourceModuleImport.FileChoice::recommended).map(SourceModuleImport.FileChoice::destination))
                .containsExactlyInAnyOrder("user/src/main/java/org/customer/Bespoke.java", "user/src/main/java/org/customer/Helper.java",
                        "user/src/main/java/org/customer/Other.java", "user/src/main/java/org/customer/Unrelated.java", "user/src/main/resources/bespoke.properties");
        assertThat(analysis.files()).filteredOn(f -> f.source().endsWith("OriginalConfig.java"))
                .allMatch(f -> f.wiring() && !f.recommended());
        assertThat(analysis.files()).noneMatch(f -> f.source().endsWith("Application.java") || f.source().contains("target/"));
        assertThat(analysis.beans()).extracting(SourceModuleImport.BeanRequirement::name).containsExactly("renamedBean", "explicitBean");
        assertThat(analysis.beans().get(0).sourceDefinitions()).anyMatch(d -> d.contains("OriginalConfig.java:4") && d.contains("oldBeanName"));
        assertThat(analysis.properties()).extracting(SourceModuleImport.PropertyFile::environment).contains("dev", "").doesNotContain("prod");
        String report = SourceModuleImport.report(analysis, Set.of(), Set.of());
        assertThat(report).contains("renamedBean", "explicitBean", "first delivery", "later delivery", "not evidence")
                .doesNotContain("CUSTOM_SECRET", "https://customer", "custom.value=preserved");
        assertThat(target().resolve("user")).doesNotExist();
    }

    @Test void transferPreservesOriginalBytesResourcesAndPackagesAndLeavesGeneratedAndOriginalUntouched() throws Exception {
        fixture(); var analysis = analyse();
        byte[] before = Files.readAllBytes(source().resolve("src/main/java/org/customer/Bespoke.java"));
        String generated = Files.readString(target().resolve("generated/src/main/java/studio/ComponentFactory.java"));
        Set<String> files = new LinkedHashSet<>(); analysis.files().stream().filter(SourceModuleImport.FileChoice::recommended).forEach(f -> files.add(f.source()));
        Path report = SourceModuleImport.apply(analysis, files, Set.of(), false);
        assertThat(Files.readAllBytes(target().resolve("user/src/main/java/org/customer/Bespoke.java"))).isEqualTo(before);
        assertThat(Files.readAllBytes(source().resolve("src/main/java/org/customer/Bespoke.java"))).isEqualTo(before);
        assertThat(Files.readString(target().resolve("generated/src/main/java/studio/ComponentFactory.java"))).isEqualTo(generated);
        assertThat(target().resolve("user/src/main/resources/bespoke.properties")).exists();
        assertThat(target().resolve("user/src/main/java/org/customer/OriginalConfig.java")).doesNotExist();
        assertThat(report).exists();
    }

    @Test void dependenciesKeepPinnedVersionsAndExclusionsWithoutGuessingManagedOrPlatformVersions() throws Exception {
        fixture(); var analysis = analyse();
        var custom = analysis.dependencies().stream().filter(d -> d.dependency().getArtifactId().equals("custom-client")).findFirst().orElseThrow();
        assertThat(custom.importable()).isTrue();
        assertThat(analysis.dependencies()).filteredOn(d -> d.dependency().getGroupId().equals("org.ikasan")).allMatch(d -> !d.importable());
        assertThat(analysis.dependencies()).filteredOn(d -> d.dependency().getArtifactId().equals("inherited")).allMatch(SourceModuleImport.DependencyChoice::importable);
        SourceModuleImport.apply(analysis, Set.of(), Set.of(custom.id()), false);
        var model = new MavenXpp3Reader().read(new StringReader(Files.readString(target().resolve("pom.xml"))));
        assertThat(model.getDependencies()).hasSize(1);
        assertThat(model.getDependencies().get(0).getVersion()).isEqualTo("2.3.4");
        assertThat(model.getDependencies().get(0).getExclusions().get(0).getArtifactId()).isEqualTo("parser");
    }

    @Test void inheritedVersionsAndOptionalFlagsAreRetainedWithoutInventingVersions() throws Exception {
        fixture();
        String originalPom = Files.readString(source().resolve("pom.xml"));
        write(source(), "pom.xml", originalPom.replace("<artifactId>inherited</artifactId>", "<artifactId>inherited</artifactId><optional>true</optional><scope>runtime</scope>"));
        var analysis = analyse();
        var inherited = analysis.dependencies().stream().filter(d -> d.dependency().getArtifactId().equals("inherited")).findFirst().orElseThrow();
        assertThat(inherited.importable()).isTrue();
        SourceModuleImport.apply(analysis, Set.of(), Set.of(inherited.id()), false);
        var model = new MavenXpp3Reader().read(new StringReader(Files.readString(target().resolve("pom.xml"))));
        var dependency = model.getDependencies().get(0);
        assertThat(dependency.getVersion()).isNull();
        assertThat(dependency.isOptional()).isTrue();
        assertThat(dependency.getScope()).isEqualTo("runtime");
    }

    @Test void directGeneratedConstructorReferencesAreRecognisedWithoutResourceFields() throws Exception {
        fixture();
        write(target(), "generated/src/main/java/studio/DirectFactory.java", "package studio; class DirectFactory { Object create() { return new org.customer.Unrelated(); } }");
        var analysis = analyse();
        assertThat(analysis.files()).anySatisfy(file -> {
            assertThat(file.source()).endsWith("Unrelated.java");
            assertThat(file.recommended()).isTrue();
            assertThat(file.reason()).contains("Referenced by generated code");
        });
    }

    @Test void newImplementationsDefaultToImportButWiringDuplicatesAndGeneratedDebugSubclassesDoNot() throws Exception {
        fixture();
        write(source(), "src/main/java/org/customer/FlowDebug.java", "package org.customer; class FlowDebug extends org.ikasan.studio.component.DebugTransitionComponent { public void debug(Object value) {} }");
        var analysis = analyse();
        assertThat(analysis.files()).anyMatch(f -> f.source().endsWith("Unrelated.java") && f.recommended() && f.previous() == null);
        assertThat(analysis.files()).anyMatch(f -> f.source().endsWith("FlowDebug.java") && !f.recommended());
        assertThat(analysis.files()).filteredOn(SourceModuleImport.FileChoice::wiring).noneMatch(SourceModuleImport.FileChoice::recommended);
    }

    @Test void dependencyComparisonIncludesUncheckedReviewOnlyDeclarationsWithoutApplyingThem() throws Exception {
        fixture(); var analysis = analyse();
        for (var dependency : analysis.dependencies()) {
            var comparison = SourceModuleImport.compareDependency(analysis, dependency.id());
            assertThat(comparison.original()).contains("<artifactId>" + dependency.dependency().getArtifactId() + "</artifactId>");
            assertThat(comparison.destination()).isEmpty();
        }
        assertThat(target().resolve("user")).doesNotExist();
        assertThat(Files.readString(target().resolve("pom.xml"))).doesNotContain("custom-client");
    }

    @Test void existingUserCodeNeedsExplicitReplacementAndStaleSourceOrDestinationBlocksAllWrites() throws Exception {
        fixture();
        write(target(), "user/src/main/java/org/customer/Bespoke.java", "developer work");
        var analysis = analyse();
        String bespoke = "src/main/java/org/customer/Bespoke.java";
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of(bespoke), Set.of(), false)).hasMessageContaining("Approve replacement");
        assertThat(Files.readString(target().resolve("user/src/main/java/org/customer/Bespoke.java"))).isEqualTo("developer work");
        write(target(), "user/src/main/java/org/customer/Bespoke.java", "new edits");
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of(bespoke), Set.of(), true)).hasMessageContaining("Destination changed");
        var refreshed = analyse(); write(source(), bespoke, "changed source");
        assertThatThrownBy(() -> SourceModuleImport.apply(refreshed, Set.of(bespoke), Set.of(), true)).hasMessageContaining("Original source changed");
        assertThat(target().resolve("user/import-reviews")).doesNotExist();
    }

    @Test void ambiguousClassNamesAreNotAutomaticallyRecommendedAndDuplicatesAreRejected() throws Exception {
        fixture();
        write(source(), "another/src/main/java/org/customer/Bespoke.java", "package org.customer; public class Bespoke { }");
        var analysis = analyse();
        assertThat(analysis.notes()).anyMatch(n -> n.contains("Ambiguous class"));
        assertThat(analysis.files()).filteredOn(f -> f.destination().endsWith("Bespoke.java")).noneMatch(SourceModuleImport.FileChoice::recommended);
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis,
                Set.of("another/src/main/java/org/customer/Bespoke.java", "src/main/java/org/customer/Bespoke.java"), Set.of(), false)).hasMessageContaining("Multiple sources");
        assertThat(target().resolve("user")).doesNotExist();
    }

    @Test void generatedContractChangesInvalidateReviewAndPartialWritesAreRolledBack() throws Exception {
        fixture(); var analysis = analyse();
        String bespoke = "src/main/java/org/customer/Bespoke.java";
        write(target(), "generated/src/main/java/studio/NewFactory.java", "class NewFactory { }");
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of(bespoke), Set.of(), false)).hasMessageContaining("Generated code changed");
        var refreshed = analyse();
        write(target(), "user/src/main/resources", "existing file blocks a resource directory");
        var selected = new LinkedHashSet<String>(); selected.add(bespoke); selected.add("src/main/resources/bespoke.properties");
        assertThatThrownBy(() -> SourceModuleImport.apply(refreshed, selected, Set.of(), false)).isInstanceOf(java.io.IOException.class);
        assertThat(target().resolve("user/src/main/java/org/customer/Bespoke.java")).doesNotExist();
        assertThat(Files.readString(target().resolve("user/src/main/resources"))).isEqualTo("existing file blocks a resource directory");
        assertThat(target().resolve("user/import-reviews")).doesNotExist();
    }

    private static final String DEBUG = "user/src/main/java/org/ikasan/studio/component/DebugTransitionComponent.java";
    private static final String COPY = "user/src/main/java/org/ikasan/studio/component/utils/DeepCopyUtil.java";
    private static final String MODEL = "generated/src/main/model/model.json";

    private void supportFixture() throws Exception { supportFixture("V4.1.6"); }

    private void supportFixture(String pack) throws Exception {
        fixture();
        var module = org.ikasan.studio.core.model.ikasan.instance.Module.moduleBuilder().version(pack)
                .name("Support example").applicationPackageName("org.example").port("8080")
                .h2PortNumber("8092").h2WebPortNumber("8093").flows(List.of()).build();
        write(target(), MODEL, org.ikasan.studio.core.generator.ModelTemplate.create(module));
        String debug = org.ikasan.studio.core.generator.DebugTransitionComponentTemplate.create(module);
        String copy = org.ikasan.studio.core.generator.DeepCopyUtilTemplate.create(module);
        write(target(), DEBUG, debug); write(target(), COPY, copy);
        write(source(), DEBUG, debug); write(source(), COPY, copy);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void supportVerificationUsesTheSavedPackAndIgnoresFormattingAndComments(String pack) throws Exception {
        supportFixture(pack);
        String original = "// Original checkout header\n" + Files.readString(source().resolve(DEBUG)).replace("    ", "\t");
        write(source(), DEBUG, original.replace("\n", "\r\n"));
        assertThat(analyse().managedFiles()).hasSize(2).allMatch(file -> file.status() == SourceModuleImport.SupportStatus.SUPPLIED);
    }

    @Test void supportClassesAreSuppliedSeparatelyWhileCustomerNamesAndReferencesRemainImportable() throws Exception {
        supportFixture();
        write(source(), "src/main/java/org/customer/DeepCopyUtil.java", "package org.customer; public class DeepCopyUtil { }");
        write(source(), "src/main/java/org/customer/Bespoke.java", "package org.customer; public class Bespoke extends org.ikasan.studio.component.DebugTransitionComponent { Helper helper; }");
        var analysis = analyse();
        assertThat(analysis.managedFiles()).hasSize(2).allMatch(f -> f.status() == SourceModuleImport.SupportStatus.SUPPLIED);
        assertThat(analysis.files()).noneMatch(f -> f.destination().equals(DEBUG) || f.destination().equals(COPY));
        assertThat(analysis.files()).anyMatch(f -> f.destination().equals("user/src/main/java/org/customer/DeepCopyUtil.java"));
        assertThat(analysis.files()).anyMatch(f -> f.source().endsWith("Helper.java") && f.recommended());
        String destination = Files.readString(target().resolve(DEBUG));
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of(DEBUG), Set.of(), true))
                .hasMessageContaining("Studio-managed support files cannot be imported");
        var report = SourceModuleImport.apply(analysis, Set.of("src/main/java/org/customer/Bespoke.java"), Set.of(), false);
        assertThat(Files.readString(target().resolve(DEBUG))).isEqualTo(destination);
        assertThat(Files.readString(report)).contains("Studio-managed support files (not imported)", "Supplied by Studio");
    }

    @Test void differingOriginalsAreFlaggedWithoutReplacingGeneratedSupport() throws Exception {
        supportFixture();
        String original = Files.readString(source().resolve(DEBUG)).replace("return payload;", "return null;");
        write(source(), DEBUG, original);
        var analysis = analyse();
        assertThat(analysis.managedFiles()).anyMatch(f -> f.source().equals(DEBUG) && f.status() == SourceModuleImport.SupportStatus.ORIGINAL_DIFFERS);
        SourceModuleImport.apply(analysis, Set.of(), Set.of(), true);
        assertThat(Files.readString(target().resolve(DEBUG))).contains("return payload;").doesNotContain("return null;");
        assertThat(Files.readString(source().resolve(DEBUG))).isEqualTo(original);
    }

    @Test void absentUnverifiedOrLocallyChangedDestinationSupportIsNotReportedAsSupplied() throws Exception {
        supportFixture();
        String generated = Files.readString(target().resolve(DEBUG));
        Files.delete(target().resolve(DEBUG));
        assertThat(analyse().managedFiles()).anyMatch(f -> f.source().equals(DEBUG) && f.status() == SourceModuleImport.SupportStatus.DESTINATION_MISSING);
        write(target(), DEBUG, generated.replace("return payload;", "return null;"));
        assertThat(analyse().managedFiles()).anyMatch(f -> f.source().equals(DEBUG) && f.status() == SourceModuleImport.SupportStatus.DESTINATION_DIFFERS);
        Files.delete(target().resolve(MODEL));
        assertThat(analyse().managedFiles()).anyMatch(f -> f.source().equals(DEBUG) && f.status() == SourceModuleImport.SupportStatus.UNVERIFIED);
    }

    @Test void sourceSupportDestinationSupportAndSavedModelChangesInvalidateReviewBeforeWrites() throws Exception {
        supportFixture();
        for (Path path : List.of(source().resolve(DEBUG), target().resolve(COPY), target().resolve(MODEL))) {
            var analysis = analyse(); String before = Files.readString(path);
            Files.writeString(path, before + "\n");
            assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of("src/main/java/org/customer/Bespoke.java"), Set.of(), false))
                    .hasMessageContaining("changed since");
            assertThat(target().resolve("user/src/main/java/org/customer/Bespoke.java")).doesNotExist();
            assertThat(target().resolve("user/import-reviews")).doesNotExist();
            Files.writeString(path, before);
        }
    }

    @Test void rejectsDestinationSymlinksAndHonoursCancellation() throws Exception {
        fixture(); var analysis = analyse();
        Files.createSymbolicLink(target().resolve("user"), source());
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of("src/main/java/org/customer/Bespoke.java"), Set.of(), false)).hasMessageContaining("symbolic links");
        assertThatThrownBy(() -> SourceModuleImport.analyse(source(), target(), () -> { throw new IllegalStateException("cancelled"); })).hasMessage("cancelled");
    }
}
