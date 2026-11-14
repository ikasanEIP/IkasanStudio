# Import a module from runtime metadata and a source checkout

**Tools → Ikasan Studio → Import Module…** guides the transfer into an existing Studio
Maven project. It combines runtime acquisition, local source review and local
configuration. This workflow needs no AI service, Copilot connection or internet-based
source analysis. Use a fresh destination project when evaluating an existing application.

## 1. Fetch and review the runtime model

Select **Import an Ikasan module**, then choose **Fetch from running module…**, supply its URL and registered module name, then
select the matching meta-pack and application package. Review the topology/configuration
import as usual. See [runtime JSON import](IkasanRuntimeJson.md) for endpoints, credentials
and version matching. The wizard enables **Review source code from the original project**. This optional step also applies when importing a Studio model. **Import local configuration** is a separate optional follow-up for either import type; when both are selected, properties follow source review. Source review opens only after model validation and generation succeed.

Runtime import commits the model and generates Studio-owned files. Cancelling a later
source or configuration stage does not undo that model import. No stage starts or stops
the running source module.

## 2. Review the original source checkout

Choose the original Maven project or reactor directory containing `src/main/java`.
Browse starts comparison automatically. For a typed/pasted path, press Enter or leave
the field; press Enter again to refresh after edits. Directory errors appear inline.
The checkout stays unchanged. Analysis reads local files and does not execute
application classes, Maven plugins, builds or scripts.

Before defaults are selected, Studio compares the imported model's exact, case-sensitive
module name with `module.name` declarations in source properties and the saved model
of a Studio checkout. Generated source properties/models are read only for identity;
generated implementation files remain excluded from transfer. Folder names and Maven
artifact IDs are hints, never proof of identity. Referenced implementation matches are
shown as additional evidence.

A unique matching application in a reactor is selected automatically. Otherwise choose
the source module before comparison. Sibling module code/properties/dependencies are
excluded; verified local-parent POM declarations are retained. Mismatched names, missing
names, external placeholders and conflicting profile names require **Use this checkout
anyway** before selection/import is enabled. Approving restores the normal default
selections. The completion report records identity evidence and any explicit override.
Changes to the imported model or checkout identity metadata require a fresh comparison.

The **Classes and Dependencies** table shows original-source evidence on the left and proposed destination paths on the right. Exact file matches are hidden by default; use **Show identical files** to reveal them. Dependency rows, unresolved wiring/scan issues and pending edits remain visible. Equality is cached during background analysis:

- Exact implementation types referenced by generated `@Resource` fields and matching
  original `@Bean` methods seed the recommendations. Explicit imports, AST type references,
  same-package helpers, used wildcard imports and static imports extend the source set.
  This is conservative static analysis, not complete Java symbol or dependency resolution.
- Java files retain their package, original bytes, comments and line endings beneath
  `user/src/main/java`. Files start without pending copies. Use **Copy all** in the
  middle column to stage a complete original file, or copy individual changes in its diff.
  Review original Spring wiring and flow debug subclasses carefully before copying.
- Original Spring configuration and classes containing `@Bean` methods are unselected by
  default. The original Spring Boot application is excluded because Studio supplies startup.
  Copying an old configuration wholesale can duplicate Studio's native components and beans.
- Literal `@PropertySource("classpath:…properties")` resources on recommended source classes
  are offered at the same path beneath `user/src/main/resources`. Canonical application
  properties are reviewed separately for local configuration.
- Ordinary additional direct Maven dependencies are selected by default. Original
  explicit versions, exclusions, scopes and optional flags are retained. An omitted
  version remains omitted and uses destination dependency management, so JAXB API/runtime
  declarations do not require a separate decision just because their version is inherited.
  Existing declarations are not duplicated. Conflicting source-module versions, unresolved
  expressions, non-portable system dependencies and platform-baseline declarations remain
  manual review items with disabled copy actions. Double-click their description to
  inspect the declarations; the importer does not guess versions or copy local JAR paths.
  Original parents, BOMs, repositories, profiles and plugins are not automatically copied.

Double-click a row or press Enter to open IntelliJ's native diff. The original is on
the left. For approved Java source files, the destination on the right is an editable
preview: use the normal diff arrows to copy individual changes, or edit it directly.
Click **Save preview** to keep the diff edits in memory and stage that row for import.
**Cancel** or closing the comparison discards edits from that comparison session,
preserving any previously staged copy. Saving a preview does not write project files.
Reopening the file starts from the current staged preview. The middle button changes to **Revert** after a whole-file copy or
partial edit. Revert restores the starting destination preview and removes that file
from the pending import; manually restoring the starting text also clears the edit. Cancelling or changing checkout discards staged edits. Copy all or Save preview authorises replacement of that file. No source/destination files are written until
**Perform Actions**. Whole-file imports without staged edits retain original bytes.

Properties resources and dependency comparisons remain read-only.
Dependency rows compare the selected declaration with its current destination counterpart,
including unresolved/manual-review declarations. Opening a comparison alone does not
change import selection. The middle column contains the copy/revert actions.

`DebugTransitionComponent` and `DeepCopyUtil` in their exact Studio packages appear in
**Studio support files**, immediately after **Classes and Dependencies**. They are
supplied for the destination meta-pack and start without pending copies. Exactly identical
files are hidden by default; select **Show identical files** to reveal them. The equality
check is cached during background analysis and compares original bytes, including comments,
formatting and line endings. Matching source/destination files are hidden even when they differ
from the raw generated template (for example after IntelliJ shortens imports). Missing or
unverified destination files and actual source/destination differences stay visible,
as do files with pending edits. The tab offers
the same **Copy all**, **Revert** and editable diff with **Save preview**/**Cancel**.
Use a partial merge for useful custom debug code, taking care to retain target-version
API compatibility. Customer classes with the same simple names remain normal source choices.
Destination files are compared with templates rendered from the saved model. Formatting
and comments are ignored for classification; the diff retains the exact snapshots.

Copy all or Save preview explicitly authorises replacement of that support file.
Applying a changed support file records its destination in
`user/import-reviews/support-customisations.txt`; normal regeneration then preserves it
as developer code. Keep this file with the project. Untouched support files continue to
be supplied by Studio. To restore template generation for a customised support file,
remove its line from this record and regenerate; that explicitly replaces the local file.
When upgrading Ikasan, review customised support classes against the target APIs.
The saved model, customisation record and source/destination snapshots are rechecked
before applying, and the original checkout remains unchanged.

Only explicitly staged developer-owned files are replaced; changed source, destination, POM or generated bean contracts
invalidate the corresponding review. Reanalyse after edits. Multiple selected source files
cannot target the same destination. File writes are rolled back on a transfer failure.

The scan excludes test sources, generated/build output and symbolic links. It is bounded
to 10,000 visited entries, 64 MiB of relevant input, and 2 MiB per read file. Select a narrower
module directory if the reactor is too large. Reflective/dynamic references, custom composed
Spring annotations, inherited build configuration and non-properties resources require review.

## 3. Complete configuration and wiring

Source review has two tabs: **Classes and Dependencies** and **Studio support files**.
Selected Spring configuration and dependency decisions appear on their rows with a warning icon;
select a row to see the action and source locations beneath the table. Scan failures
without a matching file appear as non-copyable rows in
**Classes and Dependencies**, so they remain visible without another review tab.
Copy, Save preview and Revert update these details in the background. Routine identity,
transfer and property-file inventories stay in the saved import report. Confirmation-only
Spring bean/wiring reminders are also retained in that report rather than shown as
extra rows or warning markers in the source-import screen.

**Copy all** or **Save preview** explicitly chooses the content to replace that destination
file when **Perform Actions** is applied. No extra replacement checkbox is required;
unchecked/unstaged files are untouched and stale-review checks still protect intervening edits.

Finish the wiring under `user/`, in a package scanned by the generated application's
`@ComponentScan`, or add an explicit, narrowly scoped registration there. Classes keep
their original packages; copying a class does not automatically put its package into the
application's component scan. Use the original factory call sites to determine real property
prefixes and configuration resource IDs. Match the required names or supply explicit
aliases/qualifiers. Avoid scanning unrelated original configuration or creating circular
bean dependencies. The importer does not rewrite arbitrary Spring factories or invent
constructor arguments, configuration IDs, business values or placeholder defaults.

After transferring selected files, **Continue to configuration import** offers the discovered
`application.properties`, profile-specific files and other properties in resources or
`config/`. Choose a discovered file, choose **Use Selected Discovered File**, adapt its settings for
local development, then use **Copy all** or double-click the file row and choose **Save preview**. Import only the relevant settings. Refer to
[local configuration import](ProjectFilesAndRecovery.md#import-local-configuration)
for local storage, precedence and Run/Debug configurations.

A value-free review is saved under `user/import-reviews/`. It lists transferred files,
bean contracts, dependency decisions, discovered property-file paths and remaining tasks.
The properties themselves remain in their imported configuration files, not in this report.
Reports can contain customer class names and source paths; keep them with the project.

**Tools → Ikasan Studio → Import Source Code…** reopens source review for an already
imported/generated module. **Import Configuration…** remains available independently.

## Verify the reconstructed application

Refresh Maven after dependency changes and use the JDK required by the selected meta-pack
(JDK 25 for released Ikasan 5.1.0). Compile, package and launch the application with the
intended environment. Class transfer and successful compilation do not prove that its Spring
context or flows work. Check bean names, component scanning, type binding, placeholders,
external dependencies and the original runtime-import review.

Where real external infrastructure is unavailable, record that path as awaiting setup.
Before calling a flow operational, verify first delivery, idle readiness and later delivery
in the same running application without resetting beans or restarting flows. A process that
stays alive while waiting for an unavailable endpoint is not evidence of successful delivery.
