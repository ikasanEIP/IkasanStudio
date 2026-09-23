# Ikasan migration regression tests

A maintained regression fixture, separate from the distributed plugin. It provides a complete Studio module for checking upgrades with real flows, developer-owned
implementations and one acceptance report. It covers all **26 executable component
keys plus the Exception Resolver** in the bundled 3.3.9 and 4.1.6 catalogues. Twelve
flows share transformations and transport pairs to keep the number of outputs small.

The fixture uses Studio's actual proposal validator, model serializer, migration engine
and FreeMarker templates. It is separate from production plugin code. Nothing modifies
existing projects or the local Ikasan reference repositories.

## Create and open the baseline

Requirements: JDK 17, Maven, Python 3 and the repository's Gradle wrapper. Initial setup
may download Maven/Gradle dependencies. From the Studio repository:

```sh
python3 regression-tests/migration/fixture.py create --name my-baseline

# Or specify the version and workspace name:

python3 regression-tests/migration/fixture.py create --version V3.3.9 --name MigrationRegression
```

Open `regression-tests/migration/build/my-baseline/pom.xml` as a Maven project in
IntelliJ with Studio installed. The model is already in
`generated/src/main/model/model.json`; its baseline version is V3.3.9.
The `create` command generates that model automatically from the checked-in fixture
inputs; no preparation step is needed. **Copy the complete project when testing
behaviour**: importing the model alone cannot provide custom code.
The generated workspace contains all required `user/` implementations, including recipe
converters. No throwing or unfinished implementations are intended.

For interactive use, start disposable services in another terminal before Run module:

```sh
python3 regression-tests/migration/fixture.py serve regression-tests/migration/build/my-baseline
```

Keep that command running. It binds only to loopback: FTP 2121, SFTP 2222 and SMTP 2525,
using the deliberately public local-test credentials `fixture` / `fixture`. It creates
private fixture directories under the project, including `/upload` as seen by each
file-transfer server. It never reads personal SSH keys or modifies `~/.ssh`.
If a port is occupied, stop the conflicting **fixture** instance or use another workspace
with explicitly adjusted settings; do not stop an unrelated service. SFTP tests password
authentication, not production key authentication or host-key security policy.

All model flows use automatic startup. The Generic Consumer in Fanout is deliberately
an input-driven source: it remains running and waits for `ControlledInput.send(Order)`.
The automated check supplies its inputs. Scheduled examples repeat every five seconds;
the Event Generating Consumer is deliberately paced to one event per second. These
choices belong to this regression fixture, not general AI generation policy.

## Capture before and after evidence

```sh
python3 regression-tests/migration/fixture.py verify regression-tests/migration/build/my-baseline --report regression-tests/migration/build/my-baseline/migration-before
```

This compiles the actual generated application and runs the reusable JUnit acceptance
suite with `ikasan-test` at the module's resolved version. It creates its **own** isolated
application, in-memory H2, embedded JMS broker, FTP/SFTP roots and SMTP receiver. Ports
are allocated locally for the automated run. It does not connect to or stop your IDE
module. Each run uses a fresh working directory, so old duplicate history cannot mask
or contaminate a result. Test-only manual startup lets observers attach before inputs;
the saved model remains AUTOMATIC. Test teardown stops only that test application.

Now use **Migrate Ikasan Version** in Studio to upgrade the same workspace to 4.1.6.
Wait for generation to complete, then run:

```sh
python3 regression-tests/migration/fixture.py verify regression-tests/migration/build/my-baseline --report regression-tests/migration/build/my-baseline/migration-after
python3 regression-tests/migration/fixture.py compare regression-tests/migration/build/my-baseline/migration-before/report.json regression-tests/migration/build/my-baseline/migration-after/report.json --report regression-tests/migration/build/my-baseline/migration-comparison
```

Each evidence directory contains `report.md`, machine-readable `report.json`, runtime
observations, and `build.log`. Comparison checks both runs passed, the same checks ran,
user-source SHA-256 hashes stayed unchanged, and model configuration/topology survived.
Only the version and known JMS/JAXB namespace substitutions are normalized; unexpected
changes fail comparison for investigation. Interface changes requiring user-code edits
are deliberately **not** silently accepted.

For unattended baseline/target checks, `create --version V4.1.6 --name my-target` uses
Studio's migration engine before rendering. This checks the engine, not IntelliJ's
migration dialogs or Undo/Restore UX. Keep the interactive migration check for releases.

The named `migration-before`, `migration-after` and `migration-comparison` directories
are inside the module root so their Markdown reports are visible in IntelliJ. For a ZIP
workspace, run the shorter commands in its README from that project directory.

Wiretap verification enables Spring's `debug` profile, which activates Studio's generated
logging-wiretap triggers. For interactive inspection, add `--spring.profiles.active=debug`
to the Application run configuration's program arguments. **Core Pipeline → Enrich Order**
has both persisted and logging wiretaps before and after enrichment. Persisted captures
can be inspected in Studio's wiretap viewer; logging captures appear in the module log.

## What the report proves

| Flows | Assertions |
| --- | --- |
| Core Pipeline | Nested batch unpacking, custom splitting, two filters rejecting unwanted samples, translation, broker enrichment, exact Priority/Standard/Rejected outputs across two batches |
| Core Pipeline wiretaps | Twelve persisted H2 snapshots and twelve messages from the actual logging-wiretap job; all six orders captured before and after enrichment across two batches |
| Fanout | Generic Consumer and Generic Producer, both recipients, Audit mutation cannot alter Fulfilment's payload, two inputs |
| Exclusion | Actual `TransformationException` → `excludeEvent`, record and payload persisted in H2, invalid event never reaches output, subsequent valid delivery |
| Event Source | Event Generating Consumer emits later events and remains running; Dev Null output observed through a flow listener |
| JMS Sender / Receiver | Actual embedded JMS object-message delivery, explicit type narrowing, object extraction, XML output for two orders |
| FTP Sender / Receiver | Actual upload/download and content conversion, rescan suppresses duplicate, later new file delivered without restart |
| SFTP Sender / Receiver | Same checks over a real SFTP connection |
| Local Files | Actual contents of two input files, not merely their filenames |
| Mail | Email conversion and two bodies received by an SMTP server, not merely producer invocation |
| Whole module | Every exercised flow running while idle; verification leaves model and user sources unchanged |

The 12 remaining catalogue entries are Module, Flow, Debug Transition and visual endpoint
markers. They are identified separately in coverage: they are **not 12 additional runtime
components**. Module/flow/router structure is exercised by model generation and migration;
this is not a screenshot/UI test of every marker or a runtime test of a database endpoint.
There is no Sequencer component in these bundled catalogues.

A Spring context closing successfully is reported separately and does not establish that
all framework background workers terminated. Surefire runs a bounded forked test JVM;
check its logs when investigating shutdown. Runtime timeouts/build failures produce a
failed report. Missing future executable component keys fail coverage rather than being
silently omitted. This is component/behaviour coverage, not every property permutation,
TLS option, failure policy or external vendor implementation.

## Maintaining the fixture

- `define_fixture.py` is the deterministic specification; `operations.json`, `coverage.json`
  and `project/user/src/` are its outputs. Run it only when intentionally revising the
  fixture; it writes those checked-in fixture files, never an existing generated workspace.
- `builder/` consumes the headless generator. It refuses to overwrite an existing workspace.
- `project/verification/` contains reusable actual-flow tests and isolated test services.
- The builder generates `build/<name>/generated/src/main/model/model.json` as part of
  `create`. There is no separately maintained or checked-in model output.
- Running `define_fixture.py` is only needed when changing the fixture specification,
  not before creating a normal test project.
- When adding a pack, add a supported migration path and version choice, then run the same
  assertions. Review new catalogue keys and intentional interface changes explicitly.
- Reports are timestamped by default; explicit report directories must be new. Keep before
  evidence outside the project if you intend to delete the test workspace.
