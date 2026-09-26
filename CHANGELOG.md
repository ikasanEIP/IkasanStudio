# Ikasan Studio changelog

User-facing release notes for Ikasan Studio. The plugin build includes the matching
version's notes, or the Unreleased notes while that version is being prepared.

## [Unreleased]

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

- Filename lists now stay comma-separated when saving the model or copying flows, preventing literal list brackets from reappearing after reload. Regex character classes are preserved.

- In-place migrations now remove unmodified, unversioned source-component dependencies retired by the target pack, avoiding unmanaged legacy JAXB dependencies after upgrading to 4.1.6 while preserving explicit overrides.

### Added

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
