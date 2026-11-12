# Runtime import configuration audit — 12 November 2026

The audit covers all 12 concrete configuration classes declared by the bundled
V3.3.9, V4.1.6, V5.1.0 and optional V5.0.1-SNAPSHOT component libraries. Generic
developer implementations, custom packs, module/flow/invoker configuration and
custom message-provider wiring remain separate review responsibilities.

## Evidence

The V3 and V4 reference sources were inspected under
`/home/hidavi/dev/ws/ik3/ikasan3` and `/home/hidavi/dev/ws/ik4/ikasan4`.
The documented `/home/hidavi/dev/ws/ik5/ikasan5` path was absent. Rather than
infer V5 behaviour from another major version, `javap -p -c` was used against
the locally cached **5.1.0 release JARs**. The resulting field/type inventory
and corrected builder methods were also checked against the exact 3.3.9 and
4.1.6 release JARs. The optional snapshot was compared using its cached
`5.0.1-SNAPSHOT-sources.jar` artifacts; this evidence does not certify future
snapshot builds.

All inspected release artifacts use group `org.ikasan`, with versions 3.3.9,
4.1.6 and 5.1.0: `ikasan-builder`, `ikasan-component-converter`,
`ikasan-email-endpoint`, `ikasan-ftp-endpoint`, `ikasan-jms-spring`,
`ikasan-quartz-endpoint`, `ikasan-sftp-endpoint` and `ikasan-utility-endpoint`.
Constructor defaults and builder configuration creation/overrides were checked
before declaring defaults; a Java field initializer alone is insufficient.

The independent [field/type inventory](../src/test/resources/studio/runtime-import/configuration-fields.json)
includes inherited fields. These inventories match across the inspected versions
after normalising Java type qualification. The inventory is an audited expectation,
not generated from Studio descriptors during tests. Refresh it against the exact
runtime APIs when changing a supported Ikasan baseline.

## Findings and changes

| Component | Result |
| --- | --- |
| Local File Consumer | Import `filenames` as a typed string list, preserving regex escapes and commas. Declare unchanged scheduling/recovery defaults, `dynamicFileName=false` and `followSymbolicLinks=true`. |
| SFTP Consumer | Add builder-supported `filterDuplicates`, `filterOnFilename`, `filterOnLastModifiedDate` and `moveOnSuccess` properties and import bindings. Declare unchanged scheduling/recovery defaults and `provideRelativePath=false`. |
| SFTP Producer | Declare the verified `dontUseFileRename=false` default. |
| Scheduled Consumer, FTP Consumer | Existing mappings/default declarations cover supported fields; retain the explicit limitations below. |
| Spring JMS Consumer, Spring JMS Producer | Every inspected configuration field already has a typed mapping, including the JNDI maps. Builder-only controls such as receive timeout are not fields in these runtime configuration records. |
| Email Producer, Logging Producer | Every inspected configuration field already has a typed mapping. |
| JMS Map Message To Payload Converter, Object To XML String Converter | Every inspected configuration field already has a typed mapping. |
| FTP Producer | Existing mappings cover all fields except the configuration-only password-file path. |

The scheduling/recovery defaults are `cronExpressions=[]`,
`passthroughProperties={}`, `persistentRecovery=true` and
`recoveryTolerance=1800000`. Only an exact configuration-class match and an equal
typed JSON value suppress a warning. No string-to-boolean/number coercion, blanket
empty-value filtering or credential suppression was added.

New SFTP boolean properties are optional, without materialised Studio defaults.
Existing models continue to use runtime defaults unless explicitly configured;
imported true/false values are applied through the verified builder setters.
The existing fully populated SFTP test fixture already supplied `moveOnSuccess`,
but the missing descriptor previously caused generation to ignore it. The reviewed
golden output now includes its property and setter.

Typed local-file lists use the existing JSON-list property generation/parser.
Legacy comma-separated deployment overrides remain accepted. Generated default
property values now use JSON lists so entries containing commas remain intact.

V5.1.0 advances from pack revision **1.0.5 to 1.0.6**, retaining generator API 4
and JDK 25. Existing revision-locked V5 models require explicit adoption with
backup. V3/V4 and the retained development pack keep their snapshot revisions.

## Settings that still require review

These fields have no supported Studio builder mapping. Non-null supplied values
remain in the source snapshot and produce review warnings; they are not silently
applied to generated code.

| Components | Remaining unsupported fields |
| --- | --- |
| All scheduled/FTP/SFTP/local-file consumers | `jobName`, `jobGroupName`, `description` |
| FTP and SFTP consumers | `sourceDirectoryURLFactory` (custom wiring) |
| Local File Consumer | `filePath`, `fileNameSpelExpression`, `filePathSpelExpression` |
| SFTP Consumer and Producer | `privateKeyPassphrase`, including an explicit empty string; the construction default is null and there is no builder setter |
| FTP Producer | `passwordFilePath` |

Changed values for unmapped defaults also remain warnings: for example nonempty
`cronExpressions`/`passthroughProperties`, nondefault recovery settings,
`provideRelativePath=true`, `dynamicFileName=true`, `followSymbolicLinks=false`
and `dontUseFileRename=true`. Null/omitted fields remain unset. Unknown fields,
wrong types and mismatched configuration classes remain reviewable.

In the reported SFTP example, the four filter/move controls will now import and
the five unchanged scheduling/recovery/relative-path defaults will be quiet.
The explicitly empty `privateKeyPassphrase` supplied by the original application's
harness will still be listed. Review the original harness/configuration wiring;
retaining the source JSON does not reproduce that wiring.

## Verification

`RuntimeImportConfigurationCoverageTest` accounts for every inventoried field as a
typed mapping, a typed default or an explicit unsupported field, and checks that
every concrete configured palette component has an inventory entry. It runs in
both root and headless bundled-pack tests without requiring local Ikasan sources.
This catches descriptor omissions against the reviewed inventory; it does not
automatically discover changes in a future runtime release.

`IkasanRuntimeImportTest` verifies all four packs: filename-list import/save/load
and property generation, SFTP false filter values and true move values, generated
builder calls, provenance, unchanged defaults, changed unsupported defaults,
wrong types, mismatched classes and credential-value exclusion from review text.
The pack suite retains versioned Java/properties golden expectations.

Verification for this change:

- `./gradlew test --offline`: 1,801 tests, no failures, errors or skips.
- `./gradlew -p headless :studio-bundled-packs:test --offline`: 580 tests,
  no failures, errors or skips.
- `git diff --check`: passed.

These are configuration-contract and generated-output checks. They do not establish
SFTP connectivity, custom provider equivalence or end-to-end application readiness.
No source/destination application or running module was modified by this audit.
