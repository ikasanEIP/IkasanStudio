# Ikasan Studio features

Design, build, run and test Ikasan integrations inside IntelliJ IDEA.

**Version** identifies the first distribution containing each capability:

- **Beta** — available in the beta baseline and subsequent nightly builds.
- **Nightly** — additions available in the [GitHub nightly ZIP](../downloads/ikasanstudio-1.0.0-beta.1.zip).

The beta baseline currently uses the [archived V3/V4 ZIP](../downloads/ikasanstudio-1.0.0-beta.No-Ik5.zip); confirmation that this is the published beta artifact is pending. Availability was reviewed on **6 November 2026**. The channel matters: beta and nightly archives can contain the same plugin version number.

## Visual flow design

| Version | Feature | Developer benefit |
| --- | --- | --- |
| Beta | Drag-and-drop flow editor | Compose integrations visually inside IntelliJ. |
| Beta | Module, flow and component property editors | Configure behaviour without hand-writing repetitive wiring. |
| Beta | Routers, filters, splitters, brokers and translators | Build branching and transformation paths. |
| Beta | Exception resolver configuration | Define exception handling and recovery policies. |
| Beta | Property search and optional settings | Find configuration quickly. |
| Beta | Canvas-to-code navigation | Open the implementation behind a component. |
| Beta | Undo/redo and flow duplication | Revise designs and reuse existing flows. |

## Components and code generation

| Version | Feature | Developer benefit |
| --- | --- | --- |
| Beta | JMS, FTP, SFTP, local-file and email components | Connect common integration transports. |
| Beta | Scheduled and event-generating consumers | Start processing from timers or generated events. |
| Beta | Custom component and implementation scaffolds | Add application-specific Java behaviour. |
| Beta | Java, Maven and properties generation | Generate application structure from the visual model. |
| Beta | Separate generated and developer-owned source | Regenerate wiring while retaining business implementations. |
| Beta | Payload-type guidance | Spot declared type mismatches before running. |
| Beta | Suggested converter recipes | Generate common payload extraction and construction code. |
| Nightly | JMS MapMessage-to-payload and payload-to-map converters | Bridge JMS map messages and file-transfer payloads. |

## Import and reuse

| Version | Feature | Developer benefit |
| --- | --- | --- |
| Beta | Studio model import and export | Transfer saved designs between projects. |
| Beta | Runtime metadata and configuration JSON import | Reconstruct existing Ikasan modules from exported JSON. |
| Beta | Flow copy/paste between matching-version projects | Reuse flows and their flow-owned Java implementations. |
| Beta | Find and replace model references | Update shared class and bean references after refactoring. |
| Nightly | Generate Studio module from running Ikasan module | Acquire module metadata and component configuration through REST. |
| Nightly | Import Ikasan module using import Wizard | Bring topology, implementations and properties into one workflow. |
| Nightly | Import Ikasan source code using import Wizard | Assisted merge of bespoke classes, unit tests, helpers and additional dependencies. |
| Nightly | Import Ikasan configuration using import Wizard | Assisted merge of configurations and properties. |
| Nightly | Checkout identity and reactor-module checks | Avoid importing code from the wrong application. |
| Nightly | Whole-file and partial source copying | Review native diffs, save previews and revert pending changes. |
| Nightly | Support-file customisation import | Retain explicitly copied debug/support changes during regeneration. |
| Nightly | Inline wiring and scan findings | Resolve issues beside the affected source or dependency. |

## Environment configuration

| Version | Feature | Developer benefit |
| --- | --- | --- |
| Beta | Generated Spring properties and external overrides | Separate configured defaults from deployment settings. |
| Nightly | Properties import from files or pasted text | Recover configuration used by bespoke classes. |
| Nightly | Shared and named environment files | Maintain settings for dev, int and other targets. |
| Nightly | Key-change review and native value diffs | Inspect configuration before replacing files. |
| Nightly | Git-ignored local configuration | Keep machine-specific settings outside shared configuration. |
| Nightly | Environment Run/Debug configurations | Switch environments without changing canvas defaults. |

## Run, debug and inspect

| Version | Feature | Developer benefit |
| --- | --- | --- |
| Beta | IntelliJ Application Run/Debug configurations | Run modules with normal IDE process and debugger controls. |
| Beta | Canvas flow start/stop controls | Control individual flows during development. |
| Beta | Debug components and code navigation | Place breakpoints along the processing path. |
| Beta | Wiretaps before and after components | Capture events around selected processing steps. |
| Beta | Module-local Blue Console access | Open flow controls, REST APIs and module administration. |
| Beta | Wiretap and excluded-event viewers | Inspect retained runtime records without the Dashboard. |
| Beta | FTP/SFTP duplicate-history viewer | Investigate persisted duplicate-filter records. |
| Beta | SFTP file browser | Browse, download and explicitly delete selected remote files. |
| Beta | Independent H2 launch | Start a separate development database server. |
| Nightly | H2 launch through IntelliJ's Run window | Use project-resolved H2, process tracking and normal Stop/Rerun controls. |

## Local development services

| Version | Feature | Developer benefit |
| --- | --- | --- |
| Beta | Local FTP test server | Exercise file transfers with inspectable test directories. |
| Beta | Local SFTP test server | Test SSH file transfers without an installed SSH service. |
| Beta | Local mail capture and web inbox | Inspect outgoing email without sending it externally. |
| Beta | JMS test-reader flows | Inspect producer messages using an existing broker. |
| Beta | Test-message injection | Send selected payloads into supported running consumers. |
| Beta | Real file-scan triggers | Exercise configured acquisition rules on demand. |

## Testing and validation

| Version | Feature | Developer benefit |
| --- | --- | --- |
| Beta | Generated structural verification tests | Check flow factories and declared implementation contracts. |
| Beta | Editable JUnit business flow-test scaffolds | Supply meaningful inputs, paths and expected outputs. |
| Beta | Single-flow and bulk test generation | Create tests for selected flows together. |
| Beta | Local FTP, SFTP and SMTP test fixtures | Exercise supported transports in isolated test environments. |
| Beta | Batch, splitter, filter and router test support | Express delivery counts and expected processing paths. |
| Beta | First delivery, idle readiness and later delivery checks | Test long-lived flow behaviour. |
| Beta | Native Expected/Actual failure diagnostics | Compare results and navigate to developer assertions. |
| Beta | Validate Module and implementation-readiness findings | Find invalid properties, missing source and recognised unfinished scaffolds. |
| Nightly | JMS MapMessage test fixtures | Supply map messages to generated JMS flow tests. |

## Versions, migration and extensibility

| Version | Feature | Developer benefit |
| --- | --- | --- |
| Beta | Ikasan 3.3.9 pack — Java 11 | Generate applications for the supported Ikasan 3 baseline. |
| Beta | Ikasan 4.1.6 pack — Java 17 | Generate applications for the supported Ikasan 4 baseline. |
| Nightly | Bundled Ikasan 5.1.0 pack — Java 25 | Build Ikasan 5 applications without snapshot opt-in. |
| Beta | Previewed Ikasan 3.3.9 ↔ 4.1.6 migration | Review model, generated-code and Maven changes before applying. |
| Beta | Supported import replacements and JDK alignment | Reduce repetitive work during version migration. |
| Beta | Migration snapshots and restoration | Recover files changed by a migration. |
| Beta | External meta-pack installation and management | Add independently packaged component libraries and templates. |
| Beta | Project pack-revision locks | Prevent silent adoption of a different installed pack revision. |
| Beta | Exportable command-line tools | Validate, render and perform supported migrations outside IntelliJ. |

## Optional AI assistance and recovery

| Version | Feature | Developer benefit |
| --- | --- | --- |
| Beta | MCP connection to compatible assistants | Expose the live model and selected component catalogue. |
| Beta | Validated model-change proposals | Add or edit supported flows, components, routing and properties. |
| Beta | Proposal review, approval settings and undo | Control how assistant changes reach the design. |
| Beta | File-based AI proposals | Exchange structured changes without a live MCP connection. |
| Beta | Generated schemas, catalogues and project guidance | Give coding assistants project-specific context. |
| Beta | Automatic model backups and recovery mode | Recover from unreadable or invalid saved models. |
| Beta | Diagnostic bundle collection | Share structured troubleshooting evidence. |
| Nightly | Detailed runtime-import diagnostics | Investigate REST, version-matching and configuration-import failures. |

## Scope

Component and tool availability depends on the selected pack. Business implementations and test expectations remain developer-owned. Runtime viewers are read-only; JMS readers require a broker. Supported version migration remains **3.3.9 ↔ 4.1.6**; bundling Ikasan 5 does not add a supported migration path to it.

## Maintaining this matrix

1. Add implemented capabilities to the appropriate table as **Nightly** when they reach the downloadable nightly ZIP.
2. Promote a row to **Beta** only when that capability is included in the published beta; retain separate rows for newer enhancements.
3. Update the reviewed date, release baseline and download links when promoting a build. Do not infer availability from the plugin version string alone.
4. Keep each row to one capability and one short developer benefit. Put operational detail in the linked [workflow guides](../README.md#documentation).

Working beta reference: `downloads/ikasanstudio-1.0.0-beta.No-Ik5.zip`, SHA-256 `f982176ff0b8f0fadf520f75c57366ea292193edddc51955e88c41d994058421`. This identifies the inspected archive; it does not establish Marketplace publication. Replace this provisional reference with the confirmed published artifact or immutable source commit.
