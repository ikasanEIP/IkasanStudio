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
                        "user/src/main/java/org/customer/Other.java", "user/src/main/resources/bespoke.properties");
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
        assertThat(analysis.dependencies()).filteredOn(d -> !d.dependency().getArtifactId().equals("custom-client")).allMatch(d -> !d.importable());
        SourceModuleImport.apply(analysis, Set.of(), Set.of(custom.id()), false);
        var model = new MavenXpp3Reader().read(new StringReader(Files.readString(target().resolve("pom.xml"))));
        assertThat(model.getDependencies()).hasSize(1);
        assertThat(model.getDependencies().get(0).getVersion()).isEqualTo("2.3.4");
        assertThat(model.getDependencies().get(0).getExclusions().get(0).getArtifactId()).isEqualTo("parser");
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

    @Test void rejectsDestinationSymlinksAndHonoursCancellation() throws Exception {
        fixture(); var analysis = analyse();
        Files.createSymbolicLink(target().resolve("user"), source());
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of("src/main/java/org/customer/Bespoke.java"), Set.of(), false)).hasMessageContaining("symbolic links");
        assertThatThrownBy(() -> SourceModuleImport.analyse(source(), target(), () -> { throw new IllegalStateException("cancelled"); })).hasMessage("cancelled");
    }
}
