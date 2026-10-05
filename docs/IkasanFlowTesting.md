# Testing flows with the Ikasan test framework

For Studio-owned tests requiring no completion, see [Generated verification baselines](GeneratedVerification.md).
These provide automatic structural checks; the business tests below remain developer-owned.

Use **Tools → Ikasan Studio → Generate Flow Test…**, or right-click a flow and choose
**Generate Flow Test…**, to create a reusable Ikasan test scaffold. Custom component unit
tests can still live in `user/src/test/java`. Application-level flow tests live in a separate,
developer-owned `user-flow-tests` Maven module: `generated` already depends on `user`, so putting
a test dependency on `generated` inside `user` would create a Maven dependency cycle.

To generate several tests at once, right-click the module on the canvas and choose
**Generate Flow Tests…**. Select flows using their checkboxes, or use **Select All** / **Select None**.
All flows are selected initially. Studio validates the selected scaffolds before writing files,
offers **Skip Existing**, **Archive and Regenerate**, or **Cancel** when selected tests already exist,
and reports how many tests were created or preserved. Only selected tests can be archived. The first new test
opens in the editor. If a selected flow has no consumer, deselect it or add its consumer first.

## Optional local FTP server

When the module contains components marked `supportsTestFtpServer` in its meta-pack,
the generation dialog selects **Use a local test FTP server** by default when an FTP
flow is selected. Selecting it regenerates shared setup (with the usual archive confirmation)
and enables the server in `module-test.properties`. Existing properties are backed up before
changing the enablement flag; custom credentials and unrelated settings are preserved.
Leaving the box unchecked preserves current settings; it does not disable an existing fixture.
You can also enable it manually:

```properties
test.ftp.enabled=true
# Optional disposable-server credentials:
# test.ftp.username=ikasan
# test.ftp.password=ikasan
```

This starts a real Apache FTP server before the Spring application, bound to loopback on
an allocated port. Each test gets a unique FTP home inside the shared JUnit `TemporaryFolder`. Shared support overrides the
module's FTP host, port, credentials and source/output directories using the meta-pack's
property labels; all those directories become `/` inside that temporary home. Other settings,
such as filename filters, remain unchanged. The option covers **all plain FTP endpoints in
that test context**, not SFTP or FTPS. Unsupported or missing connection mappings fail before
the application starts. Leave the option off to use your separately configured test server.

In a test's `supplyInput`, seed consumer files using:

```java
Path home = context.getBean(LocalFtpTestServer.class).root();
Files.writeString(home.resolve("input.txt"), "test content");
```

Choose filenames and content that match your consumer. Scheduled consumers still need their
normal test trigger. In `verifyReceivedOutput`, inspect that same directory and assert the
producer's final filename and contents. Observing a payload at the producer alone does not
prove the correct file was delivered. Closing the application context stops the server. JUnit
then removes its temporary files when the test finishes, including after test failures.
No existing IDE server is stopped.

For existing tests, the FTP choice also selects **Archive and regenerate shared module setup**
to refresh `ModuleFlowTestSupport`. Accept the archive/regenerate confirmation for that file.
Without the FTP choice, shared-setup regeneration leaves `module-test.properties` unchanged. Generation adds missing FTP/MINA test dependencies to an
existing test POM, preserving its XML and saving a timestamped backup. Existing dependency
versions remain developer-owned. Review any custom server helper before regenerating; existing
helpers are preserved. Shared support must be regenerated after adding or changing FTP endpoints.

Test teardown preserves the original startup/assertion failure; cleanup failures appear as
suppressed exceptions rather than replacing the useful diagnosis.

## Comparing business objects

Override `assertOutput(Object actual, int batch)` in the business test to assert fields directly.
The hook receives the original payload observed after the selected producer, on the test thread;
`outputText` and its decoding flag are not used unless your override calls them.
For example, with independently prepared `firstExpectedOrder` and `secondExpectedOrder` fixtures:

```java
@Override
protected void assertOutput(Object actual, int batch) {
    assertTrue("Expected an Order", actual instanceof Order);
    Order expected = batch == 1 ? firstExpectedOrder : secondExpectedOrder;
    Order order = (Order) actual;
    assertEquals("Order reference", expected.reference, order.reference);
    assertEquals("Order quantity", expected.quantity, order.quantity);
    assertEquals("Order number", expected.getOrderNumber(), order.getOrderNumber());
}

@Test
public void testFirstAndLaterDeliveryWithoutRestart() throws Exception {
    runTest(TEST_REVIEWED, PRODUCER_NAME);
}
```

Import the domain class and JUnit assertions in your test. Remove unused expected-text constants,
`outputText` overrides and decoding flags. Assertions still run for both batches with idle/running
checks and cleanup. `verifyReceivedOutput` remains a separate receiver-side check; its `expected`
text argument is null when using this two-argument runner. Adapt that override to your domain
fixtures if you need actual file/message delivery checks.

Payloads are retained by reference, not serialized or deep-copied. Components must not mutate
objects after delivering them. Existing four-argument `runTest` calls use the same hook, whose
default implementation compares `outputText(actual)` with the supplied expected strings.
Refresh module support to adopt the new hook; existing business tests remain compatible.

## Comparing actual output as text

New scenario tests include:

```java
private static final boolean DECODE_OUTPUT_CONTENT_AS_TEXT = true;

@Override
protected String outputText(Object payload) {
    return outputText(payload, DECODE_OUTPUT_CONTENT_AS_TEXT);
}
```

The former `STRINGIFY_ACTUAL_OUTPUT` flag is now named `DECODE_OUTPUT_CONTENT_AS_TEXT`.
Disabling it uses `String.valueOf(actual)` (normally `toString()`); it does not enable object equality.

With the flag enabled, `FIRST_EXPECTED_OUTPUT` and `SECOND_EXPECTED_OUTPUT` are compared with
content rather than a payload object's identity string:

| Actual value | Compared text |
| --- | --- |
| Ikasan `Payload` | `getContent()` decoded as UTF-8 |
| `byte[]` | UTF-8 text |
| `File` or `Path` | File's UTF-8 contents |
| `List<File>` | Contents concatenated in list order, without added separators |
| JMS `TextMessage` | Message body from `getText()` |
| Text, numbers, booleans, characters | Their text representation |
| `null` | The literal string `null` |

The helper uses the pack's JMS namespace (`javax.jms` or `jakarta.jms`) and needs no additional
endpoint dependencies for modules that do not use those types. It does not acknowledge messages,
read streams, deserialize JMS object messages or advance binary-message cursors. Other types,
unreadable files and malformed UTF-8 fail explicitly; override `outputText(Object)` for binary
formats, another encoding, selected object fields or other JMS bodies. Asynchronous decoding
failures are surfaced on the test thread rather than appearing only as an output timeout.

Set the flag to `false` for the original `String.valueOf` comparison. Existing developer-owned
tests retain their behaviour; regenerate the test and shared setup, or adopt the override manually
after regenerating shared setup. Physical file/JMS delivery remains a separate receiver assertion;
reading a Payload's bytes alone does not prove successful transport delivery.

## Batch inputs and physical file checks

Scenario tests declare `FIRST_BATCH_INPUT` and `SECOND_BATCH_INPUT` beside the expected output
constants. `supplyInput()` uses them directly for JMS and local-file input, or references them
in the sample-provider guidance. Expected outputs remain independent: changing an input must
not silently change the assertion to match it.

Shared support provides two receiver-side checks:

```java
assertFileContents(directory.resolve("result.txt"), expected);

assertDeliveredFileContents(directory, "*.dat", batch == 1
        ? new String[]{FIRST_EXPECTED_OUTPUT}
        : new String[]{FIRST_EXPECTED_OUTPUT, SECOND_EXPECTED_OUTPUT});
```

Both wait up to ten seconds and compare exact UTF-8 contents, including whitespace. The second
checks the exact count and contents of matching direct regular files, irrespective of their
order or names. It preserves duplicate counts, so two identical expected payloads still require
two files. Use the first check for a known filename or intentional overwrite; use the second
for one new file per batch. Choose a final-filename glob; unmatched files are ignored. Include
all relevant files if temporary-file leftovers must also fail the test. These helpers never
create, remove or alter files and do not follow symbolic links.

Producers declaring `flowTestFileDelivery` in their meta-pack (currently FTP and SFTP) receive
receiver-side checks. For isolated FTP, the generated code calls
`assertDeliveredFileContents(localFtpDirectory(context), "*", ...)` and checks accumulated contents:
one file after batch one, two after batch two. The shared `localFtpDirectory(context)` exposes
the actual server home and gives an actionable error if the server is disabled. Adapt the assertion
for checksums, multiple outputs or overwrites. Local SFTP tests use a separate temporary directory for the selected producer. For external FTP/SFTP, use a locally accessible
test-server directory or download the final files to an isolated directory first: a remote path
is not a local filesystem path. Binary contents need byte-level assertions instead of these text checks.
The same helpers are available for custom components writing local files. Merely consuming local
files does not imply that the flow produces an output file.

Regenerate shared setup to obtain the helper and wrappers in existing projects. Existing tests
are developer-owned; regenerate them with a backup or copy the relevant input constants and
assertions manually.

## Numbered tasks in each generated test

Scenario scaffolds have a checklist and **TODO 1–5** beside the relevant code.
The compact self-generating-source/discard-sink observation scaffold described below needs only
two tasks: review settings, then enable the test.

For scenario scaffolds:

1. **Connections:** review common settings in `src/test/resources/module-test.properties` and any scenario overrides in `flowTestProperties()`. For a local-file
   consumer whose filename property is generated, Studio supplies a JUnit temporary directory
   and the exact filename-property override. JUnit cleans up that directory. Other endpoints
   and startup beans still need their own test settings.
2. **Input:** supply the two input batches. Local-file scaffolds create one file per batch;
   edit the two sample contents. Other scheduled inputs deliberately fail until their
   `prepareInputBatch()` is implemented; merely firing a scan creates no input.
3. **Results:** review the selected producer, expected values and `describeOutput()`.
   A sole producer is selected automatically. Local-file payload lists are compared using
   UTF-8 file contents, not temporary paths; custom objects still need meaningful extraction.
4. **Component path:** review `configureExpectedPath()`. Straightforward linear flows receive
   a consumer/converter/translator/broker/producer sequence repeated twice, assuming one event
   per batch. The generated test calls `harness.assertIsSatisfied()` explicitly. Routes,
   splitters, filters, sequencers, unknown component types and exception-resolution scenarios
   require an explicit scenario; a guard prevents that unfinished expectation setup from passing.
   Add receiver-side delivery, branch and exclusion assertions where required.
5. **Run:** set `TEST_REVIEWED=true` after completing the other tasks and use the supplied Maven command.

The rule is used explicitly rather than as `@Rule`: the generated test starts it, checks its
expectations, and stops it in `finally`. Payload assertions and first/idle/later running-state
checks remain separate from the framework's component-invocation checks. Scheduled tests use
manual triggers with ordinary scheduling disabled by the harness; they do not verify cron timing.

These are five categories of work, not a promise that complex flows need only five edits.
Existing developer-owned tests are unchanged; generate a new test, or explicitly archive and
regenerate an existing one, to get the revised scaffold.

## Generate and complete a test

1. Save the module and open files, and wait for generation to finish. Select a saved flow
   that has a consumer.
2. Generate the scaffold. Studio adds `user-flow-tests` to the root POM, creates the test module
   POM and README if missing, and opens the new Java test. Existing files are preserved;
   generating the same test again offers **Keep Existing** or **Archive and Regenerate**.
   Archiving preserves the original beside the test as `YourFlowTest.java.bak<timestamp>-<unique-id>`
   before creating a fresh scaffold. Custom assertions and settings remain in the backup; they
   are not copied into the new scaffold. If generation fails, Studio attempts to restore the
   original and retains the backup. Batch generation offers the same archive option for selected existing tests.
   Commit the new files.
3. Read `user-flow-tests/README.md`. Fill in isolated test settings, the output producer name,
   input batches and expected payloads. Consult `LOCAL_TEST_ENVIRONMENT.md` for local services.
   Review application startup beans and connection settings before enabling the test.
4. Set `TEST_REVIEWED=true` only after completing the scenario. Run from the project root:
   `mvn -pl user-flow-tests -am test`. Until configured, the test deliberately fails **before**
   starting Spring or any external services; it does not silently skip or report success.

The version-specific FreeMarker templates use `IkasanFlowTestRule` to start the real flow,
attach an output listener, and check first delivery, idle readiness and later delivery without
restarting. Scheduled consumers receive explicit triggers. Other consumers need a transport-
specific input implementation. Teardown stops the isolated test flow and closes its context;
the saved model and normal module startup settings are unchanged.

This initial generator does not infer business expectations, provision test services or automatically
complete router/exclusion scenarios. It lists producer names to help extend assertions for every
branch. Observing a producer's flow payload does not prove external delivery; add receiver-side
assertions. The scaffold uses a separate in-memory H2 database and test-only MANUAL startup,
including per-flow overrides. It still requires review of connection settings and startup beans.
Existing module POMs are preserved: if you already have `user-flow-tests`, check its dependencies and
JUnit 4 test provider. New module POMs explicitly select Surefire's JUnit 4 provider.

## Shared module test setup

Generated tests extend the developer-owned abstract `ModuleFlowTestSupport` in the same
package. It loads module-wide connection overrides from `src/test/resources/module-test.properties`,
enforces isolated H2 and test-only MANUAL startup, and opens a fresh
Spring context for each scenario. Each test closes its context with try-with-resources.
Local-file temporary directories, input batches and assertions stay in the individual tests.

The UTF-8 properties file is created with commented property keys, without copying live values
or credentials. Uncomment the settings you need using values from `LOCAL_TEST_ENVIRONMENT.md`.
Use standard Java properties escaping (forward slashes are easiest for file paths). Spring can
resolve environment placeholders such as `${TEST_PASSWORD}` in values. Missing/commented keys
retain the application's configuration; they do not become isolated test defaults.

Settings apply in order: application configuration, shared test properties, flow-specific Java
overrides, then enforced random server port, fresh in-memory H2 and MANUAL startup. The loader
fails clearly before starting Spring if the file is missing. Regenerating Java support preserves
the properties file; consult generated application.properties for newly introduced keys.

Review the common connection settings once using `LOCAL_TEST_ENVIRONMENT.md`. The whole
module context still loads: MANUAL flow startup cannot prevent other beans connecting during
initialisation. If you start isolated services, add JUnit setup/teardown with cleanup even
when startup fails. No external service is automatically provisioned or stopped by the base class.

The support class is created when missing and otherwise preserved. Both single-flow and
module-level generation offer **Archive and regenerate shared module setup** (off by default).
Select it after adding/renaming flows or changing connections. The confirmation lists existing
selected test/support files: archive them to regenerate, skip them to preserve, or cancel.
Backups retain exact original contents with timestamped `.bak` names. Merge custom connection
settings from the backup; regeneration deliberately does not guess how to merge Java edits.
Existing independent tests still work; explicitly regenerate them to adopt shared setup.

## Short scenarios and JMS tests

`ModuleFlowTestSupport.verifyFlow(...)` owns listener attachment, the Ikasan rule, two deliveries,
bounded output waits, idle/running checks and cleanup. Individual tests supply their expected path,
inputs, payload representation and expected values. `verifyScenario(...)` exposes the same lifecycle
for scenarios where an input is deliberately filtered or rejected; it does not demand two outputs.
Declare every expected invocation, assert delivered values or bounded absence explicitly, and include
later valid input when checking recovery. The helper waits for framework expectations and requires
RUNNING before teardown. A missing output alone does not prove exclusion: also inspect stored exclusions.

For a configured Spring JMS **queue** consumer, generation adds `JmsFlowTestSupport` and
`ModuleJmsTestConfig`. The scenario sends text messages through a real connection. If the sole producer
is also a configured Spring JMS queue producer, it additionally receives/asserts the actual output
text for each batch. Otherwise, add external receiver checks where needed.

When JMS consumers share one configured embedded (`vm://`) broker, generation supplies
`test.jms.broker-url` as a Spring reference to the consumer's provider-URL property. This keeps
the test sender aligned with application settings and any test overrides. For existing properties
files, a missing reference is added with a backup; explicit values are preserved. External or
multiple distinct broker URLs require deliberate configuration.

Set `test.jms.broker-url` (and optional username/password) in `module-test.properties`, and point the
flow's connection/destination overrides at the same isolated broker and dedicated queues. Input and
output queues must be distinct. The helper uses physical queue names; adapt JNDI aliases explicitly.
The supplied configuration uses ActiveMQ; other vendors, topics, object/binary messages, selectors,
custom factories and routes require deliberate adaptation. Existing properties files are preserved:
add the new `test.jms.*` keys yourself if needed. No broker-wide purge is performed.

Use `testConfigurationClasses()` to add test-only Spring configurations, including shared sample-data
beans. Retrieve those beans from the opened context, e.g. `context.getBean("sampleOrder", String.class)`.
These generated tests use explicit contexts, not SpringRunner: fields annotated `@Autowired` in the
JUnit test are not automatically injected. Each test method must open and close its own context.

For a duplicate-filter scenario, call `verifyScenario` with an expectation sequence covering the
first complete path followed by only the second consumer/filter invocation. In the scenario callback,
send the first input and assert its output; resend it and assert no second output within a bounded
interval. `JmsFlowTestSupport.assertNoMessage(queue)` supports isolated queue absence checks. Add a
later distinct input if the test must prove continued delivery, extending the expected path accordingly.

Shared Java helpers are developer-owned and preserved. When updating older generated tests to this
API, select **Archive and regenerate shared module setup** so the base class contains the new helpers.
Review changes to JMS helper/configuration files manually if they already exist. The JMS helper uses
the selected pack's `javax.jms` or `jakarta.jms` API; a major-version migration may require updating
those developer-owned imports before rerunning the same assertions.

## Migration and ownership

The test dependency inherits `${version.ikasan}` from the root POM. Migration between 3.3.9 and
4.1.6 updates that property and retains the `user-flow-tests` module; it does not rewrite developer
assertions or test source. Both templates use their shared rule API. Run the same completed tests
before and after migration. Future framework interface changes or application type changes may
still require deliberate test edits. Normal Studio generation never owns `user-flow-tests/`.

Compilation and component invocation counts alone do not establish successful delivery.

## Choose the matching release

| Studio pack | Test dependency | Rule API |
| --- | --- | --- |
| V3.3.9 | `org.ikasan:ikasan-test:3.3.9` | JUnit 4 `IkasanFlowTestRule` |
| V4.1.6 | `org.ikasan:ikasan-test:4.1.6` | JUnit 4 `IkasanFlowTestRule`; additionally exposes `withAssertEndState()` |

These signatures were checked against the released Maven jars. Do not copy a newer
major version's APIs or upgrade the application's dependency management just to add tests.
Both rules share the methods used by Studio’s version-specific flow-test templates.
If the application uses JUnit Platform, enable JUnit Vintage for these JUnit 4 tests;
otherwise the tests may compile but never execute. Verify the Surefire test count.

In the developer-owned POM that owns the test sources, add test-scoped dependencies
on `ikasan-test` at the matching version and `junit:junit:4.13.2`. Retain the application's
Spring Boot/BOM versions. Replace an explicit older JUnit dependency rather than adding
another conflicting declaration. Do not hand-edit a generated POM.

## Studio regression coverage

Studio maintains the generated-test templates and their scaffold tests rather than a
separate framework teaching example. Generate a test for your actual flow and complete
its numbered tasks as described above.

The [migration regression fixture](../regression-tests/migration/README.md) exercises a
complete generated application with isolated services and reusable runtime assertions
before and after migration. It complements scaffold tests: compiling a test establishes
API compatibility, while running it with meaningful assertions establishes behaviour.
Neither component invocation counts nor a successful compilation prove external delivery.

## Extend verification to match the brief

- **JMS:** start the real receiver before the sender. Assert payload and correlation ID
  at the receiver's final producer. Sender invocation alone is insufficient.
- **Single router:** send inputs for every requested branch; assert the chosen producer
  receives each ID and the other branches do not.
- **Multi router:** assert both final outputs and payload values. Include a mutation-sensitive
  input to detect unintended shared-payload changes; do not assume fan-out copies payloads.
- **Exclusion:** send valid, invalid, valid input. Assert a stored exclusion for the invalid
  event, no final delivery for that ID, and successful later valid delivery in the same run.
- **External transports:** consult `LOCAL_TEST_ENVIRONMENT.md`, use isolated test paths,
  and assert the remote result. Missing services are blockers, not successful tests.

Bound every asynchronous wait and include useful failure messages. Test teardown may
stop its isolated flows; normal ESB flows must stay ready between batches. Report delivery,
continued readiness, and graceful JVM shutdown separately. A timeout or forced exit is
not proof of clean shutdown. For the observed 3.3.9 shutdown limitation, see the generated
version-specific guidance rather than suppressing the failure.

For migration, retain the same application-level assertions and samples, resolve the target
release's test dependency, then run the tests against regenerated target-version code.
Do not weaken assertions to accommodate changed behaviour. Automatic generation of test
classes on canvas edits is not part of this initial implementation.
The shared base class supplies a fresh, empty `flowTestProperties()` map on every invocation.
Ordinary flow tests inherit it without boilerplate. Local-file tests override it to supply their
JUnit temporary directory; other tests need an override only for dynamic or scenario-specific settings.
Shared properties are reloaded for every application context. Existing base classes must be explicitly
archived and regenerated when adopting this default method.

New flow tests override `getFlowName()` and `defineExpectedPath()` and delegate their standard
scenario to `runTest(...)`. The base class checks the setup guard before opening a fresh context,
then closes it on success or failure. Override `outputText(Object)` only when the default text
representation is unsuitable. Local-file tests use the shared file-content decoder.
For complex scenarios, `runTest(TEST_REVIEWED, context -> { ... })` supplies the same context lifecycle
while leaving input and assertions explicit. Existing direct `verifyFlow`/`verifyScenario` callers
remain supported. Regenerate shared module setup when generating tests that use these new helpers.

### Self-generating source and discard sink

For a direct Event Generating Consumer → Dev Null Producer using the built-in provider,
the meta-pack selects a compact observation test. It has no `supplyInput()` or expected-payload
placeholders: review shared settings, then enable it. The test checks the initial payload sequence declared by the meta-pack (`Test Message 1`,
`Test Message 2`, `Test Message 3` for the bundled built-in providers), continued running and a
fresh later invocation without restarting. Only the initial samples are retained; later events
are counted. It verifies that the isolated test flow stops during teardown. This checks initial
content/order and continued processing, not idle behaviour or external delivery.

Keep the generated expected values unchanged for before/after migration tests: they are literals
in developer-owned source, not read from the new pack. Regenerating a test deliberately takes a
new snapshot and therefore should not be done between baseline and upgrade verification. Custom providers, intermediate
components, branches and exception resolvers retain the more explicit scenario scaffold.

Generated JUnit methods start with `test`, for example
`testGeneratedEventsReachProducerAndFlowKeepsRunning`. JUnit discovers them through `@Test`;
the name is for readability. Existing developer-owned tests are preserved. To adopt the new
scaffold, archive/regenerate the selected flow test and shared module setup.

Standard generated scenarios call `runTest(TEST_REVIEWED, PRODUCER_NAME, FIRST_EXPECTED_OUTPUT, SECOND_EXPECTED_OUTPUT)`.
Override `supplyInput(context, harness, batch)` to provide each batch, and optionally
`verifyReceivedOutput(context, batch, expected)` for external delivery checks. The support class
calls these methods directly through its standard scenario wiring; no method references or lambdas
are needed in the concrete test. JMS scaffolds supply both overrides when a single output queue
is known, opening and closing a helper connection for each operation. Advanced callback overloads
remain available, and older callback-based tests still work with regenerated shared support.

### Generic Consumer sample timing

New Generic Consumer implementations retain a one-minute default for both the initial wait and
the delay after each sample poll. Override their class-specific properties in `module-test.properties`:

```properties
studio.sample-consumer.org.example.acap.flow3.MyGenericConsumer.initial-delay-ms=500
studio.sample-consumer.org.example.acap.flow3.MyGenericConsumer.poll-delay-ms=2000
```

Use the exact implementation class from the generated consumer's `@Value` annotations.
A fixture constructing the consumer directly can instead call `setSampleInitialDelayMillis(500)`
and `setSamplePollDelayMillis(2000)` before starting it. Initial delay may be zero; repeat delay
must be positive. Changes take effect on the next start, not during an active poller.

This only changes the sample source's timing. A test using that autonomous poller can deliberately
leave `supplyInput` empty, but must still configure its expected payloads and external delivery checks.
Keep the repeat delay longer than the standard test's one-second idle assertion and account for
processing time, or use a custom scenario for continuous input. For strictly controlled batches,
implement a controllable source instead of relying on wall-clock scheduling.

Existing developer-owned consumer classes are preserved. Regenerating a flow test does not update
them; port the timing fields/setters and scheduling change into the consumer before using these keys.

### Deterministic input for the sample Generic Consumer

`submitNow(String)` is available by default whenever a new sample consumer is running.
Generated tests enable deterministic fixture input in `module-test.properties` by default,
using the implementation class declared by the metadata. For example:

```properties
studio.sample-consumer.org.example.acap.flow3.MyGenericConsumer.fixture-input-enabled=true
```

The generated input override is executable by default:

```java
@Override
protected void supplyInput(ConfigurableApplicationContext context, IkasanFlowTestRule harness, int batch) {
    context.getBean(MyGenericConsumer.class)
            .submitNow(batch == 1 ? FIRST_BATCH_INPUT : SECOND_BATCH_INPUT);
}
```

Fixture mode emits nothing automatically and starts no polling worker. The running consumer
accepts each submission synchronously through its real event factory and transactional dispatch;
dispatch exceptions propagate to the fixture. It remains ready between submissions. Calls before
start, after stop, or with null content fail explicitly. Normal polling mode also accepts submissions. A fixture that constructs
the consumer directly can call `setSampleInputEnabled(true)` before starting it.

The `submitNow` operation supplies input directly; it does not queue or automatically
retry failed submissions. Normal mode remains the default and retains its configured polling delays.
Existing developer-owned classes are never rewritten to add the API. The metadata-driven scaffold
calls `submitNow` directly; older/custom implementations declaring this capability must provide it.
Generation adds missing fixture-input settings with a backup and preserves explicit values.
Remove a setting if not needed; set it to `false` to retain automatic polling across subsequent
regeneration, and adapt the scenario to account for automatic events. Meaningful inputs, expected
outputs and appropriate receiver-side assertions still need developer review.

### Delivery timeouts on CI

Set `test.delivery.timeout-seconds=10` in `user-flow-tests/src/test/resources/module-test.properties`.
The default is 10 seconds when absent; use a positive whole number, such as 60 for a slow Bamboo agent.
This controls each producer-output, generated-event, file-delivery, JMS receive and component-path
completion wait. Successful checks return immediately. The deliberate one-second idle/no-message
observation windows remain separate; they are not delivery deadlines.

The shared JUnit timeout reads this setting from `module-test.properties` and allows 60 seconds
for overhead plus ten delivery-timeout intervals. Set CI timeout increases in that shared file.
It replaces the fixed 60-second annotation in newly generated tests. Network connection and Spring
startup operations still depend on their own connection settings and are bounded by the outer test limit.
Existing developer-owned files are preserved: regenerate the shared support and affected tests using
archive-and-replace, or update them manually, and add the property to the existing properties file.
Remove old `@Test(timeout = 60000)` limits if longer delivery waits are needed.

### FTP consumer input files

For FTP consumers with an externalised filename pattern, add fixture files under
`user-flow-tests/src/test/resources` and set the paths in the test class:

```java
private static final String FIRST_BATCH_INPUT_FILENAME = "/input/orders-1.xml";
private static final String SECOND_BATCH_INPUT_FILENAME = "/input/orders-2.xml";
```

The generated defaults are `/<flow name>/<consumer name>/first.txt` and
`/<flow name>/<consumer name>/second.txt`, relative to the test resources root.
The shared `noSpaces()` helper replaces spaces in both directory names with underscores:
for example, flow `flow 2` and consumer `my ftp consumer` use
`/flow_2/my_ftp_consumer/first.txt`. Runtime names remain unchanged.

`prepareInputBatch` streams each classpath resource unchanged to `localFtpDirectory(context)`
using its basename (`orders-1.xml`, for example). It validates the basename against the runtime
consumer pattern, refuses existing files, and stages the copy outside the scanned directory
before publishing it. Use distinct matching filenames and enable `test.ftp.enabled=true`.
No `test.ftp.input.filename.batch1/batch2` settings are needed by these new tests.
The helper does not connect to external FTP/SFTP servers. The inline-text `FtpInputFixture.write`
helper remains available for custom scenarios and existing tests.

### Expected-output resources

File-producer tests use `FIRST_EXPECTED_OUTPUT_RESOURCE` and `SECOND_EXPECTED_OUTPUT_RESOURCE`,
by default `/<flow name>/<producer name>/first.txt` and
`/<flow name>/<producer name>/second.txt`. Both directory names use `noSpaces()` to replace
spaces with underscores, matching the input-resource convention. Add independent expected
UTF-8 files under that directory in `src/test/resources`; change the paths for XML or other text fixtures.
The test reads these only when the reviewed scenario runs. Expected-resource basenames do not
constrain producer filenames. Review the delivered-file glob or use
`assertFileMatchesResource(deliveredPath, expectedResource)` for a known output filename.
`assertDeliveredFileResources(directory, glob, resources...)` checks count and contents,
including duplicates, without depending on delivery order. Comparisons preserve whitespace;
failures show bounded context around a text difference rather than dumping large XML files.
External SFTP output must first be downloaded or made locally accessible; these helpers do not fetch it.
The local test SFTP server exposes its endpoint directory directly.

For inputs other than FTP and local files, inline text remains the default, with a commented alternative using
`readTestResource("/input/first.txt")`. This helper also lets custom assertions read an FTP
input fixture as UTF-8 text. Classpath access works in IntelliJ, Maven and CI without absolute
paths. Generation creates missing resource directories and `first.txt` / `second.txt` fixtures
for resource-based inputs and file/email expected outputs. Their initial contents are
`first expected payload` and `second expected payload`, without trailing newlines.
Existing directories and files are preserved, including when regenerating a test.
Replace sample contents with meaningful input and independent expected output fixtures before
setting `TEST_REVIEWED=true`. Missing resources after changing paths produce an actionable error.
Regenerate the affected tests, `ModuleFlowTestSupport`, `FtpInputFixture` and
`FileDeliveryAssertions` together using archive-and-replace to adopt these defaults.

The shared FTP directory uses `TemporaryFolder.builder().assureDeletion().build()`: failure to
remove it fails the test. The folder encloses the shared timeout rule. Both FTP consumer input
creation and FTP producer delivery assertions use `localFtpDirectory(context)`, so they retain
the same directory throughout a scenario and its two batches. Server startup receives a fresh
subdirectory for each application context. Server shutdown does not delete files; JUnit removes
them after the test, including ordinary assertion failures. A forcibly terminated JVM can still
leave temporary files behind. Regenerate shared support together with `LocalFtpTestServer`;
existing developer-owned tests and properties are not silently replaced.

### Shared support package

New flow tests remain in `org.ikasan.studio.flowtests`. Shared setup, FTP/JMS helpers and
file/payload assertions are generated into `org.ikasan.studio.flowtests.support` beneath
`user-flow-tests/src/test/java`. Scenario templates explicitly import the helpers they use.

Existing developer-owned files in the original package are retained so existing tests continue
to compile. Regenerating a scenario uses the new support package; port any custom shared setup
to that package before running it. Remove the old helpers only after all your tests have moved
and no references remain. Archive-and-replace continues to protect existing files in the new
support package when you regenerate shared setup.

### Local test SMTP server

For flows containing a component with the meta-pack's `supportsTestMailServer` capability,
the flow and module generation dialogs offer **Use a local test SMTP server**, selected by
default. Accept archive-and-regenerate for shared setup and the email helpers. Existing
properties and POM changes are backed up. The option enables this setting in
`user-flow-tests/src/test/resources/module-test.properties`:

```properties
test.smtp.enabled=true
```

Each test application starts an embedded GreenMail inbox on `127.0.0.1` with a dynamically
allocated port. Before the manually started flow runs, its mail producers are configured to
use that inbox, including producers whose original host/port were generated as Java literals.
The fixture also replaces extended mail-session connection settings with plain, unauthenticated
local SMTP settings. Recipients, subject and content remain the application's responsibility.
These changes apply only to the test application; they do not edit the model or generated code.
Closing the context stops the server; startup/configuration failures also clean up the server.
No mail is forwarded and there is no dependency on an already-running MailHog process.

This is separate from the canvas **Start Test Mail Server** tool, which launches MailHog for
interactive use. Both Ikasan 3.3.9 and 4.1.6 email endpoints use `javax.mail`, so their templates
use GreenMail 1.6.15 (Ikasan 4's Jakarta JMS support does not imply Jakarta Mail support).

Generated email-producer scenarios use expected body resources such as
`/flow2/my_email_producer/first.txt` and `second.txt`. The payload adapter extracts
`EmailPayload.getEmailBody()`. `verifyReceivedOutput` additionally checks the actual SMTP inbox
with `localSmtpServer(context).assertBody(batch, expected, deliveryTimeout(context))`.
The timeout comes from `test.delivery.timeout-seconds` (default 10). The default expects one
mailbox delivery per batch and compares exact decoded text; adapt the assertion for multiple
recipients, MIME alternatives or attachments. `receivedMessages()` exposes captured messages
for subject, address and attachment assertions. Input and expected files remain developer-owned.

For an existing flow such as Flow2, regenerate the test and shared support with both FTP and
SMTP options selected, approve backups, supply the expected email-body resources, then set
`TEST_REVIEWED=true`. Choosing not to use the SMTP option preserves current settings; to disable
an existing fixture, explicitly set `test.smtp.enabled=false` and provide external-server
configuration and receiver assertions.

### FTP input age and missing-output diagnostics

Ikasan's bundled FTP consumer configurations default to a 120-second minimum file age.
A test that copies a new file and fires one scan can therefore receive no output even when
FTP and SMTP connections are healthy. The local test FTP fixture now configures consumers
before flow startup using `test.ftp.consumer.min-age-seconds=0`. The generated properties file
includes that setting and a comment. Existing properties files can omit it (zero is the helper's
default); add it explicitly when changing the age for a deliberate age-filtering scenario.
This affects only local test FTP consumers. Production configuration, filename matching and
duplicate filtering remain unchanged. Complete inputs are staged outside the scanned directory
before publication, so this does not expose partially copied fixture data.

When no producer output arrives, the assertion reports the flow, producer, batch, timeout and
flow state, with pointers to consumer filters and earlier endpoint errors. It no longer presents
a missing observation as an actual null payload comparison.

Reusable support APIs document ownership, input/output semantics, cumulative counts, timeout
behaviour and failure cases in Javadoc. Business tests can use these helpers directly; callers
starting fixtures themselves own their cleanup. To adopt the minimum-age fix, refresh shared
module support and update `LocalFtpTestServer` using the utility-update procedure below.
Existing business scenario edits and resources need not change.

### Local-file consumer resources

Local-file consumers now default to the same resource-based input convention as FTP consumers:
`FIRST_BATCH_INPUT_FILENAME` and `SECOND_BATCH_INPUT_FILENAME` resolve to
`/<flow name>/<consumer name>/first.txt` and `second.txt`, with spaces replaced by underscores.
For Flow4 these are `/flow4/my_local_consumer/first.txt` and `second.txt`, relative to
`user-flow-tests/src/test/resources`. Change the paths for XML or other fixture formats.

`FileInputFixture.localFilenames` overrides the test consumer's filenames to select exactly
those two basenames in its JUnit temporary directory. It quotes regex characters in filenames.
`prepareLocalBatch` copies the chosen classpath file without converting its bytes to text,
stages outside the scanned directory, and removes only named batch fixtures before the next scan.
It rejects duplicate basenames; unrelated files are left alone. The application/model filename
configuration remains unchanged. JUnit reports a failure if temporary-directory cleanup fails.
The same streaming implementation backs FTP resource copying; FTP's configured filename pattern
is still validated. Inline text helpers remain available for custom tests.

Regenerate the affected tests and shared helpers, including `FileInputFixture` and
`FtpInputFixture`, to adopt these defaults. Existing business test sources and resource files
are not silently overwritten.


### Local test SFTP server

For flows with SFTP consumers or producers, **Use a local test SFTP server** is selected by
default in the Generate Flow Test dialogs. It enables `test.sftp.enabled=true` in
`module-test.properties`, adds test-scoped Apache MINA SSHD dependencies and regenerates
shared support after the normal archive confirmation. Existing properties and fixture files
are preserved. When adopting this in an existing test suite, regenerate `ModuleFlowTestSupport`
and `LocalSftpTestServer` together and regenerate the affected scenario to get file input defaults.

Each test application starts an embedded loopback listener on an allocated port. No installed
SFTP service, Docker container, fixed port or personal SSH key is required in Bamboo.
A fresh host key and matching `known_hosts` file retain SSH host verification. The server and
sessions stop when the context closes; JUnit removes its temporary files and keys afterwards.
Password authentication uses `test.sftp.username` / `test.sftp.password` (default `ikasan`).
`test.sftp.consumer.min-age-seconds=0` permits immediate consumption of complete fixtures;
set a positive value only when deliberately testing age filtering.

SFTP inputs use `/flow_name/consumer_name/first.txt` and `second.txt` resources, with missing
samples created during generation. Each resource is copied into the endpoint's temporary
server directory before the consumer is triggered. Filenames must match the model's
`filenamePattern`. The same flow processes both batches without restarting.

`localSftpDirectory(context, FLOW_NAME, componentName)` exposes a separate directory for each
endpoint. Generated producer tests use it with `assertDeliveredFileResources` to verify real
delivery. Input and output directories cannot accidentally share files. Common delivery
waits use `test.delivery.timeout-seconds` (default 10). Test-only endpoint configuration is
applied before flow startup; production configuration is unchanged. Other transports in the
same flow, such as JMS, still need their own test connection settings.


When the existing-file dialog appears, **Skip Existing** keeps existing scenario and support
classes unchanged while creating missing tests, helpers and resource files. This also applies
when a local FTP, SFTP or SMTP server is selected. Choose archive and regenerate when you want
to update existing shared support; otherwise ensure your retained support provides the APIs
used by the new test. Skipping support does not cancel creation of a missing flow test.

### Scheduled timer events

For a Scheduled Consumer using its default Quartz message provider, the scaffold uses
`ScheduledEventFixture.fire(harness, consumerName, text)` to send each batch through the real
consumer immediately. The event remains a `JobExecutionContext`, with fixture text in
`ScheduledEventFixture.TEXT_KEY`; the generated `outputText` reads that text for comparison.
The harness suppresses normal cron firing and keeps the same flow running for both batches.

When a broker obtains its own business data, prepare its database/service fixture and use
`ScheduledEventFixture.fire(harness, consumerName)` instead. This fires the same deterministic
timer event without attaching fixture text. Remove unused batch-input constants and compare
the resulting business payload in `outputText`; do not call `ScheduledEventFixture.text` on a
trigger-only event.

`ScheduledEventFixture.create(consumer)` and `create(consumer, text)` are also available for
custom assertions. Existing three-argument `fire` calls remain supported. Existing utility files
are preserved during generation: to adopt these overloads, back up and remove only
`support/utils/ScheduledEventFixture.java`, then generate again. Older projects may still have
this helper directly in `support`; update the test import to `support.utils` when adopting it.
Each call creates an independent context with a fixed fire time and copied job data.
It has no live scheduler: this checks downstream processing, not cron timing or recovery.
A custom message provider retains the input-preparation scaffold because its data requirements
cannot be inferred. Existing developer-owned tests are preserved; regenerate with the normal
backup option or use the helper directly.

### Shared support refresh detection

Keep your business tests. Studio tells you when shared test support needs refreshing.
When opening either flow-test generation dialog, Studio compares the shared support's
recorded fingerprint with the current model. Missing fingerprints or the older helper-package
layout also select refresh. The dialog explains the change; use the normal archive-and-regenerate
confirmation to replace support. Existing business scenarios and fixture resources are
excluded from automatic replacement. Skip Existing still preserves support, so it remains
stale until refreshed.

After one refresh with this version of Studio, tests also check the saved model before
Spring starts. Renamed, added or removed components and changes to pack-declared external
settings trigger an actionable failure. JSON formatting, property ordering, canvas fields,
Java business-code edits and a version-number-only change do not. The fingerprint records
no connection values, only their digest and the property names selected by the pack.
Run tests from the project root or user-flow-tests directory so the saved model can be found.

Shared refresh preserves module-test.properties. Review its connection references and
any affected business test names after a rename; unresolved property references now identify
the setting requiring review before Spring creates application beans. This check does not
validate business expectations or promise that every manual override remains appropriate.
During migration comparisons, keep the before/after tests frozen; preserve any stale-support
failure evidence before deliberately refreshing. Actual component/API changes may require
support updates even though a version-number change alone does not.


### Module wiring and reusable utilities

`org.ikasan.studio.flowtests.support` contains the model-dependent
`ModuleFlowTestSupport` and `ModuleJmsTestConfig`. Refresh these when Studio detects
changed module wiring.

`org.ikasan.studio.flowtests.support.utils` contains `ScheduledEventFixture`,
`FileInputFixture`, `FtpInputFixture`, `FileDeliveryAssertions`, `OutputTextSupport`,
`JmsFlowTestSupport`, `FlowTestSupportFingerprint`, and the local FTP, SFTP and SMTP
servers. Their source contains no module-specific mappings. Generation creates missing
utilities, but refreshing module support preserves existing utilities and any custom edits.

**Model changes require no utility regeneration.** Utilities remain meta-pack supplied:
Ikasan API upgrades or Studio fixes can still require updates. To adopt such an update,
back up the affected utility, remove that file, then generate again and review the diff.
For migration comparisons, retain the original tests until the comparison is complete.

Existing helpers in the old package are retained so existing business-test imports continue
to compile. Newly generated tests use `support.utils`; when adopting the new layout, update
any custom imports or explicit server-class lookups to use the new utility types. Inherited
helpers such as `localFtpDirectory` and `localSmtpServer` avoid those explicit lookups.


### Recovering test properties after refactoring

In either **Generate Flow Test** or **Generate Flow Tests**, select
**Refresh test properties (archive existing)** to recreate `module-test.properties`
from the current model. This option is off by default. It replaces stale component-property
references and custom settings with fresh defaults, saving the previous file beside it as
`module-test.properties.bak<timestamp>-<id>`. Review the backup and restore custom settings
you still need before running tests.

Existing business test classes and fixture resources are preserved when this option is selected.
Properties refresh still runs if you choose **Skip Existing** for shared support. Refresh stale
shared support too when Studio flags it; rebuilding properties does not repair component names
or expectations inside business tests. The selected local FTP/SFTP/SMTP options are applied to
the fresh properties after generation.


### Preparing and cleaning up instance fixtures

Both `runTest` and `runObservationTest` call `prepareFixtures(context)` once after Spring
starts and before starting the test flow. Override it to initialise expected-object instance
fields, load resources, seed databases or access Spring beans. Keep per-batch input preparation
in `supplyInput(context, harness, batch)`.

```java
private Order firstExpectedOrder;
private Order secondExpectedOrder;

@Override
protected void prepareFixtures(ConfigurableApplicationContext context) throws Exception {
    firstExpectedOrder = new Order("first-reference", 10);
    secondExpectedOrder = new Order("second-reference", 20);
}
```

Override `cleanupFixtures(context)` for resources your fixture owns. The default hooks do
nothing. Cleanup runs before Spring closes, after the standard flow teardown, even when setup
only partially succeeds or assertions fail. It must tolerate uninitialised fields. Cleanup
failures are suppressed onto the original failure; when cleanup alone fails, the test fails.
Application-startup failures occur before either hook. Spring and JUnit retain ownership of
their own resources, including the context and temporary test folders.

Refresh shared module support to adopt the hooks; existing tests and utility classes are preserved.

After a flow or component rename, Studio checks test-property references to generated
JMS, FTP, SFTP, email and local-file configuration once code generation succeeds.
If references no longer exist, a persistent, non-flashing banner appears above the canvas.
**Show details** lists the affected settings. Studio also checks when the editor opens
and when either properties file is saved; the banner disappears once references resolve.
**Refresh test properties…** opens the generation dialog with **Refresh test properties
(archive existing)** selected. Confirm the refresh, then review custom settings against
its backup. Existing business tests are preserved. The advisory checks saved files;
unsaved test-property edits must be saved before refreshing. Test startup also reports
invalid references, including changes made outside Studio. Shared configuration is
validated for the whole module, even when the selected flow does not use JMS.

The same banner also compares existing shared support with the saved model fingerprint,
including changes to flow names, components and connections. **Refresh flow test setup…**
opens the generation dialog with stale support refresh selected. Choose **Archive and
Regenerate** when prompted; **Skip Existing** leaves that support stale. Properties refresh
is selected only when broken property references were detected. Checks run on editor opening
and saved model/support changes, and the banner remains until both issues are resolved.
Existing business tests and fixture files are preserved during shared-support refresh.
