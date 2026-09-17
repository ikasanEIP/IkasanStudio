# AI bridge implementation handover — 17 September 2026

## Current state after stash recovery — 17 September 2026

This section supersedes the historical Python-adapter and unfinished-work notes below.

The sandbox failed to load because plugin.xml declared `ikasanstudio.native-mcp`, but the
native module was absent. The recoverable popped stash (`b214be9d24649b246ed3cd28029588a46297f8c2`)
already contained empty native-toolset/adapter files and omitted the Gradle integration.
Recovered the missing source from recorded edits and the surviving working distribution,
preserving unrelated checkout changes and the existing Git index. Recovery copies are in
`/tmp/ikasan-recovery`; this directory is temporary, so the working tree is the durable source.

- `settings.gradle.kts` includes `:native-mcp`; the root build uses
  `pluginModule(runtimeOnly(project(":native-mcp")))`. Do not use pluginComposedModule here.
- The native module descriptor must be bundled as `ikasanstudio.native-mcp.xml` in
  `lib/modules/ikasanstudio.native-mcp.jar`. It uses the public marker
  `intellij.mcpserver.terminal`; `intellij.libraries.kotlinx.io` is internal and rejected at runtime.
- The standalone adapter is Java, built by `mcpAdapterJar` and bundled under `studio/ai`.
  It uses the running IDE's Java executable. Stable project connection paths and rotated
  session files replace the previous Python/temp-directory implementation.
- The connection dialog retains the seven spaced steps, bold inline settings button,
  translated wording, aligned checklist, and automatic reopening preference.
- `verifyReleaseArchive` now rejects missing declared content-module descriptors.
  Verified against a temporary ZIP with the native module removed.
- Full Gradle test suite and release archive audit passed. The normal Run Plugin sandbox was refreshed. A fresh headless IDEA 2026.2.2 startup passed,
  registered all four Studio MCP tools, and reported no invalid/missing descriptor errors.
  This is plugin-loading verification, not a new manual AI-client interaction test.

Startup verification command:

```sh
JAVA_TOOL_OPTIONS=-Djava.awt.headless=true ./gradlew runIdeModern --args='traverseUI /tmp/ikasan-recovery/smoke-options true' -PstudioSandboxDirectory=/tmp/ikasan-recovery/smoke-sandbox --no-configuration-cache
```


## Resume completion — 17 September 2026

The implementation and automated verification below are now complete. The remaining sections retain the earlier handover as history; the interrupted edits and automated checks listed there are no longer pending.

- Bound proposal publication and snapshot revision registration to the originating bridge session, preventing old work from surviving a stop/restart.
- Added regression coverage for restart during proposal validation, missing HTTP authentication, valid-token requests with an Origin header, and revision isolation between services.
- Added known payload-mismatch rejection and router, decorator, and exception-resolver snapshot round-trip coverage. Fixed snapshot capture for resolutions with null action-property maps. The legacy resolver fixture is normalised to the action name used by loaded Studio models.
- `./gradlew test validateMetaPacks buildPlugin --no-configuration-cache` completed successfully after the final code changes. `git diff --check` passed.
- Plugin artifact: `build/distributions/ikasanstudio-1.0.0.zip`. No commits or staging changes were made.
- Manual live-IDE/AI-client and light/dark visual verification remain outstanding. The initial operation scope remains complete linear flows; redaction remains limited to recognised credential field names, as documented in StudioAiBridge.md.

User paused work to restart Unix. Resume implementing and verifying this feature; it is not yet declared finished.

## User request and constraints

Implement the agreed live-model AI workflow: read the in-memory Studio design and selected meta-pack catalogue; submit structured operations; validate and preview; developer applies in Studio as one undoable model change; detect stale proposals; persist/regenerate through existing Studio APIs. MCP should allow an AI embedded in IntelliJ to use this without modifying model.json behind Studio.

Read root AGENTS.md. Another Codex is working in the same checkout, including the newer IDE sandbox configuration. Preserve unrelated changes and staging. Do not spawn agents unless expressly requested. The previous sandbox chooser workaround and newer IDE configuration are separate work.

## Implemented, present on disk

- `headless/studio-generator/.../core/ai/LiveModelSnapshot.java`: copies the live model into a detached value graph on EDT; JSON encoding and validation happen off EDT. Includes properties, unknown fields, routes/transitions, decorators, exception resolvers.
- `.../core/ai/ModelProposal.java`: prepares operations against a detached candidate. Supports addFlow, addComponent, setProperty, connect (complete ordered component-name list). Initial scope is complete linear flows; router editing, deletion, renaming, exception-resolver changes and migration are explicitly unsupported. Validates metadata, required properties, types/choices/regex, generated-name collisions, payload mismatches, conversion recipe IDs/types, and persisted-model round-trip. Materialises defaults and resolves placeholders. Reversible ChangeSet retains original objects for existing flows/components.
- `src/main/java/.../intellij/ai/StudioAiService.java`: opt-in project service, snapshot revisions (last 8), proposals/statuses (last 16), one pending review, freshness/pending-property/generation/migration checks. Apply uses existing persistence/generation API and a global UndoableAction. Immediate save failure rolls model back; asynchronous generation failure leaves applied model and reports generation_failed. Undo remains available after bridge stop. Canvas/property refresh and runtime restart-required indication are wired.
- `StudioAiConnectionAction.java`, `StudioAiProposalDialog.java`: Tools/Find Action setup dialog, copy config/stop buttons; nonmodal proposal review with operation details and Apply/Cancel.
- `StudioMcpProtocol.java`: initialize, ping, tools/list, tools/call, notifications, batches; four tools: studio_snapshot, studio_catalogue, studio_propose, studio_proposal_status. Negotiates MCP 2025-03-26.
- `src/main/resources/studio/ai/studio_mcp.py`: standard-library-only Python stdio adapter. Private loopback HTTP RPC to IDE, token authentication, proxy disabled. Java server rejects Origin headers and >1 MiB bodies. Config/token stored in private temporary directory under IDE system directory, not project. Stop invalidates server/token. This is MCP stdio, NOT a public Streamable HTTP endpoint.
- plugin.xml action registration; aligned English/Japanese bundle strings.
- AiProjectContractGenerator and archetype AGENTS guidance updated to prefer live bridge while Studio is open, close Studio before offline edits. Catalogue now includes validation/conditional-required metadata.
- README, docs/AiFriendlyProjects.md updated; docs/StudioAiBridge.md contains setup, scope, protocol example, lifecycle, privacy and failure semantics.
- Tests: ModelProposalTest, StudioMcpProtocolTest, StudioAiServiceTest.

## Verification so far

- compileJava passed.
- Core/protocol/contract focused tests passed after correcting an immutable test fixture and synchronising archetype instructions.
- StudioAiServiceTest (2 tests) passed: actual Python subprocess talking MCP initialize/tools-list to actual local HTTP server; preview does not mutate; stale/pending-property rejection; immediate-save rollback; one registered global undo with undo/redo and reload protection; stopping bridge preserves undo.
- git diff --check passed at last check.
- Started `./gradlew test validateMetaPacks buildPlugin --no-configuration-cache` (PTY session 19797). Last sampled output had full tests running; completion was not collected before interruption. Logs/report files may contain final outcome. Just before writing this note, XML reports for ModelProposalTest (6 tests) and StudioAiServiceTest (2 tests) showed zero failures, timestamps 2026-09-17T08:25:44/52Z. Do not infer full-suite/build success from these two reports.
- No manual live-IDE/AI-client or light/dark visual verification performed yet. Do not claim it was.

## Interrupted edit NOT applied

The final Python edit command was aborted while awaiting permission (sandbox helper failed). A subsequent rg confirmed the following intended changes are NOT on disk:

1. Capture `HttpServer session = server` at start of service.call; pass it to propose; before opening preview on EDT require `server == session`. Existing code checks isRunning/disposed only. This closes stop/restart race where an old in-flight validation could open a preview in a new bridge session.
2. Add ModelProposalTest.snapshotRoundTripsRoutersDecoratorsAndResolvers using TestFixtures.getEventGeneratingConsumerRouterFlow, getEventGeneratingConsumerCustomConverterDevNullProducerWithWiretapsFlow, getExceptionResolverFlow. Compare JSON trees from original versus deserialize(JSON of LiveModelSnapshot.capture()). Important to verify preservation of unrelated advanced flows.
3. Add known payload-mismatch test inserting Converter into the FTP test flow with fromType java.lang.String, toType java.lang.Integer, userImplementedClassName WrongType; require rejection (confirm actual error wording).
4. Add HTTP missing-auth/origin rejection and revision isolation checks to StudioAiServiceTest. Existing tests exercise successful transport but not these boundaries. Better origin test should send VALID token plus Origin and assert 403, independently of missing-token test. A second service should reject a revision issued by the first.

## Remaining work

- Inspect full test/build outcome. Finish the above small lifecycle fix and meaningful regression tests.
- Review snapshot fidelity, model/undo semantics, sensitive-field redaction limitations and any newly surfaced failures. Keep implementation scope documented accurately (linear flows only).
- Review service lifecycle/threading: start now checks readiness on EDT BEFORE taking its startup monitor, avoiding invokeAndWait/stop deadlock; already-running start returns config without readiness check so Stop is accessible during pending edits. Stop removes connection file asynchronously; script/temp directory remain. Could clean these if useful, but avoid unnecessary scope.
- Full suite + validateMetaPacks + buildPlugin are required after final changes; tests are appropriate for model mutation. Do not repeatedly run broad checks without changes/failures.
- Check plugin ZIP location under build/distributions and final git diff --check. Do not commit or alter staging unless requested.
- Final reply should explain how to use Tools → Connect AI to Ikasan Studio, Python 3 requirement, preview/apply/undo, first-version linear-flow scope, tests, and honest manual verification limits.

## Tool/environment notes

Sandbox often fails with `bwrap: loopback: Failed RTM_NEWADDR: Operation not permitted`. apply_patch additions often succeeded; updates often failed. Approved-prefix perl edits worked. For required commands failing sandbox, rerun via exec_command with require_escalated and a concrete justification; do not bypass approval. The last aborted script did not run, so reconstruct only the needed edits.

User can resume by saying: “Continue the AI bridge implementation using docs/AI_BRIDGE_HANDOVER.md.”
