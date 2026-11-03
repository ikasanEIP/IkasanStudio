# Dependency security fixes — 2 November 2026

The project owner authorised Studio-owned dependency fixes and explicitly retained H2 at
the Ikasan core baseline pending an upstream decision. This change preserves all H2
versions, Ikasan BOM coordinates, generated Java baselines and sample business logic.

## Alert coverage

The authenticated GitHub Dependabot API returned 65 open alerts across 11 advisories.
Comparing every affected manifest's direct declaration with the alert's patched-version
floor gives:

| Package | Alert instances | Local result |
| --- | ---: | --- |
| `org.apache.activemq:activemq-broker` | 36 | Updated |
| `junit:junit` | 9 | Updated |
| `com.h2database:h2` | 20 | Retained per owner instruction; unresolved |

These are local fixes awaiting push and GitHub reassessment. No alerts were dismissed or
marked resolved through the API. H2's 20 retained alerts include 10 critical and 10 high
severity instances in the currently reported sample manifests.

## Version choices and ownership

- V3 retains the Java 11 / `javax.jms` API and updates ActiveMQ client and broker from
  `5.19.5` to `5.19.12`.
- V4 retains Java 17 / `jakarta.jms` and updates both artifacts from `6.2.4` to `6.3.3`.
- Released V5 and the independently installable ExampleIkasan5 pack target JDK 25 and
  explicitly override the BOM's older ActiveMQ client/broker with `6.3.3`. The opt-in V5
  development pack receives the same security override; its historical snapshot identity
  and Java baseline remain unchanged. That snapshot's historical Java 26 runtime was not
  recertified by this change.
- Pack manifests record these as narrow, reasoned security exceptions. Matching JMS
  component descriptors carry the same versions, so regeneration updates existing POM
  declarations rather than restoring vulnerable pins. The V4 authoring example's manifest
  follows the updated bundled baseline.
- All nine checked-in sample root POMs use JUnit `4.13.2`; legacy pack POM templates also
  use `4.13.2` instead of `3.8.1`. The archetype already used `4.13.2`.
- Released V5 and ExampleIkasan5 advance their independent pack revisions to `1.0.1`.
  Existing revision-locked V5 projects require explicit **Adopt Installed Meta-Pack
  Revision…** before using the changed revision. The application Ikasan version remains
  `5.1.0`.

Apache's [release list](https://activemq.apache.org/components/classic/download/) identifies
`5.19.12` and `6.3.3` as current maintenance releases. Its
[security advisories](https://activemq.apache.org/components/classic/security) include fixes
beyond the minimum versions in the reported alerts: in particular
[CVE-2026-74761](https://activemq.apache.org/security-advisories.data/CVE-2026-74761-announcement.txt)
requires at least `5.19.11` or `6.3.2`. Stopping at `6.2.6` would leave that issue unresolved.
JUnit documents the compatible [4.13.2 release](https://github.com/junit-team/junit4/releases/tag/r4.13.2).

## Verification

- Full plugin test suite: **1,649 tests**, zero failures, errors or skips.
- Full standalone headless suites: **663 tests**, zero failures, errors or skips.
- Isolated provider tests cover V3, V4 and released V5: generated JNDI configuration,
  normal and XA connection factories, ObjectMessage serialisation allowlists, and producer
  cleanup on success and failure.
- A real local broker/client test verifies first delivery, idle readiness, later delivery,
  transaction rollback/redelivery and explicit cleanup. Each variant uses its own isolated
  dependency classpath.
- The same broker probe was compiled and run separately on JDK **11**, **17** and **25**,
  using Maven-resolved dependency graphs under the exact, unchanged Ikasan **3.3.9**,
  **4.1.6** and **5.1.0** BOMs. Client, broker and openwire-legacy resolve consistently
  to `5.19.12`, `6.3.3` and `6.3.3`, respectively.
- Representative V3 and V4 `routesAndBranches` user/generated reactors compile application
  and test sources on their required JDKs. A freshly rendered released V5 compatibility
  fixture compiles application and test sources on JDK 25. These Maven checks use
  `-DskipTests test`; they are compilation checks, not full module runtime acceptance.
- The independent V5 example passes Maven `verify`, including archive installation and
  restart/discovery checks. The V4 authoring example is also verified against the updated
  locally published V4 baseline.
- The plugin ZIP builds and passes `verifyReleaseArchive`.

The broker tests do not claim full module/ESB runtime certification or resolution of the
previously documented Ikasan 5 XA integration failures. Interactive IDE and externally
connected flow acceptance checks were not run for this dependency-only change.
