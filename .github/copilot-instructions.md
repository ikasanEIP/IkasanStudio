# Copilot Instructions for IkasanStudio

## Project Overview

IkasanStudio is an IntelliJ IDEA plugin (target: IU 2025.3, Java 17) that provides a visual drag-and-drop designer for [Ikasan Enterprise Integration Platform (EIP)](https://github.com/ikasanEIP/ikasan) modules. Users design integration flows visually; the plugin auto-generates production Java code via FreeMarker templates.

- **Branch strategy**: `main` = development/SNAPSHOT; `1_0_x` = formal releases to Maven Central

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

The codebase is split into two top-level packages under `org.ikasan.studio`:

### `core` — Framework-independent business logic
- **`core.model.ikasan.instance`** — Runtime domain objects: `Module`, `Flow`, `FlowElement`, `FlowRoute`, `ExceptionResolver`, `ComponentProperty` — persisted to `model.json` and shown on canvas
- **`core.model.ikasan.meta`** — Metadata/schema for each component type. `IkasanComponentLibrary` is the singleton registry. `ComponentMeta` → `ComponentTypeMeta` → `ComponentPropertyMeta` form the hierarchy.
- **`core.generator`** — Code generation engine; each `*Template.java` drives one FreeMarker `.ftl` template. `BuildContext` is the singleton FreeMarker config holder.
- **`core.io`** — JSON (de)serialization via Jackson (`ComponentIO`, `PojoDeserialisation`) for persisting the visual model

### `ui` — IntelliJ Platform integration
- **`ui.intellij`** — Tool window registration (`DesignerToolWindowFactory`), IntelliJ services
- **`ui.component.canvas`** — The visual design surface (`DesignerCanvas`, `CanvasPanel`)
- **`ui.component.palette`** — Drag-and-drop component palette (`PaletteTabPanel`)
- **`ui.component.properties`** — 35+ property editor panels, one per component type
- **`ui.viewmodel`** — Bridges instance models to UI rendering. All view handlers extend `AbstractViewHandlerIntellij`: `IkasanModuleViewHandler`, `IkasanFlowViewHandler`, `IkasanFlowComponentViewHandler`. `ViewHandlerCache` manages the registry.
- **`ui.UiContext`** — Project-level IntelliJ `@Service` holding references to canvas, palette, properties panel, module model, and PSI utilities per-project. Access via `project.getService(UiContext.class)`.
- **`ui.model.StudioPsiUtils`** — Centralized PSI API calls; prefer over direct PSI usage.

### Metapacks — Version-specific templates
`src/main/resources/studio/metapack/{version}/` (e.g., `V3.3.8`, `V4.0.x`) contains:
- `library/` — JSON component descriptors deserialized into `ComponentTypeMeta` / `ComponentMeta`
- `templates/` — FreeMarker `.ftl` files mirroring the target Java package structure

Adding support for a new Ikasan version means adding a new metapack directory — no Java changes required.

### Data flow
```
User interaction on Canvas
  → updates core.model (Module / Flow / FlowElement)
    → triggers core.generator templates
      → writes generated Java into project's generated/ directory
```

## Key Conventions

### Naming
| Concept | Pattern | Example |
|---|---|---|
| View handlers | `Ikasan{Entity}ViewHandler` | `IkasanFlowViewHandler` |
| Code generators | `{Entity}Template` | `FlowTemplate`, `ApplicationTemplate` |
| UI panels | `{Entity}Panel` or `{Entity}Dialogue` | `ComponentPropertiesPanel`, `PropertiesPopupDialogue` |
| Utilities | `{Scope}Utils` | `StudioPsiUtils`, `StudioBuildUtils` |
| Test classes | `{ClassName}Test` or `{ClassName}UITest` | `FlowTemplateTest`, `CronPanelUiTest` |

### IntelliJ-specific rules (critical for plugin stability)
- **Never let exceptions bubble up to IntelliJ** — catch, log with stack trace, and recover or abort. Uncaught exceptions cause IntelliJ to recommend disabling the plugin.
- **Never use `@NotNull`** — these surface to users as plugin errors.
- **Never log above `warn`** with IntelliJ's logger — `error`-level logs show stack traces directly to users.
- **EDT safety**: UI operations touching Swing must run on the EDT. Use `ApplicationManager.getApplication().invokeLater(...)` or `WriteCommandAction` for write actions.
- **Logger**: Use `com.intellij.openapi.diagnostic.Logger` (not SLF4J/Log4j).
- **PSI**: Use `StudioPsiUtils` — never call PSI APIs directly.

### Testing
- `src/test/java` mirrors `src/main/java` exactly — test classes live in the same package as the class under test.
- Parameterized tests use `@ParameterizedTest` + `@MethodSource` to cover multiple Ikasan versions.
- Shared test fixtures: `TestFixtures` and `AbstractGeneratorTestFixtures`.
- UI visual tests live in `src/testHarness/java/` and are tagged `@Tag("harness")`. `PanelTestHarness` (in `org.ikasan.studio.ui.test`) provides a mock IntelliJ `Project` for testing panels without launching the full IDE. Call `PanelTestHarness.cleanup()` in `@AfterAll`. For CI/CD, use `createPanel()` (headless) rather than `runVisualTest()`.

### Lombok
Lombok is used project-wide. Prefer `@Getter`/`@Setter`/`@Builder`/`@ToString` annotations over hand-written boilerplate.

### Code generation templates
FreeMarker `.ftl` files live in `src/main/resources/studio/metapack/{version}/templates/`. Template context variables are populated by the corresponding `*Template.java` class in `core.generator`.

## CI
GitHub Actions on every push/PR: compile → `test -PexcludeHarness` → Qodana scan → `verifyPlugin` → draft release artifact. See `.github/workflows/build.yml`.
