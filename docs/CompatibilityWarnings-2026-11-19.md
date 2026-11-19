# IntelliJ API warning cleanup — 19 November 2026

This records API maintenance in upstream source for the next plugin release.
It does not certify other release artifacts or Marketplace approval. Compilation remains
on IDEA 2024.3.7, minimum compatibility remains 2024.2, and the newest configured
verification boundary is now IDEA 2026.3.

## Changes

- Replace the H2 and module-startup `ProcessAdapter` subclasses with `ProcessListener`.
  Its default callbacks are available on the supported baseline. Output handling,
  deliberate-stop detection, listener removal and disposal behavior are preserved.
- Use project-scoped `ReadAction.nonBlocking(...).expireWith(project).executeSynchronously()`
  for read-only POM text/PSI, application-properties PSI, source-directory discovery,
  source-relocation lookups and offline-export model-path discovery. These operations
  return their results without publishing shared state inside a retryable computation.
  This does not move work to a background thread by itself; existing background,
  progress and EDT/refactoring boundaries remain in place.
- Propagate cancellation from POM/directory lookup and source relocation instead of
  logging it as an unexpected lookup/refactoring error.
- Make generation rollback preparation retry-safe: build the entire snapshot list
  within each read attempt, check cancellation between artifacts, and publish only
  a completed attempt. An interrupted read cannot append duplicate rollback entries
  or retain pre-write bytes. Disk reads remain off the EDT and commit still validates
  the captured file/document stamps.
- Extend `testIdeCompatibility` to cover module-startup/H2 process state, generation
  rollback and generated-file navigation as well as the existing refactoring,
  Maven synchronization, clipboard, localization and optional MCP loading checks.

## Deliberately retained

| API | Reason |
| --- | --- |
| `ReadAction.run` in PSI navigation-handle initialization | The computation publishes navigation targets into shared view-handler state. Repeating it is not equivalent to retrying a pure lookup; it needs a separate collect/apply refactor. |
| `FileEditorManager.openFile(VirtualFile, boolean)` | Legacy alternatives are also deprecated in 2026.3. The newer coroutine/request overload is experimental and absent from the compilation baseline. Preserve existing focus, debugger selection and custom editor behavior. |
| `FileSaverDescriptor(String, String, String...)` | The compilation-target SDK only exposes this public constructor. The newer descriptor constructor is absent; keep extension filters and native save-dialog behavior intact. |
| Terminal `createShellWidget` | New terminal-tab APIs are not a baseline-compatible replacement. A future version adapter needs interactive mail command, focus and shutdown coverage. |
| `WriteIntentReadAction` | Required lock acquisition at save/refactoring boundaries. Switching to the underlying application API retains the experimental status; removing the lock would be incorrect. |
| `MavenSyncSpec` / `updateAllMavenProjects` | Migration must await completed Maven synchronization before compilation. Keep the tested coroutine implementation until a stable equivalent supports the IDE range. |
| Optional native MCP dependency | Retain the optional content-module and required class-loader dependencies. Unavailable native MCP on older IDEs does not make the core plugin require it. |

Do not replace deprecated methods with equally deprecated alternatives or call
lower-level locking methods merely to hide verifier findings. The goal is supported
behavior across the IDE range, rather than a zero-warning report at any cost.

References: [API compatibility](https://plugins.jetbrains.com/docs/intellij/verifying-plugin-compatibility.html),
[threading model](https://plugins.jetbrains.com/docs/intellij/threading-model.html),
[embedded terminal](https://plugins.jetbrains.com/docs/intellij/embedded-terminal.html).

## Verification

- `./gradlew cleanTest test testIdeCompatibility -PstudioSandboxDirectory=build/api-modernization-sandbox`: **PASS**. Root suite: **1,892 tests**; IDEA 2026.3 integration suite: **73 tests**; no failures or skips.
- The newer-IDE suite includes the real pending-write snapshot retry regression.
- `./gradlew verifyReleaseArchive verifyPlugin -PstudioSandboxDirectory=build/api-modernization-sandbox`: **PASS**. Archive audit checked 1,500 resources and 677 Studio classes.
- Binary compatibility: **Compatible** on IDEA Community 2024.2 (IC-242.20224.300), Community 2024.3.7 (IC-243.28141.18), and IDEA 2026.3 (IU-263.7307.200).
- IDEA 2026.3 now reports **7 deprecated API usages**, down from 19; **13 experimental usages** remain. The seven retained deprecated findings are three editor openings, two save-descriptor constructors, one terminal creation and one navigation-handle read action. The two older boundaries report the same 13 experimental usages and no deprecated usages.

The artifact was built only for upstream development verification, retaining the
upstream plugin identity/version; it is not a new published release. Its SHA-256 is
`627d982606af54affd52d33d2e8eced9ba4e0ccdba48c5cdfbe9cbb85d01992b`.
Reports are under `build/reports/tests/`, `build/reports/pluginVerifier/` and
`build/reports/release/`. Interactive installation/unload, terminal focus and
cross-platform workflows still need candidate-specific manual verification.
The retry regression uses a real pending EDT write to cancel background snapshot
preparation and checks that the restarted read publishes exactly one fresh snapshot
per artifact.
