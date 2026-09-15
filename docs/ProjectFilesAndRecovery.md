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

## Renaming a flow

Change the flow name in Properties and choose **Update Code**. If its Java package
changes, **Rename and refactor** shows the old/new package and affected files.
Studio uses IntelliJ package refactoring to retain implementations, helper classes
and subpackages, update Java references, and update its default Spring bean names
and matching `@Resource`/`@Qualifier` references. Custom bean names are preserved.
The model is saved before generated code and navigation are refreshed. Class names
are retained; renaming a flow does not require renaming each implementation class.

Wait for Maven import, indexing and any active generation to finish. An existing
destination package blocks the rename rather than merging or deleting files.
Packages shared outside the project's `user/` and `generated/` source trees, or
containing explicitly supplied external implementations, require manual refactoring.
Review references in external configuration or arbitrary string literals yourself;
these are not globally replaced.

Cancel leaves the name and files unchanged. A refactoring failure attempts to restore
the affected sources and references. If generation fails after a successful rename,
the new model name and moved implementations are retained: fix the reported problem
and run **Update Code** again. Do not delete the moved implementations or regenerate
them as empty stubs. Commit the model and source changes together.

To reverse the operation, rename the flow back through Properties. Whole-operation
IntelliJ Undo is not supported: the protected model save and asynchronous generation
are not a single undo transaction, so Studio blocks partial Undo of the refactor.

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
