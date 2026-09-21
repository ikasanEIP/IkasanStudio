# Ikasan Studio changelog

User-facing release notes for Ikasan Studio. The plugin build includes the matching
version's notes, or the Unreleased notes while that version is being prepared.

## [Unreleased]

Initial 1.0.0 release in preparation. These notes describe the planned release scope;
release verification and outstanding checks are tracked separately in
[Release-candidate verification](docs/ReleaseCandidateVerification.md).

### Added

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
