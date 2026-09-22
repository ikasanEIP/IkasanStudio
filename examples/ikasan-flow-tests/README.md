# Ikasan flow-test reference

[ScheduledFlowExample.java](src/test/java/org/ikasan/studio/examples/ScheduledFlowExample.java)
is a reusable helper for an actual scheduled consumer → converter → producer flow.
Copy/adapt it into the application's developer-owned test sources and call it from a
JUnit 4 test with a real flow from an isolated application context. See
[setup, lifecycle and acceptance guidance](../../docs/IkasanFlowTesting.md).

Compile the helper against each supported release:

```sh
mvn -f examples/ikasan-flow-tests/pom.xml clean test-compile
mvn -f examples/ikasan-flow-tests/pom.xml -Pikasan4 clean test-compile
```

These commands check compatibility only; this reference project deliberately has no
application and therefore no runtime delivery test. The consuming application's test
must assert its own meaningful expected outputs. Do not report these commands as evidence
that a generated application works. The isolated example POM is not a replacement for
an application's existing dependency management.

The reference helper was successfully compiled against both released artifacts on
21 September 2026. Application runtime verification remains the consuming project's responsibility.
