# CLAUDE.md

This file provides guidance to Claude Code when working in this repository. Read [AGENTS.md](AGENTS.md) first: it is the primary project context for product decisions, IntelliJ UX, lifecycle, code ownership, and approved reference repositories. This file supplements that guidance with implementation and command details.

For exact package paths, versions, and task behaviour, check the current source, `gradle.properties`, and Gradle build files; historical baseline sections in documentation may describe earlier implementations.

## What This Is

Ikasan Studio is an **IntelliJ IDEA plugin** that provides a visual drag-and-drop designer for Ikasan Enterprise Integration Platform modules. It persists a version-neutral JSON model and generates Java, Maven, and configuration artefacts through FreeMarker templates. Preserve the separation between Studio-owned `generated/` output and developer-owned `user/` implementation.

- **Java compilation toolchain**: 17.
- **Build target**: IntelliJ IDEA Community 2024.3.7 (`platformType=IC`, `platformVersion=2024.3.7`). Compatibility is declared from build 242 with no upper bound; this declaration does not prove compatibility with every future IDE.
- **Verifier targets**: configured in `build.gradle.kts` using the current target and the oldest/newest verification properties in `gradle.properties`.
- **Distribution**: the plugin ZIP is published to JetBrains Marketplace by `.github/workflows/release.yml`. The README separately documents `main` for development/SNAPSHOT builds and `1_0_x` for formal Maven Central builds; do not confuse Maven artefact publication with plugin publication.

## Build & Test Commands

Run these from the repository root:

```bash
# Build the plugin ZIP for distribution
./gradlew buildPlugin

# Run the root automated plugin test suite (excludes the separate harness source set)
./gradlew test

# Run a single test class or method
./gradlew test --tests 'org.ikasan.studio.testing.packs.FlowTemplateTest'
./gradlew test --tests 'org.ikasan.studio.testing.packs.FlowTemplateTest.testCreateFlowWith_brokerComponent'

# Run headless engine and pack tests without configuring the IntelliJ plugin build
./gradlew -p headless test

# Run a focused pack test in the standalone build
./gradlew -p headless :studio-bundled-packs:test --tests 'org.ikasan.studio.testing.packs.FlowTemplateTest'

# Select tagged engine or pack tests in the root build
./gradlew test -PstudioTestSuite=engine
./gradlew test -PstudioTestSuite=packs

# Run root/headless checks and meta-pack validation (includes network checks for BOMs/help URLs)
./gradlew check

# Run enabled tests from the separate UI harness source set
./gradlew runHarness

# Launch IntelliJ with the plugin loaded for manual testing
./gradlew runIde

# Plugin binary compatibility verification
./gradlew verifyPlugin

# Static analysis
./gradlew qodanaScan
```

`-PexcludeHarness` is unnecessary and is not read by the current build: harness tests live in `src/testHarness/java`, outside the standard test source set. Do not use `allTests` as a verification gate: it is not wired to the harness source set and sets `ignoreFailures=true` for all failures.

If another sandbox IDE is running, package with an isolated sandbox, for example `./gradlew buildPlugin -PstudioSandboxDirectory=build/verification-sandbox`, rather than closing the user's IDE. See [headless/README.md](headless/README.md) for standalone module commands and test reports.

## Architecture

Packages below are relative to `org.ikasan.studio`.

### Headless core (`headless/studio-generator/src/main/java/`)

- **`core.model.ikasan.instance`** — Persisted domain objects including `Module`, `Flow`, `FlowElement`, `FlowRoute`, `ExceptionResolver`, and `ComponentProperty`.
- **`core.metapack`** — `ComponentLibrary` exposes metadata indexed by pack ID through immutable packaged-library snapshots. Metadata classes live in `core.metapack.model`; loading and validation have their own subpackages. Do not introduce a global active-pack selection.
- **`core.generator`** — FreeMarker generators such as `FlowTemplate`, `ApplicationTemplate`, and `ModuleConfigTemplate`. `core.BuildContext.getFreemarkerConfig(version)` creates a configuration for the requested pack; it is not a singleton configuration holder.
- **`core.io` and `core.persistence.json`** — JSON IO, model serialisation, and persistence helpers.
- **`core.StudioBuildUtils`** — Shared build/resource utilities.

The root `settings.gradle.kts` includes the standalone `headless` build as a composite build. The plugin depends on `studio-generator` and `studio-bundled-packs`; `studio-test-kit` supplies reusable pack contracts and rendering helpers for tests. Keep the headless engine independent of IntelliJ and Swing.

### IntelliJ integration and UI (`src/main/java/`)

- **`intellij.editor`** — Main-editor integration. `IkasanStudioFileEditor` exclusively owns `DesignerUI`; the stripe launcher must only open/focus that editor. Preserve deliberate tab closure and disposal of UI references while retaining the project model.
- **`intellij.project`** — Project initialisation and generated-project synchronisation.
- **`intellij.psi.StudioPsiUtils`** — Centralised PSI/file operations; prefer this over direct PSI usage.
- **`intellij` subpackages** — Platform adapters including settings, execution, debugging, and the tool-window launcher.
- **`ui.component.canvas`** — Visual design surface (`DesignerCanvas`, `CanvasPanel`).
- **`ui.component.palette`** — Component palette (`PaletteTabPanel`).
- **`ui.component.properties`** — Property editor panels (`ComponentPropertiesPanel`, `ComponentPropertiesTabPanel`).
- **`ui.viewmodel`** — Model-to-UI view handlers and `ViewHandlerCache`.
- **`ui.UiContext`** — Project-level service holding UI/model state. Access through `project.getService(UiContext.class)` and preserve multi-project isolation.

### Meta-packs (`src/main/resources/studio/metapack/`)

The current official packs are `V3.3.9` and `V4.1.6`. Each supplies `metapack.json`, component descriptors/icons under `library/`, and FreeMarker templates under `templates/`. Shared JSON schemas live under `schema/`.

`headless/studio-pack-v3` and `headless/studio-pack-v4` package the respective resource directories. `studio-bundled-packs` selects those artefacts; the root plugin resource source set excludes `studio/metapack/**` to avoid duplicate packaging.

A new pack needs a valid manifest, metadata, templates, packaging configuration, and contract/generated-output validation. Do not assume adding a directory alone provides support for another Ikasan version. See [METAPACK.md](src/main/resources/studio/metapack/METAPACK.md) and [IndependentMetaPackArtifacts.md](docs/IndependentMetaPackArtifacts.md).

### Ancillary Maven projects

`ikasan-studio-ancillary/` contains the project archetype and IDE mediator. These are separate from the Gradle plugin and standalone headless builds.

## Key Conventions

### IntelliJ stability and threading

- Catch failures at IntelliJ integration boundaries, log useful context, and provide a recoverable user-facing state. Preserve platform cancellation/control-flow exceptions; do not swallow them in broad recovery handlers.
- Honour IntelliJ nullability contracts, including `@NotNull` on platform overrides. Validate nullable input and provide recovery instead of returning null where the API forbids it.
- Use `com.intellij.openapi.diagnostic.Logger` for IntelliJ-facing code; the headless core uses SLF4J. Expected user/configuration problems should not be logged as fatal IDE errors.
- Make Swing mutations on the EDT. Keep expensive Maven, PSI/index, filesystem, JSON, and meta-pack work off the EDT. UI scheduling, read actions, and write commands solve different problems: `invokeLater` schedules work on the EDT; it does not grant read/write access. Use the appropriate platform read action for PSI reads and write command for undoable PSI changes.
- Follow project disposal and indexing lifecycle checks before applying asynchronous results.
- Use IntelliJ Platform controls and preserve keyboard access, accessibility, light/dark themes, and normal scaling.
- Keep the navigation label **Console** and identify it as the module-local **Blue Console** in guidance; it is distinct from the central Ikasan Dashboard.

### Implementation and tests

- Follow nearby naming patterns: `Ikasan{Entity}ViewHandler`, `{Entity}Template`, `{Entity}Panel`/`{Entity}Dialogue`, and `{Scope}Utils`.
- Lombok is used throughout the project for boilerplate such as getters, setters, builders, and `toString` methods.
- Tests live primarily in `src/test/java`; some mirror production packages while engine/pack suites use `org.ikasan.studio.testing`. Headless modules reuse selected root tests and also have module-local tests.
- Parameterised pack tests use `@ParameterizedTest` and `@MethodSource`; shared fixtures include `TestFixtures` and `AbstractGeneratorTestFixtures`.
- Run focused checks appropriate to the change, then required broader checks. Never treat ignored test failures as a passing verification result.
- Whenever `src/main/resources/messages/studioBundle.properties` changes, update `studioBundle_ja.properties` in the same change, preserving keys, placeholders, HTML, and meaning.
- Preserve unrelated working-tree changes.

### UI harness

- Harness tests live in `src/testHarness/java/org/ikasan/studio/ui/harness/`.
- `ComponentTestHarness` extends `BasePlatformTestCase`, supplies a real IntelliJ test fixture, carries `@Tag("harness")`, and bridges setup/teardown to Jupiter.
- `showInFrame(...)` displays a Swing frame and blocks until it is closed. `PanelHarnessTest` is disabled for automated runs because its methods require human interaction; use individual IDE test runs for manual inspection as described in that class.
- Automated harness tests must complete without waiting for a person. Do not add interactive blocking tests to the standard suite or CI.

## CI and Release

[build.yml](.github/workflows/build.yml) runs on pushes to `main` and on pull requests. It builds/audits the plugin archive, then runs standalone headless tests and root `check`, Qodana inspection, and plugin verification in separate jobs. A successful non-PR run prepares a draft release after those jobs complete.

[release.yml](.github/workflows/release.yml) publishes to JetBrains Marketplace when a GitHub release is released or prereleased, using environment-provided signing and publishing credentials. Do not publish merely to validate a change.

## Autonomy Policy

Work autonomously and carry the requested task through investigation, implementation, relevant checks, and correction of failures caused by the change.

Do not stop to ask whether to continue, run authorised read/write commands, use Python or Gradle, run builds/tests, fix failures caused by your changes, or make routine implementation choices and small necessary refactorings. Progress updates do not require ending the task.

Ask only when:

1. A significant architecture/design decision has alternatives with materially different consequences.
2. A requirement is ambiguous enough that choosing incorrectly could cause substantial wasted work.
3. An action would be destructive or difficult to reverse and is not already authorised.
4. Continuing would consume paid credits/tokens beyond the user's included allowance, if that transition is exposed by the environment. Do not claim to know account usage or billing state when it is unavailable.
5. Additional access is genuinely required and has not already been granted. The read-only Ikasan reference repositories listed in `AGENTS.md` are already authorised; respect environment-enforced permissions.

For a decision, give the alternatives, trade-offs, and recommendation in one concise question. Otherwise, make the most reasonable engineering decision and continue within the authorised scope.
