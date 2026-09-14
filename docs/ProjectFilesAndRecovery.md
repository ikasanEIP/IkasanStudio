# Project files, backups and recovery

| Location | Ownership and handling |
| --- | --- |
| `generated/src/main/model/model.json` | Source-of-truth visual model. Commit it; its location under `generated/` does not make it disposable. |
| Other Studio-generated Java/configuration in `generated/` | Output from the selected pack. Regeneration can replace it. Make persistent changes through model properties or supported user implementations. |
| `user/` | Developer-owned implementations and resources. Studio creates stubs; existing implementations are protected unless you explicitly approve regeneration. |
| Root and module Maven POMs | Build configuration. Studio updates relevant generated dependencies/settings; review diffs, especially during migration. |
| `.ikasan-studio/migrations/` | Local migration recovery snapshots, including file contents. Preserve when recovering a migration. |

Use **Jump to Code** to locate the actual implementation before editing. Property changes affecting an existing user class can request regeneration confirmation and offer backup controls. A Java backup is written beside the original as `Foo.java.backup<timestamp>`; a required backup failure aborts the overwrite. Creating a stub does not implement application-specific logic for you.

Commit the model and your implementations together. Do not delete the whole `generated/` directory as a build-cleaning shortcut. Maven `target/` directories are separate build output.

## Automatic model backups

Before replacing an existing valid model, Studio retains its previous contents beside it as `model.json.bak.1`, rotating up to `.bak.3`. `.bak.1` is the most recent saved predecessor. These are a short history of saves, not three days of history or a backup of the whole project.

Studio validates the candidate and existing model, writes a temporary sibling and uses atomic replacement. An invalid model or unavailable atomic replacement produces a save failure rather than deliberately overwriting the original with a partial file. Preserve the error and investigate permissions, disk space and filesystem capabilities.

## Recover an unreadable model

1. Copy the project, including the original model and backups, before manual repairs. Stop the module and avoid concurrent Studio sessions writing the same project.
2. Open Studio. If the model cannot load safely, the recovery view preserves the original and offers only backups that validate.
3. Select the desired validated backup. Studio restores it and preserves the rejected model as `model.json.rejected.<timestamp>.<id>`.
4. If no valid backup exists, restore the model from version control or repair a copy, then use **Reload from Disk**. The normal toolbar reload control is available through advanced controls in **Settings → Tools → Ikasan Studio**.
5. Inspect the restored flows, component settings and selected pack before generation. Review generated file diffs and run application tests. Restoring a model does not automatically restore matching developer Java, external databases or delivered messages.

For a failed version migration, follow [migration snapshot restoration](IkasanVersionMigration.md#restore-versus-migrate-back). Do not replace only the model while leaving the project POM and generated Java on another Ikasan version.

Models, adjacent backups and migration snapshots can contain entered credentials and endpoint details. Apply the same access controls and sharing review to backups as to the project. Use your team's secret-management conventions and do not commit actual credentials.
