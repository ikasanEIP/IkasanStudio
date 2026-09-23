# Testing flows with the Ikasan test framework

Use **Tools → Ikasan Studio → Generate Flow Test…**, or right-click a flow and choose
**Generate Flow Test…**, to create a reusable Ikasan test scaffold. Custom component unit
tests can still live in `user/src/test/java`. Application-level flow tests live in a separate,
developer-owned `user-flow-tests` Maven module: `generated` already depends on `user`, so putting
a test dependency on `generated` inside `user` would create a Maven dependency cycle.

## Generate and complete a test

1. Save the module and open files, and wait for generation to finish. Select a saved flow
   that has a consumer.
2. Generate the scaffold. Studio adds `user-flow-tests` to the root POM, creates the test module
   POM and README if missing, and opens the new Java test. Existing files are preserved;
   generating the same test again reports that it already exists. Commit the new files.
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
Both rules share the methods used in the [reference example](../examples/ikasan-flow-tests/README.md).
If the application uses JUnit Platform, enable JUnit Vintage for these JUnit 4 tests;
otherwise the tests may compile but never execute. Verify the Surefire test count.

In the developer-owned POM that owns the test sources, add test-scoped dependencies
on `ikasan-test` at the matching version and `junit:junit:4.13.2`. Retain the application's
Spring Boot/BOM versions. Replace an explicit older JUnit dependency rather than adding
another conflicting declaration. Do not hand-edit a generated POM.

## A first reusable test

The reference helper takes a real configured `Flow`, drives its scheduled consumer
through `IkasanFlowTestRule`, checks consumer/converter/producer invocation order,
and captures the actual final payload. It checks a first result, an idle interval with
the flow still running, and a second result without a reset or restart.

Instantiate the application's generated Spring configuration in an isolated test context,
obtain the real module bean and flow, and call the helper from a JUnit 4 `@Test`:

```java
ScheduledFlowExample.verifyTwoDeliveries(
    module.getFlow("Sample Orders"),
    "Generate Order", "Create Order", "Log Order",
    "ORDER-001", "ORDER-002");
```

Adapt component names and expected payloads to the brief. Implement the real converter
and sample source first. Use test-only configuration to keep flows from starting before
listeners are attached, isolate the H2 database and transport destinations, and close the
test context in teardown. Never change the deployed model's AUTOMATIC startup setting
for test convenience. `IkasanFlowTestRule` suppresses the scheduled consumer's normal
schedule in its default mode and triggers it explicitly; this does **not** verify the
production schedule, pacing or continuous runtime behaviour. Test those separately.

The example is a reference helper, not a standalone module or a passing demonstration:
`test-compile` verifies API compatibility; real delivery requires an application's actual
flow and assertions. It does not manufacture a mocked flow to claim runtime success.

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