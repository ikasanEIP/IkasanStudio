# Ikasan 5.1.0 released meta-pack

`studio-pack-v5-release` supplies **V5.1.0**, runtime **5.1.0**, independent pack revision
**1.0.9** and generator API **5**. It is included in normal Studio plugin and CLI distributions.
It requires no snapshot setting or separate installation. Generated applications require
**JDK 25**; the Studio plugin, generator and resource-only pack authoring build use JDK 17.

The baseline is the published `org.ikasan:ikasan-eip-standalone-bom:5.1.0`, release tag
[`ikasaneip-5.1.0`](https://github.com/ikasanEIP/ikasan/tree/ikasaneip-5.1.0)
(commit `565c8f854e5b5ca0e690c78a0ba6112b89af8ff7`). Help links use that exact tag.
The complete library/templates were carried forward from the V5 snapshot adapter and are
verified separately against released dependencies; no runtime dependency is silently downgraded. Revision 1.0.1 declares the tested ActiveMQ
client/broker 6.3.3 security override; H2 continues to follow the Ikasan core baseline.

The unused V5.0.1-SNAPSHOT pack and its optional bundled payload have been removed. V5.1.0 is the sole built-in Ikasan 5 pack. Automatic version-migration choices remain the established V3/V4 paths.

## Authoring and verification

```sh
./gradlew -p headless :studio-pack-v5-release:test :studio-pack-v5-release:jar
python3 headless/studio-pack-v5-release/scripts/verify.py --java-home /path/to/jdk-25 --maven /path/to/mvn
```

The explicit verifier renders fresh fixture projects, compiles the component catalogue and
runs local transport, processing, input, reactor and transaction tests against the release.
Its logs, dependency inventories, archive hashes and reports are retained under `build/verification/`.
A failing or skipped runtime scenario remains a failing gate; package rendering alone is not
runtime evidence. The independently versioned `org.ikasan:commons-dbcp2:1.0.0` fork and
`org.ikasan:bigqueue:1.0.0` are narrow documented exceptions to the 5.1.0 platform-version audit, as declared by the release parent.
Consult the dated verification report for actual outcomes; the existence of this command is
not a claim that every runtime gate or Marketplace workflow has passed.

For a standalone authoring project that can coexist with the bundled pack, see
[`ikasan-studio-ancillary/ikasan5-metapack`](../../ikasan-studio-ancillary/ikasan5-metapack/README.md).

## Transaction limitation

The unmodified 5.1.0 runtime still reproduces XA resource-end/rollback errors in injected
JMS, FTP and SFTP transaction-failure scenarios. Successful later delivery does not count
as clean rollback. These checks stay failing; no candidate runtime patch or dependency
downgrade is applied. See the [dated release verification](../../docs/Ikasan510Verification-2026-10-29.md).

Revision 1.0.2 adopts the [RE2/J property-validation policy](../../src/main/resources/studio/metapack/METAPACK.md#property-validation-safety-generator-api-2) and semantic Java package-name checks. Existing 1.0.1 model locks require explicit revision adoption.

Revision 1.0.3 removes the legacy H2 Maven template in favour of the engine-provided [independent H2 Java launcher](../../docs/IndependentH2.md). Existing model revision locks require explicit adoption.

Revision 1.0.4 adds per-component launch selection for harness-owned overrides. Regenerate code after explicit revision adoption; component configuration remains unchanged.

Revision 1.0.5 declares concrete FTP/SFTP producer runtime identities for REST import while retaining saved Studio component identities.

Revision 1.0.6 imports Local File Consumer filename lists and SFTP Consumer duplicate-filter/move controls, and declares verified unmapped runtime defaults. Existing model locks require explicit revision adoption. See the [runtime configuration audit](../../docs/RuntimeImportConfigurationAudit.md) for remaining unsupported settings and verification limits.

Revision 1.0.7 recognises the library `org.ikasan.component.endpoint.util.producer.DevNull` runtime class as Dev Null Producer. Runtime import now retains builder construction instead of creating a Generic Producer that requires an external Spring bean. Existing model locks require explicit revision adoption; re-import to correct previously imported Generic Producer classifications.

Revision 1.0.8 applies wrapped-component settings to the outer component rather than its delegate. Runtime import uses the declared delegate contract, and source review can offer a unique compatible implementation for explicit registration. Existing V5 model locks require explicit revision adoption before regeneration. Re-import previously imported wrapper/delegate components to correct their saved implementation identity.

Revision 1.0.9 adds validated Maven compiler/Surefire defaults through the shared build contract.
Explicit root-POM plugin versions and configuration are preserved. Existing model locks require
explicit revision adoption; this does not change the target runtime (5.1.0) or Java baseline (25).
