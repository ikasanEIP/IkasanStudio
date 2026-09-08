# Ikasan Studio — AI Agent Project Memory

Compiled 2026-09-08 from every Markdown file in the repository. This is my working memory/summary for future sessions; the authoritative project context remains `AGENTS.md`, and per-document detail lives in the files cited below.

## 1. Project identity

- **What**: IntelliJ IDEA plugin — a visual, n8n-like designer/IDE for Ikasan (Enterprise Integration Platform / ESB) modules. Users compose modules from flows and components on a drag-and-drop canvas; the plugin persists a **version-neutral JSON model** and generates Java/Maven/config artefacts from **version-specific FreeMarker templates**.
- **Plugin coordinates**: `com.github.ikasaneip.ikasanstudio` (group), `IkasanStudio` (name), version `1.0.0`, `pluginSinceBuild = 242`, no upper bound. License: Apache 2.0.
- **Actual build target (gradle.properties)**: `platformType = IC`, `platformVersion = 2024.3.7`, bundled plugins `com.intellij.java`, terminal, maven. Java toolchain 17. Gradle wrapper 9.4.0.
- **Branch strategy**: `main` = development/SNAPSHOT; `1_0_x` = formal releases to Maven Central.
- **Owner/contact**: david@davihilton.net. Approaching first formal Marketplace release (end of Beta).

## 2. Mission / product north star (from AGENTS.md)

> Provide an IDE for Ikasan developers as an IntelliJ plugin conforming to IntelliJ UI/interaction standards; interaction should be easy and intuitive, a bit like n8n; formal Marketplace release soon.

Practical translation: developer product (not just diagram renderer/code generator); follow IntelliJ Platform conventions; optimise first-run discoverability (no hidden knowledge of tool-window icons or meta-packs); Marketplace readiness/stability/docs/onboarding are product requirements.

## 3. Language convention

- The project language is **English**; all discussions, documentation, comments, and user-facing text must be in English.
- The Japanese i18n resource files are for an overseas client only — keep English and Japanese action/message resources synchronized (per `docs/DiagnosticsAndPrivacy.md`), but never treat Japanese as the project language.

## 4. Terminology that matters

- **Blue Console** (button label: **Console**): module-local developer console shipped with every module — start/stop flows, error states, module REST API, admin tasks. Tooltip/guidance must call it Blue Console, **never** Ikasan Dashboard. Local login `admin` / `admin`.
- **Ikasan Dashboard**: central control point for multiple modules — aggregate error view, replay rejected messages, remote module REST. Distinct product.
- **generated/** = Studio-owned output (regeneratable). **user/** = developer-owned stubs/implementation (never overwrite without explicit consent).
- **Meta-pack** = version-specific compatibility layer (component library JSON + icons + FreeMarker templates + `metapack.json` manifest). The directory name is the stable model identifier (e.g. `V3.3.9`, `V4.1.6`); the manifest records exact Ikasan version and Java version.

## 4. Current implemented state (August–September 2026)

- Editor-based onboarding: `IkasanStudioFileEditor` is the exclusive owner of `DesignerUI`; project-scoped virtual file in main editor area; right-stripe squid icon = one-click launcher (opens/focuses editor, hides empty tool window); **Tools → Open Ikasan Studio** and Find Action are fallbacks; deliberate tab closure is respected; reopen preserves the model.
- Top controls: **Run module** and **Console**. Run module creates/reuses a standard Java Application run config for `generated/src/main/java/org/ikasan/studio/boot/Application.java`, selects it, launches via normal execution APIs. Workspace module discovery uses non-blocking read action.
- New modules materialise `flowStartupType=AUTOMATIC` (V3.3.8/V4.0.x-era templates emit `ikasan.module.activator.startup.type.defaultStartupType=AUTOMATIC`); existing modules keep their choice.
- Empty-canvas onboarding: add flow → add consumer/components → run valid module → wait for startup → open Console. Hints disableable in Settings → Tools → Ikasan Studio; flow-level hints sit below empty-flow artwork.
- Version migration **V3.3.9 ↔ V4.1.6** implemented (`Migrate…` on canvas / Tools menu; recovery snapshots under `.ikasan-studio/migrations/`; directional rules in `studio/metapack/{V4.1.6|V3.3.9}/migrations/from-*.json`; JDK: 11 for V3.3.9, 17 for V4.1.6). Engine in `core/migration`, IntelliJ orchestration in `MigrationController`.
- Converter recipes: 29 explicit recipes per pack; FTP/SFTP, email (text/attachment), JMS (String/bytes/Map), Logging/Dev Null, Generic Producer. Metadata-driven, `ConversionRecipeMeta` with stable ids; filename policy differs FTP/SFTP (auto-generated unique names) vs email attachments (`message.dat` fallback); email attachments need the producer's `hasAttachments=true` (generated call emitted commented-out with warning).
- AI-friendly offline contract: generation maintains `generated/IKASAN_STUDIO.md`, `generated/src/main/model/model.schema.json`, `generated/src/main/model/component-catalogue.json`, and root `AGENTS.md` (only when missing). `AiProjectContractGenerator` owns format; `CONTRACT_VERSION` bump only for incompatible format changes.
- Diagnostics/privacy: JetBrains Marketplace error reporter registered (no auto telemetry); **Tools → Collect Ikasan Studio Diagnostics…** produces local ZIP (`environment.txt`, `studio-redacted.log` — last 2 MiB, structured events only, `README.txt`); `StudioDiagnosticEvent` with fixed event ids and salted-context hashes; never log maps/toString/property values/tokens/URLs/raw exception messages.
- Testing infrastructure: ArchUnit boundary tests; failure-injection suite; performance benchmark (`performanceTest`); accessibility automated review + interactive release matrix.
- Known release-blocker-grade items: interactive a11y sign-off incomplete; initial generation is slow (~10 s first commit) with synchronous PSI work; `TerminalView` scheduled-removal usage in `LaunchH2Action`.

## 5. Build / test / verification commands

```bash
./gradlew test                          # full automated suite
./gradlew test --tests "Class#method"   # focused
./gradlew test -PexcludeHarness         # CI-style, no harness
./gradlew allTests                      # all incl. visual (ignores ThreadLeak from UI tests)
./gradlew runHarness                    # visual Swing harness
./gradlew runIde                        # sandbox IDE (also runIdeInternal for internal-mode checks)
./gradlew buildPlugin                   # distributable plugin ZIP
./gradlew verifyPlugin                  # Plugin Verifier (2024.3.7: compatible)
./gradlew qodanaScan                    # static analysis
./gradlew validateMetaPacks --no-configuration-cache   # meta-pack validation + suite + BOM resolution + HTTPS help links
./gradlew performanceTest --no-configuration-cache     # opt-in benchmark (40 flows/480 comps default)
./gradlew cleanTest test                # release gate
```

Migration compile fixtures (when emitted by tests): `mvn -B -f build/migration-compile/{V4.1.6|V3.3.9}/pom.xml -DskipTests compile`.

## 6. Architecture map

- `core` (framework-independent; no IntelliJ/Swing deps allowed):
  - `core.model.ikasan.instance` — `Module`, `Flow`, `FlowElement`, `FlowRoute`, `ExceptionResolver`, `ComponentProperty` (persisted to `model.json`).
  - `core.model.ikasan.meta` — `IkasanComponentLibrary` (registry), `ComponentMeta` → `ComponentTypeMeta` → `ComponentPropertyMeta` hierarchy.
  - `core.generator` — one `*Template.java` per FreeMarker `.ftl`; `BuildContext` holds FreeMarker config.
  - `core.io` — Jackson JSON (de)serialisation.
  - `core.migration` — framework-independent version-migration engine.
  - `core.StudioBuildUtils`, `core.BuildContext`.
- `ui` (IntelliJ-dependent):
  - `ui.intellij.editor` — virtual file, main-editor integration, restoration, single `DesignerUI`.
  - `ui.intellij.toolWindow.DesignerToolWindowFactory` — stripe launcher only; must never build a second designer.
  - `ui.DesignerUI` — assembles canvas/palette/properties; initialises after indexing.
  - `ui.UiContext` — project-level `@Service` (`project.getService(UiContext.class)`); single owner per project; editor close clears UI-only refs, keeps model.
  - `ui.component.canvas|palette|properties`, `ui.viewmodel` (handlers extend `AbstractViewHandlerIntellij`; `ViewHandlerCache`), `ui.model.StudioPsiUtils` (central PSI/VFS access).
  - `ui.intellij.StudioProjectInitialisationService` — startup states inside editor.
- Meta-packs: `src/main/resources/studio/metapack/{V3.3.9,V4.1.6}/` each with `metapack.json`, `library/` (category `component-type-meta_en_GB.json` + per-component `component-meta.json`), `templates/` (package-path mirror), `migrations/` (V4.1.6 & V3.3.9). Shared docs: `METAPACK.md`, `METAPACK_COMPLIANCE.md`, `schema/`.
- Ancillary Maven projects (`ikasan-studio-ancillary/`, independent SDLC): `ikasan-studio-ide-mediator` (IDE↔core bridge) and `ikasan-studio-project-archetype` (Maven archetype producing new Studio projects).
- Approved reference Ikasan source trees (read-only): `/home/hidavi/dev/ws/ik3/ikasan3`, `/home/hidavi/dev/ws/ik4/ikasan4`, `/home/hidavi/dev/ws/ik5/ikasan5`.

## 7. Critical coding rules (plugin survival)

1. **No exceptions bubble to IntelliJ** — catch, log stack trace, recover/abort; else IDE suggests disabling the plugin.
2. **Never use `@NotNull`** — surfaces as plugin error to users.
3. **Never log above `warn`** with IntelliJ's logger — `error` shows stack traces to users.
4. EDT safety: Swing mutations on EDT only; heavy Maven/PSI/FS/JSON/meta-pack work off EDT; write actions via `WriteCommandAction`.
5. Logger: `com.intellij.openapi.diagnostic.Logger` (not SLF4J/Log4j) in IntelliJ-facing code.
6. PSI/VFS through `StudioPsiUtils`, not direct APIs.
7. Lombok project-wide (`@Getter/@Setter/@Builder/@ToString`).
8. Tests mirror main package structure; parameterised tests via `@ParameterizedTest`+`@MethodSource`; shared fixtures `TestFixtures`, `AbstractGeneratorTestFixtures`; UI visual tests in `src/testHarness/java` tagged `@Tag("harness")`, use `PanelTestHarness.cleanup()` in `@AfterAll`, headless `createPanel()` for CI.
9. Naming: `Ikasan{Entity}ViewHandler`, `{Entity}Template`, `{Entity}Panel`/`Dialogue`, `{Scope}Utils`.
10. FreeMarker style: prefer Java getter style `${getMeta().getName()}` (readme note in template dirs).
11. Do not modify unrelated working-tree changes (repo may hold WIP; e.g. untracked `rename.sh`, pre-existing uncommitted metapack work).

## 8. ArchUnit boundary rules (executable, `ArchUnitBoundaryTest`)

- `org.ikasan.studio.core..` must not depend on `ui..`, `intellij..`, `com.intellij..`.
- `core.model..` must not depend on persistence/view adapters (serialisation config → `core.persistence..`).
- `org.ikasan.studio.integration..` must not depend on UI/IntelliJ.
- PSI/VFS/execution APIs belong under `org.ikasan.studio.intellij..`; finite named-class migration list in the test source is a ratchet (remove entries when adapters land; never add casually).
- Static fields must not retain projects/modules/Studio modules/virtual files/paths/files/classloaders (incl. generic type args). Project state → project-level service.
- Test imports `build/classes/java/main` explicitly (runs after `compileJava`).

## 9. Meta-pack contract essentials

- `metapack.json`: `schemaVersion:1`, `id` == directory name, exact `ikasanVersion` (never range/x), `javaVersion`, `dependencyManagement` (BOM == ikasanVersion), `compatibilityOverrides` (exact version + reason only).
- Component metadata declares deps without versions where BOM manages them; direct third-party versions are exceptional and must appear in overrides.
- Loading is atomic: any invalid category/component file rejects the whole pack with source paths + failures (no partial library). Duplicate JSON keys detected.
- Release verification: `validateMetaPacks`, maximal + minimal fixture modules, Maven dependency convergence, no `org.ikasan` dep resolving off-version, full Gradle suite.
- Meta-pack security (roadmap): templates are executable codegen inputs; need signed official packs, trust model, restricted FreeMarker wrapper, helper allow-list, path-traversal prevention, dependency allow/deny, preview before first use.

## 10. Roadmap status (docs/IkasanStudioRoadmap.md, reviewed 30 Jul 2026)

Four phases all "Not started" formally, with immediate next actions defined:
1. **Trustworthy foundation** — formal `metapack.json` + JSON Schemas, stable component/property IDs, structured validation diagnostics, transactional pack loading, standalone `metapack-validator` CLI, compile fixtures vs Ikasan 3.3.8, V3.3.8 metadata/API audit, deterministic `GenerationPlan`, fix known generator/canvas defects.
2. **Low-code usability** — searchable palette, semantic property editors, inline validation/quick fixes, auto layout, generation preview + source-ownership rules, explicit migration wizard.
3. **Interactive execution** — proper run configs, runtime process-state tracking, Studio Runtime protocol, event injection, live traces, pause/step/retry, breakpoints/mock nodes, payload redaction/retention.
4. **Meta-pack ecosystem** — independent versioned pack artifacts, signing/trust, org pack inheritance, migration-provider APIs, SDK + reference pack, compatibility matrix, CI badges.

Architectural principles: visual model is source of truth; stable IDs; pack = versioned validated product contract; generation deterministic/previewable/atomic; version change = explicit migration (not metadata swap); runtime/debug separate from codegen; customer packs need trust model; core usable without IntelliJ.

Known defects to fix early (roadmap §Codebase robustness): `Stream.peek()` without terminal op in selection/deselection; debug action can delete selected component; RuntimeExceptions escaping Swing callbacks; button-local start/stop booleans instead of process state; hard-coded generated paths/class names; shared mutable static meta-pack caches; generate only changed files; central Java-literal escaping; reliable JAR/external pack discovery; explicit file ownership.

## 11. Open handoff task (.agents/component-drag-move-handoff.md, 2026-08-28)

User asked for in-canvas component drag/move (same flow/route or cross-flow) **without delete/recreate**, preserving exact `FlowElement` instance + config + user-class associations; neighbours reconnect on success; invalid drop restores exact original position; grey translucent ghost while dragging; keep valid/invalid flow highlighting. Read-only investigation done; no code changed at handoff. Key facts gathered: canvas = `DesignerCanvas.java`; palette path (`CanvasImportTransferHandler` → `requestToAddComponent`) is the copy path and must stay so; existing `mouseDragAction` only mutates temp x/y (cosmetic); `mouseReleaseAction` just repaints; `componentDraggedToFlowAction` already highlights targets; `insertNewComponentBetweenSurroundingPair`/`getSurroundingComponents` handle new-item positioning; consumer on `Flow.consumer`, exceptionResolver on `Flow.exceptionResolver`, elements in `FlowRoute.flowElements`; `Flow.removeFlowElement` returns `FlowElementRemoval` with `undo()`; routers cascade child routes; undo pattern = `DeleteComponentUndoableAction`; recommended approach = keep model unchanged while dragging, validate source-excluded move transaction, commit by relocating same instance, register one global undoable action, regenerate with flow-scoped vs full generation request. Tests to add enumerated in the handoff doc.

## 12. Docs index (all Markdown reviewed)

**Root**
- `README.md` — user/dev intro; install from disk or Marketplace; archetype project creation (IntelliJ Maven Archetype or CLI; manual fallback Appendix A); simple flow walkthrough; high-level motivations; version-neutral model rationale; application split Core/Metapack/UI; epics; known issues (build needs consumer+producer; Maven re-pull after creation; deleted-component bug workaround); reporting problems (Marketplace error reporter, diagnostics action); plugin dev guidelines (the critical rules above).
- `AGENTS.md` — authoritative tool-neutral project context (mission, UX baseline, architecture, priorities, conventions, reference trees).
- `CLAUDE.md` — Claude-specific quick reference. **Stale bits**: says platform IU 2025.3 (actual IC 2024.3.7) and metapacks `V3.3.8/V4.0.x/VHS3.3.x` (actual `V3.3.9/V4.1.6`). Commands and conventions are otherwise accurate.
- `CHANGELOG.md` — only "Unreleased / Initial scaffold" entry (template default).
- `CONTRIBUTING.md` — contribution flow, JDK 17 + IC 2024.3.7, structure, engineering expectations, PR/CI requirements.
- `SECURITY.md` — private vulnerability reporting (GitHub Security tab or email), scope.
- `CODE_OF_CONDUCT.md` — Contributor Covenant 2.1.

**docs/**
- `IkasanStudioRoadmap.md` — deepest technical/product roadmap (see §10).
- `IkasanVersionMigration.md` — V3.3.9↔V4.1.6 migration workflow, what changes, restore vs migrate-back, snapshot format, maintenance/verification.
- `MarketplaceReleaseManualChecklist.md` — threading/lifecycle release blockers, external process/harness checks, automated gates.
- `DiagnosticsAndPrivacy.md` — error reporting, diagnostics ZIP contents, structured events, leak fixes, verification.
- `ConversionRecipes.md` — converter recipe UX, MVP coverage table, filename policy, email-attachment switch, metadata/maintenance, verification.
- `AccessibilityReview.md` — 2026-09-07 source+automated review; keyboard nav added; per-area findings/remaining gaps; interactive release matrix outstanding.
- `AiFriendlyProjects.md` — offline AI contract files, ownership, maintenance, verification, migration note.
- `ArchitectureBoundaryTests.md` — ArchUnit rationale, rules, bytecode import, response to violations.
- `FailureInjectionTesting.md` — injected failure coverage table, strengthened safeguards, remaining manual exercises.
- `PerformanceTesting.md` — benchmark usage, recorded 2026-09-07 numbers (workstation/constrained), coverage, changes driven, remaining VDI/interactive work.

**Internal readmes**
- `src/main/resources/studio/metapack/METAPACK.md` — what a pack is/provides, lifecycle, directory structure, manifest/dependency management, component metadata, templates/ownership, creation steps.
- `src/main/resources/studio/metapack/METAPACK_COMPLIANCE.md` — normative manifest/override/verification rules.
- `src/main/java/org/ikasan/studio/core/metapack/README.md` — anatomy of `ComponentTypeMeta`/`ComponentMeta`/`ComponentPropertyMeta`; pack structure and usage via singleton `ComponentLibrary`.
- `src/main/java/org/ikasan/studio/core/io/readme.md` — metadata vs instance data rationale.
- `src/main/java/org/ikasan/studio/ui/Readme.md` — EDT reminder.
- metapack template `Readme.md` (V3.3.9 & V4.1.6) — FreeMarker getter-style preference.
- `src/test/resources/studio/metapack/TestV1|V2/library/Readme.md` — test-only components.
- Empty files: `libs/README.md`, `src/test/java/org/ikasan/studio/core/generator/Readme.md`.

**Ancillary / generated-project**
- `ikasan-studio-ancillary/README.md` — mediator + archetype submodules.
- `archetype-resources/AGENTS.md` — agent instructions shipped into generated projects (model.json is source of truth; don't edit generated/ except model.json; user/ is developer-owned). Keep generated guide (`AiProjectContractGenerator.agentsGuide()`) identical to this file.
- `generated/src/main/Readme.md`, `user/src/main/java/Readme.md`, `user/src/test/java/Readme.md` — archetype placeholder guidance.

**.github / .agents**
- `.github/copilot-instructions.md` — Copilot variant of CLAUDE.md; **same staleness** (IU 2025.3, V3.3.8/V4.0.x).
- `.github/pull_request_template.md` — PR template (summary/approach/verification/screenshots/checklist incl. no secrets).
- `.agents/component-drag-move-handoff.md` — open feature handoff (see §11).

## 13. Document discrepancies to keep in mind

1. `CLAUDE.md` + `.github/copilot-instructions.md` say **IU 2025.3**; `gradle.properties`/`AGENTS.md`/`CONTRIBUTING.md` say **IC 2024.3.7** (authoritative).
2. Older docs (`CLAUDE.md`, copilot, roadmap history, README quickstart) mention metapacks **V3.3.8 / V4.0.x / VHS3.3.x**; actual shipped packs are **V3.3.9 and V4.1.6** (plus test packs TestV1/TestV2).
3. `README.md` "Template ToDo list" and Marketplace badge placeholders remain unfilled (pre-release).
4. Test counts in docs vary by date (646 / 651 / 659 tests) — snapshots of different review dates, not contradictions.

## 14. Quick decision defaults

- New feature → prefer core model + focused tests first; keep UI thin; never leak meta-pack complexity into first-run UX.
- Version-specific work → check the matching pack and, where relevant, the approved local Ikasan source tree; never infer from another major version.
- Regeneration → preserve `user/`; make `generated/` overwrites explicit and previewable; treat generation as transactional (GenerationPlan direction).
- Failure UX → recoverable notification/editor state; structured `StudioDiagnosticEvent` with fixed enum id; no raw values in logs.
- Multi-project → project-scoped `UiContext`; be alert to static caches (`IkasanComponentLibrary` etc.).
