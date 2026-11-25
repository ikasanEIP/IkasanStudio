package org.ikasan.studio.core.maven;

import java.util.List;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.ikasan.studio.core.metapack.model.MetaPackManifest.BomImport;
import org.ikasan.studio.core.metapack.model.MetaPackManifest.MavenPlugin;
import org.ikasan.studio.core.metapack.model.MetaPackManifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class IkasanPomModelTest {
    private static MetaPackManifest pluginManifest() {
        return new MetaPackManifest(2, "Custom", "5.1.0", "25",
                List.of(), List.of(), "1.0.0", 5, List.of(
                new MavenPlugin(
                        "org.apache.maven.plugins", "maven-compiler-plugin", "3.8.0")));
    }

    @Test void missingPluginDefaultsAreManagedWithoutActivatingExecutionsAndAreIdempotent() {
        Model model = new Model();
        IkasanPomModel pom = new IkasanPomModel(model);
        pom.applyBuildContract(pluginManifest());
        assertThat(model.getBuild().getPlugins()).isEmpty();
        assertThat(model.getBuild().getPluginManagement().getPlugins()).singleElement().satisfies(plugin -> {
            assertThat(plugin.getArtifactId()).isEqualTo("maven-compiler-plugin");
            assertThat(plugin.getVersion()).isEqualTo("3.8.0");
            assertThat(plugin.getExecutions()).isEmpty();
        });
        IkasanPomModel reapplied = new IkasanPomModel(model);
        reapplied.applyBuildContract(pluginManifest());
        assertThat(reapplied.isDirty()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void explicitPluginVersionsAndConfigurationRemainDeveloperOwned(boolean managed) {
        Model model = new Model();
        Build build = new Build();
        model.setBuild(build);
        Plugin plugin = new Plugin();
        plugin.setArtifactId("maven-compiler-plugin");
        plugin.setVersion("${team.compiler.version}");
        Xpp3Dom config = new Xpp3Dom("configuration");
        plugin.setConfiguration(config);
        if (managed) {
            build.setPluginManagement(new PluginManagement());
            build.getPluginManagement().addPlugin(plugin);
        } else build.addPlugin(plugin);
        new IkasanPomModel(model).applyBuildContract(pluginManifest());
        assertThat(plugin.getVersion()).isEqualTo("${team.compiler.version}");
        assertThat(plugin.getConfiguration()).isSameAs(config);
    }

    @Test void unversionedManagedPluginRetainsItsConfigurationWhileAcquiringTheDefault() {
        Model model = new Model();
        Build build = new Build();
        model.setBuild(build);
        build.setPluginManagement(new PluginManagement());
        Plugin plugin = new Plugin();
        plugin.setArtifactId("maven-compiler-plugin");
        Xpp3Dom config = new Xpp3Dom("configuration");
        plugin.setConfiguration(config);
        build.getPluginManagement().addPlugin(plugin);
        new IkasanPomModel(model).applyBuildContract(pluginManifest());
        assertThat(build.getPluginManagement().getPlugins()).containsExactly(plugin);
        assertThat(plugin.getVersion()).isEqualTo("3.8.0");
        assertThat(plugin.getConfiguration()).isSameAs(config);
    }

    @ParameterizedTest
    @CsvSource({"3.3.9,11", "4.1.6,17", "5.1.0,25"})
    void detectsBuildSettingsWithoutNewDependenciesAndPreservesUnrelatedConfiguration(String version, String javaVersion) {
        Model model = new Model();
        model.setGroupId("example");
        model.setArtifactId("imported-module");
        model.addProperty("maven.compiler.source", "1.7");
        model.addProperty("maven.compiler.target", "1.7");
        model.addProperty("maven.compiler.release", "7");
        model.addProperty("team.setting", "keep");
        model.addModule("user");
        Build build = new Build();
        Plugin plugin = new Plugin();
        plugin.setArtifactId("maven-compiler-plugin");
        plugin.setVersion("3.8.0");
        build.addPlugin(plugin);
        model.setBuild(build);
        Dependency dependency = new Dependency();
        dependency.setGroupId("example");
        dependency.setArtifactId("custom-library");
        dependency.setVersion("2.0");
        model.addDependency(dependency);
        MetaPackManifest manifest = new MetaPackManifest(1, "V" + version,
                version, javaVersion, List.of(new BomImport(
                        "org.ikasan", "ikasan-eip-standalone-bom", version)), List.of());
        IkasanPomModel pom = new IkasanPomModel(model);
        assertThat(pom.isNewDependency(List.of(dependency))).isFalse();
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
