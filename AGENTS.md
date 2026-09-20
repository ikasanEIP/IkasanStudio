# Ikasan Studio Project Context

This file is durable project context for AI coding agents and human contributors. Read it before making product, UX, architecture, or implementation decisions in this repository.

## Mission

The project owner's mission statement is:

> The purpose of this project is to provide an IDE for Ikasan developers. It is an IntelliJ plugin, so it must conform to the UI standards and interaction standards for IntelliJ plugins. It uses a project template provided by JetBrains for this purpose. Soon we would like to formally release this plugin and make it available on the IntelliJ Marketplace so developers can easily integrate it into their own IntelliJ IDEA IDE. Ikasan is an ESB. I am a developer on the Ikasan ESB project. I am the main developer on this Ikasan Studio IntelliJ IDE plugin project. I would like the interaction to be easy and intuitive, a bit like n8n.

Treat this as the product north star. In practical terms:

- Ikasan Studio is a developer product, not merely a diagram renderer or code generator.
- Follow IntelliJ Platform UI, lifecycle, threading, accessibility, notification, progress, and interaction conventions.
- Optimise the first-run and common workflows for discovery and confidence. Users should not need hidden knowledge about tool-window icons or internal meta-pack concepts to begin.
- Aim for the approachable visual workflow e.g. n8n, while retaining normal IntelliJ behaviour, terminology, keyboard access, themes, and platform integration.
- Marketplace readiness, stability, compatibility, documentation, and professional onboarding are product requirements rather than optional polish.

## Product Summary

Ikasan Studio is an IntelliJ IDEA plugin for visually creating and maintaining modules for the Ikasan Enterprise Integration Platform (ESB). A developer composes a module from flows and components on a drag-and-drop canvas. The Studio persists a version-neutral JSON model and generates Java, Maven, and configuration artefacts from version-specific FreeMarker templates.

The intended value is to reduce Ikasan learning time and repetitive implementation effort, enforce good implementation patterns, make integration behaviour visible, and simplify migration between supported Ikasan versions.

### Blue Console and Dashboard

The Ikasan Blue Console and the Ikasan Dashboard are distinct products and must not be described or labelled as interchangeable:

- The **Blue Console** is shipped with every Ikasan module and operates independently of the Dashboard. It is a developer-facing, module-local console used to start and stop flows, identify flows in stopped or error states, examine the module REST API, and perform administrative tasks.
- The **Ikasan Dashboard** is a central control point for multiple modules. It provides an aggregate view of running modules and their error states, supports replaying rejected messages, and lets operators interact with remote modules through their REST interfaces.

In space-constrained Studio navigation, the agreed button label is **Console**. Its tooltip and guidance must identify it as the module-local **Blue Console** and must never describe it as the **Ikasan Dashboard**.

## User Journey

The principal new-user journey is:

1. Install Ikasan Studio into IntelliJ IDEA.
2. Create a Maven project from the Ikasan Studio archetype.
3. Allow IntelliJ to import, index, and resolve the generated Maven project.
4. Enter Ikasan Studio and select an appropriate meta-pack/Ikasan version.
5. Configure the module.
6. Drag flows and components onto the canvas and edit their properties.
7. Generate, inspect, run, test, and debug the resulting Ikasan application within IntelliJ.

The generated project separates Studio-owned output in `generated/` from developer-owned implementation in `user/`.

### Editor-based onboarding

Ikasan Studio is hosted in IntelliJ's main editor area, alongside Java and configuration files. A newly generated Ikasan project opens the Studio editor automatically on first use. On later project launches, the editor is restored only when the developer left it open; deliberately closing the tab is respected.

The Ikasan squid icon remains on the far-right tool-window stripe as a discoverable one-click launcher. It opens or focuses the single project-scoped Studio editor and immediately hides the empty launcher tool window. **Tools → Open Ikasan Studio** and IntelliJ Find Action provide fallback access.

`IkasanStudioFileEditor` is the exclusive owner of `DesignerUI`. Closing the tab disposes editor-owned canvas, palette, properties, and view-handler references while preserving the project model for a clean reopen. Project initialization starts through `StudioProjectInitialisationService` and presents Maven import, indexing, model, meta-pack, and recoverable failure states inside the editor.

## Implemented UX Baseline

The following behaviour is implemented; preserve it unless a later product decision explicitly replaces it. Verification results are candidate-specific: consult the dated release evidence linked below rather than treating this document as a current test report.

- The empty-canvas onboarding progresses through adding a flow, adding a consumer and other components, running a valid module, waiting for startup, and opening Console. Detailed hints can be disabled in **Settings → Tools → Ikasan Studio**. Flow-level hints are positioned below the empty-flow artwork so they do not overlap it.
- The top controls use **Run module** and **Console**. Console guidance tells developers to wait until module startup completes, explains its Blue Console purpose, and notes the local `admin` / `admin` login.
- **Run module** creates or reuses a standard IntelliJ Java Application run configuration for `generated/src/main/java/org/ikasan/studio/boot/Application.java`, selects it in IntelliJ, and launches it through the normal execution APIs. Developers can subsequently use IntelliJ Run or Debug controls. Workspace module discovery uses a non-blocking read action and never performs slow file-index work on the EDT.
- Newly configured modules materialise `flowStartupType=AUTOMATIC`. The V3.3.9 and V4.1.6 property templates generate `ikasan.module.activator.startup.type.defaultStartupType=AUTOMATIC` in `application.properties`. Existing configured modules retain their chosen value.
- `IkasanStudioFileEditorProvider` hosts the designer in the main editor region. The right-stripe squid icon is a direct one-click launcher, Tools and Find Action remain fallbacks, open-state restoration respects deliberate tab closure, and the editor tab uses the same squid icon as the stripe launcher.
- `DesignerUI` has a single owner per project. Closing the editor clears UI-only `UiContext` references while retaining the module and project model for a clean reopen.
- CodeQL Java/Kotlin analysis uses manual Gradle compilation under JDK 17 so Lombok-generated code is traced. GitHub workflows use current Node 24-compatible major action versions.
- Focused tests cover hint progression, module defaulting, run-configuration matching, editor state/icon behaviour, and UI-context disposal. The [14 September release audit](docs/ReleaseAudit-2026-09-14.md) records automated results for that candidate, including verification against IDEA 2024.2, 2024.3.7 and 2026.2.2, and lists outstanding manual release checks. Re-run the [release-candidate gates](docs/ReleaseCandidateVerification.md) for a new candidate.

## Architecture

Packages below are relative to `org.ikasan.studio`. IntelliJ/UI sources live under `src/main/java/`; framework-independent production core sources live under `headless/studio-generator/src/main/java/`.

### IntelliJ integration and UI

- `src/main/resources/META-INF/plugin.xml` registers the plugin, dependencies, editor provider, right-stripe launcher, application settings, actions, and notification group.
- `intellij.editor` owns the project-scoped virtual file, main-editor integration, restoration state, and the single `DesignerUI` instance.
- `intellij.toolwindow.DesignerToolWindowFactory` retains the familiar right-stripe icon as a one-click launcher for the editor tab; it must never construct a second designer.
- `intellij.project` owns project initialization and generated-project synchronization.
- `ui.DesignerUI` assembles the canvas, palette, and properties UI and coordinates initialization after indexing.
- `ui.UiContext` is a project-level IntelliJ service holding each project's UI and model context.
- `ui.component.canvas`, `ui.component.palette`, and `ui.component.properties` implement the primary designer interaction.
- `ui.viewmodel` maps the domain model to visual components.
- `intellij.psi.StudioPsiUtils` centralises interaction with project files and IntelliJ PSI.

### Core model and generation

- `core.model.ikasan.instance` contains module, flow, route, component, property, and exception-resolution instances persisted in the Studio model.
- `core.metapack.model` describes available component types and properties. `core.metapack.ComponentLibrary` exposes immutable packaged-library snapshots indexed by pack ID; `core.metapack.loading` and `core.metapack.validation` handle discovery and validation.
- `core.io` and `core.persistence.json` handle JSON IO, model serialisation and persistence helpers.
- `core.generator` and `core.BuildContext` generate project artefacts with FreeMarker. `BuildContext.getFreemarkerConfig(version)` creates a configuration for the requested pack.
- `core.StudioBuildUtils` contains shared build and resource utilities.

The root [settings.gradle.kts](settings.gradle.kts) includes `headless/` as a composite build. The plugin consumes `studio-generator` and `studio-bundled-packs`; `studio-test-kit` supplies reusable pack contracts. See [headless/README.md](headless/README.md) for module ownership and standalone commands, and [architecture boundary tests](docs/ArchitectureBoundaryTests.md) for enforced dependency rules.

### AI integration

- `intellij.ai` owns the project-scoped live-model bridge, proposal validation/application integration, and connection UI. Framework-independent proposal operations live in `core.ai`.
- `native-mcp/` is an optional Kotlin integration with IntelliJ’s bundled MCP Server. Its descriptor is [ikasanstudio.native-mcp.xml](native-mcp/src/main/resources/ikasanstudio.native-mcp.xml); the main plugin descriptor declares the optional content module.
- `src/mcpAdapter/java/` supplies the bundled Java adapter for manual client setup.
- [Studio AI bridge](docs/StudioAiBridge.md) documents connection routes, proposal operations, file-based proposals, approval settings and developer-code protection. [AI-friendly projects](docs/AiFriendlyProjects.md) documents the generated schema, catalogue and ownership guide. Keep the archetype discovery instructions aligned with `AiProjectContractGenerator.agentsGuide()`.

### Meta-packs

Meta-pack sources live under `src/main/resources/studio/metapack/`. The bundled packs are `V3.3.9` and `V4.1.6`; each supplies `metapack.json`, a component library (JSON descriptors and icons), and version-specific FreeMarker templates. Shared schemas live under `schema/`. Historic VHS and older-version references do not identify currently bundled packs.

`headless/studio-pack-v3` and `headless/studio-pack-v4` package the respective resource directories; `studio-bundled-packs` selects their exact revisions. The root plugin excludes these resources from its own resource source set to avoid duplicate packaging. Follow the [meta-pack authoring guide](src/main/resources/studio/metapack/METAPACK.md), [compliance requirements](src/main/resources/studio/metapack/METAPACK_COMPLIANCE.md), and [independent pack release guide](docs/IndependentMetaPackArtifacts.md) when changing or adding packs.

The meta-pack layer is the compatibility boundary between the version-neutral Studio model and generated code for a particular Ikasan version. Do not leak meta-pack implementation complexity into the first-run UX unless a user must make a meaningful choice.

### Ancillary Maven projects

`ikasan-studio-ancillary/` contains:

- the Maven archetype that creates the initial Studio project; and
- the IDE mediator dependency used by the plugin.

The main plugin itself uses Gradle and the IntelliJ Platform Gradle Plugin.

## Build, Test, and Distribution

- Java compilation/headless toolchain: 17. The optional `native-mcp` module uses a Java 21 toolchain and the IDEA 2026.2.2 SDK, while emitting JVM 17 bytecode; see [native-mcp/build.gradle.kts](native-mcp/build.gradle.kts). Run sandbox IDEs with their supplied JetBrains Runtime.
- Main plugin compilation/test target: IntelliJ IDEA Community 2024.3.7, with compatibility declared from build 242 and no upper bound. [gradle.properties](gradle.properties) and [build.gradle.kts](build.gradle.kts) define the target, sandbox and verifier versions. An absent upper bound is not evidence of future compatibility; see [supported versions](docs/SupportedVersions.md).
- `./gradlew test` runs the root plugin test suite.
- `./gradlew -p headless test` runs the standalone engine, test-kit and pack suites without configuring the IntelliJ build.
- `./gradlew check` includes headless checks and meta-pack validation; BOM/help-URL checks require network access. See [engine and meta-pack testing](docs/TestingEngineAndMetaPacks.md) for focused suite selection.
- `./gradlew runHarness` runs the separate visual Swing harness.
- `./gradlew buildPlugin` builds the distributable plugin ZIP; `./gradlew verifyReleaseArchive` audits its contents. If another sandbox is running, use an isolated directory, for example `-PstudioSandboxDirectory=build/verification-sandbox`.
- `./gradlew runIdeModern` launches the newer IntelliJ IDEA sandbox (version selected by `sandboxIdeVersion`); the shared **Run Plugin** configuration uses this task.
- `./gradlew runIde` launches the Community 2024.3.7 regression sandbox; the shared **Run Plugin (2024.3.7 Regression)** configuration uses this task. Compilation and automated tests retain the 2024.3.7 target.
- `./gradlew verifyPlugin` checks the configured oldest, compilation-target and newest IDE boundaries. It does not replace interactive workflow testing.
- Qodana and GitHub Actions provide static analysis and CI checks.
- Marketplace publication is configured through the IntelliJ Platform Gradle Plugin and environment-provided signing/publishing credentials.

Tests cover the model, JSON IO, meta-pack lookup, code generators, PSI/file behaviour, styling, and a visual UI harness. UX lifecycle changes should add focused tests where IntelliJ test infrastructure permits and be manually exercised with `runIde` from new-project creation through first usable canvas.

## Engineering Priorities and Risks

1. **Intuitive IntelliJ-native UX.** Improve onboarding, loading/empty/error states, discoverability, keyboard access, theme behaviour, and feedback without inventing interaction patterns that conflict with IntelliJ.
2. **Marketplace quality.** Preserve IDE responsiveness, avoid uncaught plugin exceptions, verify supported IDE builds, maintain accurate plugin metadata and documentation, and keep publication repeatable.
3. **EDT and read/write-action correctness.** PSI, project lifecycle work, background loading, and Swing updates must use the appropriate IntelliJ APIs and threads.
4. **Multi-project isolation.** `UiContext` is project-scoped. `ComponentLibrary` shares immutable snapshots keyed by pack ID, and `BuildContext` creates per-request FreeMarker configurations. Preserve those boundaries; do not introduce a global active-pack selection or mutable static project state.
5. **Meta-pack resilience.** Invalid or incomplete packs should produce actionable user-facing diagnostics and must not destabilise the IDE.
6. **Generated/user code ownership.** Never overwrite developer-owned code unexpectedly. Make regeneration scope and consequences clear.
7. **Version compatibility.** Validate generated output against the corresponding Ikasan source and APIs, not assumptions based on another major version.

## Working Conventions

- Prefer IntelliJ Platform components and APIs (`JB*` controls, services, progress APIs, notifications, tool-window APIs, disposal/lifecycle utilities) over custom substitutes.
- Keep expensive Maven, PSI, filesystem, JSON, and meta-pack work off the EDT. Make all Swing mutations on the EDT.
- Catch failures at IntelliJ integration boundaries, log useful context, and provide a recoverable user-facing state. Avoid reporting expected user/configuration problems as fatal IDE errors.
- Use `UiContext` through `project.getService(UiContext.class)` for project-specific state.
- Route PSI and virtual-file operations through `StudioPsiUtils` unless there is a strong reason to introduce a focused abstraction.
- Preserve the separation between framework-independent `core` logic and IntelliJ-dependent `intellij`/`ui` logic.
- Treat the JSON model as the version-neutral source of truth and meta-packs as version-specific adapters/templates.
- Review both light and dark themes and normal IntelliJ scaling when changing visuals.
- Do not modify unrelated working-tree changes. This repository may contain work in progress.

## Approved Ikasan Reference Source Trees

The project owner has explicitly granted full read access to these local repositories for understanding APIs, validating generated code, and comparing version behaviour:

- `/home/hidavi/dev/ws/ik3/ikasan3` — Ikasan version 3
- `/home/hidavi/dev/ws/ik4/ikasan4` — Ikasan version 4
- `/home/hidavi/dev/ws/ik5/ikasan5` — Ikasan version 5

All three paths were present when this document was written. Treat them as reference repositories. Read freely when work concerns the corresponding Ikasan API or generated output. Do not edit them unless the user separately requests changes there and the execution environment permits those writes.

## Documentation Notes

Use this file as the primary tool-neutral project context. For exact package paths, versions and task behaviour, check the current source and build configuration. Dated audits and handovers record evidence for their stated checkout; roadmap proposals are not proof of implemented behaviour.

- [README.md](README.md) introduces the product and links to user workflows; [CONTRIBUTING.md](CONTRIBUTING.md) covers development setup, engineering expectations and pull requests. [CLAUDE.md](CLAUDE.md) provides supplementary tool-specific implementation and command guidance.
- [Getting started](docs/GettingStarted.md), [supported versions](docs/SupportedVersions.md), [troubleshooting](docs/Troubleshooting.md) and [known limitations](docs/KnownLimitations.md) describe the supported user journey and its constraints.
- [Project files and recovery](docs/ProjectFilesAndRecovery.md), [Ikasan version migration](docs/IkasanVersionMigration.md) and [flow copy/paste](docs/FlowCopyPaste.md) explain model ownership, recovery, migration and cross-project reuse.
- [Type guidance](docs/TypeGuidance.md), [converter recipes](docs/ConversionRecipes.md), [JMS object messages](docs/JmsObjectMessages.md) and [generated-code warnings](docs/GeneratedCodeWarnings.md) document component configuration and generated-code contracts.
- [Harnesses](docs/Harnesses.md) covers the user-facing email, FTP/SFTP and JMS test tools. [Failure-injection testing](docs/FailureInjectionTesting.md), [performance testing](docs/PerformanceTesting.md) and [accessibility review](docs/AccessibilityReview.md) record focused checks and remaining interactive exercises.
- [Release-candidate verification](docs/ReleaseCandidateVerification.md) and the [Marketplace manual checklist](docs/MarketplaceReleaseManualChecklist.md) define release checks. [Diagnostics and privacy](docs/DiagnosticsAndPrivacy.md) describes logging and diagnostic collection; [SECURITY.md](SECURITY.md) explains vulnerability reporting; [CHANGELOG.md](CHANGELOG.md) records release changes.
- [Ikasan Studio roadmap](docs/IkasanStudioRoadmap.md) contains product/technical plans and a July 2026 status snapshot. Check the implementation and current feature documentation before treating its unfinished items as present-day gaps. [AI support overview](docs/AiSupportOverview.md) explains the current architecture and developer journey; [Studio AI bridge](docs/StudioAiBridge.md) defines the detailed feature contract.
- [Ancillary README](ikasan-studio-ancillary/README.md) describes the separately versioned Maven archetype and IDE mediator. The [archetype AGENTS.md](ikasan-studio-ancillary/ikasan-studio-project-archetype/src/main/resources/archetype-resources/AGENTS.md) is shipped into generated applications; its instructions govern those applications, while this root file governs Studio development, including archetype templates. Keep both filenames so agents can discover the instructions in each resulting project.

Keep user-facing terminology and screenshots aligned with the supported plugin and Ikasan versions. Whenever `src/main/resources/messages/studioBundle.properties` changes, update `src/main/resources/messages/studioBundle_ja.properties` in the same change, preserving keys, format placeholders, HTML markup and user-facing meaning.
