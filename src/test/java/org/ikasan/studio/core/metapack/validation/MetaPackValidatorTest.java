package org.ikasan.studio.core.metapack.validation;

import org.apache.maven.model.Dependency;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.model.MetaPackManifest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetaPackValidatorTest {
    @Test
    void rejectsAnIkasanDependencyFromAnotherRelease() {
        ComponentMeta component = new ComponentMeta();
        component.setName("Broken");
        component.setJarDependencies(Set.of(dependency("org.ikasan", "ikasan-ftp-endpoint", "4.1.5")));

        assertThatThrownBy(() -> MetaPackValidator.validate("V4", manifest("V4"), Map.of("Broken", component)))
                .hasMessageContaining("instead of 4.1.6");
    }

    @Test
    void rejectsAnUndocumentedThirdPartyOverride() {
        ComponentMeta component = new ComponentMeta();
        component.setName("Broken");
        component.setJarDependencies(Set.of(dependency("example", "library", "1.2.3")));

        assertThatThrownBy(() -> MetaPackValidator.validate("V4", manifest("V4"), Map.of("Broken", component)))
                .hasMessageContaining("without a matching compatibilityOverrides entry");
    }

    /**
     * implementingClass is no longer @lombok.NonNull (see ComponentMeta), so a meta-pack component omitting it
     * deserializes to null instead of failing construction with a raw, context-free Jackson/Lombok error - this
     * validator is now what actually catches it, with a message naming the offending component and field.
     */
    @Test
    void rejectsAComponentMissingImplementingClass() {
        ComponentMeta component = new ComponentMeta();
        component.setName("Broken");

        assertThatThrownBy(() -> MetaPackValidator.validate("V4", manifest("V4"), Map.of("Broken", component)))
                .hasMessageContaining("implementingClass")
                .hasMessageContaining("is required");
    }

    private static MetaPackManifest manifest(String id) {
        return new MetaPackManifest(1, id, "4.1.6", "17",
                List.of(new MetaPackManifest.BomImport(
                        "org.ikasan", "ikasan-eip-standalone-bom", "4.1.6")), List.of());
    }

    private static Dependency dependency(String group, String artifact, String version) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(group);
        dependency.setArtifactId(artifact);
        dependency.setVersion(version);
        return dependency;
    }
}
