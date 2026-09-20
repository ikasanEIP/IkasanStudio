# Duplication review

Run `./gradlew cpdReport` to inspect substantial repeated blocks in the plugin and headless generator's production Java. The report is written to `build/reports/cpd/duplicates.txt`.

The task uses PMD CPD 7.27.0 with a minimum of 120 tokens. It is optional and is not wired into `check`. Duplicate matches do not fail the task; tool errors still do. Analysis dependencies are isolated from the plugin's runtime dependencies. The first run downloads these tools through Gradle's configured repositories.

Tests, generated output, and version-specific meta-pack resources are outside this report's scope. Similar version templates can be intentional compatibility boundaries. A clean report is not proof that all duplicated behaviour is absent.

Review matches for shared behaviour that ought to change together. Prefer a small focused helper where it prevents inconsistent bug fixes. Leave trivial constructors, validation guards, and readable template wrappers alone unless consolidation offers a concrete benefit. Duplication reduction is primarily a maintenance improvement; measure runtime performance separately.

The September 2026 efficiency pass indexed JMS consumers by destination, connection factory, provider URL and queue/topic mode; shared test-injection background execution and error handling; reused its JSON mapper; and precompiled diagnostic log patterns. No persistent model cache was introduced, so property edits are reflected on the next connection calculation.

At the 120-token threshold, the initial report after this pass found two groups: context-menu construction and the short consumer/debug-session validation shared by the test-message actions. These are review candidates, not required cleanup.

See the [official CPD documentation](https://pmd.github.io/pmd/pmd_userdocs_cpd.html) for detector options and limitations.
