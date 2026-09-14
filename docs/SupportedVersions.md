# Supported versions

This page describes the repository's current release candidate, reviewed on 14 September 2026. Check the release notes of the ZIP you install: an older download may contain different packs or IDE requirements.

| Layer | Current candidate |
| --- | --- |
| IntelliJ minimum | IDEA 2024.2, platform build 242 |
| Compilation target | IDEA Community 2024.3.7 |
| Binary compatibility checks | IDEA Community 2024.2 and 2024.3.7; IDEA 2026.2.2 |
| Bundled Ikasan packs | V3.3.9 and V4.1.6 |
| V3.3.9 project JDK | Java 11 |
| V4.1.6 project JDK | Java 17 |
| Plugin development toolchain | Java 17; separate from the generated application's JDK |

Run IntelliJ using its supplied runtime. Configure the generated project's SDK, Maven runner/importer and Application Run/Debug JRE for its Ikasan version. Changing the project SDK does not require replacing IntelliJ's runtime.

The descriptor has no upper IDE build limit. That permits installation on later IDEs; it is not a claim that future releases have been tested. Binary verification also does not replace interactive installation, upgrade, uninstall and project smoke tests. See the [candidate audit](ReleaseAudit-2026-09-14.md) for the actual evidence and outstanding checks.

Only the two listed packs are bundled. Historic V3.3.x/V4.0.x/VHS references and Ikasan 5 reference sources are not additional supported packs. Use the supported [migration workflow](IkasanVersionMigration.md) to move between V3.3.9 and V4.1.6; custom Java still requires review and application tests.
