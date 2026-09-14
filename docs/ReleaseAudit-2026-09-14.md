# Compatibility and release audit — 14 September 2026

Status: **automated gates passed; manual release sign-off remains open**.

Audited checkout: `813f6499` plus the release-engineering changes in this working tree.
Candidate: `build/distributions/ikasanstudio-1.0.0.zip`.
SHA-256: `db427df85b6a17fdf5237b57c0a4bbc9febccf221f577489e3197133f25fa9e1`.
Rebuilding may produce a different archive; retain the report matching the ZIP actually distributed.

| Check | Result and evidence |
| --- | --- |
| Complete plugin test suite | PASS: 936 tests, no failures/errors/skips; `build/test-results/test/` |
| Standalone Java 17 headless suites | PASS: 325 tests, no failures/errors/skips across five modules; `headless/*/build/test-results/test/` |
| Plugin distribution | PASS: `buildPlugin` |
| Oldest supported release | PASS: IDEA Community 2024.2, IC-242.20224.300; four deprecated API usages |
| Compilation target | PASS: IDEA Community 2024.3.7, IC-243.28141.18; four deprecated API usages |
| Newest pinned stable release | PASS: IDEA 2026.2.2, IU-262.10315.125; sixteen deprecated API usages, including one scheduled for removal; no internal API failures |
| Packaged resources and metadata | PASS: 1,020 runtime resources compared with source, 363 Studio classes checked for Java 17 bytecode; both V3.3.9 and V4.1.6 present; `build/reports/release/archive.json` |
| Clean-profile startup/settings | PASS on IDEA 2024.3.7 / JBR 21.0.7: headless startup loaded IkasanStudio 1.0.0 and completed traversal of 219 configurables; no ERROR entries in the isolated log |
| Interactive installation, upgrade and uninstall | NOT RUN; requires the real Plugins UI and a recorded previous-version candidate |
| Open every item 2 golden project in boundary IDEs | NOT RUN in this session; the new golden-project suite was not located in this checkout |
| Clean machine and cross-OS harness workflows | NOT RUN; the temporary IDE profile does not clear Maven caches or external tool installations |

Verifier evidence is under `build/reports/pluginVerifier/<IDE build>/`. The installed Gradle plugin supplied
the compilation target's JBR 21 to Plugin Verifier for all three binary checks; those checks must not be
described as runtime execution on each IDE's bundled JBR. The isolated IDE smoke test did execute JBR 21.0.7.
The standalone engine suites use the declared Java 17 toolchain.

Isolated startup command:

```bash
./gradlew buildSearchableOptions --rerun -PstudioSandboxDirectory=/tmp/ikasan-studio-release-smoke-20260914
```

Its log is `/tmp/ikasan-studio-release-smoke-20260914/ikasanstudio/IC-2024.3.7/log/idea.log`.
This exercises startup and settings, not user installation, project initialization, execution or uninstall.

## Gaps fixed

- Added explicit oldest/current/newest Plugin Verifier targets and aligned the CI cache key with that configuration.
- Added an archive audit to CI and a reusable `verifyReleaseArchive` Gradle task.
- Included the repository's BSD licence in the plugin JAR and replaced scaffold-only release notes.
- Removed the unused production Mockito Inline dependency and its bundled Mockito/Byte Buddy test tooling.
- Replaced diagnostics' internal plugin-manager lookup with public `PluginAware` descriptor injection.
- Updated an obsolete test that expected a nonfunctional Jump to Code item even without a navigation target.
- Aligned the Gradle wrapper task's configured version with the already checked-in 9.7.1 wrapper.

## Remaining release work

Follow [ReleaseCandidateVerification.md](ReleaseCandidateVerification.md) for installation, upgrade, uninstall,
every-pack project opening and clean-profile sign-off. Link the completed item 2 results from their actual
branch/checkout. This audit did not recreate or claim those results.

`MigrationController` still calls `MavenProjectsManager.scheduleImportAndResolve()`, which the latest IDE
marks for removal. Replace that migration/import integration before the API is removed, and retain the
verifier boundary checks for future candidates. Ordinary deprecations are listed in the verifier reports.

The archive inventory retains third-party notices already embedded in libraries. The separately supplied
`ikasan-spec-component-3.1.0.jar` and `ikasan-studio-ide-mediator-1.0.2.jar` have no embedded licence/notice
entries; confirm their distribution notices before publication. Project-built Studio JARs and the generated
searchable-options JAR are covered by the project licence included in the main plugin JAR.
