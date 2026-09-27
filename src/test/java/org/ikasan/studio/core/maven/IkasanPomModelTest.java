package org.ikasan.studio.core.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IkasanPomModelTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"3.3.9,11", "4.1.6,17"})
    void detectsBuildSettingsWithoutNewDependenciesAndPreservesUnrelatedConfiguration(String version, String javaVersion) {
        Model model = new Model();
        model.setGroupId("example");
        model.setArtifactId("imported-module");
        model.addProperty("maven.compiler.source", "1.7");
        model.addProperty("maven.compiler.target", "1.7");
        model.addProperty("maven.compiler.release", "7");
        model.addProperty("team.setting", "keep");
        model.addModule("user");
        var build = new org.apache.maven.model.Build();
        var plugin = new org.apache.maven.model.Plugin();
        plugin.setArtifactId("maven-compiler-plugin");
        plugin.setVersion("3.8.0");
        build.addPlugin(plugin);
        model.setBuild(build);
        Dependency dependency = new Dependency();
        dependency.setGroupId("example");
        dependency.setArtifactId("custom-library");
        dependency.setVersion("2.0");
        model.addDependency(dependency);
        var manifest = new org.ikasan.studio.core.metapack.model.MetaPackManifest(1, "V" + version,
                version, javaVersion, java.util.List.of(new org.ikasan.studio.core.metapack.model.MetaPackManifest.BomImport(
                        "org.ikasan", "ikasan-eip-standalone-bom", version)), java.util.List.of());
        IkasanPomModel pom = new IkasanPomModel(model);
        assertThat(pom.isNewDependency(java.util.List.of(dependency))).isFalse();
        String original = pom.getModelAsString();
        assertThat(pom.needsBuildContractUpdate(manifest)).isTrue();
        assertThat(pom.isDirty()).isFalse();
        assertThat(pom.getModelAsString()).isEqualTo(original);
        pom.applyBuildContract(manifest);
        assertThat(model.getProperties()).containsEntry("maven.compiler.source", javaVersion)
                .containsEntry("maven.compiler.target", javaVersion)
                .containsEntry("maven.compiler.release", javaVersion)
                .containsEntry("version.ikasan", version).containsEntry("team.setting", "keep");
        assertThat(model.getDependencies()).containsExactly(dependency);
        assertThat(model.getBuild().getPlugins()).containsExactly(plugin);
        assertThat(model.getModules()).containsExactly("user");
        assertThat(model.getDependencyManagement().getDependencies()).singleElement()
                .satisfies(bom -> assertThat(bom.getVersion()).isEqualTo(version));
        assertThat(pom.needsBuildContractUpdate(manifest)).isFalse();
    }

    @Test
    void importsAndUpdatesBomIdempotently() {
        IkasanPomModel pom = new IkasanPomModel(new Model());

        pom.addOrUpdateBomImport("org.ikasan", "ikasan-eip-standalone-bom", "4.1.6");
        pom.addOrUpdateBomImport("org.ikasan", "ikasan-eip-standalone-bom", "4.1.6");

        assertThat(pom.model.getDependencyManagement().getDependencies()).singleElement().satisfies(bom -> {
            assertThat(bom.getVersion()).isEqualTo("4.1.6");
            assertThat(bom.getType()).isEqualTo("pom");
            assertThat(bom.getScope()).isEqualTo("import");
        });
    }

    @Test
    void removesAnExistingDirectVersionWhenTheBomBecomesAuthoritative() {
        Model model = new Model();
        Dependency existing = new Dependency();
        existing.setGroupId("org.ikasan");
        existing.setArtifactId("ikasan-ftp-endpoint");
        existing.setVersion("4.1.5");
        model.addDependency(existing);
        IkasanPomModel pom = new IkasanPomModel(model);
        Dependency managed = existing.clone();
        managed.setVersion(null);

        pom.checkIfDependancyAlreadyExists(managed);

        assertThat(model.getDependencies()).singleElement().extracting(Dependency::getVersion).isNull();
        assertThat(pom.isDirty()).isTrue();
    }

    @Test
    void alignsAnExplicitOverrideExactlyInsteadOfChoosingTheNewestVersion() {
        Model model = new Model();
        Dependency existing = new Dependency();
        existing.setGroupId("example");
        existing.setArtifactId("library");
        existing.setVersion("9.0");
        model.addDependency(existing);
        IkasanPomModel pom = new IkasanPomModel(model);
        Dependency selectedByMetaPack = existing.clone();
        selectedByMetaPack.setVersion("2.0");

        pom.checkIfDependancyAlreadyExists(selectedByMetaPack);

        assertThat(model.getDependencies()).singleElement()
                .extracting(Dependency::getVersion).isEqualTo("2.0");
    }
}
