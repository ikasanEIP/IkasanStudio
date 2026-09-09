package org.ikasan.studio.testing.engine;

import org.ikasan.studio.core.metapack.validation.MetaPackValidator;

import org.apache.maven.model.Dependency;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.model.MetaPackManifest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@org.junit.jupiter.api.Tag("engine")
class MetaPackValidatorTest {
    @Test
    void rejectsAnIkasanDependencyFromAnotherRelease() {
        ComponentMeta component = new ComponentMeta();
        component.setName("Broken");
        component.setJarDependencies(Set.of(dependency("org.ikasan", "ikasan-ftp-endpoint", "9.8.6")));

        assertThatThrownBy(() -> MetaPackValidator.validate("SyntheticValidator", manifest("SyntheticValidator"), Map.of("Broken", component)))
                .hasMessageContaining("instead of 9.8.7");
    }

    @Test
    void rejectsAnUndocumentedThirdPartyOverride() {
        ComponentMeta component = new ComponentMeta();
        component.setName("Broken");
        component.setJarDependencies(Set.of(dependency("example", "library", "1.2.3")));

        assertThatThrownBy(() -> MetaPackValidator.validate("SyntheticValidator", manifest("SyntheticValidator"), Map.of("Broken", component)))
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

        assertThatThrownBy(() -> MetaPackValidator.validate("SyntheticValidator", manifest("SyntheticValidator"), Map.of("Broken", component)))
                .hasMessageContaining("implementingClass")
                .hasMessageContaining("is required");
    }

    @Test
    void preservesLegacySchemaOneWithoutInventingARevision() throws Exception {
        var legacy = new com.fasterxml.jackson.databind.ObjectMapper().readValue("""
                {"schemaVersion":1,"id":"SyntheticValidator","ikasanVersion":"9.8.7",
                 "javaVersion":"17","dependencyManagement":[
                 {"groupId":"org.ikasan","artifactId":"ikasan-eip-standalone-bom","version":"9.8.7"}]}
                """, MetaPackManifest.class);
        org.junit.jupiter.api.Assertions.assertNull(legacy.packVersion());
        MetaPackValidator.validate("SyntheticValidator", legacy, validComponents());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"1.0.0", "2.7.1-SNAPSHOT"})
    void packRevisionIsIndependentOfRuntimeVersion(String revision) throws Exception {
        MetaPackValidator.validate("SyntheticValidator", revisionManifest(revision, 1), validComponents());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = {"latest", "1.+", "[1,2)", "1.0"})
    void rejectsMissingOrDynamicPackRevisions(String revision) {
        assertThatThrownBy(() -> MetaPackValidator.validate("SyntheticValidator", revisionManifest(revision, 1), Map.of()))
                .hasMessageContaining("packVersion");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullSource
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 2})
    void rejectsMissingOrIncompatibleGeneratorApi(Integer api) {
        assertThatThrownBy(() -> MetaPackValidator.validate("SyntheticValidator", revisionManifest("1.0.0", api), Map.of()))
                .hasMessageContaining("generatorApiVersion");
    }

    private static Map<String, ComponentMeta> validComponents() {
        var category = org.ikasan.studio.core.metapack.model.ComponentTypeMeta.builder().componentShortType("End Point").build();
        category.setComponentShortType("End Point");
        var component = new ComponentMeta();
        component.setName("Synthetic endpoint");
        component.setComponentType("End Point");
        component.setComponentTypeMeta(category);
        component.setAllowableProperties(Map.of());
        component.setImplementingClass("org.example.Endpoint");
        return Map.of(component.getName(), component);
    }

    private static MetaPackManifest revisionManifest(String revision, Integer api) {
        var legacy = manifest("SyntheticValidator");
        return new MetaPackManifest(2, legacy.id(), legacy.ikasanVersion(), legacy.javaVersion(),
                legacy.dependencyManagement(), legacy.compatibilityOverrides(), revision, api);
    }

    private static MetaPackManifest manifest(String id) {
        return new MetaPackManifest(1, id, "9.8.7", "17",
                List.of(new MetaPackManifest.BomImport(
                        "org.ikasan", "ikasan-eip-standalone-bom", "9.8.7")), List.of());
    }

    private static Dependency dependency(String group, String artifact, String version) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(group);
        dependency.setArtifactId(artifact);
        dependency.setVersion(version);
        return dependency;
    }
}
