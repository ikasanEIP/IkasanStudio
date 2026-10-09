# Compatibility warning cleanup — 9 October 2026

This review follows Marketplace's Community beta.2 report. The report marks the
plugin compatible, but flags deprecated and experimental APIs. Warning counts
vary by IDE build; do not treat source call-site counts as a Marketplace result.

## Changes

- Compose `StudioBundle` around `DynamicBundle(Class, String)` rather than
  inheriting from its deprecated constructor. The bundle's owning class loader
  is explicit; message keys and translations are unchanged.
- Replace three `createSingleFileDescriptor()` calls with an explicit
  `FileChooserDescriptor` using the same six selection flags. Both factory names
  are deprecated on newer IDEs; the descriptor constructor supports our baseline.
- Iterate scheduled-consumer scan criteria through Jackson `fieldNames()` and
  `get(name)`, avoiding the deprecated `fields()` method without requiring a
  newer Jackson API.
- Use project-scoped `ReadAction.nonBlocking(...).executeSynchronously()` for
  five background operations: initial content-root discovery, flow-package rename
  planning, test rename planning, clipboard source capture and source relocation.
  These computations build local results and can be retried when a write is
  requested. Their enclosing background/progress workflows remain intact.
- Extend the newer-IDE test task to include localisation, flow-package rename,
  test rename and clipboard coverage.

## Retained deliberately

- **FileSaverDescriptor:** the compilation-target SDK (2024.3.7) only exposes the
  varargs constructor. The newer constructor overloads are absent. Keep the two
  existing calls until a separately verified compatibility adapter or minimum-IDE
  change is justified.
- **Other blocking reads:** short EDT/refactoring lookups and reads that mutate
  navigation targets or accumulate rollback snapshots are not safe mechanical
  conversions to restartable computations. A subsequent lifecycle refactor should
  separate data collection from mutations before changing these calls.
- **Terminal creation:** `createShellWidget` supports the existing IDE range and
  launchers. The newer tabs API is not a drop-in baseline replacement. Retain the
  two calls until a version-specific adapter has interactive coverage for H2 and
  the mail harness, including command execution, focus and cleanup.
- **WriteIntentReadAction:** required at Swing save/refactoring boundaries on
  newer IDEs. Do not remove lock acquisition to silence an experimental warning.
- **MavenSyncSpec/updateAllMavenProjects:** migration must await completed Maven
  import before compiling. Keep the tested coroutine implementation until an
  equivalent stable API is available.
- **Optional MCP parent-plugin dependency:** retain the class-loader dependency.
  The community beta.2 compatibility audit records runtime loading evidence;
  Marketplace's unresolved-optional-dependency warning merits a JetBrains report,
  not removing a necessary dependency.

References: [2026 API changes](https://plugins.jetbrains.com/docs/intellij/api-notable-list-2026.html),
[threading model](https://plugins.jetbrains.com/docs/intellij/threading-model.html),
[terminal API](https://plugins.jetbrains.com/docs/intellij/embedded-terminal.html).

## Verification

Final combined Gradle run passed:

- 34 focused tests on the 2024.3.7 compilation-target IDE.
- 43 selected compatibility tests on IDEA 2026.2.2.
- Plugin Verifier: compatible on IDEA 2024.2, 2024.3.7 and 2026.2.2.

The expanded tests needed test-only adaptation for the newer internal unlock hook,
undo confirmation dialogs and IntelliJ's suppressed `RethrownStack` diagnostic.
Rollback assertions still check original files, external references and actual failures.

The 2026.2.2 verifier reports 11 deprecated usages and 13 experimental usages.
The older two IDEs report 13 experimental usages and no deprecated usages.
This is a local upstream build, not a new Marketplace verification of the community
artifact. IDEA 2026.3 EAP and interactive terminal/file-dialog workflows were not rerun.
