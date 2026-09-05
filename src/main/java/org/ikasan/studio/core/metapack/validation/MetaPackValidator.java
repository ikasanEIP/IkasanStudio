package org.ikasan.studio.core.metapack.validation;

import org.apache.maven.model.Dependency;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.model.MetaPackManifest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Enforces the version contract before a meta-pack can be exposed to Studio. */
public final class MetaPackValidator {
    private MetaPackValidator() { }

    public static void validate(String directoryName, MetaPackManifest manifest,
                                Map<String, ComponentMeta> components) throws StudioBuildException {
        if (manifest == null) fail(directoryName, "is missing metapack.json");
        if (manifest.schemaVersion() != 1) {
            fail(directoryName, "uses unsupported schemaVersion " + manifest.schemaVersion());
        }
        require(directoryName, "id", manifest.id());
        require(directoryName, "ikasanVersion", manifest.ikasanVersion());
        require(directoryName, "javaVersion", manifest.javaVersion());
        rejectDynamicVersion(directoryName, manifest.ikasanVersion());
        if (!directoryName.equals(manifest.id())) fail(directoryName, "declares id " + manifest.id());
        if (manifest.dependencyManagement().isEmpty()) {
            fail(directoryName, "must declare at least one dependencyManagement BOM import");
        }
        for (MetaPackManifest.BomImport bom : manifest.dependencyManagement()) {
            require(directoryName, "BOM groupId", bom.groupId());
            require(directoryName, "BOM artifactId", bom.artifactId());
            require(directoryName, "BOM version", bom.version());
            rejectDynamicVersion(directoryName, bom.version());
            if ("org.ikasan".equals(bom.groupId()) && !manifest.ikasanVersion().equals(bom.version())) {
                fail(directoryName, "imports Ikasan BOM " + bom.version()
                        + " but declares Ikasan " + manifest.ikasanVersion());
            }
        }

        Map<String, MetaPackManifest.CompatibilityOverride> overrides = new HashMap<>();
        for (MetaPackManifest.CompatibilityOverride override : manifest.compatibilityOverrides()) {
            require(directoryName, "override groupId", override.groupId());
            require(directoryName, "override artifactId", override.artifactId());
            require(directoryName, "override version", override.version());
            require(directoryName, "override reason", override.reason());
            rejectDynamicVersion(directoryName, override.version());
            String key = override.groupId() + ":" + override.artifactId();
            if (overrides.putIfAbsent(key, override) != null) {
                fail(directoryName, "declares duplicate compatibility override " + key);
            }
        }

        Set<String> usedOverrides = new HashSet<>();
        Map<String, String> versions = new HashMap<>();
        for (ComponentMeta component : components.values()) {
            if (component.getJarDependencies() == null) continue;
            for (Dependency dependency : component.getJarDependencies()) {
                String key = dependency.getGroupId() + ":" + dependency.getArtifactId();
                String version = dependency.getVersion();
                if ("org.ikasan".equals(dependency.getGroupId()) && version != null
                        && !manifest.ikasanVersion().equals(version)) {
                    fail(directoryName, component.getName() + " declares " + key + ":" + version
                            + " instead of " + manifest.ikasanVersion());
                }
                if (version == null) continue;
                rejectDynamicVersion(directoryName, version);
                String previous = versions.putIfAbsent(key, version);
                if (previous != null && !previous.equals(version)) {
                    fail(directoryName, "declares conflicting versions for " + key + ": "
                            + previous + " and " + version);
                }
                if (!"org.ikasan".equals(dependency.getGroupId())) {
                    MetaPackManifest.CompatibilityOverride override = overrides.get(key);
                    if (override == null || !version.equals(override.version())) {
                        fail(directoryName, component.getName() + " explicitly versions " + key + ":" + version
                                + " without a matching compatibilityOverrides entry");
                    }
                    usedOverrides.add(key);
                }
            }
        }
        for (String override : overrides.keySet()) {
            if (!usedOverrides.contains(override)) {
                fail(directoryName, "declares unused compatibility override " + override);
            }
        }
    }

    private static void require(String pack, String field, String value) throws StudioBuildException {
        if (value == null || value.isBlank()) fail(pack, "has a blank " + field);
    }

    private static void rejectDynamicVersion(String pack, String version) throws StudioBuildException {
        String upper = version.toUpperCase();
        if (version.contains("[") || version.contains("(")
                || upper.equals("LATEST") || upper.equals("RELEASE")) {
            fail(pack, "uses non-reproducible version " + version);
        }
    }

    private static void fail(String pack, String reason) throws StudioBuildException {
        throw new StudioBuildException("Meta-pack " + pack + " " + reason);
    }
}
