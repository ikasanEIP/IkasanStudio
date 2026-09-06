# AI-friendly projects

Studio provides an offline project contract. No network, MCP server or cloud connection is required.

## Files and ownership

Source-generation requests (FULL, MODULE_STRUCTURE, FLOW and PROPERTIES) maintain:

- `generated/IKASAN_STUDIO.md`: model-editing workflow and ownership guidance.
- `generated/src/main/model/model.schema.json`: structural JSON Schema.
- `generated/src/main/model/component-catalogue.json`: keys, roles, implementation classes,
  properties, defaults, types and choices from the selected ComponentLibrary.
- Root `AGENTS.md`: discovery instructions, created only when missing.

MODEL_ONLY requests do not generate these files. Generated contract files participate in the
source-generation transaction. The catalogue is derived automatically from the selected meta-pack.
The editable model remains the source of truth; implementations under `user/` remain developer-owned.

New archetype projects include root `AGENTS.md` immediately. Other contract files appear after
source generation. Existing root instructions are preserved, even if the file is empty. For a
project created with the earlier empty archetype file, copy the discovery guidance into it, or
remove it only if it is truly empty and regenerate. Never remove corporate or team instructions.

## Maintenance

`AiProjectContractGenerator` owns the generated prose, schema and catalogue format.

- Component and property metadata changes automatically appear after regeneration.
- Update `modelSchema()` manually when persisted model structure changes, checking actual JSON
  serialization and representative saved models.
- Update `studioGuide()` when editing, validation, reload or ownership behaviour changes.
- Keep `agentsGuide()` identical to the archetype's `archetype-resources/AGENTS.md`.
- Increment `CONTRACT_VERSION` when contract format or meaning changes in a way consumers need
  to distinguish, especially incompatible schema or catalogue changes. Ordinary metadata refreshes
  and wording corrections do not require a bump. The version is emitted in the catalogue.

Unknown module, flow and component properties remain permitted to accommodate component-specific
properties and future model extensions. Agents should preserve unfamiliar fields. Transition
objects currently have a closed schema. Structural checks do not replace Studio's semantic
validation of components, required properties, transitions, routes and meta-pack compatibility.

## Verification and future integrations

Run `./gradlew test` and `./gradlew validateMetaPacks --no-configuration-cache` after contract changes. The focused
`AiProjectContractGeneratorTest` checks every shipped meta-pack, basic schema and guidance, and
consistency between archetype and generated discovery instructions. Exercise source generation
in the IDE to verify the complete project-file lifecycle.

A future local MCP server can expose the same contract and delegate validation or generation
to Studio. Keep the files independently usable so that MCP does not become mandatory.
