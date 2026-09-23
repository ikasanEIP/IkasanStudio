![Ikasan](docs/images/Ikasan-title-transparent.png)

# Ikasan Studio

<!-- Plugin description -->
Ikasan Studio is an IntelliJ IDEA plugin for designing and maintaining applications built with the Ikasan Enterprise Integration Platform (ESB).

Compose modules from flows and components on a visual canvas, configure their properties, and generate Java, Maven and application configuration from a version-neutral JSON model. Inspect, run and debug the resulting application using IntelliJ's normal development tools.

Studio separates generated framework code from developer-owned implementations and uses version-specific meta-packs to support different Ikasan releases.
<!-- Plugin description end -->

**Release status:** preparing for the first JetBrains Marketplace release. Marketplace installation is not yet available. Use a candidate plugin ZIP supplied for testing, or build one from this repository. A verified Marketplace listing link will be added here after publication.

[Get started](#get-started) · [Documentation](#documentation) · [Build and contribute](#build-and-contribute) · [Report a problem](#report-a-problem)

## What you can do

- Design flows with a component palette, property editor and visual connections in IntelliJ's main editor area.
- Generate application code and configuration while keeping business logic in developer-owned source files.
- Run and debug modules through IntelliJ, inspect their module-local Blue Console, and exercise supported endpoints with development harnesses.
- Use type guidance and converter recipes when connecting components with different payload types.
- Copy flows between projects and preview migrations between supported Ikasan versions.
- Connect an AI client to the live design or import structured proposals, with validation, undo and developer-code protection.

Studio helps with integration structure and repetitive implementation work. Application-specific stubs still need business logic and tests; generated code and suggested conversions do not establish that an integration works against its external systems.

## Get started

### Requirements

| Layer | Current repository configuration |
| --- | --- |
| IntelliJ IDEA | Minimum 2024.2, platform build 242 |
| Plugin compilation/test target | IDEA Community 2024.3.7 |
| Configured binary verification | IDEA 2024.2, 2024.3.7 and 2026.2.2 |
| Bundled Ikasan meta-packs | V3.3.9 and V4.1.6 |
| Generated V3.3.9 application | Java 11 |
| Generated V4.1.6 application | Java 17 |

Run IntelliJ with its supplied runtime and configure the application's project SDK, Maven runner/importer and Run/Debug JRE for its Ikasan version. These are separate from the toolchains used to develop the plugin.

See [Supported versions](docs/SupportedVersions.md) and [Known limitations](docs/KnownLimitations.md) before choosing a candidate. The plugin has no upper IDE build limit, but that does not mean every later IDE has been tested. Verification evidence is specific to the candidate described in the [release audit](docs/ReleaseAudit-2026-09-14.md).

### Install and create a module

1. Obtain the candidate ZIP or [build the plugin](#build-and-contribute). In IntelliJ, choose **Settings → Plugins → gear → Install Plugin from Disk**, select the ZIP and restart if prompted.
2. Create a **Maven Archetype** project using `org.ikasan.studio:ikasan-studio-project-archetype` and the version supplied for your candidate. If it is unavailable in your configured repositories, use the [manual archetype fallback](#manual-archetype-fallback).
3. Allow Maven import and indexing to finish. Studio opens in an editor tab; select the appropriate Ikasan pack and configure the module.
4. Add a flow, a consumer and the remaining components. Configure their properties and select **Update Code**.
5. Select **Run module**, wait for application startup, then open **Console**. Use IntelliJ Run/Debug controls for subsequent development.

Follow [Your first module in five minutes](docs/GettingStarted.md) for a Scheduled Consumer → Logging Producer example requiring no external broker, FTP server or email account.

Reopen Studio with the squid icon on the far-right stripe, **Tools → Ikasan Studio → Open Ikasan Studio**, or Find Action. Closing its editor tab is respected on later project launches.

**Console** opens the module-local **Blue Console**, which provides flow control and module administration. It is distinct from the central **Ikasan Dashboard**, which manages multiple modules. The walkthrough's local example uses `admin` / `admin`.

### Your model and code

| Location in an Ikasan application | Purpose |
| --- | --- |
| `generated/src/main/model/model.json` | The source-of-truth visual model; commit it to version control |
| Other generated Java/configuration under `generated/` | Studio-owned output that regeneration can replace |
| `user/` | Developer-owned implementations and resources; generated stubs need application-specific implementation |
| `generated/IKASAN_STUDIO.md` | Generated model-editing and code-ownership guidance |
| Root `AGENTS.md` | Discovery instructions for AI tools, created only when missing |

Commit the model and your implementations together. Do not delete the whole `generated/` directory as a cleaning step: it contains the model. Read [Project files and recovery](docs/ProjectFilesAndRecovery.md) for backups, flow renaming and recovery, and [AI-friendly projects](docs/AiFriendlyProjects.md) for the generated schema and component catalogue.

### AI-assisted development

Start with the [AI support overview](docs/AiSupportOverview.md) for architecture, connection routes and how project guidance helps the assistant.

Choose **Tools → Ikasan Studio → Connect AI to Ikasan Studio…** for supported IntelliJ MCP setup or a manual client configuration using the bundled Java adapter. An AI client can read the live model and catalogue and submit supported changes. File-based proposals are also available.

Studio validates proposals and applies them according to its approval settings. **Always ask for approval** is off by default; **Confirm deletes** is on by default. Potential replacement of developer-owned code requires review. Consult [Studio AI bridge](docs/StudioAiBridge.md) for connection steps, supported operations, settings, status checks and undo behaviour.

## Documentation

### Using Studio

| Guide | Read it for |
| --- | --- |
| [Getting started](docs/GettingStarted.md) | Create, configure, run and debug your first module |
| [Supported versions](docs/SupportedVersions.md) | IDE compatibility, bundled packs and application JDKs |
| [Project files and recovery](docs/ProjectFilesAndRecovery.md) | Ownership, model backups, flow renaming and restoration |
| [Type guidance](docs/TypeGuidance.md) | Payload type warnings and converter suggestions |
| [Converter recipes](docs/ConversionRecipes.md) | Reusable payload extraction and construction |
| [Harnesses](docs/Harnesses.md) | Local mail/FTP testing, JMS readers, message injection and real scans |
| [Generate Flow Test](docs/IkasanFlowTesting.md) | Developer-owned Ikasan flow-test scaffolds, completion steps and migration |
| [JMS object messages](docs/JmsObjectMessages.md) | ActiveMQ trusted packages and Java-object payloads |
| [Flow copy/paste](docs/FlowCopyPaste.md) | Reuse flows and update shared references |
| [Command-line migration and verification](docs/CommandLineMigration.md) | Verify your own project before/after upgrades; preview and apply migrations without IntelliJ |
| [Ikasan version migration](docs/IkasanVersionMigration.md) | Preview, apply and recover migrations between supported packs |
| [AI support overview](docs/AiSupportOverview.md) | Architecture, onboarding and AI project context |
| [Studio AI bridge](docs/StudioAiBridge.md) | Live MCP access and structured model proposals |
| [AI-friendly projects](docs/AiFriendlyProjects.md) | Generated instructions, schemas and component catalogues |
| [Generated-code warnings](docs/GeneratedCodeWarnings.md) | Template checks and application-specific warning limits |
| [Troubleshooting](docs/Troubleshooting.md) | Maven, indexing, ports, startup, debugging and generation failures |
| [Diagnostics and privacy](docs/DiagnosticsAndPrivacy.md) | Logging, local data, network activity and diagnostic collection |
| [Known limitations](docs/KnownLimitations.md) | Current feature boundaries and outstanding verification |

### Developing Studio and meta-packs

| Guide | Read it for |
| --- | --- |
| [Contributing](CONTRIBUTING.md) | Setup, engineering expectations, tests and pull requests |
| [Project context](AGENTS.md) | Product mission, architecture, terminology and working conventions |
| [Headless generator and test kit](headless/README.md) | Standalone modules, source ownership and build commands |
| [Meta-pack authoring](src/main/resources/studio/metapack/METAPACK.md) | Manifests, descriptors, templates and pack lifecycle |
| [Meta-pack compliance](src/main/resources/studio/metapack/METAPACK_COMPLIANCE.md) | Required metadata, dependencies and verification |
| [Independent pack artifacts](docs/IndependentMetaPackArtifacts.md) | Pack versioning, compatibility and publication commands |
| [Engine and meta-pack testing](docs/TestingEngineAndMetaPacks.md) | Test ownership, focused suites and expected outputs |
| [Architecture boundary tests](docs/ArchitectureBoundaryTests.md) | Dependency rules and how to address violations |
| [Failure-injection testing](docs/FailureInjectionTesting.md) | Recovery and failure-path checks |
| [Performance testing](docs/PerformanceTesting.md) | Measurements, results and interactive checks |
| [Accessibility review](docs/AccessibilityReview.md) | Keyboard access, themes and UI verification |
| [Code quality](docs/CodeQuality.md) | Optional duplication analysis and maintenance guidance |
| [Ancillary projects](ikasan-studio-ancillary/README.md) | Maven archetype and IDE mediator |
| [Claude Code guidance](CLAUDE.md) | Supplementary tool-specific development instructions |

### Releases and project direction

| Guide | Read it for |
| --- | --- |
| [Changelog](CHANGELOG.md) | Recorded release changes |
| [Release-candidate verification](docs/ReleaseCandidateVerification.md) | Automated gates, archive audits and installation/upgrade checks |
| [Marketplace manual checklist](docs/MarketplaceReleaseManualChecklist.md) | Lifecycle, multi-project and external-process release exercises |
| [14 September 2026 release audit](docs/ReleaseAudit-2026-09-14.md) | Evidence and outstanding checks for that specific candidate |
| [Product and technical roadmap](docs/IkasanStudioRoadmap.md) | Architectural direction and historical planning context |

Roadmap status tables and dated audits describe their recorded checkout, not necessarily the current working tree. They do not replace the current feature guides or verification of a new candidate.

## Build and contribute

Read [CONTRIBUTING.md](CONTRIBUTING.md) and [AGENTS.md](AGENTS.md) before changing the plugin. The main plugin and headless engine use Java 17. The optional native MCP module needs a Java 21 toolchain for its newer IntelliJ SDK and emits JVM 17 bytecode. Gradle can provision the configured toolchains; first builds require network access for dependencies and IDE downloads.

```sh
git clone https://github.com/ikasanEIP/IkasanStudio.git
cd IkasanStudio
./gradlew test
./gradlew buildPlugin
```

The plugin ZIP is written to `build/distributions/`. Use `gradlew.bat` on Windows.

| Command | Purpose |
| --- | --- |
| `./gradlew -p headless test` | Test the engine, test kit and packs without configuring IntelliJ |
| `./gradlew check` | Run root/headless checks and meta-pack validation; remote BOM/help checks need network access |
| `./gradlew buildPlugin verifyReleaseArchive verifyPlugin` | Build, audit the ZIP and check configured IDE compatibility boundaries; the archive audit needs Python 3 |
| `./gradlew runIdeModern` | Launch the newer sandbox selected by `sandboxIdeVersion` |
| `./gradlew runIde` | Launch the IDEA Community 2024.3.7 regression sandbox |
| `./gradlew runHarness` | Run enabled tests in the separate Swing harness suite |

In IntelliJ, **Run Plugin** uses the newer sandbox; **Run Plugin (2024.3.7 Regression)** uses the compilation-target IDE. Reload all Gradle projects after changing build configuration. Versions are configured in [gradle.properties](gradle.properties) and [native-mcp/build.gradle.kts](native-mcp/build.gradle.kts).

To package while another sandbox is running, use `./gradlew buildPlugin -PstudioSandboxDirectory=build/verification-sandbox`. The newer sandbox's IDEA 2026.2.2 file-chooser workaround is scoped to that sandbox in [build.gradle.kts](build.gradle.kts); it does not change installed users' settings.

### Repository layout

| Location | Responsibility |
| --- | --- |
| `src/main/java/org/ikasan/studio/intellij/` | IntelliJ editor, lifecycle, PSI, execution, settings and AI integration |
| `src/main/java/org/ikasan/studio/ui/` | Canvas, palette, properties and view models |
| `headless/studio-generator/` | Framework-independent model, persistence, validation, migration and generation |
| `headless/studio-test-kit/` | Reusable engine/pack testing support |
| `headless/studio-pack-v3/`, `headless/studio-pack-v4/` | Packaging for the two official meta-packs |
| `headless/studio-bundled-packs/` | Selection of exact pack revisions and combined pack tests |
| `src/main/resources/studio/metapack/` | Pack manifests, metadata, templates, icons and shared schemas |
| `native-mcp/`, `src/mcpAdapter/java/` | Optional native IntelliJ MCP integration and the Java adapter |
| `ikasan-studio-ancillary/` | Separately versioned Maven archetype and IDE mediator |
| `src/test/java/`, `src/testHarness/java/` | Root automated tests and the separate visual harness suite |

The root Gradle build includes `headless/` as a composite build. Core generation is shared by the plugin and standalone consumers. Meta-packs adapt the version-neutral model to specific Ikasan APIs; adding a pack requires packaging and contract validation, not just a new resource directory. A downloadable pack marketplace and a standalone generation CLI are not currently provided.

### Manual archetype fallback

Use the archetype version supplied for your candidate. The checked-out archetype declares `1.0.3`; this does not establish that it is available from Maven Central. Contributors can install the parent and archetype locally from the repository root:

```sh
mvn -f ikasan-studio-ancillary/pom.xml -N install
mvn -f ikasan-studio-ancillary/ikasan-studio-project-archetype/pom.xml install
```

Then, in a separate directory where you want the application:

```sh
mvn archetype:generate -DarchetypeGroupId=org.ikasan.studio -DarchetypeArtifactId=ikasan-studio-project-archetype -DarchetypeVersion=1.0.3 -DgroupId=org.example -DartifactId=my-module -DinteractiveMode=false
```

On Windows shells that split Maven property arguments, quote each complete `-Dname=value` argument. Open the generated root `pom.xml` in IntelliJ, wait for Maven import/indexing, and continue with [module configuration](docs/GettingStarted.md#2-configure-the-module). See [Troubleshooting](docs/Troubleshooting.md) for repository or SDK failures.

## Report a problem

Use the repository's [bug report](https://github.com/ikasanEIP/IkasanStudio/issues/new?template=bug_report.yml) or [feature request](https://github.com/ikasanEIP/IkasanStudio/issues/new?template=feature_request.yml) template. Include the plugin version/commit, full IDE build, operating system, selected pack and reproduction steps.

**Tools → Ikasan Studio → Collect Ikasan Studio Diagnostics…** creates a local ZIP; it does not upload it. Review its contents and remove sensitive information before attaching anything. See [Diagnostics and privacy](docs/DiagnosticsAndPrivacy.md) for collection and error-reporting behaviour.

Report suspected vulnerabilities privately using [SECURITY.md](SECURITY.md). Participation is governed by the [Code of Conduct](CODE_OF_CONDUCT.md).

## Licence and acknowledgements

Ikasan Studio is licensed under the [Apache License 2.0](LICENSE.txt). It is part of the [Ikasan project](https://github.com/ikasanEIP) and is based on the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template).
