package org.ikasan.studio.core.migration;

import org.ikasan.studio.core.io.ComponentIO;
import java.util.Map;

/** Directional rules travel with the target pack; reverse migration has its own explicit contract. */
public record MigrationRules(int schemaVersion, String sourceVersion, String targetVersion,
                             Map<String, String> componentMappings, Map<String, String> typePrefixes, Map<String, String> userImportPrefixes,
                             ImportApplicability userImportApplicability) {
    public record ImportApplicability(int sourceMajor, int targetMajor) { }

    public MigrationRules {
        componentMappings = Map.copyOf(componentMappings);
        typePrefixes = Map.copyOf(typePrefixes);
        userImportPrefixes = userImportPrefixes == null ? Map.of() : Map.copyOf(userImportPrefixes);
        if (!userImportPrefixes.isEmpty() && (userImportApplicability == null
                || major(sourceVersion) != userImportApplicability.sourceMajor()
                || major(targetVersion) != userImportApplicability.targetMajor()))
            throw new IllegalArgumentException("User import rules do not apply to this major-version migration");
        for (var prefix : userImportPrefixes.entrySet()) {
            if (!prefix.getKey().matches("(?:[A-Za-z_$][A-Za-z0-9_$]*\\.)+")
                    || !prefix.getValue().matches("(?:[A-Za-z_$][A-Za-z0-9_$]*\\.)+"))
                throw new IllegalArgumentException("User import rules must map Java package prefixes ending in a dot");
        }
    }

    private static int major(String version) {
        return Integer.parseInt(version.replaceFirst("^V", "").split("\\.")[0]);
    }

    public static MigrationRules load(String source, String target) throws Exception {
        if (!ModelMigration.SUPPORTED_VERSIONS.contains(source) || !ModelMigration.SUPPORTED_VERSIONS.contains(target)) {
            throw new IllegalArgumentException("Unsupported migration path.");
        }
        MigrationRules rules = ComponentIO.deserializeResource("studio/metapack/" + target + "/migrations/from-" + source + ".json", MigrationRules.class);
        if (rules.schemaVersion() != 1 || !rules.sourceVersion().equals(source) || !rules.targetVersion().equals(target)) {
            throw new IllegalArgumentException("Migration rule identity does not match the selected versions.");
        }
        return rules;
    }
}
