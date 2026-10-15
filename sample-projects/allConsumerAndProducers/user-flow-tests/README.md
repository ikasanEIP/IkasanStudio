# Flow tests

These are developer-owned scaffolds using the actual generated application and IkasanFlowTestRule.
They deliberately FAIL before starting services until you complete the test scenario.
Set CONFIGURED only after supplying isolated connection settings, repeatable inputs and expected outputs.
Consult ../LOCAL_TEST_ENVIRONMENT.md for test services. Never point tests at production systems.
Test-only MANUAL startup does not change the model or normal application startup settings.

Run from the project root: `mvn -pl user-flow-tests -am test`.
For one test: `mvn -pl user-flow-tests -am -Dtest=YourFlowTest -Dsurefire.failIfNoSpecifiedTests=false test`.
Assert actual delivery at external receivers as well as observed flow payloads. For routers, assert
every expected branch and absence of unwanted deliveries. For exclusions, assert stored exclusions
and delivery of subsequent valid input. Component invocation alone proves none of these.
Scheduled tests explicitly trigger the consumer; separately verify real scheduling and pacing.
Isolated test flows stop during teardown; production/demo flows must stay running between batches.

Migration updates the inherited version.ikasan dependency. Studio never regenerates these tests.
Run before and after migration; review framework API changes if a future major version needs them.
Keep assertions unchanged when comparing behaviour. Add application-specific timeouts and reliable
cleanup for any local services you start. A stuck JVM is a test/shutdown failure, not a passing run.
