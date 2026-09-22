# Testing flows with the Ikasan test framework

Use Ikasan's `ikasan-test` framework to leave reusable tests in the application's
`user/src/test/java`, alongside tests of custom component behaviour. This complements
runtime verification through Studio; compilation and component invocation counts alone
do not establish successful delivery.

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
