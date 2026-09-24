# Ikasan Studio 1.0 Marketplace release plan

Prepared 24 September 2026. This is a proposed four-week plan, with dates driven by
readiness rather than a fixed launch promise. It complements the existing
[release-candidate verification](ReleaseCandidateVerification.md) and
[manual checklist](MarketplaceReleaseManualChecklist.md). No publication is authorised
by this document. Working assumption: a free, open-source release under the repository's
BSD 3-Clause licence; the product owner must confirm this and the publishing organisation.

## Recommended route

Internal ZIP testing → hidden Marketplace submission → review and final acceptance →
public 1.0 launch. Prepare the account and listing now; submit only a release-quality candidate.

The first Marketplace upload must be manual; later updates can use Gradle publishing.
Use unique candidate versions such as `1.0.0-beta.1`, retaining `1.0.0` for the final artifact.
A non-default channel requires testers to add a repository in IntelliJ.
[JetBrains publishing instructions](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html).

For the initial upload, select **Hidden** to prepare the listing and obtain approval before
launch. Hidden plugins remain accessible by direct link: this is not confidential distribution.
A plugin can only be hidden initially, and making the plugin public is irreversible; individual
versions can subsequently be hidden. If a listing already exists, check its status before
choosing this route. [Hidden release](https://plugins.jetbrains.com/docs/marketplace/hidden-plugin.html).

## What we already have, and what remains

| Area | Repository evidence | Work before release |
| --- | --- | --- |
| Packaging | `buildPlugin`, `verifyReleaseArchive`, bundled offline migration tools | Audit the actual candidate, including third-party licences and ZIP contents |
| Compatibility | Plugin Verifier boundaries and both Ikasan packs | Refresh newest stable IDE target; record installation and workflow evidence on supported IDEs/OSes |
| Regression | Plugin/headless tests; `regression-tests/migration/` | Run fresh migration baseline/target reports, including wiretaps and preserved user code |
| Distribution | Gradle signing and publishing configured | Confirm account ownership, signing material, secrets and protected publication process |
| Documentation | Getting started, migration, AI, testing, recovery and diagnostics guides | Clean-machine walkthrough; remove stale wording and publish accurate limitations |
| Listing | Stable ID `com.github.ikasaneip.ikasanstudio`, vendor metadata, icon, README description | Confirm public contact/URLs, screenshots, licence, privacy statement and listing status |

This is a source/configuration review, not release sign-off. Account state, repository secrets,
branch protection, signing credentials and Marketplace ownership have not been inspected.

## Week 1 — ownership, scope and publishing controls

**Owner: product owner for identity and decisions; maintainer for engineering.**

- Confirm vendor organisation, primary publisher and backup administrator; check whether this
  plugin ID already has a Marketplace listing. Establish a monitored support contact.
- Prepare the vendor profile, accept the Developer Agreement through an authorised person,
  confirm licensing/source link, and complete the requested trader/non-trader declaration.
  [Upload process](https://plugins.jetbrains.com/docs/marketplace/uploading-a-new-plugin.html),
  [approval requirements](https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html).
- Freeze the intended 1.0 scope. Classify outstanding issues as release blockers or deferred
  improvements. Include AI with/without MCP, migration, test generation and remote-file browsing
  explicitly in the supported-feature list.
- Before using GitHub releases, harden `.github/workflows/release.yml`: add a protected publication
  environment/manual approval, check tag equals embedded version, fail if required signing inputs
  are absent, and associate release with successful gates for the same commit.
- Change the pipeline to promote an immutable verified artifact. Currently it rebuilds and patches
  release notes during publication; the resulting ZIP need not match the previously tested one.
  Finalise notes before candidate creation. Record unsigned and signed hashes, verify signing
  preserves payload, and install-test the exact author-signed ZIP selected for upload.
- Configure `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD` and, for later automated
  uploads, `PUBLISH_TOKEN` in protected secrets. Keep recovery ownership and certificate expiry
  documented separately from secret values. Check signing actually ran: Gradle may skip signing
  when inputs are absent. [Signing documentation](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html).

**Exit:** named release owner, confirmed distribution model and a publication path that cannot
silently bypass candidate approval. Workflow changes above are planned, not yet implemented.

## Week 2 — release candidate and realistic testing

**Owner: maintainer, with named testers for Windows, macOS and Linux.**

Build from a clean, recorded commit with a unique candidate version:

```sh
./gradlew -p headless cleanTest test
./gradlew cleanTest test buildPlugin verifyReleaseArchive verifyPlugin
```

Follow the linked verification documents for exact IDE boundaries and evidence. In particular:

- Install from disk in clean profiles; create a project without relying on a developer's Maven cache.
  Confirm the published archetype and mediator dependencies resolve from the documented repositories.
- Exercise both packs, generation, Run/Debug, Console, properties, routers, exclusions, wiretaps,
  local transport harnesses, remote browsing, flow tests and offline-tool export.
- Exercise MCP on a supported modern IDE and file proposals without MCP, including approval,
  deletion confirmation and developer-source protection. Verify older IDEs load without native MCP.
- Run the migration fixture before/after 3.3.9 → 4.1.6, plus Restore and an existing-project upgrade.
- Cover two open projects, cancellation, editor reopen, indexing, unavailable endpoints, uninstall
  and reinstall. Treat Studio-attributed threading/lifecycle errors and unexpected user-code loss
  as blockers. Record light/dark, scaling and keyboard checks.

**Exit:** candidate ZIP/hash, automated reports and completed manual matrix; no unresolved blockers.
Changes after testing require a new candidate and proportionate re-verification.

## Week 3 — listing and Marketplace review

**Owner: product owner/publisher, with maintainer answering technical questions.**

Prepare a short English description explaining visual design, generated/user ownership,
AI assistance and migration. Use the existing demo videos and a small set of clean screenshots.
State prerequisites, supported Ikasan/IDE versions, external services and known limitations.
Keep Blue Console distinct from Dashboard. Check the icon on both themes and all public links.

Review privacy wording against actual behaviour: optional AI model sharing, user-submitted
error reports, diagnostics, harness downloads, remote file access and credential handling.
The current diagnostics introduction predates parts of the AI bridge and needs reconciliation
with `StudioAiBridge.md`; avoid an unqualified claim that model data never leaves the machine.
Provide the applicable privacy policy if personal data is collected, as required by the
[approval guidelines](https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html).

Manually upload the signed candidate, select the intended vendor/channel and Hidden flag,
complete listing details and supply concise reviewer setup instructions with disposable test data.
Do not publish the GitHub release merely to create the first Marketplace entry: our present release
workflow reacts to both `prereleased` and `released` events. The channel comes from `pluginVersion`,
not the GitHub prerelease checkbox: `1.0.0` maps to default, `1.0.0-beta.1` to beta.

Every new plugin and update is reviewed. JetBrains gives no guaranteed review time and advises
contacting Marketplace support if there is no response after 3–4 working days. Reserve a week
for review/questions as our planning allowance, not a service commitment.
[Review process](https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html).

**Exit:** approved candidate/listing and completed beta feedback. A changed final 1.0 artifact
still needs its own verification and Marketplace approval; beta approval does not cover it.

## Week 4 — public launch and support

**Owner: product owner signs off; publisher executes; maintainer monitors.**

1. Freeze final `1.0.0` metadata, notes and candidate; complete evidence/sign-off for that artifact.
2. Upload the final version hidden, obtain approval, then explicitly make it available on the
   default channel and unhide the plugin when ready. Verify both settings; removing Hidden alone
   does not move a beta-channel build to the default channel.
3. Install from the actual Marketplace listing in a fresh supported IDEA profile. Verify first-run
   project creation and an upgrade from the beta. Record the delivered version and installation result.
4. Publish matching GitHub release notes/assets without accidentally uploading the same version
   twice. Confirm support links, troubleshooting and supported versions are visible.
5. Monitor Marketplace reports and support daily for the first week. Prepare `1.0.1` for urgent fixes.
   If a bad version escapes, hide that version and give explicit recovery instructions; hiding it
   does not uninstall or downgrade existing users. Preserve previous artifacts and migration backups.

**Launch decision:** named owner accepts the candidate evidence, published limitations and support
arrangements. Passing CI alone is insufficient. If review or testing slips, move the announcement.
