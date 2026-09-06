# Migrating Ikasan versions

Studio can migrate a saved project between **V3.3.9 and V4.1.6 in either direction**.
Use **Migrate…** on the canvas, or **Tools → Migrate Ikasan Version…** (also available
through Find Action). You can also right-click the module in the designer and choose
**Migrate Ikasan Version…**. Configured modules show their version as read-only in Properties.

## Workflow

1. Save open project files and apply or discard pending Studio property edits. Wait for
   source generation and indexing to finish. The canvas must agree with the saved model.
2. Select the target version. Studio analyses an isolated copy of `model.json` and renders
   the proposed generated files and root Maven POM without writing them.
3. Review the migration report and the **File changes** tab. Unsupported components,
   missing required properties, incompatible choices, and structures that would lose data
   block Apply. Correct those issues in the source model and preview again.
4. Apply. Studio saves a recovery snapshot under `.ikasan-studio/migrations/`, checks that
   the reviewed files have not changed, and writes the reviewed contents. A write failure
   triggers restoration of previously written files.
5. By default Studio requests Maven import and an IntelliJ build, reporting build success,
   errors or cancellation through a notification. Install/select the target JDK in Project
   Structure: the shipped contracts specify Java 11 for V3.3.9 and Java 17 for V4.1.6.
   Run application tests and check runtime behaviour before deploying.

The conversion itself is offline. Maven import or compilation can need downloads when
required dependencies are not already cached. Compilation failure does not undo the
migration: use the errors to update your implementation, or restore the recovery snapshot.

## What changes

- The model's selected version and supported component identities are mapped through
  explicit directional rules supplied with the target meta-pack.
- JMS, JAXB and resource exception type references move between `javax` and `jakarta` in
  recognised type fields. Resolver keys and caught exception types change together.
  Arbitrary descriptions and opaque extension values are not subjected to text replacement.
- Shared properties retain their values. Changed defaults are made explicit where a source
  value can be preserved; unsupported type/property changes require a separate rule or
  source-model correction.
- Studio regenerates its application, module configuration, flows, factories, generated
  property classes, properties, H2 POM and offline AI contract. The root POM adopts the
  target Ikasan BOM, dependencies and Java compiler settings. Other dependencies remain
  available for developer code; review their compatibility in the POM diff.
- Existing root `AGENTS.md` and files under `user/` are preserved. Migration does not
  rewrite custom implementations or regenerate existing user stubs. Review custom classes,
  injected beans, JMS/JAXB/resource imports and Debug support for target API compatibility.
- Opaque module, flow and component JSON fields survive loading and subsequent saving.
  If another model structure cannot survive Studio's round trip, migration is blocked.

A model conversion does not prove equivalent runtime behaviour. Unsupported version pairs
and unrecognised component identities are blocked rather than guessed.

## Restore versus migrate back

**Tools → Restore Previous Ikasan Migration…** previews restoration of the latest completed
snapshot. It restores exact original file contents, removes files introduced by the migration,
and leaves unrelated files alone. Team instructions added to `AGENTS.md` after migration
are retained. Review carefully: subsequent edits to the affected generated
files and root POM are replaced. A new snapshot preserves the state before restoration, so the
restoration itself can be reversed. All snapshots remain in the history directory.

To keep new flows and other subsequent model edits while returning to the previous Ikasan
version, use **Migrate…** again and choose that version. This runs reverse conversion rules
against the current model; it does not restore an old copy.

Snapshots use versioned JSON containing the report and Base64-encoded original/proposed file
bytes. A `committed: false` snapshot records an interrupted or failed operation and is not
selected automatically as a completed migration. If automatic recovery is reported incomplete,
inspect that snapshot and the IDE log before reloading or saving the model.

## Maintenance and verification

The framework-independent engine is in `core/migration`. `MigrationController` owns IntelliJ
progress, review, reload and build integration. Project-scoped guards exclude concurrent Studio
generation; filesystem preconditions reject external edits after preview. File IO and template
rendering run outside the EDT. The preview uses IntelliJ's native diff viewer.

Directional rules live at:

- `studio/metapack/V4.1.6/migrations/from-V3.3.9.json`
- `studio/metapack/V3.3.9/migrations/from-V4.1.6.json`

Adding another supported path requires reviewed component mappings, any necessary property/type
conversion logic, target API validation and tests in both directions. Do not infer downgrade
support simply by reversing an upgrade rule that discards information.

Run `./gradlew test validateMetaPacks --no-configuration-cache`. Migration tests cover both
versions, routed flows, exception identities, opaque JSON, ownership, stale previews, rollback
and snapshot restoration. They also emit representative projects for real compiler checks:

```sh
mvn -B -f build/migration-compile/V4.1.6/pom.xml -DskipTests compile
mvn -B -f build/migration-compile/V3.3.9/pom.xml -DskipTests compile
```

Before release, exercise the dialog in light and dark themes: cancel a preview, apply each
direction, inspect build results, restore a snapshot, and repeat with custom code and unsaved
edits. Check that deliberate editor closure and other open projects remain unaffected.
