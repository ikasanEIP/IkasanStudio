# Ikasan 5.1.0 released meta-pack

`studio-pack-v5-release` supplies **V5.1.0**, runtime **5.1.0**, independent pack revision
**1.0.0** and generator API **1**. It is included in normal Studio plugin and CLI distributions.
It requires no snapshot setting or separate installation. Generated applications require
**JDK 25**; the Studio plugin, generator and resource-only pack authoring build use JDK 17.

The baseline is the published `org.ikasan:ikasan-eip-standalone-bom:5.1.0`, release tag
[`ikasaneip-5.1.0`](https://github.com/ikasanEIP/ikasan/tree/ikasaneip-5.1.0)
(commit `565c8f854e5b5ca0e690c78a0ba6112b89af8ff7`). Help links use that exact tag.
The complete library/templates were carried forward from the V5 snapshot adapter and are
verified separately against released dependencies; no runtime dependency is silently downgraded.

The old `studio-pack-v5` / `V5.0.1-SNAPSHOT` remains an opt-in compatibility pack for existing
snapshot models. It is not an alias for V5.1.0, and existing projects are not silently relabelled
or upgraded. Automatic version-migration choices remain the established V3/V4 paths.

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
