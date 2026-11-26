package org.ikasan.studio.testing.packs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.project.DistributionScaffolding;
import org.ikasan.studio.core.project.DistributionScaffolding.FileChange;
import org.ikasan.studio.core.project.StudioProjectLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Map;
import java.util.stream.Stream;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.ModelTemplate;
import org.ikasan.studio.core.migration.MigrationArtifacts;
import org.ikasan.studio.core.migration.MigrationWorkspace.Change;
import org.ikasan.studio.core.migration.MigrationWorkspace.Snapshot;
import org.ikasan.studio.core.migration.MigrationWorkspace;
import org.ikasan.studio.core.migration.ModelMigration.Plan;
import org.ikasan.studio.core.migration.ModelMigration;
import org.ikasan.studio.core.model.ikasan.instance.Module;

class ProjectDistributionLayoutTest {
    @TempDir Path root;
    private static final Path ARCHETYPE = Path.of("ikasan-studio-ancillary/ikasan-studio-project-archetype/src/main/resources/archetype-resources");

    private void createProject() throws Exception {
        for (String path : List.of("pom.xml", "jar/pom.xml", "jar/generated/pom.xml", "jar/user/pom.xml", "jar/.ikasan-studio-application", "distribution/pom.xml")) {
            Path target = root.resolve(path);
            Files.createDirectories(target.getParent());
            Files.writeString(target, Files.readString(ARCHETYPE.resolve(path))
                    .replace("${groupId}", "org.example").replace("${rootArtifactId}", "orders")
                    .replace("${artifactId}", "orders").replace("${version}", "1.0-SNAPSHOT"));
        }
    }
    private void apply(List<FileChange> changes) throws Exception {
        for (FileChange change : changes) {
            Path path = root.resolve(change.path());
            Files.createDirectories(path.getParent());
            Files.writeString(path, change.content());
        }
    }

    @ParameterizedTest @ValueSource(strings={"V3.3.9", "V4.1.6", "V5.1.0"})
    void distributionTracksSelectedPackWithoutReplacingDeveloperCustomisations(String pack) throws Exception {
        createProject();
        assertThat(StudioProjectLayout.applicationRoot(root)).isEqualTo(root.resolve("jar"));
        assertThat(StudioProjectLayout.applicationRoot(root.resolve("jar"))).isEqualTo(root.resolve("jar"));
        String original = Files.readString(root.resolve("pom.xml"));
        Files.writeString(root.resolve("pom.xml"), original.replace("</properties>", "<company.setting>keep</company.setting></properties>"));
        List<FileChange> changes = DistributionScaffolding.prepare(root, ComponentLibrary.getMetaPackManifest(pack));
        assertThat(Files.exists(root.resolve("bin/ikasan.sh"))).isFalse();
        apply(changes);
        String parent = Files.readString(root.resolve("pom.xml"));
        assertThat(parent).contains("<company.setting>keep</company.setting>", "<module>jar</module>", "<module>distribution</module>");
        assertThat(parent).contains("<version.ikasan>" + ComponentLibrary.getMetaPackManifest(pack).ikasanVersion());
        assertThat(Files.readString(root.resolve("distribution/pom.xml"))).contains("maven-assembly-plugin", "${studio.application.artifactId}");
        assertThat(Files.readString(root.resolve("distribution/distribution.xml"))).contains("<scope>runtime</scope>", "../bin", "../jar/generated/src/main/resources");
        assertThat(DistributionScaffolding.prepare(root, ComponentLibrary.getMetaPackManifest(pack))).isEmpty();
        Path parentPom = root.resolve("pom.xml");
        String customisedFormatting = Files.readString(parentPom).replace("<properties>", "<!-- Team notes -->\n    <properties>");
        Files.writeString(parentPom, customisedFormatting);
        assertThat(DistributionScaffolding.prepare(root, ComponentLibrary.getMetaPackManifest(pack))).isEmpty();
        assertThat(Files.readString(parentPom)).isEqualTo(customisedFormatting);
        Files.writeString(root.resolve("bin/ikasan.sh"), "custom launcher\n");
        Files.writeString(root.resolve("distribution/distribution.xml"), "custom assembly\n");
        assertThat(DistributionScaffolding.prepare(root, ComponentLibrary.getMetaPackManifest(pack))).isEmpty();
        assertThat(Files.readString(root.resolve("bin/ikasan.sh"))).isEqualTo("custom launcher\n");
    }

    @Test void reviewedMigrationAndRestoreCoverBothApplicationAndDeploymentContracts() throws Exception {
        createProject();
        apply(DistributionScaffolding.prepare(root, ComponentLibrary.getMetaPackManifest("V3.3.9")));
        Module module = TestFixtures.getMyFirstModuleIkasanModule("V3.3.9", List.of());
        String source = ModelTemplate.create(module);
        Path model = root.resolve("jar/generated/src/main/model/model.json");
        Files.createDirectories(model.getParent());
        Files.writeString(model, source);
        byte[] originalParent = Files.readAllBytes(root.resolve("pom.xml"));
        byte[] originalApplication = Files.readAllBytes(root.resolve("jar/pom.xml"));
        Plan plan = ModelMigration.analyse(source, "V4.1.6");
        assertThat(plan.canApply()).isTrue();
        Map<String, String> artifacts = MigrationArtifacts.render(root, plan,
                Files.readString(root.resolve("jar/pom.xml")));
        assertThat(artifacts).containsKeys("pom.xml", "jar/pom.xml", "jar/generated/src/main/model/model.json");
        List<Change> changes = MigrationWorkspace.prepare(root, artifacts);
        MigrationWorkspace.commit(root, changes, plan.report());
        assertThat(Files.readString(root.resolve("pom.xml"))).contains("<version.ikasan>4.1.6</version.ikasan>");
        Snapshot snapshot = MigrationWorkspace.latest(root);
        MigrationWorkspace.commit(root,
                MigrationWorkspace.prepareRestore(root, snapshot), "restore");
        assertThat(Files.readAllBytes(root.resolve("pom.xml"))).isEqualTo(originalParent);
        assertThat(Files.readAllBytes(root.resolve("jar/pom.xml"))).isEqualTo(originalApplication);
        assertThat(Files.readString(model)).isEqualTo(source);
    }

    @Test void legacyAndUnrelatedJarDirectoriesAreNotConverted() throws Exception {
        Files.createDirectories(root.resolve("jar"));
        assertThat(StudioProjectLayout.applicationRoot(root)).isEqualTo(root);
        assertThat(DistributionScaffolding.prepare(root, ComponentLibrary.getMetaPackManifest("V4.1.6"))).isEmpty();
        try (Stream<Path> entries = Files.list(root)) {
            assertThat(entries.map(p -> p.getFileName().toString()).toList()).containsExactly("jar");
        }
    }

    @Test void switchingPackUpdatesOnlyManagedSettingsAndPreservesExplicitAssemblyVersion() throws Exception {
        createProject();
        apply(DistributionScaffolding.prepare(root, ComponentLibrary.getMetaPackManifest("V3.3.9")));
        Path distribution = root.resolve("distribution/pom.xml");
        Files.writeString(distribution, Files.readString(distribution).replace("<version>3.7.1</version>", "<version>3.7.0</version>"));
        apply(DistributionScaffolding.prepare(root, ComponentLibrary.getMetaPackManifest("V4.1.6")));
        assertThat(Files.readString(root.resolve("pom.xml"))).contains("<maven.compiler.source>17</maven.compiler.source>", "<version.ikasan>4.1.6</version.ikasan>");
        assertThat(Files.readString(distribution)).contains("<version>3.7.0</version>");
    }
}
