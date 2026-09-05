package org.ikasan.studio.core.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IkasanPomModelTest {
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
