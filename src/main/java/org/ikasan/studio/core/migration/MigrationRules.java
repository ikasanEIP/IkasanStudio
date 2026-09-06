package org.ikasan.studio.core.migration;

import org.ikasan.studio.core.io.ComponentIO;
import java.util.Map;

/** Directional rules travel with the target pack; reverse migration has its own explicit contract. */
public record MigrationRules(int schemaVersion, String sourceVersion, String targetVersion,
                             Map<String, String> componentMappings, Map<String, String> typePrefixes) {
    public MigrationRules {
        componentMappings = Map.copyOf(componentMappings);
        typePrefixes = Map.copyOf(typePrefixes);
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
