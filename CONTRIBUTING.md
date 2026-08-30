# Contributing to Ikasan Studio

Thank you for helping improve Ikasan Studio. Contributions may include bug reports, documentation, tests, component metadata, user-interface improvements, and code changes.

Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before participating. Security vulnerabilities should be reported using [SECURITY.md](SECURITY.md), not through a public issue.

## Before you start

- Search existing issues and pull requests to avoid duplicating work.
- For a substantial feature or architectural change, open a feature request before investing significant effort.
- Keep changes focused. Do not combine unrelated cleanup with a bug fix or feature.
- Preserve developer-owned code under generated projects' `user/` source tree. Studio-owned generated output belongs under `generated/`.

## Development setup

The plugin uses:

- JDK 17
- The Gradle wrapper included in this repository
- IntelliJ IDEA Community 2024.3.7 as the current compile/test target
- IntelliJ Platform APIs and interaction conventions

Clone the repository and run the automated tests:

```bash
git clone https://github.com/ikasanEIP/IkasanStudio.git
cd IkasanStudio
./gradlew test
```

Useful commands:

```bash
./gradlew test          # Automated tests
./gradlew buildPlugin   # Build the distributable plugin ZIP
./gradlew verifyPlugin  # Run IntelliJ Plugin Verifier
./gradlew runIde        # Launch a sandbox IDE with the plugin installed
./gradlew runHarness    # Run visual Swing harnesses for manual inspection
```

The first Gradle run downloads IntelliJ Platform and project dependencies and can take several minutes.

## Project structure

- `src/main/java/org/ikasan/studio/core/` contains framework-independent model, JSON, metadata, and generation logic.
- `src/main/java/org/ikasan/studio/ui/` contains IntelliJ integration and Swing UI code.
- `src/main/resources/studio/metapack/` contains version-specific component metadata, icons, and FreeMarker templates.
- `ikasan-studio-ancillary/` contains the Maven archetype and IDE mediator projects.
- `src/test/java/` contains automated tests.
- `src/testHarness/java/` contains visual harnesses that require human inspection.

Read [AGENTS.md](AGENTS.md) for the product mission, architecture, terminology, and engineering constraints that apply to all contributions.

## Engineering expectations

- Use IntelliJ Platform services and UI components where appropriate.
- Keep filesystem, PSI, Maven, JSON, and meta-pack work off the Event Dispatch Thread. Perform Swing mutations on the EDT.
- Keep project state isolated between simultaneously open IntelliJ projects.
- Catch failures at IntelliJ integration boundaries, log useful context, and present recoverable user-facing states.
- Keep version-neutral behavior in the core model and version-specific behavior in meta-packs.
- Do not unexpectedly overwrite developer-owned files.
- Test UI changes in light and dark themes and at normal IntelliJ scaling.
- Use **Console** for the module-local Ikasan Blue Console. Do not describe it as the Ikasan Dashboard.

## Tests

Add focused regression tests for behavior changes. Run at least:

```bash
./gradlew test
```

For changes to IntelliJ integration, plugin metadata, or compatibility, also run:

```bash
./gradlew verifyPlugin
```

For canvas, onboarding, drag-and-drop, lifecycle, or theme changes, launch `./gradlew runIde` and manually exercise the affected workflow. Describe manual checks in the pull request.

Changes to generated output or a meta-pack should be validated against the corresponding Ikasan version rather than inferred from another version.

## Pull requests

- Create a branch from the current `main` branch.
- Write a concise title explaining the user-visible outcome.
- Explain the problem, the chosen solution, risks, and verification performed.
- Include screenshots or a short recording for visible UI changes.
- Update documentation and `CHANGELOG.md` when appropriate.
- Ensure CI checks pass and address review feedback.
- Keep commits understandable; maintainers may squash them when merging.

By submitting a contribution, you agree that it is licensed under the repository's existing [Apache License 2.0](LICENSE.txt).

