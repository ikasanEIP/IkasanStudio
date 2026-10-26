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

Machine-readable schemas for these files live in [`schema/`](schema/). The schemas provide editor/tooling
support, while Studio's semantic validator checks relationships between files and resources.

The directory name is the stable identifier stored in the Studio model, such as `V3.3.9` or `V4.1.6`. The
manifest separately records the exact tested Ikasan release, such as `4.1.6`.

## Manifest and dependency management

The manifest declares the pack's exact build platform:

```json
{
  "schemaVersion": 2,
  "packVersion": "0.1.0-SNAPSHOT",
  "generatorApiVersion": 1,
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
- An optional `testPayloadAdapter` for constructing interface-based synthetic payloads. The supported
  `ikasan-file-transfer-payload` adapter presents a single-file chooser and reconstructs an Ikasan `Payload`
  with the selected filename, bytes and standard `fileName` attribute inside the debug module.

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

For a separate custom-pack project and the install/restart workflow, see the
[custom meta-pack handover](../../../../../docs/CustomMetaPackHandover.md).

## Further requirements

The normative rules and release checklist are defined in
[`METAPACK_COMPLIANCE.md`](METAPACK_COMPLIANCE.md). A pack that fails those rules should not be made available
to users or included in a Marketplace release.

## Flow-test observation capabilities

A consumer can declare `flowTestInputMode: "self-generating"` when starting the flow supplies
continuous input without a test sender. List properties that change this contract in
`flowTestInputModeInvalidatedByProperties` (for example a custom event provider).
A discard producer can declare `flowTestObservationOnly: true`: reaching it is observable,
but it has no business output or external delivery contract. Do not set this flag for external
producers or use it to skip content, log-format or persistence assertions.

Only a direct consumer-to-producer flow with these capabilities, no invalidating property,
no branches and no exception resolver receives the compact observation scaffold. Other flows
retain explicit input, path and payload assertions. The version-specific
`flowObservationTestTemplate_en.ftl` and shared support template own the generated test code.
Consumers may also declare `flowTestExpectedInitialOutputs`, an ordered array of up to 100
text samples from their default provider. The scaffold snapshots these literals into developer-owned
test source; it never loads target-pack expectations at runtime or updates assertions on migration.
Omit the array for sources with no deterministic initial payload contract. Invalidating properties
also invalidate these expectations, causing fallback to the explicit scenario scaffold.

The observation retains only the declared initial samples, verifies their content and order,
requires first and later producer invocation while the same flow is running, and verifies stopped
state during bounded test teardown. Without samples it checks invocation/state only. It does not
claim to verify idle behaviour, subsequent payload content or external delivery.

`flowTestInputMode: "sample-submission"` supplies fixture guidance for generated sample consumers
that implement `submitNow(String)` while running and a per-class `fixture-input-enabled` property
which disables automatic polling for deterministic tests. Submission itself requires no opt-in. It does not
inspect existing developer code: generated tests call the declared submitNow API directly.
Shared properties default those consumers to fixture-input-enabled=true; explicit false settings are preserved. This mode never selects the automatic observation-only scaffold.

### File-delivery flow-test guidance

A producer may declare `flowTestFileDelivery: true` when its observable outcome is delivered
files. For a single such producer, the scenario template emits a receiver-check outline using
shared `assertFileContents` / `assertDeliveredFileContents` helpers. FTP and SFTP declare this
capability; the generator does not select it by component name or implementing class. The
`supportsTestFtpServer` and `supportsTestSftpServer` capabilities select local server directory assertions.
The developer reviews expected text and filename patterns; custom external servers also need a receiver directory;
local-file consumers alone do not imply file output. These checks read UTF-8 files visible to
the test JVM; they do not implement remote FTP/SFTP access or binary comparison.

### Automatic verification contracts

`generatedVerificationTest_en.ftl`, `generatedVerificationSupport_en.ftl` and
`generatedVerificationMaven_en.ftl` produce the explicitly refreshed, Studio-owned structural
baseline under `generated/src/test`. The Maven template contributes test dependencies and
Surefire configuration to the existing application POM. `usesBuilderInFactory` determines whether a component getter can be asserted.
`verificationImplementationContract` optionally names the user implementation's raw interface
when different from `componentType` (for example `FilterRule`). These checks never infer custom
business input or start external endpoints. Runtime coverage remains explicitly not verified.

### Developer import migration rules

Directional migration JSON may declare `userImportPrefixes`, separately from model `typePrefixes`.
Keys and values must be Java package prefixes ending in a dot. These trusted rules are used
only when the developer opts in during preview. The shared engine edits import declarations
under `user/src/main/java`, preserving tests, literals, comments and other code. Do not use this
mechanism for behavioural API changes; add only mappings verified for the version pair.

`userImportApplicability` is required when import mappings are present, for example
`{"sourceMajor": 3, "targetMajor": 4}` for 3.x → 4.x. The loader rejects a rule whose
major-version direction does not match its source/target identities. Reverse mappings
use their own 4.x → 3.x declaration. This describes rule applicability; it does not widen
the set of tested/supported migration version pairs.

A filter can declare `flowTestPassThroughFilter: true` only when its standard implementation
always forwards the input. This lets the flow-test generator include `.filter(componentName)`
in an otherwise linear expected path. Debug Transition uses this capability. The default is
false: ordinary filters, routing, splitting and exception-resolution scenarios still need
explicit expectations. Developer-owned changes to the filter must be reviewed when completing
the test scaffold; this flag does not prove custom code is pass-through.

#### SMTP flow-test fixtures

`supportsTestMailServer` opts a component into local SMTP test generation and the generation
dialog checkbox. The pack's `localSmtpTestServerTemplate_en.ftl` owns the adapter contract:
`getConfiguration()` exposes the email connection setters used by that template. The generator
passes flow/component names, not Java implementation checks. A new component with a different
configuration API needs a corresponding pack adapter before enabling this capability.
Keep the GreenMail dependency in `flowTestPomTemplate_en.ftl` aligned with the endpoint's Mail
API, independently of JMS namespace changes. Both currently bundled email endpoints use Javax
Mail. The fixture must bind loopback, allocate its port, never forward messages, and close with
the test application or after startup failure.


#### SFTP flow-test fixtures

`supportsTestSftpServer` opts consumers/producers into the local SFTP generation choice.
The pack's helper configures the actual endpoint before flow startup; Ikasan builder proxies and producers
expose the SFTP configuration through `getConfiguration()`.
The configuration contract includes remote host/port, credentials, private key/passphrase,
known-hosts filename, preferred key exchange and source/output directory setters. Consumer
configuration also exposes `setMinAge(Long)`. `filenamePattern` and its externalised property
label supply input filtering. The core generator selects these behaviours by capability,
not component class name. Each endpoint gets a separate JUnit-owned server directory.
Keep helper APIs and SSHD dependencies aligned with the pack's Java/Ikasan baseline.

Consumers forwarding a raw Quartz timer context can declare `flowTestInputMode: "scheduled-context"`.
This generates synchronous `ScheduledEventFixture` input and fixture-text extraction for assertions.
Declare `flowTestInputModeInvalidatedByProperties: ["messageProvider"]` when a custom provider
changes the payload contract. This mode verifies processing, not scheduling accuracy.

## Runtime JSON import mappings

`ComponentMeta.runtimeConfigurationProperties` is an optional list of Studio properties verified to map to runtime configuration parameters. Names match exactly by default. The importer also requires the configuration JSON's `implementingClass` to match `runtimeConfigurationClass`, or the `configuration` property's `usageDataType` when no explicit class is declared. Declare only verified scalar or string-collection mappings for that pack's Ikasan API. Unknown fields and incompatible value types remain in the retained source snapshot and are reported, never guessed. The bundled mappings cover verified scalar settings for scheduled, JMS, FTP/SFTP, local-file, email and logging components.

`runtimeConfigurationMappings` declares only name discrepancies, as REST parameter → Studio property, for example `{"mailHost":"mailhost"}`. Targets must be listed in `runtimeConfigurationProperties`. Do not add identity mappings or multiple REST names for the same target; mappings cannot shadow another exact runtime parameter. Import and native model save/load use the same binding in both directions, keeping exported parameter names correct for Ikasan. Incorrect value types or configuration classes remain unapplied and require review. Unrecognised names are never matched fuzzily. Existing application-property labels and Java setter overrides remain independent of the runtime parameter name.

`runtimeConfigurationDefaults` optionally maps runtime parameter names to typed, non-null JSON defaults verified against the generated runtime construction for this pack. When the exact configuration class matches, an unmapped value equal to its declared default is preserved in provenance without an unnecessary review warning. This does not apply configuration values or add generator setters. Changed values, unknown fields and mismatched classes still require review; strings are not coerced into numbers or booleans. Verify both the runtime default and any builder overrides before declaring a default. Scheduled Consumer declares the empty `cronExpressions` list, empty `passthroughProperties` map, `persistentRecovery=true` and `recoveryTolerance=1800000` for V3, V4 and V5. FTP Consumer declares the same inherited scheduling/recovery defaults plus `provideRelativePath=false`, verified against `FtpConsumerConfiguration` and the FTP builder in all three versions.

Runtime implementation classes may identify several palette variants. The importer asks the developer to select among exact matches; configuration class names alone do not identify message-provider wiring. User implementations require the existing `requiresStub=false` contract to avoid replacement stub generation. See [runtime JSON import](../../../../../docs/IkasanRuntimeJson.md).

### String collection properties

Set `propertyDataType` to `java.util.List` or `java.util.Map`, with `usageDataType` set to `java.util.List<String>` or `java.util.Map<String, String>`. This explicitly selects structured string-collection editing, validation, JSON persistence and Java generation. Do not use it for collections of objects or class/bean references. Defaults must be arrays of strings or objects with string values. Lists preserve duplicates and order; maps require unique keys. Null means unset, while empty arrays/objects are explicit values.

Declare a property in `runtimeConfigurationProperties` only after checking the runtime configuration getter and builder setter for that pack. Verified collection mappings include Email Producer's three recipient lists and `extendedMailSessionProperties`, plus Spring JMS Producer/Consumer `connectionFactoryJndiProperties` and `destinationJndiProperties`. Complete JMS maps take precedence over the corresponding individual JNDI setters during generation, including defaults; empty maps must stay empty. Existing `usageDataType`-only list fields retain their original handling.

Collection properties with a `propertyConfigFileLabel` emit JSON in application.properties and are read by the component factory's `stringCollectionProperties_en.ftl` helper. Lists also accept older comma-separated deployment overrides. Collection setters without an external property label receive generated mutable Java collections. Test escaping, empty/unset, wrong entry types, legacy input and both runtime imports and Studio persistence before adding further mappings.

### Flow-test delivery strategies

`flowTestDeliveryStrategy` selects receiver assertions independently of the component's
Java implementation: `none`, `file`, `ftp`, `sftp`, or `smtp`. Bundled producers declare
this explicitly. Older packs without the declaration retain the legacy capability-flag
selection. The general scaffold delegates delivery interpretation to `FlowTestDelivery`.

SMTP components declare `flowTestRecipientProperties` as groups of alternative property
names, for example `[["toRecipient", "toRecipients"], ["ccRecipient", "ccRecipients"],
["bccRecipient", "bccRecipients"]]`. Each group contributes literal addresses from one
configured property (a string or list). Conflicting alternatives, duplicate addresses,
expressions, or non-simple addresses leave recipient review guidance instead of an inferred
count. Clear configurations generate one expected body per recipient. These expectations
are a generation-time snapshot, deliberately visible and editable in the developer's test;
configuration changes do not silently update developer-owned assertions.


## Environment-specific component settings

Give built-in scalar and string-list/map setter properties a `propertyConfigFileLabel`
so configured values are written to `application.properties` and injected into generated
factories. New keys should identify both the flow and the component to avoid collisions
between multiple instances. Preserve published keys when extending an existing pack.
Unset optional values must remain absent, while explicit zero and false values must survive.

Use `propertyDataType: java.util.Map` or `java.util.List` for string collections, as well as
the corresponding generic `usageDataType`; this enables structured editing and property
serialization. Do not encode Java expressions as collection values.

Keep Java implementations, custom configuration beans, class literals and configuration
resource identifiers as references/identity rather than property bindings. A custom
configuration bean remains an explicit extension point for developer-owned fields and
behaviour; exposing standard settings must not replace or overwrite it.

### Historical Studio property names

`legacyPropertyAliases` maps old Studio property names to canonical properties. Aliases are applied when reading legacy flat models and Studio document containers, and when converting legacy models for saving. Different values under both names are rejected rather than choosing one. Saves use canonical names. Alias sources cannot shadow current properties or form chains. Keep existing `propertyConfigFileLabel` strings when renaming a property so application-property overrides remain compatible. The bundled packs correct `hasAttachments` to `hasAttachment`, SFTP `knownHostFilename` to `knownHostsFilename`, and SFTP producer `cleanupChunks` to `cleanUpChunks`. See the [property naming audit](../../../../../docs/MetaPackPropertyNamingAudit-2026-10-26.md) for source checks and intentional API differences.

The historical aliases are temporary for this development cycle and are scheduled for removal in the next release. Do not use `legacyPropertyAliases` for REST naming differences. Before removal, resave development projects/examples with canonical names and complete the cleanup checklist in the naming audit.
