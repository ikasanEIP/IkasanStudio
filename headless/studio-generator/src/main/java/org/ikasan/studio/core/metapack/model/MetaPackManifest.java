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
        Integer generatorApiVersion,
        List<MavenPlugin> mavenPlugins) {

    /** Existing manifests and callers need no Maven plugin defaults. */
    public MetaPackManifest(int schemaVersion, String id, String ikasanVersion, String javaVersion,
                            List<BomImport> dependencyManagement, List<CompatibilityOverride> compatibilityOverrides,
                            String packVersion, Integer generatorApiVersion) {
        this(schemaVersion, id, ikasanVersion, javaVersion, dependencyManagement, compatibilityOverrides,
                packVersion, generatorApiVersion, List.of());
    }

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
        mavenPlugins = mavenPlugins == null ? List.of() : List.copyOf(mavenPlugins);
    }

    public record BomImport(String groupId, String artifactId, String version) { }

    public record MavenPlugin(String groupId, String artifactId, String version) { }

    public record CompatibilityOverride(String groupId, String artifactId, String version, String reason) { }
}
