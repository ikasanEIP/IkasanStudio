# Import diagnostic reports

On restricted workstations, use **Import diagnostic report…** in the import or fetch dialog,
or **Tools → Ikasan Studio → Import diagnostic report…** after closing it. Find Action also
provides access, including during indexing. This report is separate from the existing diagnostics ZIP.

## Sending a useful screenshot

1. Reproduce the fetch or import problem. Wait for code generation to finish if import reached that stage.
2. Open the report and screenshot page 1. Include a brief description of what you expected and what appeared.
3. Select the numbered detail pages for the relevant `IMPORT-xxx` issues or failure locations.
   Include the same attempt ID when describing separate attempts. **Copy full report** copies text locally
   if your workplace permits transferring reviewed text instead of screenshots.
4. After installing a revised plugin, repeat the same operation. Compare its source-build fingerprint and results.

Nothing is uploaded. The report is retained only in the current project's memory, survives closing the import
window, and is replaced by the next fetch/import attempt. Restarting the IDE clears it. Fetch and import are
separate attempts; capture a fetch failure before starting another attempt. A cancelled attempt is labelled
Cancelled. An import remains In progress until its asynchronous generation completes or fails.

## Reading the report

Page 1 contains the plugin version, a reproducible fingerprint of plugin/engine/pack sources and build settings,
IDE build, reported Ikasan version, selected pack, current stage/outcome, document counts, and up to eight issue-category
summaries with occurrence counts. The fingerprint distinguishes rebuilt sources sharing the same beta version; it is not a Git commit
or a hash of the distributed ZIP. Nonstandard version/pack labels are omitted. Detail pages contain at most
16 logical lines; long lines can wrap. The dialog fits the invoking monitor and offers scrolling on smaller screens.
The fixed technical report format is English in both UI locales.

References are positional, not names:

- `F2`: second flow in the REST document.
- `F2/C3`: third distinct component: consumer first, then flowElements in document order.
  An identical repeated consumer is counted once, matching the importer.
- `F2/C3/P4`: fourth parameter in that component's referenced configuration record.
- `CONFIG4`: fourth record in the separate configuration document.

Important codes include:

| Code | Interpretation / next check |
|---|---|
| `METADATA_HTTP_500` / `CONFIGURATION_HTTP_500` | Server rejected this REST stage. Verify the registered module name and server log locally. |
| `JSON_PARSE_FAILED` | Invalid JSON; detail pages include line/column without the source excerpt. |
| `TRANSITION_ENDPOINT_MISSING` | A transition refers to a component absent from the flow. |
| `DISCONNECTED_COMPONENTS` | Some components cannot be reached from the consumer. |
| `CYCLIC_TOPOLOGY`, `JOIN_REQUIRES_MAPPING`, `DUPLICATE_ROUTE` | Topology needs review against Studio's tree representation. |
| `VARIANT_CLARIFICATION_REQUIRED` | The importer's exact matching/inference still requires a user choice. This records the pre-choice inspection. |
| `CUSTOM_IMPLEMENTATION_AND_WIRING_REQUIRED` | The importer uses a generic contract; provide the original implementation and wiring locally. |
| `CONFIGURATION_CLASS_MISMATCH` | The record describes another configuration class; its fields cannot be assumed compatible. |
| `CONFIGURATION_PROPERTY_UNMAPPED` | A populated field lacks a supported mapping/default exemption. Unknown field names are deliberately omitted. |
| `CONFIGURATION_TYPE_BOOLEAN_EXPECTED_STRING` (and similar) | Received JSON shape differs from the mapped Studio property type. No value is included. |

Matching and configuration diagnostics use the importer's existing rules, including exact implementing class,
configuration-class inference, explicit REST aliases, and verified defaults. They do not guess from private class
names. Known public component classes and mapped property names from the standard V3/V4/V5 pack descriptors
may appear in details. Private/custom pack metadata names are omitted. Initial clarification/custom-wiring
issues may still appear in a completed import; completion means import/generation succeeded, not that every
runtime prerequisite has been supplied or that the module is safe to start.

Failures include bounded cause types and actual Studio class/method/line locations. External exception type
names are omitted; raw messages, source filenames and paths are excluded. The report never includes module,
flow, component or route names, configuration IDs, URLs, credentials, parameter values, payloads or source JSON.
An omitted/null parameter is counted as unset; false and zero remain populated values.

Structural inspection is bounded to the importer's document limits and a maximum of 200 recorded issues;
additional issues are counted as omitted. It is observational and does not replace import validation, change
topology or property values, repair JSON, or prove runtime compatibility. Review each report before sharing.
For unfamiliar unmapped fields, support may still need a separately approved screenshot showing just the field
name and type; a report cannot reconstruct private data it intentionally excludes.
