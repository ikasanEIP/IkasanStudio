# Logging, diagnostics and privacy

## Collecting diagnostics

Use **Tools → Collect Ikasan Studio Diagnostics…**, or search for the same action in IntelliJ Find Action. It remains available during indexing and does not require the designer tab to be open. Choose a local `.zip` destination; collection runs in the background. Nothing is uploaded or sent to another application.

The archive contains exactly:

- `environment.txt`: plugin version, IDE version/build and the selected meta-pack, or `unavailable` when not known.
- `studio-redacted.log`: approved structured Studio events from the last 2 MiB of the current `idea.log`, plus a count of omitted legacy/unrecognized Studio records.
- `README.txt`: scope and privacy notes.

No project directory is scanned. Model contents, generated code, POM files, application properties, environment variables and harness output are not collected. There is no automatic model-attachment option. If a developer wants to supply a model for support, they must review and attach it separately and explicitly. Review the ZIP before sharing it.

Missing logs produce an archive explaining that no logs were collected. A write failure reports a recoverable notification. Output is written to a temporary file and atomically published; failures clean up the temporary file and preserve existing destination contents. Filesystems without atomic replacement report failure instead of falling back to a partial write. The destination must end in `.zip`.

## Structured events and levels

Privacy-sensitive generation, model/configuration, property-edit and runtime failure paths now use `StudioDiagnosticEvent`:

```text
STUDIO-DIAG v1 event=GENERATION_FAILED level=WARN module=<id> flow=<id> component=<id> error=java.io.IOException frame=org.ikasan.studio...:123
```

Use fixed event identifiers, not free-form data. `INFO` records generation/export milestones, `DEBUG` records property-change occurrence without its key/value, and `WARN` records expected configuration, IO and runtime failures. Missing/null meta-packs, malformed metadata and unsupported model properties must not invoke IntelliJ's fatal-error logger. Retain fatal logging only for actual implementation invariant violations.

Context identifiers are session-salted hashes of module, flow or component identities where the caller has that context. They correlate events without exporting the names. The salt is not exported and identifiers change after restart; they are not stable project IDs. Events without applicable context use `none`.

Exceptions retain their original cause in the operation result. Logs retain up to four cause types and eight Studio call sites per cause. Raw exception messages, template/model excerpts, property values and full configuration objects are deliberately omitted. Template failures now preserve their cause instead of replacing it with an unchained generic exception. Existing user-facing failure paths retain their actionable explanations.

Older operational log statements still use the legacy `STUDIO:` prefix. The collector does not copy these messages: historical logs may contain secrets written by earlier plugin versions. Only a complete structured event in an IntelliJ log envelope, with approved fields and a known event identifier, is exported. Oversized or malformed records are omitted. Other plugins' records and exception continuation lines are not exported. This is deliberately more restrictive than searching arbitrary text for words such as “password”.

The source `idea.log` can contain events from multiple projects in the same IDE. The collected structured events likewise cover the bounded IDE log tail; hashed context distinguishes available module/flow/component context. The metadata describes the project from which collection was invoked. No claim of project-exclusive log filtering is made.

## Leak fixes

Removed the complete properties-map print from placeholder substitution, property-value logging, module/component object dumps on audited generation/painting paths, generated file-content logging on write failure, and invalid-property value dumps. JSON/template failure logging no longer includes raw parser/template exception messages. Expected metadata/configuration failures now use warning severity.

Warning/error notification helpers record a safe event instead of copying their arbitrary message text to the log. Detailed user-facing explanations remain in the notification or existing editor failure state. This prevents a notification containing a parser excerpt from becoming a second raw model dump. Persistent actionable recovery UI remains a separate follow-up identified in [AccessibilityReview.md](AccessibilityReview.md).

These changes do not erase existing IDE logs, and the plugin cannot control logging performed by the generated application, dependencies or other installed plugins. The collector's strict export boundary protects the bundle from older free-text logs; it does not certify the entire IDE log as credential-free.

## Maintenance and verification

Never log maps, model `toString()` output, entered property values, payloads, tokens, connection URLs or raw exception messages in new diagnostic events. Add a fixed enum event when a new operation needs diagnosis; include only the narrow context needed. Keep English and Japanese action/message resources synchronized. Re-run privacy tests whenever changing the export grammar or archive entries.

```sh
./gradlew test --tests '*StudioDiagnosticBundleTest' --tests '*CollectStudioDiagnosticsActionHeavyTest' --tests '*GeneratorFailureInjectionTest' --tests '*StudioBuildUtilsTest'
./gradlew test
```

Tests cover canary credentials/model data, cause preservation, context hashing, embedded/oversized/unrecognized log records, metadata allowlisting, bounded log reads, missing logs, atomic-write failure cleanup, preservation of developer files, no configuration stdout, expected warning severity and action registration/indexing availability. Interactive file-chooser and notification behaviour should also be exercised in `runIde` before release.
