# Supported versions

This page describes the repository configuration for the public beta. Check the release notes of the version you install: an older download may contain different packs or IDE requirements. See [Installation](Installation.md) for availability and installation routes.

| Layer | Repository configuration |
| --- | --- |
| IntelliJ minimum | IDEA 2024.2, platform build 242 |
| Compilation target | IDEA Community 2024.3.7 |
| Configured binary compatibility checks | IDEA Community 2024.2 and 2024.3.7; IDEA 2026.2.2 |
| Bundled Ikasan packs | V3.3.9 and V4.1.6 |
| V3.3.9 project JDK | Java 11 |
| V4.1.6 project JDK | Java 17 |
| Plugin development toolchain | Java 17; optional native MCP module uses a Java 21 toolchain and emits JVM 17 bytecode |

Run IntelliJ using its supplied runtime. Configure the generated project's SDK, Maven runner/importer and Application Run/Debug JRE for its Ikasan version. Changing the project SDK does not require replacing IntelliJ's runtime.

The descriptor has no upper IDE build limit. That permits installation on later IDEs; it is not a claim that future releases have been tested. Binary verification also does not replace interactive installation, upgrade, uninstall and project smoke tests. The [14 September audit](ReleaseAudit-2026-09-14.md) is historical evidence for that checkout, not certification of a newer beta. Re-run the [release-candidate gates](ReleaseCandidateVerification.md) for each release and record its outstanding checks.

Only the two listed packs are bundled. Historic V3.3.x/V4.0.x/VHS references and Ikasan 5 reference sources are not additional supported packs. Use the supported [migration workflow](IkasanVersionMigration.md) to move between V3.3.9 and V4.1.6; custom Java still requires review and application tests.
