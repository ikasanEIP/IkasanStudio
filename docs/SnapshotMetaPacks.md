# Snapshot meta-packs

The built-in packs are **V3.3.9**, **V4.1.6** and **V5.1.0**. Ikasan 5.1.0 requires
**JDK 25 LTS** and is available without enabling snapshot support. The unused
V5.0.1-SNAPSHOT pack and its bundled activation payload have been removed.

For independently installed packs targeting experimental Ikasan runtimes, enable
**Settings → Tools → Ikasan Studio → Allow snapshot meta-packs**. This makes installed
snapshot runtimes available when creating or importing modules. The setting does
not download or install a pack. Use **Tools → Ikasan Studio → Install Meta-Pack…**
and follow its restart guidance when installing external packs.

Disabling the setting hides snapshot runtimes from new selections; installed
resources remain available to existing projects. Snapshot detection uses the
manifest's runtime version, rather than the pack artifact's revision.

Version migration continues to offer the established V3/V4 paths. See the
[released Ikasan 5 pack](../headless/studio-pack-v5-release/README.md) for its
verification evidence and runtime limitations, and
[independent packs](IndependentMetaPackArtifacts.md) for authoring and packaging.

## Unavailable or unwanted external packs

The unavailable-pack warning offers **Manage Meta-Packs**, also available under
**Tools → Ikasan Studio**. Select an external pack that no project still needs,
choose **Remove**, and restart IntelliJ IDEA. The warning keeps the original
validation details below its recovery instructions.

If the external pack is not listed or **Remove** is disabled, close IntelliJ IDEA
before moving its JAR out of the IDE configuration directory under
`ikasan-studio/meta-packs`. Keep the JAR if existing projects may need it restored.
Bundled packs are maintained through plugin updates, rather than external-pack removal.
