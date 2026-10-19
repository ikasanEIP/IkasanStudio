# Verify and migrate your own Studio project

These tools work with a developer's **existing Studio Maven project**. They do not require
our all-components regression fixture, its flow names or its local service harnesses.

- `studio_upgrade.py` records before/after build, test, source and model evidence.
- `studio-cli` validates models, renders a fresh output preview, and previews/applies supported version migrations using the **same engine and recovery transaction as the IntelliJ plugin**. IDE exports also include the saved project’s required custom pack.

A passing report means the existing tests passed and preservation checks passed. It is
not certification that every flow works. Tests must explicitly assert real delivery,
error handling, idle readiness and later delivery for those behaviours to be verified.
No tests, all skipped tests or missing fresh test reports produce **INCOMPLETE**, not PASS.

## Obtain the tools

For Marketplace installations, choose **Tools → Ikasan Studio → Export Offline Migration
Tools…**, select a local parent folder, and open `README.md` in the new
`ikasan-studio-offline-tools` folder. It includes the launchers, verifier, dependencies and
bundled packs shipped with that plugin build. For a configured Studio project, it also includes the exact selected custom pack JAR and its revision/checksum inventory. No website access or extra download is
needed. Export runs in the background and can be cancelled; existing folders are never
replaced. Use a different parent folder when exporting another copy.

For development or CI, build the same standalone distribution from the Studio repository
with Java 17:

```sh
./gradlew -p headless :studio-cli:distZip
```

Extract `headless/studio-cli/build/distributions/studio-cli-0.1.0-SNAPSHOT.zip` anywhere.
It contains launch scripts in `bin/`, all Java dependencies and bundled packs in `lib/`,
and this guide. No IntelliJ installation or SDK is needed. The Marketplace plugin contains this distribution as a ZIP resource; exported tools
run independently of IntelliJ. A separate public download is not required.

Use `bin/studio-cli` on Linux/macOS, or `bin/studio-cli.bat` on Windows. The CLI requires
Java 17 or newer even when migrating a Java 11 module. The plugin JAR itself is **not**
an executable utility JAR: IntelliJ-specific classes depend on the IDE runtime. The
standalone utility reuses its framework-independent engine without loading those classes.

If preferred, the equivalent plain-JVM invocation from the extracted distribution is:

```sh
java -cp 'lib/*' org.ikasan.studio.cli.StudioMigrationCli --help
```

Python 3.9+ and Maven are needed for verification. The single file
`bin/studio_upgrade.py` can also be copied into the developer's project on its own.
Its repository source is `tools/migration/studio_upgrade.py`.

## Portable custom packs, validation and rendering

Export from the project whose pack you need. The action reads the **saved** `generated/src/main/model/model.json`; apply/save model changes first if you want them reflected in the selection. A missing or empty bootstrap model still permits a generic bundled-only export. An invalid configured model, unavailable pack or revision mismatch prevents publishing the export, leaving no partial destination. Export does not rewrite the model or acquire a lock for an older unlocked project.

Keep the complete tools folder together:

```text
ikasan-studio-offline-tools/
  bin/
  lib/                    # CLI, engine, dependencies and the two official packs
  meta-packs/             # the selected custom resource-only JAR, if any
  meta-packs.json         # required pack ID and ID/revision/file/SHA-256 inventory
```

The manifest records all included packs, including the bundled JARs already in `lib/`. A configured project’s required artifact must match the one used by Studio. The custom JAR is loaded as resources, without adding it to the Java classpath. Exported tools resolve packs relative to their own location, so the folder can be moved or handed to another developer without IntelliJ or the original pack installation directory.

Before CLI commands use a model, they check the pack inventory, SHA-256 values, declared IDs/revisions, archive restrictions, metadata and template syntax. Missing, changed, duplicate or unlisted pack JARs are errors. The portable directory is read-only to pack discovery: IDE pending updates/removals are never activated there. Checksums detect changed artifacts; they do not authenticate a publisher. Use tools and packs from trusted sources.

From a separate copy of your Studio project:

```sh
/path/to/tools/bin/studio-cli validate --project /path/to/project
/path/to/tools/bin/studio-cli render --project /path/to/project --output /path/to/new-render-preview
```

`validate` checks the model/pack structure and revision lock. It does **not** compile the application, execute tests or certify runtime behaviour. `render` uses the shared Studio project renderer and writes Studio-owned artifacts, model/catalogue metadata and a proposed root POM into a **new directory outside the source project**. Existing output directories are rejected; failures clean up staging. The command leaves source-project files and developer implementations unchanged. It is an inspection preview, not an in-place regeneration command or a complete application copy. In particular, it does not copy `user/`, environment files or business tests.

Custom pack validation/rendering does not enable arbitrary pack-to-pack migration. `preview`/`apply` still support only **V3.3.9 ↔ V4.1.6**. CLI errors return **1**; successful validation/rendering return **0**. Project revision guards still apply after moving the tools. To switch pack artifacts, produce a new reviewed export instead of editing the manifest or copying untracked JARs into it.

Pack export includes descriptors/templates/artwork, not the application’s Maven dependencies or external services. Maven verification still needs those dependencies in a cache or accessible repository, and the application’s tests must establish real delivery, idle readiness and later delivery. Generic developer distributions built with `distZip` remain bundled-only until exported with a project selection; older generic distributions without a portable pack directory retain their existing CLI behaviour.

## Verify before and after an IDE upgrade

Save files, commit/back up the project and configure its test environment. These are
**your project tests**, so they may contact the services selected by your Maven profiles
and environment variables. The tool does not replace settings or start fixture servers.

From the developer's project root, with the Python script copied there:

```sh
python3 studio_upgrade.py verify . --report migration-before
# Use Studio's Migrate Ikasan Version, then wait for generation to complete.
python3 studio_upgrade.py verify . --report migration-after
python3 studio_upgrade.py compare migration-before/report.json migration-after/report.json --report migration-comparison
```

Open each directory's `report.md` in IntelliJ. JSON evidence, executed test identities
and the Maven build log are retained alongside it. Existing evidence is never overwritten;
choose fresh report names for subsequent runs. `verify` resolves a relative `--report`
under the project root, even when called from elsewhere. Without it, reports go under
`migration-reports/<timestamp>/` in the project. `compare --report` is relative to the
current directory; the default is `migration-comparison` next to the after-report directory.

The tool runs `mvn -B -V clean verify` to compile and execute the configured Surefire and
Failsafe tests. Old reports from inactive modules are not counted. Default timeout is
900 seconds; adjust with `--timeout 1800`. On POSIX systems timeout stops the test process
group; on Windows it stops the Maven process, so check for child processes after timeout.

Use the same test profiles/settings before and after, for example:

```sh
python3 studio_upgrade.py verify . --report migration-before --maven-arg=-Pacceptance
```

Repeat `--maven-arg` for other Maven arguments. `--maven /absolute/path/to/mvnw` selects a
wrapper. Set `JAVA_HOME` to a suitable JDK for each build; the verifier does not select one.
Do not pass secrets as command-line arguments: commands and model configuration are
recorded in reports, and build logs can contain sensitive values. Keep evidence local
or review it before sharing.

Comparison checks that both runs passed, executed test identities and Maven arguments
match, developer files/test sources are unchanged, and the model was preserved. It
protects `user/` (excluding build output) and additional modules' `src/` trees. It does
not inventory every external resource or arbitrary repository file. Symlinked project directories and source files are unsupported. Intentional source/API changes require human review and a new
baseline; they are not silently accepted as preservation.

Without an engine preview, model comparison conservatively permits only the root version
and JMS/JAXB substitutions in recognised type fields. Other legitimate engine mappings,
such as exception-resolver changes or materialised defaults, may need an exact preview
as described below. Such differences are reported for review rather than ignored.

### Including generated verification tests

Before collecting the before report, optionally use Studio's **Generate/Refresh Verification
Tests…** action and commit the resulting tests under `generated/src/test`. The Maven reactor
runs these structural checks alongside your configured business tests. Keep the tests unchanged
through both verification runs and comparison; CLI apply preserves these tests. Refresh it
only after reviewing the comparison, to establish a new baseline.

The model-hash warning in `GeneratedVerificationSupport` is expected after migration. Its
`BASELINE_MODEL_SHA256` identifies the saved model at generation, not Java sources. These tests cover structure and interface compatibility only; a passing report does not
prove working flows. Add and maintain runtime and business scenarios in `user-flow-tests`.
See [generated verification baselines](GeneratedVerification.md).

## Perform the upgrade from the command line

Supported paths are **V3.3.9 ↔ V4.1.6**. Stop the module, save changes and **close the project
in IntelliJ** before CLI apply. The standalone tool cannot coordinate with Studio's
in-memory model, pending edits or IDE generation guards. Use a single CLI writer.

From the project root (replace `/path/to/tools` with the extracted distribution):

```sh
python3 /path/to/tools/bin/studio_upgrade.py verify . --report migration-before
/path/to/tools/bin/studio-cli preview --project . --to V4.1.6 --plan migration-plan.json --update-user-imports true
# Read the findings in the terminal and review migration-plan.json.diff.
/path/to/tools/bin/studio-cli apply --plan migration-plan.json
# Set JAVA_HOME for the target, then:
python3 /path/to/tools/bin/studio_upgrade.py verify . --report migration-after
python3 /path/to/tools/bin/studio_upgrade.py compare migration-before/report.json migration-after/report.json --plan migration-plan.json --report migration-comparison
```

Preview renders all changes without modifying the project model, generated files or POM.
It writes only the requested new plan and its readable `.diff` companion. Blockers prevent
creating an applicable plan. The plan is tied to that project path and renderer output;
apply rejects stale files, altered developer files or a changed renderer. Keep the plan
private: it includes original/proposed generated configuration and POM contents.

Apply is explicit authorization to write the previewed generated files and root POM.
It preserves `AGENTS.md` and developer code except explicitly selected, reviewed user import updates. In the POM it removes unchanged, unversioned
source-component dependencies retired by the target pack, while retaining explicit
overrides and unrelated dependencies. It stores a recovery snapshot under
`.ikasan-studio/migrations/`, and uses the same optimistic checks and rollback-capable
writes as the IDE. It does not run tests automatically or change IDE SDKs, running services,
databases or external messages. Compilation/test failure after apply leaves the migration
applied, with evidence available for diagnosis.

Reopen the project and reload Maven after CLI migration. Set the project/Maven/run JDKs
in IntelliJ yourself (Java 11 for V3.3.9, Java 17 for V4.1.6); the CLI does not modify IDE
workspace settings. **Restore Previous Ikasan Migration** in Studio can review and restore
the CLI's recovery snapshot. Keep a version-control backup as usual.

`compare --plan` checks the exact source/target models produced by the engine preview,
including supported property and exception mappings. You can also create a CLI preview
before an IDE migration and use it for comparison without applying it from the CLI.

Verification exit codes: **0** PASS, **1** failure, **2** incomplete (no successful test
execution). CLI preview returns **2** for migration blockers; errors return **1**.
Scripts can chain steps only on success, preserving the explicit preview/review/apply
boundary. The fixture-specific `fixture.py` remains separate and is not an end-user tool.

## Verification of this tool (23 September 2026)

The packaged tools completed a real in-place V3.3.9 → V4.1.6 migration of a disposable
copy of the 12-flow regression module. Before and after Maven verification passed; the
same aggregate acceptance test executed, and all 29 protected developer/test/project
files were unchanged. The exact-preview comparison passed all seven checks. Reports
are under `regression-tests/migration/build/end-user-cli-verified/migration-before`,
`migration-after` and `migration-comparison` in the Studio development checkout.

Focused tests cover stale/tampered previews, JSON ordering across JVM runs, developer
file protection, missing/stale test evidence and source/model comparison. Existing model
migration and recovery tests passed. A real in-place build exposed obsolete JAXB
requirements left in the source POM; the shared engine now removes unchanged unversioned
retired requirements, with upgrade/downgrade and explicit-override regression coverage.

This evidence covers the Linux CLI workflow. Windows launchers are packaged by Gradle
but have not been exercised interactively. It does not replace the IDE migration/recovery
release checks or tests for each developer's integrations.

### Resolving failed verification

Reports include a **How to resolve the findings** section for each failed or incomplete check.
Maven's `-V` banner records the Java version used by the build. Comparisons show the target
minimum JDK (11 for V3.3.9, 17 for V4.1.6) and the recorded Maven Java version. Older logs
without that banner are labelled **not recorded**; the comparison does not infer a past JVM
from your current environment. Maven toolchains may select a different compiler.

For a JDK mismatch, set `JAVA_HOME` to the target JDK, update `PATH`, and confirm using the
same Maven executable or wrapper with `-version`. Then collect a fresh after report and
compare again. Compilation failures can also explain missing test cases; fix those first.
Recognised JAXB import failures include guidance on adapting developer-owned code, while
source changes remain visible for review. Reports never silently turn those changes into a pass.

### Developer import updates

In Studio, **Apply recommended migration search and replace (review changes before applying)**
is selected by default. This currently updates compatible Java import declarations only.

The IDE and offline CLI share the same Java migration engine; Python collects and compares
test evidence. The standard workflow on Windows, Linux and macOS includes compatible
developer import updates during preview:

```sh
studio-cli preview --project . --to V4.1.6 --plan migration-plan.json --update-user-imports true
studio-cli apply --plan migration-plan.json
```

Use the distribution's `.bat` launcher on Windows. To preserve all developer imports instead,
pass `--update-user-imports false` or omit the option. The selection is stored in the plan; apply
recomputes the edits and rejects stale files or altered proposals. The `.diff` includes every
changed source file. Review before applying. Recovery snapshots include the original files.

Only Java import declarations under `user/src/main/java` are eligible. Directional target-pack
`userImportPrefixes` rules cover JMS, JAXB and resource API namespaces. Comments, strings,
method bodies, fully qualified references and test sources are preserved. Files containing
Unicode escapes are left unchanged. Review remaining references and dependency compatibility
manually, then compile and run the unchanged functional tests.

Supply `--plan migration-plan.json` when comparing. Source changes pass the preservation check
only when both before/after hashes match the exact planned bytes; extra edits still fail. Without
the plan, import changes remain reported as source changes requiring review. An IDE recovery
snapshot can also supply those exact changes through `--plan`.
