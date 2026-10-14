# Ikasan 5 development pack

This is an **unreleased development pack**, not a supported runtime or Marketplace deliverable.
It is intentionally absent from `studio-bundled-packs`, the plugin palette and published migration paths.
Resources live here rather than in the released-pack directory so normal plugin/BOM/help-link gates
continue to describe only the supported V3 and V4 packs.

## Baseline

- Pack ID / runtime: `V5.0.1-SNAPSHOT` / `5.0.1-SNAPSHOT`.
- Pack revision: `0.1.0-SNAPSHOT` (independent of the runtime version).
- Reference source: Ikasan commit `811a14b1ee7c202868d4bfbcac505d5da6a5565e`, inspected 14 October 2026
  in `/home/hidavi/dev/ws/ik5/ikasan5`.
- The reference parent declares Java **26**, Spring Boot **4.1.0**, Spring **7.0.8** and ActiveMQ **6.2.6**.
- JMS, resource and JAXB metadata retain the Jakarta namespace. The V5 email endpoint still imports
  `javax.mail`; do not mechanically migrate its mail fixtures to Jakarta Mail.
- The V4 ActiveMQ 6.2.4 overrides are removed. The V5 BOM owns these versions.
- Help links are pinned to the reference commit because the formal release tag does not yet exist.
- V3 migration rules copied from V4 have been removed: they are not evidence of V5 compatibility.

The local source version is evidence for this development baseline, not a prediction of Mick's
formal release number or final Java requirement. Reconcile both with the release when it is cut.

## Repeatable checks

Run from the repository root:

```sh
./gradlew -p headless :studio-pack-v5:test
./gradlew -p headless :studio-pack-v5:generateCompatibilityFixture
# Use a Java 26 JDK for Maven; Gradle and the generator retain their existing Java 17 toolchain.
JAVA_HOME=/path/to/jdk-26 mvn -nsu \
  -f headless/studio-pack-v5/build/compatibility-fixture/pom.xml test-compile
# Bounded, local application startup + real generated observation test:
JAVA_HOME=/path/to/jdk-26 mvn -nsu \
  -f headless/studio-pack-v5/build/compatibility-fixture/pom.xml \
  -Dtest=ExampleFlowFlowTest test
./gradlew -p headless :studio-pack-v5:jar
```

The fixture includes an Event Generating Consumer → Dev Null Producer flow, generated application,
module configuration, component factory, injection controller and flow-test scaffolding/support.
The export task renders into `build/compatibility-fixture`; it does not alter any developer sample
project, publish artifacts or start endpoints. Maven needs the matching snapshot BOM/artifacts
installed locally or a configured snapshot repository. Snapshot resolution is mutable: record
resolved versions when reporting verification, and repeat against the formal release.

## Verification on 14 October 2026

- 57 generator tests and the isolated V3/V4/V5 pack contracts (3 + 3 + 4 tests) passed.
- Generated application (5 Java files) and generated flow-test/support code (15 Java files)
  compiled with Java 26.0.1 against resolved V5 snapshot artifacts.
- The generated `ExampleFlowFlowTest` passed: real Spring context startup, expected initial
  messages, one-second RUNNING observation, a later event without flow restart, and STOPPED teardown.
  This continuously producing source does **not** verify idle-after-exhaustion or rollback.
- The first startup attempt exposed ignored `javax.annotation.Resource` fields under Spring 7.
  V5 module, flow and component-factory templates now use `jakarta.annotation.Resource`;
  a rendering regression assertion protects that change.
- Maven resolved Boot 4.1.0 and **Spring 7.0.9**, whereas the inspected source checkout declares
  Spring 7.0.8. This is a moving snapshot baseline, so do not describe the runtime test as
  verification of the exact source commit. Reconcile with the formal BOM when released.
- The V5 JAR is built independently. No runtime endpoint/migration/IDE coverage beyond the
  above minimal flow is claimed. No existing sample project was changed.

## Before enabling for developer review / release

- Repeat compilation of the generated application **and test support** against the formal V5 dependency graph.
- Review Boot 4 auto-configuration packages, generated application properties, Blue Console startup,
  persistence, transaction handling and test-context startup. V4 templates remain provisional until exercised.
- Expand compilation/runtime probes to every consumer, producer, converter, filter, router, splitter,
  exception resolver and custom implementation contract; include local JMS/SMTP/FTP/SFTP fixtures.
- Run delivery, idle-readiness and later-delivery checks without restarting flows, including rollback.
- Test the real Studio create/generate/run/debug/validate/flow-test journey with the development artifact.
- Add explicit migration rules only after V4 → V5 differences have been checked with developer-owned code.
- When the formal release is available: replace the snapshot identity, BOM and commit help links with
  verified release coordinates/tag; rerun the full compliance gates before adding the artifact to
  `studio-bundled-packs`. Do not ship snapshot coordinates in a Marketplace build.

See [meta-pack compliance](../../src/main/resources/studio/metapack/METAPACK_COMPLIANCE.md).
