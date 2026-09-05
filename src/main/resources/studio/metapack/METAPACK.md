# Ikasan Studio Meta-Packs

## What is a meta-pack?

A meta-pack is the version-specific compatibility layer between Ikasan Studio's version-neutral design model
and a particular Ikasan release.

The Studio model records the developer's intent: modules, flows, components, routes, properties and exception
handling. A meta-pack supplies the Ikasan-specific knowledge needed to present those choices in the designer
and turn the model into a working Maven project.

Each meta-pack therefore represents one tested development platform. It is more than a component catalogue or
a collection of templates: its metadata, dependency-management contract and templates must work together for
the exact Ikasan version declared in its manifest.

## What does a meta-pack provide?

A meta-pack provides:

- A catalogue of modules, flows, consumers, producers, converters, translators, routers and other components.
- Component property definitions, defaults, validation, help text and display information.
- Input and output type information used to guide flow construction and report likely type mismatches.
- Conversion recipes and implementation templates for common integration patterns.
- Component artwork used by the palette and designer canvas.
- FreeMarker templates that generate Java, Maven and configuration artefacts.
- An exact Ikasan version, Java version and Maven BOM contract.

This allows the Studio user interface and persisted model to remain largely independent of an Ikasan release,
while generated code continues to use the APIs and conventions appropriate to that release.

## Lifecycle in Studio

When a developer selects a meta-pack, Studio:

1. Loads and validates its `metapack.json` manifest.
2. Loads the component categories and individual component metadata.
3. Builds the palette and property editors from that metadata.
4. Uses the metadata to validate and explain the designed flows.
5. Uses the pack's templates to generate the project artefacts.
6. Imports the declared BOM and applies the pack's Java version to the generated Maven project.

Changing a module to another meta-pack is a migration between compatibility platforms. Studio recreates the
version-specific component metadata and generated output while retaining compatible values from the
version-neutral model.

## Directory structure

Every packaged meta-pack is an immediate child of `studio/metapack/`:

```text
studio/metapack/
├── METAPACK.md
├── METAPACK_COMPLIANCE.md
└── V4.1.6/
    ├── metapack.json
    ├── library/
    │   ├── Consumer/
    │   │   ├── component-type-meta_en_GB.json
    │   │   └── components/
    │   ├── Producer/
    │   └── ...
    └── templates/
        └── ...
```

The main elements are:

- `metapack.json` — the pack identity and build contract.
- `library/` — component categories, component definitions, help text and icons.
- `templates/` — version-specific FreeMarker templates used during generation.

The directory name is the stable identifier stored in the Studio model, such as `V3.3.9` or `V4.1.6`. The
manifest separately records the exact tested Ikasan release, such as `4.1.6`.

## Manifest and dependency management

The manifest declares the pack's exact build platform:

```json
{
  "schemaVersion": 1,
  "id": "V4.1.6",
  "ikasanVersion": "4.1.6",
  "javaVersion": "17",
  "dependencyManagement": [
    {
      "groupId": "org.ikasan",
      "artifactId": "ikasan-eip-standalone-bom",
      "version": "4.1.6"
    }
  ],
  "compatibilityOverrides": []
}
```

Component metadata normally names required Maven dependencies without versions. The imported BOM supplies one
coherent, Ikasan-tested dependency set. A direct version is an exceptional compatibility override and must be
declared and explained in the manifest.

This avoids accidental dependency selection through Maven's nearest-definition rules and prevents different
components from silently assembling an untested mixture of library versions.

## Component metadata

A component definition describes both designer behaviour and generation behaviour. Depending on the component,
it can include:

- Its name, category, implementing class and factory or builder method.
- Required Maven dependencies.
- Mandatory and optional properties.
- Defaults, choices, validation rules and contextual help.
- Declared input and output types.
- User-implemented class requirements and FreeMarker templates.
- Metadata-driven conversion recipes.

Keep metadata declarative wherever possible. General Studio behaviour should interpret metadata rather than
contain special cases for individual Ikasan components.

Category help should explain the general concept once. Component help should add only information specific to
that implementation. Studio presents the category help first and the component-specific help in a subsequent
paragraph.

## Templates and ownership

Templates generate two broad kinds of output:

- Studio-owned artefacts under `generated/`, which Studio may regenerate.
- Developer-owned implementation stubs under `user/`, which must not be overwritten without explicit consent.

Templates must respect this ownership boundary. A meta-pack migration may replace generated output, but it must
not silently destroy developer-written implementation code.

## Creating or updating a meta-pack

The usual process is:

1. Choose one exact Ikasan release and identify its published BOM and supported Java version.
2. Copy the closest compatible pack as a starting point.
3. Give the new directory and manifest a stable pack identifier.
4. Update component metadata against the actual APIs of that Ikasan release.
5. Update templates for changed packages, builders, properties and configuration conventions.
6. Remove versions from dependencies managed by the BOM.
7. Document any unavoidable explicit dependency override in the manifest.
8. Generate minimal and maximal example modules and compile them with the declared Java version.
9. Run the meta-pack validation, BOM-resolution and complete Studio test suites.

Use the corresponding Ikasan source tree when validating APIs and generated code. Do not infer compatibility
from another major version.

## Further requirements

The normative rules and release checklist are defined in
[`METAPACK_COMPLIANCE.md`](METAPACK_COMPLIANCE.md). A pack that fails those rules should not be made available
to users or included in a Marketplace release.
