# Testing flows with the Ikasan test framework

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
5. **Run:** set `CONFIGURED=true` after completing the other tasks and use the supplied Maven command.

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
4. Set `CONFIGURED=true` only after completing the scenario. Run from the project root:
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
For complex scenarios, `runTest(CONFIGURED, context -> { ... })` supplies the same context lifecycle
while leaving input and assertions explicit. Existing direct `verifyFlow`/`verifyScenario` callers
remain supported. Regenerate shared module setup when generating tests that use these new helpers.

### Self-generating source and discard sink

For a direct Event Generating Consumer → Dev Null Producer using the built-in provider,
the meta-pack selects a compact observation test. It has no `sendInput()` or expected-payload
placeholders: review shared settings, then enable it. The test checks initial producer invocation,
continued running and a fresh later invocation without restarting, keeping only an event count.
It stops the isolated test flow during teardown. This is a processing/readiness check, not proof
of payload correctness, idle behaviour or external delivery. Custom providers, intermediate
components, branches and exception resolvers retain the more explicit scenario scaffold.

Generated JUnit methods start with `test`, for example
`testGeneratedEventsReachProducerAndFlowKeepsRunning`. JUnit discovers them through `@Test`;
the name is for readability. Existing developer-owned tests are preserved. To adopt the new
scaffold, archive/regenerate the selected flow test and shared module setup.
