# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

IkasanStudio is an **IntelliJ IDEA plugin** (built with the IntelliJ Platform Gradle Plugin) that provides a visual drag-and-drop designer for [Ikasan Enterprise Integration Platform](https://github.com/ikasanEIP/ikasan) modules. Users design integration flows visually; the plugin auto-generates production Java code via FreeMarker templates.

- **Branch strategy**: `main` = development / SNAPSHOT builds; `1_0_x` = formal releases to Maven Central
- **Target platform**: IntelliJ IDEA Ultimate 2025.3 (`platformVersion=2025.3`, `platformType=IU`)

## Build & Test Commands

```bash
# Build the plugin ZIP for distribution
./gradlew buildPlugin

# Run all unit/integration tests (standard)
./gradlew test

# Run a single test class
./gradlew test --tests "org.ikasan.studio.core.generator.FlowTemplateTest"

# Run a single test method
./gradlew test --tests "org.ikasan.studio.core.generator.FlowTemplateTest.testCreateFlowWith_brokerComponent"

# Run tests excluding UI harness tests (used in CI)
./gradlew test -PexcludeHarness

# Run only UI visual harness tests (tagged @Tag("harness"))
./gradlew runHarness

# Run all tests including UI visual tests (ignores ThreadLeak failures from UI tests)
./gradlew allTests

# Launch IntelliJ with the plugin loaded for manual testing
./gradlew runIde

# Plugin binary compatibility verification
./gradlew verifyPlugin

# Static analysis
./gradlew qodanaScan
```

## Architecture

The plugin is split into two top-level packages under `org.ikasan.studio`:

### `core` — Framework-independent business logic (`src/main/java/org/ikasan/studio/core/`)

- **`core.model.ikasan.instance`** — Runtime domain objects: `Module`, `Flow`, `FlowElement`, `FlowRoute`, `ExceptionResolver`, `ComponentProperty`. These are what get persisted to `model.json` and shown on the canvas.
- **`core.model.ikasan.meta`** — Metadata/schema for each component type. `IkasanComponentLibrary` is the singleton registry. `ComponentMeta` → `ComponentTypeMeta` → `ComponentPropertyMeta` form the hierarchy.
- **`core.generator`** — Code generation engine. Each `*Template.java` drives one FreeMarker `.ftl` template (e.g., `FlowTemplate`, `ApplicationTemplate`, `ModuleConfigTemplate`). `BuildContext` is the singleton FreeMarker config holder.
- **`core.io`** — JSON (de)serialization via Jackson (`ComponentIO`, `PojoDeserialisation`) for persisting the visual model.
- **`core.StudioBuildUtils`** — Shared build utility methods.

### `ui` — IntelliJ Platform integration (`src/main/java/org/ikasan/studio/ui/`)

- **`ui.intellij`** — Main-editor integration, the one-click tool-stripe launcher, IntelliJ services, run configurations, onboarding, and lifecycle.
- **`ui.component.canvas`** — Visual design surface (`DesignerCanvas`, `CanvasPanel`).
- **`ui.component.palette`** — Drag-and-drop component palette (`PaletteTabPanel`).
- **`ui.component.properties`** — Property editor panels, one per component type (`ComponentPropertiesPanel`, `ComponentPropertiesTabPanel`).
- **`ui.viewmodel`** — Bridges instance models to UI rendering. All view handlers extend `AbstractViewHandlerIntellij`: `IkasanModuleViewHandler`, `IkasanFlowViewHandler`, `IkasanFlowComponentViewHandler`, etc. `ViewHandlerCache` manages the registry.
- **`ui.UiContext`** — Project-level IntelliJ `@Service` (replaces old singleton) holding references to the canvas, palette, properties panel, module model, and PSI utilities per-project. Access via `project.getService(UiContext.class)`.
- **`ui.model.StudioPsiUtils`** — Centralized PSI API calls; prefer this over direct PSI usage.

### Metapacks — Version-specific templates (`src/main/resources/studio/metapack/`)

One directory per supported Ikasan version: `V3.3.8`, `V4.0.x`, `VHS3.3.x`. Each contains:
- `library/` — JSON component descriptors (`component-type-meta.json`, `component-meta.json`) deserialized into `ComponentTypeMeta` / `ComponentMeta`.
- `templates/` — FreeMarker `.ftl` files in a path mirroring the target Java package. Adding a new Ikasan version means adding a new metapack directory — no Java changes required.

### Data flow

```
User interaction on Canvas
  → updates core.model (Module / Flow / FlowElement)
    → triggers core.generator templates
      → writes generated Java into project's generated/ directory
```

## Key Conventions

### Naming patterns
| Concept | Pattern | Example |
|---|---|---|
| View handlers | `Ikasan{Entity}ViewHandler` | `IkasanFlowViewHandler` |
| Code generators | `{Entity}Template` | `FlowTemplate`, `ApplicationTemplate` |
| UI panels | `{Entity}Panel` or `{Entity}Dialogue` | `ComponentPropertiesPanel` |
| Utilities | `{Scope}Utils` | `StudioPsiUtils`, `StudioBuildUtils` |
| Test classes | `{ClassName}Test` | `FlowTemplateTest` |

### IntelliJ-specific rules (critical for plugin stability)
- **Never let exceptions bubble up to IntelliJ** — catch, log with stack trace, and recover or abort. Uncaught exceptions cause IntelliJ to recommend disabling the plugin.
- **Never use `@NotNull`** — these surface to users as plugin errors.
- **Never log above `warn`** with IntelliJ's logger — `error`-level logs show stack traces directly to users.
- **EDT safety**: UI operations touching Swing must run on the EDT. Use `ApplicationManager.getApplication().invokeLater(...)` or `WriteCommandAction` for write actions.
- **Logger**: Use `com.intellij.openapi.diagnostic.Logger` (not SLF4J/Log4j) for IntelliJ-facing code.

### General conventions
- **Lombok** is used project-wide: `@Getter`, `@Setter`, `@Builder`, `@ToString` over hand-written boilerplate.
- Test classes in `src/test/java` mirror `src/main/java` exactly (same package as class under test).
- Parameterized tests use `@ParameterizedTest` + `@MethodSource` to cover multiple Ikasan versions.
- Shared test fixtures: `TestFixtures` and `AbstractGeneratorTestFixtures`.

### UI visual testing (testHarness)
- UI visual tests live in `src/testHarness/java/` and are tagged `@Tag("harness")`.
- `PanelTestHarness` (in `org.ikasan.studio.ui.test`) provides a mock IntelliJ `Project` for testing panels without launching the full IDE.
- Call `PanelTestHarness.cleanup()` in `@AfterAll` to dispose resources.
- For CI/CD, use `createPanel()` (headless) rather than `runVisualTest()`.

## CI

GitHub Actions on every push/PR: compile → `test -PexcludeHarness` → Qodana scan → `verifyPlugin` → draft release artifact. See `.github/workflows/build.yml`.
