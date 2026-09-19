# Contributing to Ikasan Studio

Thank you for helping improve Ikasan Studio. Contributions may include bug reports, documentation, tests, component metadata, user-interface improvements, and code changes.

Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before participating. Security vulnerabilities should be reported using [SECURITY.md](SECURITY.md), not through a public issue.

## Before you start

- Search existing issues and pull requests to avoid duplicating work.
- For a substantial feature or architectural change, open a feature request before investing significant effort.
- Keep changes focused. Do not combine unrelated cleanup with a bug fix or feature.
- Preserve developer-owned code under generated projects' `user/` source tree. Studio-owned generated output belongs under `generated/`.

Use the repository's [bug report](.github/ISSUE_TEMPLATE/bug_report.yml) or [feature request](.github/ISSUE_TEMPLATE/feature_request.yml) template. For bugs, include the plugin version or commit, full IntelliJ build, operating system, selected Ikasan/meta-pack version, reproduction steps, and expected versus actual behaviour. Provide a minimal model or project where possible. Follow [Diagnostics and privacy](docs/DiagnosticsAndPrivacy.md) when collecting logs, and remove credentials and customer data from attachments.

## Development setup

The plugin uses:

- JDK 17 for the main plugin and headless engine; the optional native MCP module uses a JDK 21 toolchain and emits JVM 17 bytecode
- The Gradle wrapper included in this repository
- IntelliJ IDEA Community 2024.3.7 as the current compile/test target
- IntelliJ Platform APIs and interaction conventions

The native MCP module compiles against IDEA 2026.2.2 and its bundled MCP Server plugin. Gradle's configured toolchain resolver can provision the required JDKs. Run sandbox IDEs with their supplied JetBrains Runtime; generated Ikasan applications have their own JDK requirements. See [Supported versions](docs/SupportedVersions.md), [gradle.properties](gradle.properties), and [native-mcp/build.gradle.kts](native-mcp/build.gradle.kts) for the current configuration.

Clone the repository and run the automated tests:

```bash
git clone https://github.com/ikasanEIP/IkasanStudio.git
cd IkasanStudio
./gradlew test
```

Useful commands:

```bash
./gradlew test                   # Root plugin tests
./gradlew -p headless test       # Standalone engine, test-kit and pack tests
./gradlew check                  # Root/headless checks and meta-pack validation
./gradlew buildPlugin            # Build the distributable ZIP in build/distributions/
./gradlew verifyReleaseArchive   # Audit the ZIP and its packaged resources
./gradlew verifyPlugin           # Check configured IDE compatibility boundaries
./gradlew runIdeModern           # Launch the newer sandbox used by Run Plugin
./gradlew runIde                 # Launch the 2024.3.7 regression sandbox
./gradlew runHarness             # Run enabled tests in the separate Swing harness suite
```

The first Gradle run downloads IntelliJ Platform, runtime/toolchain and project dependencies and can take several minutes. `check` also validates remote BOMs and help URLs; `verifyReleaseArchive` requires Python 3. On Windows, use `gradlew.bat` in place of `./gradlew`.

Import the root Gradle project into IntelliJ and reload all Gradle projects after build-structure changes. The root build includes the separate `headless` build; there is no need to publish its artifacts locally first. For engine-only contributions, `./gradlew -p headless test` avoids configuring or downloading IntelliJ. See [headless/README.md](headless/README.md) for module-specific commands.

To package while a sandbox IDE is running, use a separate sandbox directory, for example `./gradlew buildPlugin -PstudioSandboxDirectory=build/verification-sandbox`. Interactive Swing harness tests are separate from the standard test suite; see `src/testHarness/java/` for individual manual tests.

## Project structure

- `headless/studio-generator/src/main/java/org/ikasan/studio/core/` contains framework-independent model, JSON, metadata, and generation logic.
- `src/main/java/org/ikasan/studio/intellij/` contains IntelliJ lifecycle, editor, PSI, execution, settings and AI adapters; `src/main/java/org/ikasan/studio/ui/` contains Swing UI and view models.
- `src/main/resources/studio/metapack/` contains version-specific component metadata, icons, and FreeMarker templates.
- `headless/studio-pack-v3/` and `headless/studio-pack-v4/` package the official V3.3.9 and V4.1.6 resources; `studio-bundled-packs` selects their revisions and `studio-test-kit` provides reusable contracts.
- `native-mcp/` contains the optional IntelliJ MCP integration; `src/mcpAdapter/java/` contains the bundled Java adapter for manual client setup.
- `ikasan-studio-ancillary/` contains the Maven archetype and IDE mediator projects.
- `src/test/java/` contains automated tests.
- `src/testHarness/java/` contains visual harnesses that require human inspection.

Read [AGENTS.md](AGENTS.md) for the product mission, architecture, terminology, and engineering constraints that apply to all contributions.

## Engineering expectations

- Use IntelliJ Platform services and UI components where appropriate.
- Keep filesystem, PSI, Maven, JSON, and meta-pack work off the Event Dispatch Thread. Perform Swing mutations on the EDT.
- Keep project state isolated between simultaneously open IntelliJ projects.
- Catch failures at IntelliJ integration boundaries, log useful context, and present recoverable user-facing states.
- Preserve IntelliJ cancellation/control-flow exceptions and respect read/write-action and disposal requirements.
- Keep version-neutral behavior in the core model and version-specific behavior in meta-packs.
- Do not unexpectedly overwrite developer-owned files.
- Test UI changes in light and dark themes and at normal IntelliJ scaling.
- Use **Console** for the module-local Ikasan Blue Console. Do not describe it as the Ikasan Dashboard.
- Update both `studioBundle.properties` and `studioBundle_ja.properties` under `src/main/resources/messages/` when changing user-facing text. Keep keys, placeholders, HTML and meaning aligned.
- Preserve saved-model compatibility and existing project instructions. Changes to persistence, migration or generation must account for existing projects, backups, and developer-owned files; see [Project files and recovery](docs/ProjectFilesAndRecovery.md).

Follow the [architecture boundaries](docs/ArchitectureBoundaryTests.md), [meta-pack authoring guide](src/main/resources/studio/metapack/METAPACK.md), and [pack compliance requirements](src/main/resources/studio/metapack/METAPACK_COMPLIANCE.md) for the area being changed. AI integration and generated project guidance are documented in [Studio AI bridge](docs/StudioAiBridge.md) and [AI-friendly projects](docs/AiFriendlyProjects.md).

## Tests

Add focused regression tests for behavior changes. Run at least:

```bash
./gradlew test
```

For engine, generation, or meta-pack changes, also run `./gradlew -p headless test`. Use the [engine and pack testing guide](docs/TestingEngineAndMetaPacks.md) for focused tests and version-specific expected outputs. `./gradlew check` runs the broader checks used by CI, including meta-pack validation.

For changes to IntelliJ integration, plugin metadata, dependencies, packaging, or compatibility, also run:

```bash
./gradlew buildPlugin verifyReleaseArchive verifyPlugin
```

For canvas, onboarding, drag-and-drop, lifecycle, or theme changes, use `./gradlew runIdeModern` and the `./gradlew runIde` regression sandbox to exercise the affected workflow. Check keyboard access, light/dark themes, editor close/reopen, and project disposal where relevant. Record IDE builds, steps and results in the pull request; identify anything not exercised.

Changes to generated output or a meta-pack should be validated against the corresponding Ikasan version rather than inferred from another version.

## Pull requests

- Create a branch from the current `main` branch.
- Use the [pull request template](.github/pull_request_template.md) and link the relevant issue.
- Write a concise title explaining the user-visible outcome.
- Explain the problem, the chosen solution, risks, and verification performed.
- Include screenshots or a short recording for visible UI changes.
- Update documentation and `CHANGELOG.md` when appropriate.
- Ensure CI checks pass and address review feedback.
- Keep commits understandable; maintainers may squash them when merging.

Documentation-only changes should have their links and commands checked; describe that validation instead of claiming unrun application tests. For new dependencies or bundled assets, explain why they are needed and identify their licence and any required notices.

## Preparing a Marketplace release

Release preparation is a maintainer workflow. Ordinary contributions do not require signing credentials or a Marketplace token.

- Follow [Release-candidate verification](docs/ReleaseCandidateVerification.md) and the [Marketplace manual checklist](docs/MarketplaceReleaseManualChecklist.md). Keep the candidate ZIP/hash, test and archive reports, verifier results, and manual evidence together. Historical audits apply only to their recorded candidate.
- Verify installation, upgrade from a distinct previous candidate/release, and uninstall through the Plugins UI on the supported IDE boundaries. Exercise project creation, generation, Run/Debug, Blue Console, both bundled packs, and the relevant lifecycle and harness checks. A passing build or binary verifier does not establish these results.
- Review the release version, changelog, supported-version documentation, installation instructions, screenshots, known limitations, and dependency notices. Keep the Marketplace listing consistent with the tested candidate; add the public listing link to the README once it is published and verified.
- The [Build workflow](.github/workflows/build.yml) runs packaging/archive checks, tests, Qodana and Plugin Verifier, then prepares a draft release after successful non-PR checks. The [Release workflow](.github/workflows/release.yml) publishes to Marketplace when a GitHub release is **released or prereleased**, using repository signing/publishing secrets. Publishing a prerelease is therefore a publication action, not a validation step.

The release workflow builds from the release tag; it does not simply promote the previously audited ZIP. Record which commit and artifact were tested and reconcile the published artifact with that evidence. Keep plugin Marketplace publication separate from Maven publication of the ancillary projects and [independently versioned pack artifacts](docs/IndependentMetaPackArtifacts.md).

By submitting a contribution, you agree that it is licensed under the repository's existing [Apache License 2.0](LICENSE.txt).
