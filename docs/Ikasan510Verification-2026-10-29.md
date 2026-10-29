# Ikasan 5.1.0 pack verification — 29 October 2026

This is automated evidence for the current candidate, not Marketplace release certification.
The pack update is implemented; the full runtime gate is **not green** because the released
runtime still fails clean-rollback checks. The failures are retained, not suppressed or patched.

> Baseline correction: current packs target **JDK 25 LTS**. The initial results below
> used JDK 26; see the Java baseline correction and JDK 25 results at the end.

## Initial baseline and distribution

- Published [standalone BOM 5.1.0](https://repo.maven.apache.org/maven2/org/ikasan/ikasan-eip-standalone-bom/5.1.0/ikasan-eip-standalone-bom-5.1.0.pom).
- Release tag [ikasaneip-5.1.0](https://github.com/ikasanEIP/ikasan/tree/ikasaneip-5.1.0),
  commit `565c8f854e5b5ca0e690c78a0ba6112b89af8ff7`.
- Built-in pack `V5.1.0`, independent revision `1.0.0`, generator API `1`, application JDK **26**.
- Plugin/authoring toolchain remains JDK **17**. Normal plugin and CLI distributions include
  V3.3.9, V4.1.6 and V5.1.0. The historical V5.0.1-SNAPSHOT remains an optional compatibility
  payload; existing projects and revision locks are not silently changed.
- The independent Maven example uses **ExampleIkasan5**, runtime 5.1.0 and revision 1.0.0,
  so it can coexist with the built-in pack. Its six JDK 17 tests include actual archive
  installation and restart discovery in separate JVMs with example resources absent from
  the host classpath. Discovery and flow/test-template rendering work with snapshots disabled.

## Automated checks

- `./gradlew check buildPlugin verifyReleaseArchive` passed, including published BOM
  resolution, tagged help links and the released pack's own checks in the CI dependency graph.
- Full plugin suite: **1,626 tests**, zero failures/errors.
- Full headless suites: **657 tests**, zero failures/errors.
- Independent Maven example: **6 tests**, zero failures/errors.
- Release verification tooling: **11 Python tests** passed, including rejecting incorrect
  versions of independently versioned libraries.
- Archive audit passed with **1,502 checked source resources** and **578 Studio classes**;
  standard pack IDs are exactly V3.3.9, V4.1.6 and V5.1.0.
- `verifyPlugin` passed on IDEA Community 2024.2 (`242.20224.300`), Community 2024.3.7
  (`243.28141.18`) and IDEA 2026.2.2 (`262.10315.125`). No internal API usages were reported;
  existing experimental/deprecated warnings remain. Interactive IDE/JDK 26 workflows still
  need their normal release-candidate smoke checks.

The generated email fingerprint helper now applies the pack's declared REST-to-Studio
configuration mappings. A regression compiles the actual emitted helper, checks native saved
models for all three email configurations, and proves a changed sender still invalidates wiring.
The new release's explicit migration-rehearsal mappings also include the payload/map converters;
this does not enable a new automatic IDE migration path.

## Fresh runtime gate

```sh
python3 headless/studio-pack-v5-release/scripts/verify.py \
  --java-home /path/to/jdk-26 --maven /path/to/mvn
```

Final invocation: `headless/studio-pack-v5-release/build/verification/20261029T084631.388785Z/`.
That ignored local directory retains summary JSON/Markdown, commands, generated projects,
archive hashes, resolved dependency trees and copied test reports. The run ended with exit 1;
only the rollback suite and its required test-evidence check failed.

| Suite | Tests | Failures | Errors | Skipped | Result |
| --- | ---: | ---: | ---: | ---: | --- |
| Application, Blue Console and generated flow test | 2 | 0 | 0 | 0 | PASS |
| Component catalogue compilation (no runtime tests) | 0 | 0 | 0 | 0 | PASS |
| JMS/Text/Object, SMTP and FTP/SFTP transports | 9 | 0 | 0 | 0 | PASS |
| Routing, filtering, translation and XML processing | 4 | 0 | 0 | 0 | PASS |
| Scheduled and local-file input | 2 | 0 | 0 | 0 | PASS |
| Actual user/generated/user-flow-tests reactor | 1 | 0 | 0 | 0 | PASS |
| Explicit V3/V4 migration rehearsals (not enabled in IDE) | 2 | 0 | 0 | 0 | PASS |
| Injected transaction rollback and XA controls | 10 | 6 | 0 | 0 | FAIL |

Transport tests verify first delivery, idle readiness and later delivery in the same application,
including receiver-side SMTP and FTP/SFTP checks. The catalogue fixture is intentionally
compilation-only. Reactor archive ownership checks passed. Every resolved dependency audit
passed: platform artifacts resolve to 5.1.0; the published release parent independently pins
`org.ikasan:commons-dbcp2:1.0.0` and `org.ikasan:bigqueue:1.0.0`. These are exact exceptions,
not group-wide exclusions or version overrides.

## Remaining runtime failures

Six of ten transaction-failure/control tests fail. JMS reports XA resource-end failures
(`ARJUNA016129`). FTP/SFTP delivery can recover, but rollback still reports `ARJUNA016031`
and `ARJUNA012091` / `FINISH_ERROR`. Later delivery does not establish clean transaction
rollback. No Ikasan source tree, runtime JAR or test expectation was changed to hide this.

The adapter's generation/normal-delivery checks are green; this evidence does not establish
transaction readiness of the released Ikasan runtime. Investigating or fixing those runtime
lifecycle failures is separate work in Ikasan itself. Automatic migration choices remain V3/V4.

## Handover artifacts

- `downloads/ikasanstudio-1.0.0-beta.1.zip`: rebuilt development plugin (not a new Marketplace release).
- `downloads/ikasan5-metapack-1.0.0-example.zip`: standalone source project plus its installable JAR
  under `artifacts/`, with no build caches, test logs or private configuration included.
- Matching `.sha256` files accompany both ZIPs. Editable example sources remain under
  `ikasan-studio-ancillary/ikasan5-metapack/`; generated originals remain under `build/`.

## Java baseline correction

The initial runtime evidence above was collected with JDK 26 before the project owner confirmed JDK 25 LTS as the required baseline. Current V5.1.0 and ExampleIkasan5 metadata and verification tools now require Java 25. Inspection of all 1,700 base classes in cached released Ikasan 5.1.0 JARs found class-file major version 69 (Java 25). The historical snapshot includes Java 26 bytecode and remains a historical opt-in adapter. The earlier JDK 26 results must not be presented as JDK 25 runtime verification.
