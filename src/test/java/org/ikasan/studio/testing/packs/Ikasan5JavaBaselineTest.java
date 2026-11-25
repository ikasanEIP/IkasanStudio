package org.ikasan.studio.testing.packs;

import org.apache.maven.model.Model;
import org.ikasan.studio.core.maven.IkasanPomModel;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import org.ikasan.studio.core.metapack.model.MetaPackManifest;

@Tag("packs")
class Ikasan5JavaBaselineTest {
    @ParameterizedTest
    @ValueSource(strings = {"V5.1.0"})
    void regenerationReplacesJava26CompilerSettingsWithJava25(String pack) throws Exception {
        Model model = new Model();
        for (String key : new String[]{"maven.compiler.source", "maven.compiler.target", "maven.compiler.release"})
            model.addProperty(key, "26");
        model.addProperty("customer.setting", "retained");
        IkasanPomModel pom = new IkasanPomModel(model);
        MetaPackManifest manifest = ComponentLibrary.getMetaPackManifest(pack);
        assertThat(manifest.javaVersion()).isEqualTo("25");
        assertThat(pom.needsBuildContractUpdate(manifest)).isTrue();
        pom.applyBuildContract(manifest);
        for (String key : new String[]{"maven.compiler.source", "maven.compiler.target", "maven.compiler.release"})
            assertThat(pom.getProperty(key)).as(pack + " " + key).isEqualTo("25");
        assertThat(pom.getProperty("customer.setting")).isEqualTo("retained");
        assertThat(pom.needsBuildContractUpdate(manifest)).isFalse();
    }
}
