# Import a module from runtime metadata and a source checkout

**Tools → Ikasan Studio → Import Module…** guides the transfer into an existing Studio
Maven project. It combines runtime acquisition, local source review and environment
configuration. This workflow needs no AI service, Copilot connection or internet-based
source analysis. Use a fresh destination project when evaluating an existing application.

## 1. Fetch and review the runtime model

Select **Import an Ikasan module**, then choose **Fetch from running module…**, supply its URL and registered module name, then
select the matching meta-pack and application package. Review the topology/configuration
import as usual. See [runtime JSON import](IkasanRuntimeJson.md) for endpoints, credentials
and version matching. The wizard enables **Review source code from the original project**. This optional step also applies when importing a Studio model. **Import properties for shared settings or environments** is a separate optional follow-up for either import type; when both are selected, properties follow source review. Source review opens only after model validation and generation succeed.

Runtime import commits the model and generates Studio-owned files. Cancelling a later
source or configuration stage does not undo that model import. No stage starts or stops
the running source module.

## 2. Review the original source checkout

Select the original Maven project or reactor directory containing `src/main/java`, then
choose **Analyse Source**. The checkout stays unchanged. The analysis reads local files;
it does not execute application classes, Maven plugins, builds or scripts.

The **Classes and Dependencies** table shows original-source evidence on the left and proposed destination paths on the right:

- Exact implementation types referenced by generated `@Resource` fields and matching
  original `@Bean` methods seed the recommendations. Explicit imports, AST type references,
  same-package helpers, used wildcard imports and static imports extend the source set.
  This is conservative static analysis, not complete Java symbol or dependency resolution.
- Java files retain their package, original bytes, comments and line endings beneath
  `user/src/main/java`. New non-wiring implementation/helper sources are selected by
  default, including domain classes not proven necessary by static analysis. Existing
  destination files, ambiguous duplicates, original Spring wiring and flow debug
  subclasses require explicit selection. Review the defaults and deselect unrelated code.
- Original Spring configuration and classes containing `@Bean` methods are unselected by
  default. The original Spring Boot application is excluded because Studio supplies startup.
  Copying an old configuration wholesale can duplicate Studio's native components and beans.
- Literal `@PropertySource("classpath:…properties")` resources on recommended source classes
  are offered at the same path beneath `user/src/main/resources`. Canonical application
  properties remain separate environment configuration choices.
- Ordinary additional direct Maven dependencies are selected by default. Original
  explicit versions, exclusions, scopes and optional flags are retained. An omitted
  version remains omitted and uses destination dependency management, so JAXB API/runtime
  declarations do not require a separate decision just because their version is inherited.
  Existing declarations are not duplicated. Conflicting source-module versions, unresolved
  expressions, non-portable system dependencies and platform-baseline declarations remain
  manual review items with disabled Import checkboxes. Double-click their description to
  inspect the declarations; the importer does not guess versions or copy local JAR paths.
  Original parents, BOMs, repositories, profiles and plugins are not automatically copied.

**Compare Selected Item…**, double-clicking a row, or pressing Enter opens IntelliJ's
native read-only diff. File comparisons put the original on the left and the current
destination on the right. Dependency rows compare that declaration with its current
destination counterpart, including unresolved/manual-review declarations; comparing
never selects an item for import. Double-clicking the Import checkbox only changes
selection and does not open a diff.

`DebugTransitionComponent` and `DeepCopyUtil` in their exact Studio packages are
excluded from selectable imports because Studio supplies them for the destination
meta-pack. A separate **Studio support files** tab shows their status and supports the
same comparison gestures. Customer classes with the same simple names remain normal
source choices. Destination files are checked against templates rendered from the
saved destination model. Differing originals are flagged as possible version changes
or customisations; missing, locally changed or unverifiable destination support is
explicitly flagged rather than assumed correct. Formatting/comments are ignored for
classification; the diff retains the complete original and destination snapshots.
Neither original support files nor destination support files are overwritten by this
stage. The saved model and support snapshots must remain unchanged until apply.

Existing developer-owned files require explicit replacement approval; changed source, destination, POM or generated bean contracts
invalidate the corresponding review. Reanalyse after edits. Multiple selected source files
cannot target the same destination. File writes are rolled back on a transfer failure.

The scan excludes test sources, generated/build output and symbolic links. It is bounded
to 10,000 visited entries, 64 MiB of relevant input, and 2 MiB per read file. Select a narrower
module directory if the reactor is too large. Reflective/dynamic references, custom composed
Spring annotations, inherited build configuration and non-properties resources require review.

## 3. Complete configuration and wiring

The **Bean Contracts and Review** tab records the actual required bean names, types and
source locations from generated Java, together with matching original `@Bean` methods.
These are candidates and contracts, not a claim that Spring wiring has been completed.

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
`config/`. Choose a discovered file, choose **Use Selected Discovered File**, assign its
environment and add it for review. Import only the relevant settings. Refer to
[configuration import and environments](ProjectFilesAndRecovery.md#import-configuration-and-select-an-environment)
for precedence, local-only files and environment Run/Debug configurations.

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
