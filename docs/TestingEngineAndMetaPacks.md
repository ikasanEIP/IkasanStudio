# Engine and meta-pack tests

## Strategy

Meta-packs should eventually support independent releases, custom components and different Ikasan implementations. Shared testing code is expected: the intended boundary is a versioned, reusable engine/test kit, rather than a dependency on an IntelliJ plugin source checkout.

The current separation makes that boundary explicit without changing production packaging. Engine tests establish reusable behaviour; pack tests establish the contracts of particular releases. Keeping those responsibilities separate prevents a passing V3 test from being treated as evidence of V4 compatibility.

The generator and metadata tests have two owners under `src/test/java/org/ikasan/studio/testing/`.

| Suite | Responsibility | Inputs |
| --- | --- | --- |
| `engine` | Template loading, include isolation, rendering, diagnostics, schema/metadata validation and help composition | Small authored synthetic templates, TestV1 metadata and in-memory descriptors |
| `packs` | Official component libraries, dependency contracts, namespace choices, generated Java/properties and official catalogue contents | Explicit V3.3.9/V4.1.6 matrix and versioned expected output |

## Running the tests

Run commands from the repository root. The build uses the Java 17 toolchain. Use `gradlew.bat` instead of `./gradlew` on Windows. Initial dependency resolution requires access to the configured artifact repositories.

Run the suites with the standard IntelliJ-configured Gradle test task:

```sh
./gradlew test -PstudioTestSuite=engine
./gradlew test -PstudioTestSuite=packs
./gradlew test
```

The default remains the complete suite, including model, persistence, UI and integration tests elsewhere in `src/test/java`. The selectors run only the tagged suite, not every test under `core`. They do not imply that the build has become independent of the IntelliJ SDK; publishing a headless engine/test kit is a separate extraction step.

## Where a new test belongs

Use `engine` when changing a released pack should not change the assertion. Engine tests must not depend on `TestFixtures`, pack-specific test support, or IntelliJ/UI classes. `EngineTestBoundaryTest` enforces those Java dependencies. Synthetic templates under `EngineAlpha` and `EngineBeta` deliberately contain no Ikasan application code. Keep them small and authored by hand; never copy a production pack to make an engine fixture.

Use `packs` when the assertion names an official component, its dependencies, a Java namespace, or a particular generated artifact. Even if two packs currently emit identical output, that is still a pack contract. Preserve versioned expected files; do not generate expected output from the implementation during a test.

`PackExpectations` owns the reviewed official-version matrix, namespace policy and dependency expectations. Adding a pack requires explicitly reviewing these expectations and supplying its expected output. It must not silently inherit the last known version's policy. Test through public generator `create(...)` methods.

The existing `core.TestFixtures` remains shared by older model/UI integration tests and official pack tests. It uses real packs and is deliberately excluded from engine tests. General resource reading remains in `core.generator.TestUtils`; golden-output lookup and generator setup live with pack tests.

## What passing means

Pack tests currently verify metadata and rendered output. They do not certify compilation or startup against the target Ikasan BOM. Generated-project compilation, startup and independently published pack artifacts remain subsequent work. The split preserves existing assertions and makes that next extraction possible without treating template snapshots as engine correctness.

## Focused tests and reports

Run one engine class, one pack class, or one pack test method:

```sh
./gradlew test --tests 'org.ikasan.studio.testing.engine.TemplateEngineTest'
./gradlew test --tests 'org.ikasan.studio.testing.packs.FlowTemplateTest'
./gradlew test --tests 'org.ikasan.studio.testing.packs.FlowTemplateTest.testCreateFlowWith_brokerComponent'
```

Parameterized pack tests run all versions supplied by their method source. There is currently no Gradle option to select only V3 or only V4. The shared `PackExpectations.metaPacksToTest()` matrix includes V3.3.9 and V4.1.6; some other pack tests explicitly select versions or inspect every shipped pack. Do not narrow the shared matrix to make a failing version disappear.

`--tests` filters and `studioTestSuite` tag filters are combined. Avoid selecting an engine class while leaving `studioTestSuite=packs` in your command or Gradle properties. The supported suite values are `all`, `engine` and `packs`; an unknown value fails configuration. To explicitly request all suites:

```sh
./gradlew test -PstudioTestSuite=all
```

Results are written to:

- HTML: `build/reports/tests/test/index.html`
- JUnit XML: `build/test-results/test/`

All selectors use the same `test` task and report directory. Read or archive the results before the next run replaces them. Gradle may report `UP-TO-DATE` when the same inputs have already passed. To deliberately execute tests again:

```sh
./gradlew test --rerun-tasks
```

## Broader checks and manual verification

| Command | Purpose |
| --- | --- |
| `./gradlew test` | Complete automated suite, including tests outside the engine/pack groups |
| `./gradlew validateMetaPacks` | Automated tests plus declared BOM resolution and help-link reachability checks; requires network access |
| `./gradlew check` | Gradle verification lifecycle, including meta-pack validation |
| `./gradlew runHarness` | Separate visual Swing harnesses requiring human inspection |
| `./gradlew runIde` | Sandbox IntelliJ for manual editor, project lifecycle and threading checks |
| `./gradlew verifyPlugin` | IntelliJ plugin compatibility verification |

For complete validation, omit `studioTestSuite` and `--tests` filters: `validateMetaPacks` depends on the same `test` task and therefore inherits any test filtering. BOM resolution proves that the declared BOM is available; it does not compile a generated application. Visual harnesses are separate from the automated `test` suite.

## Workflow for changes

1. For an engine change, run the engine suite first, then the pack suite to detect changes to official generated output.
2. For an official pack change, run the pack suite and inspect failures against the matching Ikasan API. Change expected output only after establishing that the new output is correct.
3. Before considering either change complete, run the full automated suite without filters. Use broader and manual checks when the affected behaviour requires them.
4. When adding a pack, review its entry in `PackExpectations`, provide versioned expected files, and add assertions for meaningful differences such as Java namespaces, dependency coordinates and scopes. Counts alone cannot establish dependency correctness.
5. When adding tests, apply `@Tag("engine")` or `@Tag("packs")` to the appropriate class. A new untagged test will run in the full suite but will be absent from both selected suites.

## Future extraction

The next architectural step is to publish the headless generator and shared pack contract tests as versioned artifacts. A separately released pack could then run those tests from its own Maven or Gradle build. Certification should additionally compile generated projects against the exact target BOM and run representative startup/integration checks. Those capabilities are not implemented by the suite split described here.
