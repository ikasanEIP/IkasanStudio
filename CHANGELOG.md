# Ikasan Studio changelog

User-facing release notes for Ikasan Studio. The plugin build includes the matching
version's notes, or the Unreleased notes while that version is being prepared.

## [Unreleased]

- Retain the running-module import password for retries within the open fetch dialog; clear it on close without persisting it.
- Normalize Swagger URLs during running-module import, clarify case-sensitive module names, and log safe REST endpoints/statuses. Offer reviewed exact-version or newer same-line patch pack selection while rejecting downgrades and cross-version migrations.
- Generate JMS MapMessage flow-test fixtures for map consumers and message-kind guards; add sendMap support to the V3, V4 and V5 test helpers.
- Add JMS Map Message To Payload Converter to the V3, V4 and V5 development palettes, with configurable content/ID/filename fields, runtime configuration import, and direct Ikasan runtime construction.
- Add Payload To Map Converter to the V3, V4 and V5 development palettes, with direct Ikasan runtime construction, file-transfer input/Map output type guidance and documented charset/attribute precedence.

- Fetch module metadata and flow-scoped component configuration from a running Ikasan module into the existing import preview. Add optional Basic authentication without credential persistence, cancellable bounded HTTP requests, actionable REST failures and the existing mapping/review gate before import.

- Fix runtime-import version choices and pack-loading errors waiting until the modal import dialog closes. Publish background results while the dialog is open, preserving disposal guards.
- Warm the VFS before opening meta-pack install/update file choosers, preserve their modal parent, and ignore selection callbacks after the owning project or manager closes.

- Add persistent **Allow snapshot meta-packs** under Studio settings. Embed Ikasan 5 as optional resource-only data activated after opt-in and IDE restart, filter new-module/import choices by snapshot runtime, and preserve existing snapshot projects after opt-out. Include JDK 26 and demo preparation guidance.

- Retain **Run Plugin (Snapshot Packs)** and the explicit `studioIncludeSnapshotPacks` build flag for pack development. Normal classpaths contain released-runtime packs; the optional V5 payload now activates through the developer setting above. Record interactive V5 module testing separately from runtime certification.

- Fix router route-name validation rejecting valid lists such as `odd,even`. Validate legacy comma-separated patterns against the complete distinct list in the editor and saved-model checks, while preserving per-entry typed collection validation and route order.

- Save affected unsaved flow-test documents before component/flow rename, then rebuild the reference-update plan while preserving developer edits and unrelated unsaved files. Report blocked saves as recoverable guidance.

- Add **Run Flow Test** to the flow canvas context menu, using IntelliJ's normal test configuration and Run tool window for the existing flow-test class.

- Explain unsupported flow-test text comparisons with expected and actual Java types, producer/output context and explicit mapping or object-assertion guidance. Preserve original causes and developer assertion navigation; keep decoding failures distinct.

- Resolve component code-navigation targets from the current Java file after edits or regeneration, preventing double-click navigation from failing on an invalid cached PSI class.

- Add isolated V3/V4 → V5 migration rehearsals with explicit directional rules, model-preservation and rejection checks, and generated V5 runtime fixtures. Keep released IDE/CLI migration choices unchanged while the snapshot and developer-code upgrade paths remain under review.

- Verify V5 generated custom configuration beans through real SMTP delivery, including scalar, list and map environment overrides, generated factory injection, idle readiness and later delivery. Include the scenario in the explicit V5 verification suite.

- Add a V5 compatibility fixture using the real archetype Maven reactor (`user`, `generated`, `user-flow-tests`). Verify inherited build settings, a real flow test across module boundaries, resolved dependencies and archive ownership without installing fixture artifacts.

- Enforce resolved Ikasan platform versions in the V5 verification command using structured Maven dependency trees for every reactor module. Reject missing evidence and mixed versions, preserve actionable audit reports, and document the exact independently versioned Ikasan DBCP fork without changing Maven dependencies.

- Add an HTTP compatibility check for the V5 generated application's Blue Console: login-page rendering, anonymous-access protection, form login, session-cookie module/flow navigation and listener cleanup. Include it in the explicit V5 verification command.

- Extend V5 JMS rollback diagnostics with receive/send and same-session/separate-session controls. Reproduce the XA resource-end warning without an Ikasan flow, while retaining strict rollback and redelivery assertions; document the minimal failing comparison for runtime investigation.

- Add a V5 runtime fixture for the actual generated Email Converter and environment-specific email property overrides. Check delivered bodies, recipient lists and subjects, sender binding on the running producer, idle readiness, later delivery and local-server cleanup.

- Add a single explicit Ikasan 5 development verification command that exports fresh fixtures, runs the compilation/runtime matrix, preserves test reports and dependency evidence, and fails on incomplete or failing runtime checks. Known rollback failures remain visible; no candidate runtime patch is applied automatically.

- Expand Ikasan 5 development runtime coverage to ObjectMessage conversion, routing, filtering, splitting, broker/translator logic, Jakarta XML, scheduled input, local files and transparent debug components. Add standalone JMS XA controls and a separate candidate runtime patch that passes isolated FTP/SFTP rollback probes; the unmodified snapshot rollback gate remains failing.

- Add explicit Ikasan 5 development fixtures for JMS XA and FTP/SFTP producer rollback, retry, idle readiness and later delivery after an injected commit failure. Document the XA connection-factory requirement and the limits of the rollback coverage. The strict runtime gate currently exposes XA resource-end and closed-connection rollback errors in the V5 snapshot; these remain unresolved.

- Add isolated Ikasan 5 runtime fixtures for local JMS, SMTP, FTP and SFTP, with three deliveries per flow, idle-readiness checks, actual receiver assertions and transport cleanup checks. Run them explicitly with Maven and Java 26; the development pack remains unbundled.

- Extend the unbundled Ikasan 5 development pack with a repeatable compilation fixture for all 27 runtime palette components, all five exception actions, generated business stubs and 27 flow tests. Catalogue additions without coverage fail the contract test; placeholder scenarios are not run by default.

- Start an isolated Ikasan 5 snapshot meta-pack with Java 26/BOM metadata, pinned development help, pack contracts and a generated application/flow-test compilation fixture. The initial generated flow compiles and runs on Java 26 after adapting resource injection to Jakarta annotations. It is not included in the released plugin or migration choices.

- Validate Module and Check again save edited project documents before scanning, preventing stale disk contents from keeping resolved findings visible. Failed saves stop validation with an actionable message.

- Restrict local-file test consumers to prefixed test fixtures so the scan directory cannot become an input payload. Directory text-decoding errors identify the path and point to `flowTestProperties`; decoding failures retain the developer assertion location.

- Generate independent expected-output resource files for local-file flow-test scenarios with text expectations, and clarify that input strings name resource files. Preserve inferred business-object expectations.

- Group generated flow-test names at the top of the class: flow, consumer (where applicable), then producer.

- Open the selected flow-test scenario after generation or support refresh, rather than whichever shared support file was written first. For multiple flows, prefer the first newly generated scenario.

- Include the flow-test class in scenario failures. Output-count errors now report producer-specific expected/observed counts and navigate to the actual batch definition, retaining the observation cause. Expected-path failures navigate to `defineExpectedPath`.

- Validate saved flow-test `defineExpectedPath` references for unknown component names and incorrect roles, with source-line navigation and suggestions for spacing/case mistakes. Generated V3/V4 test support also rejects invalid path registrations before starting the flow, including dynamically supplied names.

- Replace the collection editor’s unset checkbox with **Clear and use component default**. **OK** saves the entered collection, including an explicitly empty list or map; **Cancel** preserves the original value.

- Validate Module opens property findings in Studio, reveals the affected property and shows full explanations with safely displayed saved values. Source findings open at the reported line.
- Validate Module now validates saved model property types, required and conditional values, choices and patterns, including recipient-list entries, and reports the flow, component and property to correct.

- Reject malformed email addresses, including trailing/repeated dots and invalid domain labels, in single recipients and recipient lists. Validate saved values on opening Properties and mark invalid fields with an error indicator and corrective guidance.

- Externalize configured email settings, including single recipients, sender and SMTP connection details, into generated `application.properties` for both Ikasan versions.

- Externalize remaining scalar and string-map settings for JMS, FTP/SFTP, local-file, scheduled, logging and XML-converter components, using component-specific keys for new mappings. Preserve existing keys and custom Java configuration beans.

- Enhance and simplify the auto-generated Flow tests.

- Offer reviewed, meta-pack-driven migrations rules in IDE and offline migration, with recovery and exact-plan source comparison.

- Read generation rollback snapshots and post-format fingerprints off the UI thread, rejecting stale files before commit.

- Add on-demand generated verification baselines under `generated/src/test`, using the existing application Maven module: model-derived structural contracts, documented runtime coverage boundaries, Maven execution and archive-on-refresh ownership.

- Make sample-consumer fixture input and isolated FTP delivery checks executable by default; add missing fixture settings with backups while preserving explicit opt-outs.

- Add selectable actual-output text conversion in generated flow tests for Ikasan payloads, files, bytes and JMS text messages, with explicit decoding failures.

- Generate named input-batch constants and reusable physical-file delivery assertions, with metadata-driven FTP/SFTP receiver-check guidance.

- Fix flow-test generation with Maven plugin/profile dependencies or dependency management; add FTP fixture dependencies only to the project dependency section while preserving existing XML.

- Offer isolated local FTP setup in flow-test generation dialogs, configuring test properties with backups while preserving custom settings.

- Add an optional per-test local FTP server, metadata-derived connection overrides and temporary delivery directories to generated flow tests; preserve primary failures during teardown.

- Add opt-in deterministic fixture submission to new Generic Consumer samples, using real event creation and transactional dispatch while preserving automatic polling by default.


- Make Generic Consumer sample initial/repeat delays configurable per implementation class, retaining one-minute defaults and allowing shorter fixture delays.


- Simplify direct self-generating-source/discard-sink flow tests using meta-pack capabilities, with bounded observation and readable `test...` method names.
- Snapshot meta-pack initial event sequences into observation tests; verify ordered payloads, later delivery and stopped state during teardown for repeatable upgrade checks.


Initial 1.0.0 release in preparation. These notes describe the planned release scope;
release verification and outstanding checks are tracked separately in
[Release-candidate verification](docs/ReleaseCandidateVerification.md).

### Fixed

- Use IntelliJ's public move-class refactoring when moving handwritten components between flows, preserving Java references and generated Spring bean names.
- Await a full Maven sync before compiling migrated projects, with project-lifetime cancellation and import-failure handling.

- Filename lists now stay comma-separated when saving the model or copying flows, preventing literal list brackets from reappearing after reload. Regex character classes are preserved.

- In-place migrations now remove unmodified, unversioned source-component dependencies retired by the target pack, avoiding unmanaged legacy JAXB dependencies after upgrading to 4.1.6 while preserving explicit overrides.

### Added



- Add a Maven meta-pack example and authoring archetype under `ikasan-studio-ancillary`, with separate runtime components, a scheduled sample flow, descriptor/rendering and generated-factory compilation tests, and installer validation of the complete resource-only JAR. The archetype generates and builds the maintained example in its integration test.


- Offline tools exports now include the saved project’s required custom meta-pack and an ID/revision/SHA-256 inventory. Exported tools are relocatable and reject missing, changed or unlisted pack JARs. New CLI **validate** and **render** commands support custom-pack models, with rendered previews written to a new directory outside the project. Existing migration paths remain V3.3.9 ↔ V4.1.6.

- Added **Install Meta-Pack…** under Tools → Ikasan Studio for complete custom pack JARs, with metadata, compatibility, resource and template-syntax validation before installation. Installed packs activate after an IDE restart.

- Added **Manage Meta-Packs…** for reviewed external-pack updates, removal, cancellation and restoration of retained revisions. Changes activate after restart, with artifact integrity checks and recovery from interrupted operations. Component overlays and simultaneous revisions of the same pack ID remain unsupported.

- Saved model documents now record the selected meta-pack revision and block loading when it differs from the available revision. Older unlocked models acquire a lock on save. **Adopt Installed Meta-Pack Revision…** provides explicit, validated adoption with a model backup; regenerate, review and test application output afterwards.

- Flow-test scaffolds now share lifecycle/assertion helpers, support explicit filtering/routing scenarios and generate JMS queue input helpers with receiver-side text checks for simple queue-to-queue flows.

- Generated flow tests load shared settings from a preserved, developer-owned `module-test.properties` file, with module property keys and environment-variable guidance.

- Flow tests now share a developer-owned `ModuleFlowTestSupport` class, with explicit backup-and-regenerate support for common test setup and fresh application contexts per scenario.

- Added Hide panels / Show panels on the designer to quickly collapse and restore Properties and Palette, preserving their width, selected tab and pending edits.

- Flow-test scaffolds now generate explicit Ikasan component-path checks for simple linear flows, isolated local-file inputs and a revised five-step setup guide. Complex scenarios remain explicitly unfinished until their expectations are supplied.

- Added module-level **Generate Flow Tests…** with per-flow checkboxes, Select All/None, and batch generation that preserves existing tests.

- Added **Browse remote files…** on SFTP producers and consumers, with directory navigation, downloads, confirmed file deletion, and host-key verification.

- Added **Generate Flow Test…** to Tools → Ikasan Studio and flow context menus. Creates developer-owned, version-aware Ikasan test scaffolds in `user-flow-tests`, preserving existing tests during generation and migration. Scenarios require test inputs, settings and assertions before they can pass.

- Bundled offline migration/verification tools with the plugin, with Tools → Ikasan Studio → Export Offline Migration Tools for local extraction without another download.

- Added standalone migration preview/apply with recovery snapshots and a reusable Maven before/after verifier for developers’ Studio projects; reports distinguish missing test coverage from passing checks.

- Added a reusable all-executable-component migration fixture with real-flow acceptance tests, isolated transport services and before/after reports checking behaviour, model structure and developer-source preservation.

- MCP catalogue queries can select component keys or return compact discovery metadata; version-specific Ikasan flow-test guidance and reference examples cover both bundled releases.

- Added an advisory implementation-readiness check with source navigation, generated reports, MCP snapshot findings and file-proposal feedback for missing source and known scaffolds.

- File-based AI proposals publish correlated result files through validation, review, generation and cancellation; rejection dialogs offer Copy feedback for AI.

- Added a read-only wiretap viewer with module/flow/component shortcuts, date filters, pagination and stored payload previews.

- Added a read-only FTP/SFTP duplicate-history viewer with client/path filters, stored file attributes and an explanation of duplicate matching.

- Added a read-only module/flow excluded-event viewer with pagination, flow/date filters, harvested status and safe text/binary payload previews through the local Ikasan REST API.

- Visual Ikasan module and flow design in the IntelliJ editor, with component properties, onboarding and Undo/Redo.
- Component libraries and code-generation templates for Ikasan 3.3.9 and 4.1.6.
- Java, Maven and configuration generation with separate Studio-generated and developer-owned source trees.
- IntelliJ Run/Debug integration and access to the module-local Blue Console.
- Flow copy/paste, conversion recipes, and local FTP/SFTP, email and JMS testing tools.
- AI-assisted model editing through MCP or imported proposal files, with approval settings and protection for developer-owned code.

### Fixed

- New AI recipe converters inherit omitted input/output types from the selected recipe while rejecting explicit type mismatches.

- Accept parameterized payload types for Generic Producers in both meta-packs; proposal property errors identify the affected component.

- Moved startup AI-guidance discovery and create-only writes off the EDT, with asynchronous VFS refresh, to avoid slow-operation errors during IDE startup.

- AI guidance keeps normal ESB flows on automatic startup and reports missing services as blockers instead of recommending manual startup to hide failures.

- Preserve explicit zero-valued component settings and filename-regex character classes during generation.
- Generate compilable, protected scheduled message providers in `user/` and support logging format configuration despite the framework builder's non-fluent setter.

<!-- At publication, move these notes under a dated [1.0.0] heading and retain
an [Unreleased] section for changes intended for the next release. Record useful
user-facing changes here; keep implementation details and test evidence in the
relevant documentation rather than maintaining a development diary. -->
