package org.ikasan.studio.core.generator;

import java.util.Map;

/** Project-local skill discovery files; create missing files without replacing developer customisations. */
public final class StudioAiSkillTemplates {
    public static final String WORKFLOW_PATH = ".agents/skills/ikasan-integration-workflow/SKILL.md";
    public static final String CLAUDE_PATH = ".claude/skills/ikasan-integration-workflow/SKILL.md";
    private StudioAiSkillTemplates() { }

    public static Map<String, String> files() {
        return Map.of(WORKFLOW_PATH, workflow(), CLAUDE_PATH, claudeEntry());
    }

    public static String workflow() {
        return """
                ---
                name: ikasan-integration-workflow
                description: Build, change, troubleshoot or review Ikasan application flows managed by Ikasan Studio, using the live model or proposal files and verifying runtime behaviour. Use for integration briefs and component demonstrations, not development of the Studio plugin itself.
                ---
                
                # Ikasan integration workflow
                
                Apply this workflow to the requested application task. A review request is read-only unless
                repairs are authorised. Honour an explicit diagram-only or batch scope; do not expand a small
                change into a full-module rewrite or an every-component demonstration.
                
                Paths below are relative to the application project root, not this skill directory.
                
                ## Establish the contract
                
                Read `AGENTS.md` and the relevant sections of `generated/IKASAN_STUDIO.md`. Obtain the current
                model and selected catalogue through `studio_snapshot` / `studio_catalogue`, or read the saved
                `generated/src/main/model/model.json` and neighbouring `component-catalogue.json` without MCP.
                If Studio is not configured, help the developer complete that step before proposing edits.
                Use the generated contract for operation syntax and current capabilities; do not assume that
                older limitations still apply. For unfamiliar APIs, follow the catalogue's version-specific
                `frameworkReference` and inspect matching interfaces, implementations and tests.
                
                Translate the brief into a small acceptance inventory: requested path, payload, observable
                outcome, dependencies and evidence needed. Resolve genuinely consequential missing decisions;
                use stated, simple sample assumptions for an underspecified demonstration. For an every-component
                request, compare exact executable catalogue keys with the resulting model, and distinguish
                containers, endpoint decorations and exception policies. Do not equate palette coverage with delivery.
                
                Reread `LOCAL_TEST_ENVIRONMENT.md` whenever test settings are needed. Accept a partial
                name=value block and literal local-test passwords; do not require a READY flag or every
                optional property. Supplied values override catalogue defaults; preserve existing unspecified
                settings. Ask only for missing required details. Use passwords only in necessary configuration,
                not chat/logs; environment references are optional and require supported runtime binding.
                For SFTP, select password OR key authentication, resolve existing key/known-host paths, and
                map the supplied directory to both ends. Check current generated guidance for these rules;
                do not carry forward an older references-only restriction. Verify real delivery with the
                developer's settings and continue independent work if an essential prerequisite is missing.

                ## Design and apply through Studio
                
                Trace payload types across the whole path. For paired transports, match endpoint identity,
                queue/topic mode or directory/filename semantics, and post-consumption behaviour. Place related
                flows together when requested. A visual connector is not evidence of a working transport.
                
                For routing, define branch outcomes, fallback behaviour and payload ownership before implementing
                router code. Exercise all requested branches and fan-out recipients; test isolation when payloads
                may be mutable. For exception policies, choose the specific exception and requested action, then
                verify the observable recovery/exclusion outcome and later valid delivery. Use the current
                catalogue rather than inventing action parameters or claiming routers require manual editing.
                
                Submit coherent, bounded operations through Studio. Read proposal status: await review only
                when required, and await successful generation before dependent work. Without MCP, use the
                hash-bound proposal-file format in the generated guide and confirm the saved result. Never
                repair a rejected proposal by editing the open model or generated Java directly. On stale state,
                reread and reconcile; on validation/generation failure, diagnose that failure before resubmitting.
                Do not blindly replay the same proposal or report it applied merely because a file was written.
                
                Inspect required beans and generated `user/` scaffolds. Complete authorised implementations;
                preserve existing developer logic. Verify registration, injection names, scanning and payload
                contracts against actual code. Unavailable external paths may be explicitly left MANUAL with
                prerequisites explained through Studio; they remain incomplete, not successful demonstrations.
                
                ## Verify the ESB behaviour
                
                Use evidence appropriate to the requested scope. For working integrations, progress from
                compilation to focused component tests, generated-application startup and actual delivery.
                Load the real generated factories; test-only substitute beans do not prove production wiring.
                
                Verify the normal Studio **Run module** configuration separately from custom launchers that
                change profiles, startup modes, directories or infrastructure. Where IDE interaction is unavailable,
                exercise the equivalent configuration and identify the remaining interactive check explicitly.
                Check expected versus actual state for every affected flow. Respect existing operator startup
                settings; do not erase persistence or force global overrides to make the test appear successful.
                
                Normal ESB flows, including demos, remain running and ready for later work. Verify first delivery,
                an idle interval and later delivery in the same application without bean reset or flow/application
                restart. For paced sources, observe separated batches and continued readiness. A finite provider
                returning null terminates an Event Generating Consumer; it is not an idle signal. Reserve finite
                fixtures for bounded tests or an explicitly requested batch. Never fake running state, busy-loop
                or hide errors. Verify worker cancellation, stop/start and normal application shutdown when those
                lifecycles are affected; forced process exit is not evidence of clean shutdown.
                
                Use authorised local services and isolated test data. Do not reuse the running project's ports
                or absolute data paths accidentally. Use bounded waits and report the failing stage. For file
                transports, check publication, receipt and consumption handling rather than filenames alone.
                Clean up only resources owned by the test within its permitted scope. Stop at missing permission,
                credentials or external access instead of widening the task; finish unblocked work.
                
                ## Report evidence and remaining work
                
                Reconcile the acceptance inventory with the resulting model and code. For each requested path,
                distinguish implemented, compiled, component-tested, runtime-delivered and still blocked work.
                Give repeatable launch/input/observation instructions and expected long-lived flow states.
                Do not label an unexplained Stopped flow or an unconfigured transport a completed success.
                For reviews, report findings and evidence first; do not change the reviewed project silently.
                """;
    }

    private static String claudeEntry() {
        return """
                ---
                name: ikasan-integration-workflow
                description: Build, change, troubleshoot or review Ikasan application flows managed by Ikasan Studio and verify runtime delivery and continued readiness. Not for developing the Studio plugin itself.
                ---
                
                # Ikasan integration workflow
                
                Read `.agents/skills/ikasan-integration-workflow/SKILL.md` from the application project root
                and follow that shared workflow for the current task. This entry enables Claude Code discovery;
                the shared file contains the maintained instructions for both MCP and proposal-file use.
                Respect review-only requests and existing project ownership and approval rules. If the shared
                file is missing, report it and use `AGENTS.md` and `generated/IKASAN_STUDIO.md`; do not invent
                operations or bypass Studio. No additional tool permissions are granted by this skill.
                """;
    }
}
