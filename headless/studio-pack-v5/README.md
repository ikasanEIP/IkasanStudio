# Ikasan 5 development pack

This is an **unreleased development pack**, not a supported runtime or Marketplace deliverable.
The plugin embeds an optional resource-only copy, outside the normal plugin/CLI classpaths.
Enable **Settings → Tools → Ikasan Studio → Allow snapshot meta-packs**, apply and
restart the IDE on first activation. The selector then offers `V5.0.1-SNAPSHOT` without
an external installation. See [demo preparation](../../docs/SnapshotMetaPacks.md). Configure the
application project SDK, Maven runner and Run/Debug JRE as JDK 26.
This development inclusion does not certify a Marketplace release or enable V5 migrations.
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

For a complete development assessment, use the single verification command from the repository root:

```sh
python3 headless/studio-pack-v5/scripts/verify.py \
  --java-home /path/to/jdk-26 --maven /path/to/mvn
```

This explicitly exports fresh fixtures, runs the pack contracts, builds its JAR, compiles the
full catalogue and runs all seven fixture suites (including the strict rollback gate). It continues
with independent suites after a Maven failure and returns a nonzero exit code if any stage fails.
Missing or skipped runtime tests are not accepted as successful runtime evidence. **The current
snapshot is expected to fail the rollback gate**; the command does not apply the candidate patch.

Timestamped evidence is saved under `build/verification/`: command logs, preserved Surefire
reports, resolved dependency trees, Java/Maven versions, Git commit and working-tree status,
pack metadata and pack archive SHA-256 hashes. `summary.md` provides a readable table with log links;
`summary.json` records stage results and test
counts. Keep this directory when comparing a snapshot with the formal release; export tasks
replace their own generated directories but do not replace this evidence. Dependency trees record
resolved versions, not immutable checksums of every snapshot dependency. The runner also audits
structured Maven dependency trees for every declared reactor module, including transitive test
dependencies. Missing/malformed evidence or a mismatched Ikasan version fails the gate and names
the module, dependency, resolved version and required version in its audit report.

One narrowly documented independent artifact is allowed: `org.ikasan:commons-dbcp2:1.0.0`, the
Ikasan DBCP fork declared by the V5 reference parent. This is an audit expectation, not a Maven
override or permission to accept other versions. Its reason is recorded in each report; reconcile
it with the formal release. All other `org.ikasan` artifacts must match the pack's `ikasanVersion`.
This checks platform-version consistency, not full third-party dependency convergence.

Gradle retains the caller's JDK configuration; only Maven uses `--java-home`. Maven needs access
to the matching dependencies. Each command has a 900-second timeout, adjustable with `--timeout`.
Run one verification at a time: its fixture directories are shared with the individual export tasks.
It starts only the disposable local test services and does not publish or
modify developer projects. To test the evidence collector itself:

```sh
python3 -B -m unittest discover -s headless/studio-pack-v5/scripts -p 'test_*.py'
```

Individual commands remain available. Run from the repository root:

```sh
./gradlew -p headless :studio-pack-v5:test
./gradlew -p headless :studio-pack-v5:generateCompatibilityFixture
# Use a Java 26 JDK for Maven; Gradle and the generator retain their existing Java 17 toolchain.
JAVA_HOME=/path/to/jdk-26 mvn -nsu \
  -f headless/studio-pack-v5/build/compatibility-fixture/pom.xml test-compile
# Bounded, local application startup + real generated observation test:
JAVA_HOME=/path/to/jdk-26 mvn -nsu \
  -f headless/studio-pack-v5/build/compatibility-fixture/pom.xml \
  -Dtest=ExampleFlowFlowTest,BlueConsoleTest test
./gradlew -p headless :studio-pack-v5:jar
```

The fixture includes an Event Generating Consumer → Dev Null Producer flow, generated application,
module configuration, component factory, injection controller and flow-test scaffolding/support.
The export task renders into `build/compatibility-fixture`; it does not alter any developer sample
project, publish artifacts or start endpoints. Maven needs the matching snapshot BOM/artifacts
installed locally or a configured snapshot repository. Snapshot resolution is mutable: record
resolved versions when reporting verification, and repeat against the formal release.

## Verification on 14 October 2026

The combined verification command was exercised on this date: nine pack tests passed, catalogue
compilation passed, and the Maven suites ran **20 tests: 17 passed, three failed, none skipped**.
All failures were the strict JMS/FTP/SFTP rollback diagnostics documented below. Dependency
evidence was collected for every suite and the runner returned exit code 1. Four evidence-collector
regressions also pass; Java 17 is rejected before fixture export. These results do not apply the
experimental file-transfer patch.

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
- The V5 JAR is built independently. Subsequent transport coverage is recorded below; migration
  and interactive IDE workflows remain unverified. No existing sample project was changed.

## Generated application and Blue Console HTTP check

Verified on 14 October 2026: both `BlueConsoleTest` and `ExampleFlowFlowTest` pass together
under Java 26; nine pack tests and four verification-script tests also pass. This targeted
run does not replace the separately failing rollback gates.

The compatibility fixture also includes `BlueConsoleTest`. It starts the generated application
with the existing isolated test database, loopback binding and allocated HTTP port. Through a
real HTTP client it checks:

- `/login.jsp` renders a login form rather than an error page.
- Anonymous module-list access requires authentication.
- The standard local `admin` / `admin` form login redirects to `/home.htm`.
- Subsequent requests use the authenticated session cookie, without an Authorization header.
- The module list contains `Ikasan5Review`, and its details page contains `Example Flow`.
- Closing the application context closes its HTTP listener.

This exercises the actual servlet, security and JSP stack against the generated V5 application;
it is not a mocked MVC test. The full verification command includes it automatically. Run it alone
with `-Dtest=BlueConsoleTest` on the compatibility fixture Maven command. It does not test browser
JavaScript, visual layout, every administrative operation or IntelliJ's Console launch action.
The test credentials apply only to the disposable local test application.

## Resolved platform-version audit

Verified on 14 October 2026: fresh structured dependency trees from all six fixture suites pass
the version audit (19 application fixtures, plus their aggregator projects). All resolved Ikasan
platform artifacts use `5.0.1-SNAPSHOT`; the independently versioned DBCP fork uses `1.0.0`.
Nine verification-script tests cover mixed transitive versions, missing child reports, malformed
and empty evidence, and rejection of a changed fork version, alongside the runtime-report checks.
No dependency versions were changed by this work. These dependency-only checks do not replace
runtime verification or resolve the known rollback failures.

## Actual archetype Maven reactor

`ReactorFixtureTest` uses the checked-in archetype's root, `user` and `generated` POMs. It applies
the production `IkasanPomModel.applyBuildContract` path and metadata dependencies to the parent,
then renders the real `user-flow-tests` POM with `FlowTestScaffold`. Developer implementations are
placed in `user`, generated sources/resources in `generated`, and test sources/resources in
`user-flow-tests`. The test module depends on `generated`, which in turn depends on `user`.

```sh
./gradlew -p headless :studio-pack-v5:generateReactorFixture
JAVA_HOME=/path/to/jdk-26 mvn -B -nsu -fae \
  -f headless/studio-pack-v5/build/reactor-fixture/pom.xml verify
```

Verified on 14 October 2026: all four reactor projects build successfully on Java 26, and the
separate flow-test module passes the generated-email scenario (first/idle/later delivery,
environment overrides and server cleanup). The archetype's plugin versions are retained; no
local `install` is needed. All four resolved dependency trees pass the V5 version audit. Built
archives contain their expected user/application classes, with no misplaced implementation or
flow-test support classes in production JARs.

The complete verification command exports and runs this fixture and enforces archive ownership.
Ten pack tests and ten verification-script tests pass after this addition. Archetype POMs are
tracked as inputs to the fixture's Gradle checks. The fixture is disposable and does not modify
the archetype or a developer's project. It validates the generated reactor layout, not interactive
Maven archetype creation, IntelliJ import or a packaged executable application launch.

## Full catalogue compilation coverage

The `ComponentCoverageTest` fixture covers all **27 runtime palette components**: seven consumers,
seven producers, four converters, two filters, two splitters, two routers, a broker, a translator
and a debug transition. Module/flow wiring and all five metadata-declared exception actions are
also generated. Decorative endpoint icons have no standalone Java implementation to compile.
An explicit component-name matrix is compared with the loaded catalogue, so adding a runtime
component without a corresponding fixture fails the test.

```sh
./gradlew -p headless :studio-pack-v5:generateComponentCoverageFixture
JAVA_HOME=/path/to/jdk-26 mvn -nsu \
  -f headless/studio-pack-v5/build/component-coverage-fixture/pom.xml test-compile
```

Verified on 14 October 2026: **71 application/stub Java files and 43 flow-test/support Java files**
compile under Java 26.0.1 with the V5 snapshot BOM. The fixture includes generated business stubs,
two populated branches for each router, local file input, JMS/FTP/SFTP/SMTP support, and email
list/map property bindings. It does not exercise every optional setter or conversion recipe.

These are **compilation fixtures**, with placeholder business logic and endpoint configuration.
Their POM defaults `skipTests=true`; they must not be counted as successful runtime delivery tests.
`COVERAGE.txt` records the component matrix alongside the generated files. The separate minimal
smoke fixture remains runnable and checks live application/flow behaviour.

All export tasks replace their own directories under `build/`; do not put developer work there.
No existing sample project is read or modified by these tasks.

## Local transport runtime fixtures

`RuntimeTransportFixtureTest` exports nine independent applications with generated factories,
configuration and flow-test support. The deliberate scenario implementations supply known data;
they do not run the unfinished business stubs from the catalogue compilation fixture.

```sh
./gradlew -p headless :studio-pack-v5:generateRuntimeTransportFixtures
JAVA_HOME=/path/to/jdk-26 mvn -B -nsu -fae \
  -f headless/studio-pack-v5/build/runtime-transports/pom.xml test
```

The Gradle task only renders the applications. **The Maven command runs the transport tests.**
It starts an embedded, non-persistent ActiveMQ broker and loopback SMTP/FTP/SFTP servers on
allocated ports; no external service or credentials are needed. Each application owns a fresh
Spring context, in-memory database and temporary directories.

| Scenario | Delivery evidence |
| --- | --- |
| JMS consumer → converter → JMS producer | Exact text received from the output queue; no extra messages |
| JMS ObjectMessage → object converter → JMS producer | Unwrapped String payload delivered as text; no extra messages |
| Generic consumer → converter → email producer | Exact SMTP bodies, including two recipient copies per input |
| Generic consumer → generated Email Converter → email producer | Generated body plus environment-overridden recipient list, sender and subject |
| Generic consumer → configured Email Converter → email producer | Generated custom configuration bean; scalar, list and map environment overrides affect delivered bodies |
| FTP consumer → Dev Null producer | Exact downloaded payload observed after the producer |
| Generic consumer → converter → FTP producer | Exact files delivered to the local FTP server |
| SFTP consumer → Dev Null producer | Exact downloaded payload observed after the producer |
| Generic consumer → converter → SFTP producer | Exact files delivered to the local SFTP server |

Each scenario delivers `first`, `later` and `third` through the same running flow. Generated
`runBatches` checks RUNNING and no extra producer output during a one-second idle window after
each delivery, and checks STOPPED on teardown. Additional assertions check that transport server
ports close, or that the owned JMS broker disappears from its registry after cleanup. The JMS
fixture explicitly starts and stops its broker, disables VM auto-creation, and keeps one client
connection open for its lifetime. Closing JMS connections alone is not treated as broker cleanup.
Queue names are literal when no destination JNDI properties are supplied, as in this fixture.

Verified on 14 October 2026 with Java 26.0.1: **all seven runtime scenarios passed** (three inputs
per flow, including receiver and cleanup checks), with nine V5 pack-contract/export tests passing after the additional coverage below.
Maven Surefire reports are under `build/runtime-transports/<scenario>/target/surefire-reports/`.

Verified separately on 14 October 2026: `smtp-generated` and the original `smtp` scenario pass,
and all nine pack tests pass. The earlier combined 20-test result predates this additional scenario.

The additional `smtp-generated` scenario exercises the actual generated Email Converter stub.
Its model supplies sender, recipient-list and subject defaults; `module-test.properties` overrides
all three without changing the generated Java. The mailbox checks both recipient addresses, subject
and body across first/idle/later delivery; the running producer configuration checks the sender.
The V5 endpoint maps `from` to the mail-session `mail.from` setting. A MIME `From` header was absent
in the captured messages, so this fixture does not claim to verify that header. Transport properties
are redirected to the local SMTP server, as in the other SMTP fixture.

Run it independently with `-pl smtp-generated` on the transport Maven command above. The complete
verification command discovers it through the exported reactor automatically.

The `smtp-configured` scenario additionally creates a custom `MailSettings` bean using the
pack's real configuration-class template. The generated factory injects it into the converter;
the fixture supplies the developer-owned fields, accessors and converter implementation.
Production properties supply defaults, while `module-test.properties` overrides a String,
integer, list and map. All four affect the delivered email body, so missing binding or injection
fails the delivery assertions. No configuration bean is manually injected by the test.

Verified separately on 14 October 2026 using Java 26.0.1: `smtp-configured` and
`smtp-generated` both pass, including first/idle/later delivery and server cleanup;
all ten V5 pack tests pass. Run the pair with `-pl smtp-configured,smtp-generated`.
The full verification runner includes the new scenario automatically. This is representative
custom-bean coverage, not proof of every custom property type or provider implementation.

These tests cover successful delivery and lifecycle, not injected failures, transaction rollback,
external server compatibility, TLS/FTPS or every optional setting. The snapshot remains mutable;
repeat the tests against the formal V5 BOM before release.

## Processing, timer and file-input runtime fixtures

```sh
./gradlew -p headless :studio-pack-v5:generateProcessingFixtures :studio-pack-v5:generateInputFixtures
JAVA_HOME=/path/to/jdk-26 mvn -B -nsu -fae \
  -f headless/studio-pack-v5/build/processing-fixtures/pom.xml test
JAVA_HOME=/path/to/jdk-26 mvn -B -nsu -fae \
  -f headless/studio-pack-v5/build/input-fixtures/pom.xml test
```

Verified on 14 October 2026: **all four processing and both input scenarios passed**.
These run generated factories and support with completed fixture business implementations:

- Single- and multi-recipient routers exercise both branches after a broker, translator, filter
  and splitter. A dropped-input batch explicitly expects zero outputs, followed by later delivery.
- Built-in message filtering and list splitting exercise a custom filter rule and a real custom
  producer, including receiver-side cumulative delivery checks.
- The object-to-XML converter marshals a Jakarta JAXB Order and checks reference/quantity values,
  including XML escaping, across three inputs.
- The scheduled consumer uses three input-free triggered batches and checks deterministic fire time.
- The local-file consumer reads three staged files. Unrelated files and a directory remain ignored.
  Both input scenarios include a debug component omitted from the expected business path.

Each scenario uses the same running flow for later work and checks bounded idle readiness and
STOPPED teardown. These are focused behavior checks, not exhaustive optional-property coverage.
Reports are in each exported application's `target/surefire-reports` directory.

## Transaction failure and rollback fixtures

```sh
./gradlew -p headless :studio-pack-v5:generateRollbackFixtures
JAVA_HOME=/path/to/jdk-26 mvn -B -nsu -fae \
  -f headless/studio-pack-v5/build/rollback-fixtures/pom.xml test
```

These three scenarios inject an exception in Spring's `beforeCommit` callback, after the real
producer has attempted its send. They require an active transaction and check the actual
`STATUS_ROLLED_BACK` completion callback, rather than assuming a thrown exception proves rollback.
The generated application's business converter carries this deliberate test-only fault; released
pack templates and normal flow tests do not inject failures.

- **JMS:** use ActiveMQ's `XAConnectionFactory` for both flow endpoints. Hold broker redelivery
  at the converter while checking that no output from the failed transaction escaped. Then allow
  the retry, verify the same JMS message ID and redelivery flag, exactly one output, and successful
  later input on the same flow. The model explicitly maps only the injected `CommitFailure` to
  `ignoreException` after rollback; the default Stop policy is not a continuous-retry policy.
- **FTP and SFTP producers:** check that the failed transaction leaves no final output file.
  The generic consumer's synchronous `submitNow` reports the commit failure to its caller; it
  does not promise automatic retry. The test explicitly resubmits the same input, checks one
  delivered file, then supplies later work without restarting the flow or resetting beans.
- All three check RUNNING during bounded idle windows, STOPPED at teardown, and closure of
  their owned transport server or broker. A scoped transaction-log assertion also rejects resource
  rollback errors: the completion callback alone can report rollback despite an XA failure.

**JMS configuration matters:** the ordinary ActiveMQ `ConnectionFactory` used by the basic
success fixture does not enlist sends in JTA. In the initial failure probe its output escaped
rollback. The rollback fixture therefore explicitly overrides both
`jms.transportprobe.transport.{input,output}.connection.factory.name` properties to
`XAConnectionFactory`. Passing a delivery test is not evidence of transactional delivery.

### Results on 14 October 2026: rollback gate is failing

The V5 pack-contract/export tests pass. The separate Maven rollback gate compiles all
three applications but **fails all three runtime tests** against the resolved snapshot. In each
case the delivery assertions, retry, later input and flow RUNNING/STOPPED assertions completed;
the final strict transaction-diagnostic assertion failed. Do not count this as clean rollback
coverage or release readiness. The successful-delivery scenarios remain separate.

| Scenario | Remaining diagnostic |
| --- | --- |
| JMS XA | `ARJUNA016129: Could not end XA resource`; ActiveMQ `TransactionContext.end` throws `XAException` with `transactionId=null` |
| FTP output | `ARJUNA016031` and `ARJUNA012091 ... FINISH_ERROR`; `DeliverFileCommand` rollback reaches a closed connection (`Connection is not open`) |
| SFTP output | Same rollback/abort diagnostics; rollback directory access fails with `java.io.IOException: Pipe closed` |

The initial success-looking file probes checked absent final files and a ROLLED_BACK callback.
Those were insufficient: transaction logs still reported failed resource cleanup. The strict
fixture deliberately rejects these diagnostics instead of suppressing them. It attaches its
scoped appender after Spring logging initialization and removes it afterward.

These are reproducible findings for investigation in the Ikasan 5 runtime/dependency lifecycle,
not confirmed fixes or proof of the precise underlying cause. No reference-runtime source was
changed. The generated fixtures and Surefire reports identify the exact failing assertion;
use the command above (or add `-pl jms`, `-pl ftp-output`, or `-pl sftp-output`) to reproduce.
The failure prevents the final post-context listener/broker assertion from being reached; normal
`runTest` exception cleanup still closes the context and owned fixtures.

These tests exercise in-process commit failure, not crashes, distributed recovery after process
restart, network outage policies or transactional FTP/SFTP consumers. SMTP delivery cannot be
retracted by JTA rollback; no transactional-email guarantee is claimed. Surefire reports are under
`build/rollback-fixtures/<scenario>/target/surefire-reports/`. Repeat against the formal V5 BOM.

### Isolated rollback investigation

A reviewable [candidate runtime patch](diagnostics/file-rollback-reconnect.patch) reconnects the
file-transfer client before rollback, restores its working directory and disconnects afterward.
In disposable fixture copies, shadowing only `DeliverFileCommand` with the patched reference source
made **both FTP and SFTP strict rollback tests pass**, including the transaction-log check, retry,
later delivery and cleanup. The patch is not applied to the reference repository, installed in Maven,
or included in the pack. The ordinary snapshot gate therefore remains failing. See the
[experiment notes](diagnostics/README.md) for scope and reproduction.

The JMS investigation now includes **seven standalone controls**, with no Ikasan flow or listener
started. On 14 October 2026, four passed and three reproduced `ARJUNA016129`:

| Control | Result |
| --- | --- |
| One send, direct Narayana rollback | Pass |
| One send, Spring-managed rollback | Pass |
| One send through Spring JmsTemplate | Pass |
| Two sends on one XA session | Pass |
| Receive and send on separate sessions; receiver closed before rollback | Resource-end warning |
| Receive and send on separate sessions; receiver closed after rollback | Resource-end warning |
| Two sends on separate XA connections/sessions | Resource-end warning |

All controls assert no output escapes rollback. Receive controls additionally check that the input
returns exactly once with its redelivery flag set. The failing controls reach those assertions and
then fail the strict transaction-log check. This reproduces the issue without generated-flow
execution, and narrows it to multiple XA sessions/connections against the same broker in this
resolved dependency graph. It does not establish whether ActiveMQ, Narayana or their integration
owns the defect, and is not evidence that resource cleanup is safe to ignore.

```sh
JAVA_HOME=/path/to/jdk-26 mvn -B -nsu \
  -f headless/studio-pack-v5/build/rollback-fixtures/pom.xml \
  -pl jms -Dtest=JmsXaLifecycleTest test
```

To reproduce the smallest failing case alone, use
`-Dtest=JmsXaLifecycleTest#twoSendSessionsRollback`. Compare with the passing
`-Dtest=JmsXaLifecycleTest#twoSendsOnOneSessionRollback`. Both send two messages and require an empty
output queue after rollback. The three failing controls deliberately keep this command red.
They are automatically included in the full verification workflow; earlier combined test counts
above predate this extension. The full JMS flow test remains a separate failing integration check.

## V3/V4 → V5 migration rehearsal

The development pack includes directional contracts from `V3.3.9` and `V4.1.6`.
They are exercised through the non-mutating `ModelMigration.analyseWithRules` pack-authoring
API. **V5 is still unavailable in the normal IDE/CLI migration workflow.** The supported
release paths and their guards remain V3 ↔ V4; this rehearsal does not apply changes to a workspace.

```sh
./gradlew -p headless :studio-pack-v5:generateMigrationFixtures
JAVA_HOME=/path/to/jdk-26 mvn -B -nsu \
  -f headless/studio-pack-v5/build/migration-fixtures/pom.xml test
```

The Gradle task loads V3 and V4 artifacts only on its own test classpath, preserving the normal
V5 pack's independent-loading contract. Three tests check catalogue mapping coverage, source
and opaque-field preservation, component names, transitions, user implementation identity,
type/import rules and rejection of unsupported paths, invalid contracts and unknown components.
It exports source/target JSON, a migration report and a generated V5 application per source version.

V3 JMS, JAXB and resource package type references use the established Jakarta mappings.
V4 already uses these namespaces, so its contract introduces no blanket package rewrite.
Email remains `javax.mail`. Import proposals use the existing conservative import-only rewriter;
comments and literals remain untouched. These rules do **not** convert arbitrary Java API calls,
fully qualified names in implementation code, custom dependencies or existing developer-owned
flow tests. In particular, review user-owned `javax.annotation.Resource` imports separately:
a blanket `javax.annotation.*` rewrite would incorrectly include packages such as
`javax.annotation.concurrent`. Newly generated V5 wiring already uses Jakarta Resource.

The exported runtime examples migrate an event consumer → Dev Null flow from each source version.
They exercise regeneration and startup/delivery under V5; they do not establish compatibility
of every existing business implementation or prove a complete in-place project upgrade.
The full verification runner includes both applications and their resolved dependency audits.
Verified on 14 October 2026: three migration rehearsal tests, both generated V5 runtime tests
(Java 26.0.1), sixteen existing V3/V4 migration/import regression tests and ten verification-runner
tests passed. The ordinary ten V5 pack tests also passed with their isolated classpath.

Before enabling migration in Studio, add representative developer-owned application upgrades,
reviewable source/test changes and backup/restore acceptance checks against the formal V5 release.

## Before enabling for developer review / release

- Repeat compilation of the generated application **and test support** against the formal V5 dependency graph.
- Extend the verified startup and Blue Console login/listing checks to remaining administrative operations,
  production persistence and deployment settings. V4-derived templates remain provisional outside tested cases.
- Extend focused runtime probes to untested optional settings and failure paths.
  Add compilation coverage for optional setters, custom configuration/provider beans and conversion recipes.
- Extend rollback coverage to file consumers, failure policies and process/network recovery; the local
  JMS and file-producer commit-failure scenarios above cover only their stated cases.
- Test the real Studio create/generate/run/debug/validate/flow-test journey with the development artifact.
- Extend the isolated migration rehearsal to developer-owned code and workspace backup/restore before enabling V5 migration.
- When the formal release is available: replace the snapshot identity, BOM and commit help links with
  verified release coordinates/tag; rerun the full compliance gates before adding the artifact to
  `studio-bundled-packs`. Do not ship snapshot coordinates in a Marketplace build.

See [meta-pack compliance](../../src/main/resources/studio/metapack/METAPACK_COMPLIANCE.md).

## Interactive module testing — 20 October 2026

The project owner reports completing Ikasan 5 testing of the routes/branches,
converters/translators and all-consumers/producers demonstration modules. This supplied
interactive evidence and exposed shared navigation, test diagnostics, flow-test execution,
rename and route-validation fixes now included in Studio. It does not establish that the
separate snapshot rollback gates have passed. The merge initially provided a development build flag; the subsequent user-facing
opt-in is documented in [snapshot meta-packs](../../docs/SnapshotMetaPacks.md).

See the [20 October merge verification](../../docs/Ikasan5MergeVerification-2026-10-20.md)
for passing distribution checks and the six remaining runtime rollback failures.
