package org.ikasan.studio.core.metapack.model;

import java.util.List;

/** Version and dependency-management contract for one packaged meta-pack. */
public record MetaPackManifest(
        int schemaVersion,
        String id,
        String ikasanVersion,
        String javaVersion,
        List<BomImport> dependencyManagement,
        List<CompatibilityOverride> compatibilityOverrides,
        String packVersion,
        Integer generatorApiVersion) {

    /** Source compatibility for legacy schema-1 manifests; revision metadata was not recorded. */
    public MetaPackManifest(int schemaVersion, String id, String ikasanVersion, String javaVersion,
                            List<BomImport> dependencyManagement,
                            List<CompatibilityOverride> compatibilityOverrides) {
        this(schemaVersion, id, ikasanVersion, javaVersion, dependencyManagement,
                compatibilityOverrides, null, null);
    }

    public MetaPackManifest {
        dependencyManagement = dependencyManagement == null ? List.of() : List.copyOf(dependencyManagement);
        compatibilityOverrides = compatibilityOverrides == null ? List.of() : List.copyOf(compatibilityOverrides);
    }

    public record BomImport(String groupId, String artifactId, String version) { }

    public record CompatibilityOverride(String groupId, String artifactId, String version, String reason) { }
}
