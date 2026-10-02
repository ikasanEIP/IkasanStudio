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

The module controllers require appropriate authority (`ALL` or `WebServiceAdmin` in the inspected implementations). Use the deployment's normal authentication. Studio reads supplied files/text; it does not connect to a running module or change its configuration.

The Dashboard's module visualisation download serializes `ModuleMetaData` to `<module-name>.json`. Its configuration dialog serializes `ConfigurationMetaData` to a file which may have a `.txt` extension. The importer accepts JSON content regardless of filename extension. The Dashboard and module-local Blue Console remain distinct products.

Source contracts inspected: `rest/rest-module/.../MetaDataApplication.java` and `ConfigurationApplication.java`, `topology/.../metadata/model`, and `configuration-service/.../metadata` in the local Ikasan reference trees. Dashboard download implementations are `GraphVisualisation` and `AbstractConfigurationDialog` in the available Dashboard source tree. Export details can vary by deployed version; the selected Studio meta-pack must match the original application's Ikasan version.

## Import into Studio

1. Create/open the destination Studio project and choose **Import model.json** from the module creation screen.
2. Select **Import Ikasan runtime metadata and configuration JSON**. Paste or choose the module metadata document.
3. Select the original Ikasan version and enter the destination application's Java package. The runtime module's `version` is an application version, not a Studio meta-pack identifier; `ikasanVersion`, when present, is checked separately.
4. In **Ikasan configuration (optional)**, paste or choose the component configuration export. A single configuration object or an array of objects is accepted. Records are matched by `configurationId`, not position or component display name.
5. Select component variants if prompted. Some variants share a runtime implementation class: a scheduled consumer's metadata, for example, does not identify every possible message provider. Consult the original application rather than guessing.
6. Review the report before importing. Import replaces the destination project's model and uses the existing generation workflow. Use a new project when exploring an existing application.

Imported flows use **MANUAL** startup settings. Project build settings and ports use Studio defaults. Bring across the original business implementations, dependencies and Spring wiring, review endpoint settings, then compile and test before starting flows. Importing metadata is not a promise of an immediately runnable reproduction of the original application.

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

Configuration application is explicitly meta-pack driven. `runtimeConfigurationProperties` lists verified scalar and string-collection parameters that map directly to component properties; the configuration class must also match the pack's configuration type (`runtimeConfigurationClass`, or the `configuration` property's `usageDataType`). The bundled packs declare verified scalar settings for scheduled, JMS, FTP/SFTP, local-file, email and logging components. Email Producer also maps `toRecipients`, `ccRecipients`, `bccRecipients` (`List<String>`) and `extendedMailSessionProperties` (`Map<String, String>`). Other configuration values, module/flow/invoker configuration and decorators are preserved for review but are not automatically applied. Custom component references use existing implementations and request no replacement stub; their construction and payload types still require review.

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
