# Ikasan model documents and runtime JSON import

Studio can import the module metadata JSON exported by core Ikasan or the Dashboard, with a separate, optional configuration JSON document. Existing Studio `model.json` imports continue to work.

## Where the documents come from

The core Ikasan source trees expose these authenticated GET endpoints, relative to the application's context path:

| Endpoint | Content |
| --- | --- |
| `/rest/metadata/module/{moduleName}` | Module metadata: flows, consumers, components, named transitions and configuration identifiers |
| `/rest/metadata/flow/{moduleName}/{flowName}` | One flow's metadata; use the module endpoint for Studio import |
| `/rest/configuration/components` | An array of component configuration records for the module |
| `/rest/configuration/flows` | Flow configuration records |
| `/rest/configuration/invokers` | Invoker configuration records |
| `/rest/configuration/module` | The module's own configuration, when configurable; this is not an export of every component |

The module controllers require appropriate authority (`ALL` or `WebServiceAdmin` in the inspected implementations). Use the deployment's normal authentication. Studio can read supplied files/text or fetch these documents from a running module. Fetch uses read-only endpoints and never changes the running module's configuration.

The Dashboard's module visualisation download serializes `ModuleMetaData` to `<module-name>.json`. Its configuration dialog serializes `ConfigurationMetaData` to a file which may have a `.txt` extension. The importer accepts JSON content regardless of filename extension. The Dashboard and module-local Blue Console remain distinct products.

Source contracts inspected: `rest/rest-module/.../MetaDataApplication.java` and `ConfigurationApplication.java`, `topology/.../metadata/model`, and `configuration-service/.../metadata` in the local Ikasan reference trees. Dashboard download implementations are `GraphVisualisation` and `AbstractConfigurationDialog` in the available Dashboard source tree. Export details can vary by deployed version; the selected Studio meta-pack must match the original application's Ikasan version.

## Import into Studio

1. Create/open the destination Studio project and choose **Import model.json** from the module creation screen.
2. Select **Import an Ikasan runtime export (instead of a Studio model)**. Paste or choose the module metadata document in **Module metadata JSON**. Leave this option unchecked when importing a complete Studio `model.json`; only the **Studio model.json** tab is shown in that mode.
3. Select the original Ikasan version and enter the destination application's Java package. The runtime module's `version` is an application version, not a Studio meta-pack identifier; `ikasanVersion`, when present, is checked separately.
4. In **Component configuration JSON (optional)**, paste or choose the component configuration export. Leave it blank to import topology only and configure components afterwards. A single configuration object or an array of objects is accepted. Records are matched by `configurationId`, not position or component display name.
5. Select component variants only if prompted. For shared runtime classes, Studio links the component's
   configurationId to its supplied configuration record and matches the exact configuration class against
   the selected pack. A unique match selects the variant automatically and records the evidence in the
   import review. This distinguishes Scheduled, FTP, SFTP and Local File consumers without relying on
   component names or cron expressions. Missing/unlinked records, unknown classes or multiple matching
   variants still require clarification. Duplicate configuration IDs are rejected. Original
   message-provider wiring still needs review; no Java classes are loaded to guess inheritance. REST
   parameters with omitted/null values are retained as unset rather than applied or rejected; duplicate
   parameter names remain invalid.
6. Review the report before importing. Import replaces the destination project's model and uses the existing generation workflow. Use a new project when exploring an existing application.

Imported flows preserve each declared `flowStartupType`: **AUTOMATIC**, **MANUAL** or **DISABLED**. Missing/null settings fall back to **MANUAL** with a flow-specific review item; unknown or malformed values block import rather than silently changing behaviour. The application HTTP port is copied from the module metadata. If absent, Studio uses its default and flags the port for review; invalid supplied ports block import. The REST URL may use a proxy port, so it is not used to infer the application port. Project build settings come from the selected meta-pack; compare the generated POM with the original for custom dependencies, repositories, plugins and profiles. Bring across the original business implementations, dependencies and Spring wiring, review endpoint settings, then compile and test before starting flows. Importing metadata is not a promise of an immediately runnable reproduction of the original application.

Runtime exports describe effective settings, so defaults left blank in the original project can appear as explicit values after import. For example, an omitted scheduler timezone can be reported as `Europe/London`, and an automatically generated configuration ID can appear as a populated field. Studio preserves those reported values: removing the timezone could change scheduling on another machine, and removing the ID could change which runtime configuration overrides are found. The export does not reliably distinguish explicit settings from resolved defaults.

## Fetch from a running module

In the import dialog, choose **Fetch from running module…**. Enter the application URL
including its context path (for example `http://localhost:8080/orders`), the registered
module name, and optional Basic authentication credentials. The registered module name is
case-sensitive (`module.name`) and may differ from the lowercase URL context: for example,
URL `http://localhost:12450/converterandtranslators5` with module `converterAndTranslators5`.
Recognized `/swagger-ui/index.html`, `/swagger-ui/` and `/swagger-ui.html` suffixes are removed
automatically (including their UI query/fragment). Other application URL queries/fragments
are rejected. The dialog displays the normalized application URL after Fetch. Empty credentials request
anonymous access; the deployment must permit the REST calls. The inspected controllers
require `ALL` or `WebServiceAdmin` authority. HTTPS uses normal JVM certificate trust;
redirects are not followed. Enter the final application URL rather than a login page.

Fetch reads `/rest/metadata/module/{moduleName}`, then
`/rest/configuration/{moduleName}/{flowName}/components` for every returned flow.
Names are encoded as individual path segments. The unqualified configuration endpoints
select the first registered module in the inspected implementation, so Studio deliberately
uses scoped component endpoints. It never invokes the older `createConfiguration` GETs,
configuration writes or flow control endpoints.

On success, the documents populate the existing editable preview and runtime import mode
is selected. Choose the matching Ikasan meta-pack and destination package, resolve any
component variant choices, then review before importing. If the selected pack is unsuitable,
Studio recommends an exact available version first, otherwise the nearest newer stable patch
in the same major/minor line (for example 4.1.3 → 4.1.6). Changing packs or using a newer patch
requires confirmation. A patch import targets the selected pack's dependencies, adds an upgrade
warning to the review and retains the original export/version as provenance. It is not a
compatibility guarantee: compile and test before starting flows. Downgrades, cross-minor/major
changes and non-exact snapshot/prerelease substitutions are rejected; use separate migration
work for those cases. Fetch alone does not modify
project files or its live model. A failed/cancelled fetch leaves the existing preview
unchanged. If component configuration is unavailable, explicitly uncheck **Fetch component
configuration for each flow** and fetch topology only; settings then require manual review.

The fetch dialog remembers the application URL, registered module name, username and
configuration checkbox in this project's local IDE workspace, including across IDE restarts.
Values are remembered after Fetch or closing the dialog, including failed requests and Cancel.
Swagger suffixes are removed before saving; an invalid URL leaves the last usable URL unchanged.
Passwords are not saved in Studio settings, project files or import provenance;
the password field remains populated while the fetch dialog stays open, so failed or cancelled
fetch attempts can be retried without re-entry. Closing the dialog (including success or Cancel)
clears the field; a newly opened dialog starts blank. Temporary request password arrays are
cleared after every attempt. The fetched configuration itself can
contain application credentials and is retained by the existing importer as provenance,
so review it before sharing the project. Fetch has a five-second connection timeout,
a twenty-second request timeout and an 8 MiB limit on metadata and combined configuration.
The IDE log records each actual GET endpoint, acquisition stage and HTTP status, making wrong
context paths and module-name casing visible. Request credentials, authorization headers,
Swagger query/fragment values and response bodies are not logged. HTTP 404/500 guidance asks
the developer to check the exact registered module name as well as context/endpoint availability;
HTTP 500 can also require checking the module server log.
Requests are cancellable while waiting for the server. No retry or partial configuration
fallback happens silently. Conflicting records with the same configuration ID are rejected.

The fetch is a sequence of read-only requests, not an atomic operational backup. Changes
to the running module during acquisition can require a fresh fetch and review. Only
component configuration is fetched in this first implementation; module/flow/invoker
configuration, handwritten code, application dependencies and wiring still require review.
Fetched imports preserve declared flow startup settings, just like file-based runtime imports. Missing/null settings fall back to **MANUAL** with review guidance.

## Export three separate files

Choose **Tools → Ikasan Studio → Export Ikasan Model Documents…**, then select a destination folder.
Studio creates a new `ikasan-model-documents` subfolder containing:

- `module.json`: the Ikasan-shaped module topology.
- `configuration.json`: the component configuration records.
- `studio.json`: Studio-specific design and generation settings, plus `modelFormat` and `formatVersion` identifying the export format.

The export uses the current in-memory model, including unsaved designer changes. It does not save or replace the project's `model.json`. Existing export folders are never overwritten; select another destination or move the previous export before repeating. Cancellation or a write failure cleans up the temporary export folder.

These are design documents, not a guarantee of a deployable runtime configuration. Review unfinished values and settings without verified runtime mappings before runtime use. Configuration and import provenance may contain credentials; handle the export like application configuration.

The existing runtime importer accepts `module.json` and `configuration.json`. Importing those two files does not restore the Studio-specific settings in `studio.json`; a three-file import UI is not yet provided. For a complete Studio project-model transfer, continue to import the project's `model.json`.

## Studio's persisted model

New saves use Ikasan-shaped module metadata and configuration records as the editable source of truth, accompanied by Studio-specific generation information. The three documents are kept in one `model.json` so saving, copying, version control and recovery remain atomic:

```json
{
  "modelFormat": "ikasan-studio-documents",
  "formatVersion": 1,
  "module": { "name": "Orders", "ikasanVersion": "3.3.9", "flows": [] },
  "configuration": [],
  "studio": {
    "metaPack": "V3.3.9",
    "properties": { "applicationPackageName": "org.example.orders" },
    "flows": {}
  }
}
```

This is a layout illustration, not a complete project. Studio supplies the other required settings.

- **`module`** owns topology: flows, consumers, components, implementation classes, named transitions and configuration IDs. Like core Ikasan exports, `flowElements` includes the consumer also identified by `consumer`; both occurrences must agree. Its optional `version` means the application's version, not the Ikasan release.
- **`configuration`** owns supported runtime settings as records linked by `configurationId`, containing typed `parameters`. Changing a mapped value here changes the configuration used by code generation. Unknown records and parameters are retained.
- **`studio`** owns the selected meta-pack, Java package, component variants, generation choices and settings without a verified runtime mapping. Per-flow and per-component settings are keyed by their names. A generated class's naming expression and a configuration ID template remain here; the resolved identities are in `module`. Invalid or unfinished mapped values are retained explicitly as `pendingConfiguration`, outside the runtime parameter records, so an unfinished design can still be saved.

These are authoritative documents, not three exports alongside a separate editable legacy model. Internally, an adapter lets the existing version-neutral Java model and templates continue to operate. The `module` and `configuration` sections can be extracted as separate JSON documents for inspection; the whole container is a Studio document and must not be sent to a core REST endpoint. Adoption of the shapes does not promise a deployable runtime export: unresolved construction choices, unmapped settings and draft values still need review. Studio does not upload configuration to a running application.

Existing flat Studio models load unchanged and convert on their next normal save or regeneration. There is no need to discard existing designs. Normal model saves retain the previous file in the existing rotating backups. IDE and command-line Ikasan migrations write the new format and retain their usual recovery snapshots. Older Studio builds will not understand the new container; restore the previous project revision before returning to one. Keep all three sections together when copying or restoring a model.

After conversion, regenerate shared flow-test support if prompted. `FlowTestSupportFingerprint` now belongs to `support`, because understanding the saved model format is Studio-dependent. The old unused copy in `support.utils` can remain; other reusable utilities and business tests are preserved. Generated verification baselines remain snapshots and should be regenerated only when intended.

For imported runtime exports, the original input documents and review report are retained as import provenance under `studio.properties.ikasanRuntimeImport`. That provenance is an immutable reference snapshot; editing it does not change the active topology or configuration. Configuration and provenance may contain credentials; handle them as application configuration.

The import review highlights work still required before running. Successfully applied fields and unset parameters do not produce review messages. Fields matching a meta-pack's verified runtime defaults also need no review item, provided the configuration class matches. Non-default and unknown fields that cannot be applied are grouped by component, with field names only; their original values remain in the import provenance. Implementation, wiring, decorator and version guidance remains visible.

Configuration application is explicitly meta-pack driven. `runtimeConfigurationProperties` lists verified scalar and string-collection Studio properties; names match REST parameters exactly unless the component declares a discrepancy in `runtimeConfigurationMappings` (REST name → Studio property). The same mapping is used when reading/writing native configuration records, so exported parameter names remain correct for Ikasan. The configuration class must also match the pack's configuration type (`runtimeConfigurationClass`, or the `configuration` property's `usageDataType`). The bundled packs declare verified scalar settings for scheduled, JMS, FTP/SFTP, local-file, email and logging components. Explicit discrepancies cover Email `mailHost`/`mailhost`, `password`/`mailPassword`, `runtimeEnvironment`/`mailRuntimeEnvironment`, `subject`/`mailSubject`, `mailFrom`/`from`, `mailTransportProtocol`/`transportProtocol`, and FTP `FTPS`/`ftps`. Type and configuration-class checks still apply; similar spellings are not guessed. Spring JMS Producer and Consumer also import complete `connectionFactoryJndiProperties` and `destinationJndiProperties` string maps from REST configuration. Imported maps override the corresponding individual JNDI fields during generation, including Studio defaults; empty maps remain explicitly empty. Provider-specific string keys are preserved. Non-string/nested map values and mismatched configuration classes remain in provenance and require review. No original project files or source code are needed. Email Producer also maps `toRecipients`, `ccRecipients`, `bccRecipients` (`List<String>`) and `extendedMailSessionProperties` (`Map<String, String>`). Other configuration values, module/flow/invoker configuration and decorators are preserved for review but are not automatically applied. Custom component references use existing implementations and request no replacement stub; their construction and payload types still require review.

Ambiguous or unsupported components, invalid configuration records, cycles, joins and disconnected graphs fail before import changes project files. These are reported rather than silently dropping components or changing routing. New mappings should be added to the meta-pack with tests against the corresponding Ikasan API.

Import is bounded to 8 MiB of text per document, 1,000 flows and 500 components per flow. A supported import is a design starting point, not an operational backup/restore mechanism.

## Verification

`IkasanModelDocumentsTest` checks legacy conversion, unchanged generated Java/properties, authoritative configuration edits, both migration directions and invalid-document rejection. Schema and test-support fingerprint checks cover both layouts. Manual editor save/reload, import, migration and recovery should be exercised in the next candidate IDE build.


`IkasanRuntimeImportTest` covers both bundled packs, configuration separation, application-version handling, preservation through Studio serialization, explicit component choices, custom implementation references and rejection of malformed input. Exercise the import dialog with representative downloaded files as part of the next [release-candidate checks](ReleaseCandidateVerification.md).

## String collection configuration

In both supported packs, Email Producer's recipient lists and extended mail-session properties offer **Edit entries…**. Lists have one string per row; maps have key and value columns. Strings are preserved exactly, including commas, quotes, whitespace and line breaks. Lists preserve order and duplicate entries; map keys must be unique. Nested values, null entries and arbitrary Java objects are not supported in these configuration fields.

Select **Leave unset** to use the component default. Clearing all rows with that option unselected supplies an explicitly empty collection. Existing comma-separated recipient lists are converted when loaded, and legacy string `"[]"` retains its previous meaning of unset. A structured JSON `[]` means explicitly empty. Invalid legacy map text remains visible for correction and is not emitted as executable Java.

Saved/exported parameter values use JSON arrays or objects and Ikasan's `ConfigurationParameterListImpl` / `ConfigurationParameterMapImpl` identifiers. Generated recipient entries in `application.properties` now use JSON arrays; the generated factory reads these without evaluating their contents as Spring expressions. Existing comma-separated recipient overrides are still accepted. Extended mail-session maps are generated as Java collection values in the component factory.

After updating a design to use these types, regenerate its application code and properties together. These changes concern configuration values; payloads such as `List<Order>` remain developer-defined Java objects.

Previously saved Studio XML converters using the old builder-class identifier are migrated to the runtime implementation identifier when loaded or migrated between Ikasan versions. XML string converters are matched using their runtime implementation class and import verified `XmlConfiguration` fields, including validation and namespace settings. Their JAXB input `objectClass` is a builder setting absent from REST metadata; supply it in Studio before generating code. It cannot be inferred from the XML root-name settings. FTP Consumer scheduling/recovery and `provideRelativePath` values matching pack-declared runtime defaults do not require review; changed unsupported values still do.

Ikasan REST can omit the `value` member of an unset configuration parameter. Studio accepts both an omitted value and explicit JSON `null` when importing and reopening saved models. Unset parameter records retain their IDs/type metadata across saves; no setter is generated for an unset optional setting such as JMS `priority`. Explicit values, including `false` and `0`, remain active settings and are type checked. Clearing a previously populated setting removes its active runtime parameter value.

When loading Studio document containers, a uniquely matching installed pack ID can be resolved despite case differences (for example `v4.1.6` → `V4.1.6`). Exact registered IDs take precedence; no version upgrade or revision change is inferred. Recognised settings placed directly under `studio` (`applicationPackageName`, `flowStartupType`, `h2DbPortNumber`, `h2WebPortNumber`, `useEmbeddedH2`) are read into `studio.properties` without changing their values. A direct `studio.review` is retained under `studio.properties.ikasanRuntimeImport.review`. Equal duplicates collapse; conflicting values block loading rather than choosing one. Unknown metadata fields still require a compatible document format. Normalization happens in memory; the input file is unchanged until an explicit save or import writes a standard document.

## Troubleshooting on restricted workstations

Use the [screenshot-friendly import diagnostic report](RuntimeImportDiagnostics.md) to share numbered
issues, REST status, configuration shapes and Studio failure locations without exporting source JSON.
