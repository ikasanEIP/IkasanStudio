# Snapshot meta-packs — external-pack demo branch

On `demo/external-ikasan5-metapack`, Studio ships only V3/V4. There is no embedded V5
payload or automatic activation provider. Enabling **Allow snapshot meta-packs** controls
selection; it does not install a pack.

Build the standalone [Ikasan 5 project](../ikasan-studio-ancillary/ikasan5-metapack/README.md),
install its JAR through **Tools → Ikasan Studio → Install Meta-Pack…**, enable the setting
under **Settings → Tools → Ikasan Studio**, and restart the IDE. The version selector then
offers **V5.0.1-SNAPSHOT**. Generated applications require **JDK 26**.

The setting persists and applies to new-module/import choices. Opt-out hides snapshots
while leaving installed resources available to existing projects. Pack sources are fixed
for the IDE session, so installations and reviewed updates activate after restart.

Use a clean sandbox/profile for the before/after demo. Previous installations and older
optional activations are not removed by switching Studio branches; installation state
belongs to the IDE configuration. See the standalone project's README for the complete
demo and internal-library handover. Known V5 rollback failures remain release blockers.
