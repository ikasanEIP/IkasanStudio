# Snapshot meta-packs

Enable **Settings → Tools → Ikasan Studio → Developer meta-packs → Allow snapshot
meta-packs**, click **Apply**, then restart IntelliJ IDEA on first activation.
Studio then offers **V5.0.1-SNAPSHOT** in the new-module Ikasan version selector.
The setting persists in the IDE configuration and applies to all projects.

Ikasan 5 is included as an optional resource-only pack in the plugin. No manual
pack installation, special Gradle flag or network download of the pack is needed.
The generated application's Maven dependencies still need to be available.
Normal plugin and offline CLI classpaths continue to contain only the released
V3/V4 packs. Explicit snapshot build configurations remain available for pack
development, but the IDE selection setting still applies.

The setting controls new-module and import selections, including installed custom
packs targeting a snapshot Ikasan runtime. Pack IDs or targeted runtime versions
ending in `-SNAPSHOT` identify snapshots; the independent pack artifact revision
does not, because the current released-runtime V3/V4 packs also use development
artifact revisions. Importing a snapshot model requires enabling this setting.

Turning it off hides snapshot choices immediately. Previously activated pack
resources are retained so existing projects can reopen and generate with their
recorded version. Pack sources stay fixed for an IDE session; first activation
and updates to the bundled snapshot take effect after restarting the IDE.

## Ikasan 5 demo preparation

1. Build and launch the updated plugin using the normal **Run Plugin** configuration
   (`./gradlew runIdeModern`), or install its built ZIP into the demo IDE.
2. Enable the setting above and restart that IDE. The normal sandbox retains its
   IDE configuration across restarts; a new or deliberately cleared sandbox needs
   its own opt-in.
3. Configure the application project SDK, Maven importer/runner and Application
   Run/Debug JRE to **JDK 26**. Keep IntelliJ itself on its supplied JetBrains Runtime.
4. Open an existing V5 demonstration module or select **V5.0.1-SNAPSHOT** when
   configuring a new module. Let Maven finish importing before running tests.
5. Run the module and demonstrate first delivery, idle readiness and later delivery
   without restarting the module. The **Console** button opens the module-local
   Blue Console after startup.

The owner has tested the routes/branches, converters/translators and
all-consumers/producers demonstration modules. V5 remains experimental: the
[20 October verification](Ikasan5MergeVerification-2026-10-20.md) records six
transaction rollback failures. Successful normal deliveries do not certify
rollback or production readiness. Supported migration choices remain V3/V4.

## Released Ikasan 5

**V5.1.0** is bundled normally and requires no snapshot setting. The old V5.0.1-SNAPSHOT
pack remains available for existing projects and explicit snapshot testing; it is not silently
upgraded to 5.1.0. See the [released pack](../headless/studio-pack-v5-release/README.md) and
[independent authoring example](../ikasan-studio-ancillary/ikasan5-metapack/README.md).
