# Ikasan 5 merge verification — 20 October 2026

The Ikasan 5 feature branch was rebased without conflicts onto `origin/main`
(`6fa91079`). Shared Studio fixes and the isolated V5 development infrastructure
are suitable for integration. This is not V5 runtime or Marketplace certification.

## Distribution boundary

Normal plugin and CLI distributions contain **V3.3.9 and V4.1.6 only**.
`-PstudioIncludeSnapshotPacks=true` additionally includes **V5.0.1-SNAPSHOT**.
The shared **Run Plugin (Snapshot Packs)** configuration supplies that flag for
fresh sandbox testing. Generated V5 applications require JDK 26.

A user-facing setting for allowing snapshot meta-packs remains future work.
The build flag controls distribution contents; it is not an installation policy
or runtime settings toggle. Supported migration choices remain V3/V4.

## Automated results

| Check | Result |
| --- | --- |
| `./gradlew -p headless test --offline` | PASS: 539 tests, no failures or skips |
| `./gradlew test buildPlugin verifyReleaseArchive --offline --no-configuration-cache` with isolated sandbox | PASS: 1,447 plugin tests, no failures or skips; default archive audit passed |
| Headless `:studio-test-kit:test` with snapshot flag | PASS: packaged discovery includes all three packs |
| Plugin `OfflineToolsExporterTest`, build and archive audit with snapshot flag | PASS: exported inventory and archive include all three packs |
| Final default build and archive audit | PASS: restored distribution contains V3/V4 only |

These are merge checks, not a new complete Marketplace release audit. Interactive
IDE compatibility verification must still follow the release-candidate gates.

The complete V5 gate was run using JDK 26.0.1 and the local Maven installation:

```sh
python3 -B headless/studio-pack-v5/scripts/verify.py \
  --java-home /home/hidavi/dev/tools/openjdk-26.0.1_linux-x64_bin/jdk-26.0.1 \
  --maven /home/hidavi/dev/tools/maven/bin/mvn
```

Application/Blue Console, component compilation, runtime transports, processing,
input, reactor and migration fixtures passed. All dependency-version audits passed.
The overall gate **FAILED** on rollback fixtures: 10 tests, six failures, no skips.

- JMS `rollbackRedeliveryAndLaterDelivery` and three XA lifecycle cases reported
  `ARJUNA016129: Could not end XA resource`.
- FTP/SFTP `rollbackRetryAndLaterDelivery` reported failed resource rollback and
  transaction abort errors (`ARJUNA016031`, `ARJUNA012091`).

Delivery recovered in these cases, but transaction cleanup was not clean. These
failures remain V5 release blockers; neither assertions nor runtime sources were
changed to suppress them. See the [V5 development guide](../headless/studio-pack-v5/README.md)
for existing investigation and readiness criteria.

Local raw evidence, including logs, dependency trees and Surefire reports, is under
`headless/studio-pack-v5/build/verification/20261020T200913.455775Z/`.
This ignored build directory is local evidence and is not shipped with the repository.

## Interactive evidence

The owner reports completing tests of the routes/branches, converters/translators
and all-consumers/producers demonstration modules. That evidence is separate from
the automated rollback gate and does not override its failures.
