# Failure-injection testing

Item 10 tests deliberately damaged input, failed writes, unavailable services and lifecycle races. Run `./gradlew test` with JDK 17. The test runtime includes the Vintage engine because IntelliJ's JUnit 3/4 platform fixtures otherwise compile but are silently skipped by Jupiter.

All injected writes use temporary test directories or disposable IntelliJ projects. HTTP tests use loopback and ephemeral ports. The Maven test uses an empty temporary repository and `-o`; it does not clear the developer's Maven cache or contact a repository. It is skipped, with a reason, when `mvn` is unavailable. No test fills the workstation disk or changes permissions on the working project.

## Coverage and acceptance checks

| Failure | Automated exercise | Checks |
| --- | --- | --- |
| Malformed/truncated `model.json` | `ModuleDeserializerSafetyTest`, `ProtectedModelFileWriterTest` | Reject malformed, truncated and empty input; retain the original bytes; offer only validated backups; preserve rejected primary during recovery. |
| Missing meta-pack resource | `GeneratorFailureInjectionTest` | A nonexistent component resource produces a recoverable build exception. |
| Broken/missing template | `GeneratorFailureInjectionTest`, `GenerationTransactionManagerHeavyTest` | FreeMarker fails after beginning output without returning partial content; invalid generated Java rejects the staged batch before files change. |
| Read-only project | `ProtectedModelFileWriterTest`, `GenerationTransactionManagerHeavyTest` | Inject `AccessDeniedException` at the writer/commit boundary; existing content remains intact. |
| Disk full | Same writer and transaction tests | Inject ENOSPC before writing and after a partial temporary-file write; inject failure after two transaction writes. Preserve model and backup, remove partial files, restore earlier artifacts and allow retry. |
| Maven offline/resolution failure | `MavenOfflineFailureInjectionTest`, `LaunchApplicationActionTest` | Real offline Maven invocation fails with an empty repository; model and developer source are unchanged. A failed generation future prevents launch and reports once. These are separate boundaries, not an end-to-end IDE Maven import test. |
| Indexing in progress | `RunDuringIndexingHeavyTest` | Real IntelliJ dumb mode defers run-configuration work; disposing the service expires the deferred action without a late callback. |
| Project closes during background work | `GeneratedProjectSynchronizerFailureInjectionTest`, `FlowErrorMonitorServiceTest`, `LaunchApplicationActionTest` | Cancel queued generations and release their guards; reject late REST state and late launch callbacks; handle executor shutdown. |
| Port occupied | `TestFtpServerServiceTest` | Bind a real external listener; start fails without closing or taking ownership of that listener. |
| Harness exits immediately | `TestMailServerFailureInjectionTest` | Launch a real immediately failing Java process; inject its non-listening state and advance the startup-grace clock. Drop ownership without attempting to stop a later external listener. A process registered after service disposal is stopped immediately. |
| Running module unavailable/slow | `ModuleControlFailureInjectionTest`, `FlowErrorMonitorServiceTest` | Exercise HTTP 503, a closed listener and a server that never sends headers; verify response timeout, retry and a Swing heartbeat while the request is pending. |
| REST malformed data | `ModuleControlClientTest`, `ModuleControlFailureInjectionTest` | Reject truncated/null/missing/duplicate flow data and trailing JSON; accept a valid response on the following poll. Unknown extension fields remain supported. |
| Simultaneous save/generation | `ProtectedModelFileWriterTest`, `GenerationTransactionManagerHeavyTest`, `GeneratedProjectSynchronizerFailureInjectionTest` | Concurrent model saves retain distinct valid backups; model saves overlap a VFS generation transaction without changing developer files; superseded queued requests wait for their replacement and terminate on project closure. |

## Safeguards strengthened

- Serialize model writes and recovery for the same normalized path within the IDE process, including backup rotation.
- Bind generation futures to project disposal and reject obsolete callbacks. Overlapping generation requests promote the replacement to full generation so changes from an earlier flow scope are retained.
- Check lifecycle/model identity again after REST work before publishing state; cancel queued immediate polls before adding another.
- Stop late-registered owned harnesses after disposal and reject late probe results.
- Defer index-dependent run setup until smart mode and suppress callbacks after project disposal.
- Preserve IntelliJ's LF document invariant and generated UTF-8/CRLF file content. Avoid loading an editor document solely to write an unopened file.

## Remaining release exercises

Automated fault injection verifies the named boundaries, not every operating system failure mode. In a disposable `runIde` project, manually verify notifications, continued editing, retry and recovery for each case above. In particular:

1. Use a read-only directory and a quota-limited disposable filesystem for genuine permission/ENOSPC errors. Keep a separate copy of the model and developer files. Compare them afterward; test persistent failure during rollback as well as the initial write. The automated transaction test injects a single write failure and permits rollback; it does not prove recovery when every rollback write also fails.
2. Enable Maven offline mode in IntelliJ, invalidate a disposable dependency, import and run. Check the normal Maven diagnostics and recovery after returning online. The CLI test does not exercise IntelliJ's importer UI.
3. Launch a deliberately failing MailHog replacement from the sandbox terminal; check the startup warning and retry controls. The automated process test uses a failing Java executable and an injected socket probe, not a MailHog installation.
4. Close a sandbox project during download, indexing, generation and runtime polling; reopen it and compare model/user files. Check that no terminal, browser, notification or stale canvas update appears after closure.
5. Observe both light and dark themes and verify that keyboard interaction continues while failures are reported. The automated Swing heartbeat checks the tested background path, not every designer operation under a slow filesystem.

Model locks are process-local and use normalized paths; they are not cross-process filesystem locks. Keep generation and recovery inside one Studio process. Transaction rollback failures are reported with suppressed causes; retain the original project copy when testing persistent filesystem failure.
