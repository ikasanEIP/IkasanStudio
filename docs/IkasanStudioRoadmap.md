# Ikasan Studio Product and Technical Roadmap

## Purpose

Ikasan Studio is intended to let developers visually build, run, and debug Ikasan integration flows from IntelliJ with:

- A low initial knowledge requirement.
- An intuitive, n8n-like authoring experience.
- Version-specific Ikasan support through meta-packs.
- Support for organisation-specific components and code-generation standards.
- A version-neutral flow model that can be migrated between Ikasan releases.

This document records the architectural assessment and proposed delivery roadmap. It is intended to be maintained as work progresses.

## Current status

| Phase | Status |
|---|---|
| 1. Trustworthy foundation | Not started |
| 2. Low-code usability | Not started |
| 3. Interactive execution | Not started |
| 4. Meta-pack ecosystem | Not started |

Last reviewed: 30 July 2026.

## Overall assessment

The meta-pack approach is a strong architectural direction. It creates the right separation between:

- The version-neutral flow model.
- Version-specific Ikasan APIs and dependencies.
- Organisation-specific coding standards.
- The IntelliJ authoring experience.

This separation is essential if Studio is to support Ikasan 3.x, 4.x, customer components, and future migration without embedding every version decision in plugin code.

The current implementation is closer to an internal resource convention than a stable extension platform. Before customers are encouraged to write their own packs, the meta-pack contract should be formalised, validated, packaged, versioned, and secured.

The plugin already provides useful visual code generation, but it does not yet provide an n8n-style interactive execution model. The current debug capability primarily consists of logging, generated wiretaps, and launching a complete application. Interactive execution should be designed as a distinct runtime subsystem.

## Architectural principles

1. The visual model is the source of truth, not generated Java.
2. Component and property identities must be stable and independent of display labels and Java implementation details.
3. A meta-pack is a versioned, validated product contract.
4. Generation must be deterministic, previewable, and atomic.
5. Changing Ikasan versions is an explicit migration, not a metadata substitution.
6. Runtime execution and debugging are separate concerns from code generation.
7. Customer-provided packs and templates must operate within an explicit trust model.
8. Core model, validation, migration, and generation functionality must be usable without IntelliJ.

## Priority findings and improvements

### 1. Define a formal meta-pack manifest and schema

The current implementation discovers meta-packs by scanning conventionally named classpath directories. The directory name acts as the pack identifier, and descriptor filenames and locale are hard-coded.

Add a `metapack.json` manifest containing at least:

```json
{
  "schemaVersion": "1.0",
  "id": "org.ikasan.official.3.3.8",
  "displayName": "Ikasan 3.3.8",
  "packVersion": "1.0.0",
  "ikasanVersionRange": "[3.3.8,3.4.0)",
  "studioVersionRange": "[1.0,2.0)",
  "generatorApiVersion": "1",
  "vendor": "Ikasan",
  "locales": ["en-GB"],
  "extends": [],
  "migrationProviders": [],
  "checksum": "..."
}
```

Publish JSON Schemas for:

- The pack manifest.
- Component categories/types.
- Components.
- Properties and editor definitions.
- Migration rules.
- Runtime and debug capabilities.

The Ikasan runtime version, meta-pack version, schema version, and generator API version must be represented separately.

### 2. Make pack loading fail-fast and transactional

The current loader can skip invalid descriptors and retain a partially loaded pack. This can leave the palette and deserialiser with an inconsistent view of the pack.

Replace this with:

1. Load into an isolated candidate pack.
2. Validate the complete candidate.
3. Publish it atomically only when valid.

Validation should return structured diagnostics such as:

- Error: component cannot be instantiated.
- Error: duplicate persistent ID.
- Error: missing template.
- Error: builder method does not exist.
- Error: mandatory pack component is missing.
- Warning: missing icon.
- Warning: missing or invalid documentation.
- Warning: property has no UI grouping.

Expose these diagnostics through a Meta-pack Problems tool window rather than directing users to IDE logs.

### 3. Introduce stable component and property IDs

Components are currently indexed primarily by display name, while deserialisation identity is inferred from `implementingClass`, `componentType`, and `additionalKey`. These values are not stable domain identifiers.

Every category, component, and property should have an immutable ID:

```json
{
  "id": "org.ikasan.endpoint.jms.consumer",
  "displayName": "JMS Consumer",
  "aliases": ["Spring JMS Consumer"],
  "properties": {
    "destination": {
      "id": "destination",
      "aliases": ["destinationJndiName"]
    }
  }
}
```

Display names and Java implementation bindings may then change without breaking persisted models.

### 4. Treat version changes as explicit migrations

Cloning an object graph using metadata from a different pack is useful but is not a complete migration strategy.

Migration must handle:

- Renamed, split, and removed properties.
- Changed property types and defaults.
- Replaced or removed components.
- Dependency changes.
- Generated user-class API changes.
- Behavioural changes with unchanged Java signatures.
- Unsupported downgrade paths.

The migration workflow should be:

```text
source model and pack
  -> validate source
  -> apply ordered migration rules
  -> present losses and required choices
  -> preview model, generated-code, and dependency changes
  -> commit target model
```

Changing a combo-box value must not silently mutate a module. Migration should be an explicit, reversible wizard that saves a migration report.

### 5. Certify packs by compiling generated applications

Golden-file tests are useful but do not prove that descriptors match the actual Ikasan API.

Each official pack should pass:

- JSON Schema validation.
- Descriptor semantic validation.
- Template syntax validation.
- Minimal generation for every component.
- Compilation against the exact supported Ikasan BOM.
- Generated-module startup smoke tests.
- Model serialisation round trips.
- Cross-version migration tests.
- Pack isolation and duplicate-ID tests.
- Representative executable integration tests.

Provide a standalone `metapack-validator` CLI and a Gradle or Maven integration so customer packs can be certified without launching IntelliJ.

At the time of this assessment, a focused run completed 91 meta-pack and generator tests with one failure in the modified V3.3.8 Event Generating Consumer generation path. The generated result included an incomplete injected class and consecutive `return` statements. This should be resolved before treating the current V3.3.8 pack as certified.

### 6. Establish a security and trust model

Third-party FreeMarker templates are executable code-generation inputs rather than harmless configuration. Packs can influence generated source, dependencies, properties, and file output.

Introduce:

- Official signed packs.
- Locally trusted organisation packs.
- Explicit approval for untrusted packs.
- Publisher identity and checksums.
- A restricted FreeMarker object wrapper.
- An allow-list of template helper functions.
- Generated-path validation to prevent traversal.
- Dependency allow/deny policies.
- A preview of generated files and dependencies before first use.

General static JVM access should not be exposed to untrusted templates.

## Meta-pack content model

The V3.3.8 pack provides a useful initial palette, including consumers, producers, filters, converters, routers, splitters, brokers, endpoints, exception handling, flow/module metadata, and debug components.

The component definition contract should explicitly separate:

```text
identity
presentation
configuration schema
design-time constraints
Ikasan API binding
dependency contribution
generation strategy
runtime and test capabilities
documentation and examples
```

These sections may initially remain in one JSON document, but their responsibilities should be distinct.

Complete an Ikasan 3.3.8 content audit covering:

- Help links that currently reference Ikasan 3.1.x.
- Placeholder or incomplete implementing classes.
- Builder methods and setter methods.
- Maven dependencies and compatible versions.
- Default values and validation.
- Mandatory versus advanced properties.
- Template special cases based on display names.
- Component examples and example payloads.

Shared templates should not contain component-name-specific behaviour. Such behaviour belongs in declarative metadata or a versioned generation strategy.

## Interactive execution and debugging

Interactive debugging should use a Studio Runtime protocol rather than repeatedly regenerating modified flows.

The runtime should initially run in a child JVM and expose a local control channel, potentially through the existing IDE mediator.

Required capabilities:

- Start, stop, and observe a module or selected flow.
- Inject an event at a compatible entry point.
- Capture input, output, headers, timing, and exceptions.
- Display live node state on the graph.
- Pause before or after a component.
- Continue, step, retry, or skip.
- Re-run a component using captured data.
- Pin prior results to nodes.
- Apply payload size, retention, and redaction policies.

Delivery order:

1. Run a selected flow with an injected test event.
2. Display a visual execution trace and per-node timing.
3. Inspect before-and-after payloads.
4. Re-run from a captured node.
5. Add pause and continue.
6. Add conditional breakpoints and mock nodes.

Runtime state must be obtained from the actual run process rather than button-local booleans. Run configuration, process lifecycle, output, termination, and restart should use IntelliJ's execution APIs.

## Low-code authoring experience

### Palette

Add:

- Search by name, purpose, protocol, and payload type.
- Recommended and advanced classifications.
- Compatibility filtering based on upstream output.
- Click-to-add alongside drag-and-drop.
- Starter recipes and reusable flow fragments.
- Example configuration and payloads.
- Required resource and credential indicators.
- Component health and validation badges.
- A command such as "Add logging after this".

### Canvas

Add:

- Deterministic automatic layout.
- Inline errors and warnings.
- Quick fixes.
- Keyboard navigation and shortcuts.
- Undoable structural operations.
- Clear connection handles and valid-drop targets.
- Minimap and zoom for large modules.
- Live runtime status overlays.

### Property editor

Extend property metadata beyond Java wrapper types and regular expressions with semantic editor types:

- Secret/password.
- Duration and size.
- URI, host, and path.
- Queue and topic.
- Cron.
- IntelliJ-aware Java class reference.
- Resource reference.
- Key/value map.
- Repeatable structured list.
- Expression.
- Payload schema/type.
- Embedded code editor.
- Connection-test action.

Use a standard checkbox or tri-state control for booleans rather than separate true and false checkboxes.

Visually distinguish:

- Mandatory properties.
- Recommended properties.
- Advanced properties.
- Defaults.
- Inherited values.
- Environment overrides.
- Sensitive values.

## Codebase robustness work

Address the following early:

- Fix selection and deselection code that uses lazy `Stream.peek()` calls without terminal operations.
- Prevent the current debug-component action from deleting the selected component.
- Replace runtime exceptions escaping from Swing callbacks with recoverable user diagnostics.
- Replace button-local module start/stop state with actual process state.
- Remove hard-coded generated application paths and class names.
- Use IntelliJ logging consistently in IntelliJ-facing code.
- Avoid IntelliJ `error` logging for expected validation and recovery paths.
- Replace shared mutable static meta-pack caches with an application-level registry of immutable packs and project-specific selections.
- Generate only changed files.
- Escape all generated Java literals centrally.
- Avoid requiring plugin-classloader `Class.forName()` resolution for schema property types.
- Make pack discovery reliable for JAR resources and external pack locations.
- Make generated file ownership explicit so user code cannot be overwritten accidentally.

## Target module structure

```text
studio-model
  Versioned flow document and stable component/property IDs

studio-metapack-api
  Manifest, schema, binding model, validation SPI, and migrations

studio-generator
  Sandboxed templates and deterministic generation plans

studio-metapack-validator
  CLI/API certification and generated-project compilation

studio-runtime-protocol
  Run, inject, trace, pause, inspect, and retry contracts

studio-intellij
  Tool windows, editors, PSI, run configurations, and UI

ikasan-metapack-3.3.8
ikasan-metapack-4.x
  Independently versioned and tested pack artifacts
```

The core model, pack API, generator, and validator must not depend on Swing or IntelliJ APIs.

## Generation architecture

Generate a complete in-memory `GenerationPlan` before modifying project files. It should contain:

- Files to create.
- Files to update.
- Files to delete.
- Dependency additions and removals.
- Validation diagnostics.
- User-owned file conflicts.

The plan should be validated and, when material, previewed before being applied atomically under one IntelliJ command.

Generated outputs should include provenance headers containing:

- Source model identity and revision.
- Meta-pack ID and version.
- Generator API/version.
- Content checksum.

## Delivery roadmap

### Phase 1: Trustworthy foundation

- [ ] Define the meta-pack manifest.
- [ ] Publish JSON Schemas.
- [ ] Introduce stable component and property IDs.
- [ ] Implement structured validation diagnostics.
- [ ] Make pack loading transactional.
- [ ] Create the standalone validator.
- [ ] Compile generated fixtures against Ikasan 3.3.8.
- [ ] Complete the V3.3.8 metadata/API audit.
- [ ] Remove component-name special cases from shared templates.
- [ ] Fix the current generator test regression.
- [ ] Fix canvas selection and debug-action defects.
- [ ] Separate runtime, pack, schema, and generator versions.
- [ ] Introduce deterministic generation plans.

Exit criteria:

- The official 3.3.8 pack validates and compiles all supported component fixtures.
- A malformed pack cannot become active.
- Model persistence uses stable IDs.
- Generation is deterministic and reports conflicts before writing.

### Phase 2: Low-code usability

- [ ] Add searchable and filterable palette.
- [ ] Add recommended and advanced property grouping.
- [ ] Add semantic property editors.
- [ ] Add inline graph validation and quick fixes.
- [ ] Add starter recipes and example payloads.
- [ ] Add click-to-add and keyboard workflows.
- [ ] Add deterministic automatic layout.
- [ ] Add generation preview and source-ownership rules.
- [ ] Add explicit migration wizard and report.

Exit criteria:

- A new user can create, validate, generate, and run a basic flow without consulting external documentation.
- Common configuration mistakes are explained at the point of entry.
- Upgrades are previewable and reversible.

### Phase 3: Interactive execution

- [ ] Add proper IntelliJ run configurations.
- [ ] Implement runtime process-state tracking.
- [ ] Define and implement the Studio Runtime protocol.
- [ ] Inject test events into selected flows.
- [ ] Render live execution traces.
- [ ] Inspect component inputs and outputs.
- [ ] Re-run from captured nodes.
- [ ] Add pause, continue, and stepping.
- [ ] Add conditional breakpoints and mock nodes.
- [ ] Add payload redaction, size, and retention controls.

Exit criteria:

- Users can execute and diagnose a flow visually without reading generated Java or IDE logs.
- Runtime controls reflect the actual child-process state.
- Captured data is bounded and sensitive values can be protected.

### Phase 4: Meta-pack ecosystem

- [ ] Extract official packs into independently versioned artifacts.
- [ ] Add signing, checksums, and trust policies.
- [ ] Support organisation pack inheritance and overlays.
- [ ] Publish migration-provider APIs.
- [ ] Provide a pack SDK, reference pack, and authoring guide.
- [ ] Publish a Studio/pack/Ikasan compatibility matrix.
- [ ] Add CI publication and certification badges.
- [ ] Define support and deprecation policies.

Exit criteria:

- Customers can create, validate, distribute, and upgrade their own packs without modifying the IntelliJ plugin.
- Pack compatibility and publisher trust are visible before installation.

## Immediate next actions

Recommended first implementation sequence:

1. Specify `metapack.json` and the first JSON Schemas.
2. Create immutable pack-domain objects and structured diagnostics.
3. Implement a validator over the existing V3.3.8 directory.
4. Add generated-project compilation against Ikasan 3.3.8.
5. Audit and correct the V3.3.8 component definitions.
6. Introduce stable IDs and model migration compatibility.
7. Replace direct generation writes with a `GenerationPlan`.
8. Fix the known canvas/debug/process-state defects.

## Review evidence

The assessment was based on:

- The plugin architecture and build configuration.
- Core model, serialisation, generation, and meta-pack loading code.
- The complete V3.3.8 meta-pack inventory.
- Representative V3.3.8 component descriptors and FreeMarker templates.
- Canvas, palette, property editor, run, and debug implementations.
- The adjacent Ikasan 3.3.x builder and component source tree.
- Existing meta-pack and generator tests.

The repository contained pre-existing uncommitted work during the review, including changes to meta classes, Event Generating Consumer metadata, V4 metadata/templates, and managed event identifier service support. Those changes were preserved.
