# Ikasan Studio Project Context

This file is durable project context for AI coding agents and human contributors. Read it before making product, UX, architecture, or implementation decisions in this repository.

## Mission

The project owner's mission statement is:

> The purpose of this project is to provide an IDE for Ikasan developers. It is an IntelliJ plugin, so it must conform to the UI standards and interaction standards for IntelliJ plugins. It uses a project template provided by JetBrains for this purpose. Soon we would like to formally release this plugin and make it available on the IntelliJ Marketplace so developers can easily integrate it into their own IntelliJ IDEA IDE. Ikasan is an ESB. I am a developer on the Ikasan ESB project. I am the main developer on this Ikasan Studio IntelliJ IDE plugin project. I would like the interaction to be easy and intuitive, a bit like n8n.

Treat this as the product north star. In practical terms:

- Ikasan Studio is a developer product, not merely a diagram renderer or code generator.
- Follow IntelliJ Platform UI, lifecycle, threading, accessibility, notification, progress, and interaction conventions.
- Optimise the first-run and common workflows for discovery and confidence. Users should not need hidden knowledge about tool-window icons or internal meta-pack concepts to begin.
- Aim for the approachable visual workflow of n8n while retaining normal IntelliJ behaviour, terminology, keyboard access, themes, and platform integration.
- Marketplace readiness, stability, compatibility, documentation, and professional onboarding are product requirements rather than optional polish.

## Product Summary

Ikasan Studio is an IntelliJ IDEA plugin for visually creating and maintaining modules for the Ikasan Enterprise Integration Platform (ESB). A developer composes a module from flows and components on a drag-and-drop canvas. The Studio persists a version-neutral JSON model and generates Java, Maven, and configuration artefacts from version-specific FreeMarker templates.

The intended value is to reduce Ikasan learning time and repetitive implementation effort, enforce good implementation patterns, make integration behaviour visible, and simplify migration between supported Ikasan versions.

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

### Known onboarding problem

After archetype creation, IntelliJ opens `pom.xml`. The user currently has to notice and select the Ikasan Studio icon on the far-right tool-window stripe. This is not sufficiently discoverable or welcoming.

The current code also has no clear initialization presentation:

- `DesignerToolWindowFactory` creates the UI and only then starts model initialization.
- `DesignerUI.initialiseIkasanModel()` waits for IntelliJ smart mode, reads/synchronises the JSON model and PSI state on a pooled thread, and later constructs the palette on the EDT.
- Until this completes, there is no explicit progress, explanation, readiness state, or failure/retry experience.
- Because tool-window content is lazily created, initialization does not start until the user discovers/opens Ikasan Studio.

A preferred future experience is IntelliJ-native progressive onboarding: detect an Ikasan Studio project, start or schedule project-context initialization at the correct lifecycle point, expose honest progress such as Maven import/indexing/model/meta-pack stages, and make the next action obvious. Opening or focusing the Studio automatically may be appropriate for a newly generated Ikasan project, but should be scoped to first use and respect user intent on subsequent opens. Use standard IntelliJ APIs for progress, tool windows, notifications, dumb/smart mode, and persisted project state.

## Architecture

### IntelliJ integration and UI

- `src/main/resources/META-INF/plugin.xml` registers the plugin, dependencies, right-anchored `Ikasan Studio` tool window, application settings, and notification group.
- `ui.intellij.toolWindow.DesignerToolWindowFactory` creates tool-window content and applies its docked/sliding setting.
- `ui.DesignerUI` assembles the canvas, palette, and properties UI and coordinates initialization after indexing.
- `ui.UiContext` is a project-level IntelliJ service holding each project's UI and model context.
- `ui.component.canvas`, `ui.component.palette`, and `ui.component.properties` implement the primary designer interaction.
- `ui.viewmodel` maps the domain model to visual components.
- `ui.model.StudioPsiUtils` centralises interaction with project files and IntelliJ PSI.

### Core model and generation

- `core.model.ikasan.instance` contains module, flow, route, component, property, and exception-resolution instances persisted in the Studio model.
- `core.model.ikasan.meta` describes available component types and properties. `IkasanComponentLibrary` discovers and caches meta-pack content.
- `core.io` serialises and deserialises JSON models and metadata.
- `core.generator` and `core.BuildContext` generate project artefacts with FreeMarker.
- `core.StudioBuildUtils` contains shared build and resource utilities.

### Meta-packs

Meta-packs live under `src/main/resources/studio/metapack/`. Each pack supplies a component library (JSON descriptors and icons) and version-specific FreeMarker templates. Present packs include Ikasan 3.x, 4.x, and a VHS variant.

The meta-pack layer is the compatibility boundary between the version-neutral Studio model and generated code for a particular Ikasan version. Do not leak meta-pack implementation complexity into the first-run UX unless a user must make a meaningful choice.

### Ancillary Maven projects

`ikasan-studio-ancillary/` contains:

- the Maven archetype that creates the initial Studio project; and
- the IDE mediator dependency used by the plugin.

The main plugin itself uses Gradle and the IntelliJ Platform Gradle Plugin.

## Build, Test, and Distribution

- Java toolchain: 17.
- Current Gradle target: IntelliJ IDEA Community 2024.3.7, with compatibility declared from build 242 and no upper bound.
- `./gradlew test` runs automated tests.
- `./gradlew runHarness` runs the separate visual Swing harness.
- `./gradlew buildPlugin` builds the distributable plugin ZIP.
- `./gradlew runIde` launches a sandbox IDE.
- `./gradlew verifyPlugin` performs plugin verification.
- Qodana and GitHub Actions provide static analysis and CI checks.
- Marketplace publication is configured through the IntelliJ Platform Gradle Plugin and environment-provided signing/publishing credentials.

Tests cover the model, JSON IO, meta-pack lookup, code generators, PSI/file behaviour, styling, and a visual UI harness. UX lifecycle changes should add focused tests where IntelliJ test infrastructure permits and be manually exercised with `runIde` from new-project creation through first usable canvas.

## Engineering Priorities and Risks

1. **Intuitive IntelliJ-native UX.** Improve onboarding, loading/empty/error states, discoverability, keyboard access, theme behaviour, and feedback without inventing interaction patterns that conflict with IntelliJ.
2. **Marketplace quality.** Preserve IDE responsiveness, avoid uncaught plugin exceptions, verify supported IDE builds, maintain accurate plugin metadata and documentation, and keep publication repeatable.
3. **EDT and read/write-action correctness.** PSI, project lifecycle work, background loading, and Swing updates must use the appropriate IntelliJ APIs and threads.
4. **Multi-project isolation.** `UiContext` is project-scoped, but `IkasanComponentLibrary` and some build state use shared static caches. Be alert to state leaking between simultaneously open projects or meta-pack selections.
5. **Meta-pack resilience.** Invalid or incomplete packs should produce actionable user-facing diagnostics and must not destabilise the IDE.
6. **Generated/user code ownership.** Never overwrite developer-owned code unexpectedly. Make regeneration scope and consequences clear.
7. **Version compatibility.** Validate generated output against the corresponding Ikasan source and APIs, not assumptions based on another major version.

## Working Conventions

- Prefer IntelliJ Platform components and APIs (`JB*` controls, services, progress APIs, notifications, tool-window APIs, disposal/lifecycle utilities) over custom substitutes.
- Keep expensive Maven, PSI, filesystem, JSON, and meta-pack work off the EDT. Make all Swing mutations on the EDT.
- Catch failures at IntelliJ integration boundaries, log useful context, and provide a recoverable user-facing state. Avoid reporting expected user/configuration problems as fatal IDE errors.
- Use `UiContext` through `project.getService(UiContext.class)` for project-specific state.
- Route PSI and virtual-file operations through `StudioPsiUtils` unless there is a strong reason to introduce a focused abstraction.
- Preserve the separation between framework-independent `core` logic and IntelliJ-dependent `ui` logic.
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

- `README.md` is the user/developer introduction and currently documents installation, archetype creation, module creation, architecture motivations, and manual project creation.
- `CLAUDE.md` contains useful implementation and command guidance but is tool-specific; this `AGENTS.md` is the primary tool-neutral project context.
- `docs/IkasanStudioRoadmap.md` contains a deeper technical/product roadmap, particularly for formalising the meta-pack ecosystem.
- Keep user-facing terminology and screenshots aligned with the actual supported plugin and Ikasan versions as the Marketplace release approaches.
