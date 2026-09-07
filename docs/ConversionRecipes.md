# Converter recipes

Select a generic **Converter** on the canvas, or drop a new one, and use **Conversion recipe** in its properties. Matching recipes are marked **suggested** using the inferred input and the downstream component's declared input types. For a new converter, one exact source/target match is selected automatically in the draft. Ambiguous matches (such as email body versus attachment) remain explicit choices. Existing saved recipes and custom converters are preserved. The selector stays available after creation. Selecting a recipe sets the source and target types; it does not save the model or replace Java code. The explanatory text remains visible below the recipe settings.

Use **Update Code** to apply changes. Existing implementation changes use Studio's normal regeneration confirmation and backup controls. Clearing the selection chooses a custom converter. Changing types to something incompatible requires choosing another recipe or clearing the selection. An unknown or incompatible saved recipe stops generation with an actionable error; it never silently falls back to a blank stub.

Recipes compose a content-extraction template with a payload-construction template into **one visible Converter and one user implementation class**. There is no runtime recipe engine, network lookup or implicit chain of extra canvas components.

## MVP coverage

Both V3.3.9 and V4.1.6 contain 29 explicit recipes, using the correct `javax.jms` or `jakarta.jms` namespace.

| Producer | Supported construction |
| --- | --- |
| FTP and SFTP | `DefaultPayload` containing bytes, a UUID and the `fileName` attribute |
| Email | Text body or binary attachment, explicitly selected even though both return `EmailPayload` |
| JMS | String, byte array, or Map extracted from a JMS MapMessage; already extracted maps can be forwarded |
| Logging and Dev Null | Accept arbitrary content; conversion is unnecessary unless the developer wants to change its representation |
| Generic Producer | Matching uses its declared input type; a custom implementation defines its business contract |

Reusable sources are String, byte array, file-transfer Payload, JMS Message, already extracted JMS content, and a single-file Local File Consumer batch. Local-file recipes reject empty or multi-file batches rather than dropping files. Split a batch first or supply custom batch logic. Scheduled job contexts, arbitrary generic-consumer events, structured application objects and unsupported producer types require a custom mapping. Studio does not invent a business document from a timer event.

Text defaults to UTF-8; the charset is configurable. File output and attachments retain an incoming filename, otherwise use the configurable fallback (`message.dat`). Attachment recipes expose the MIME type (default `application/octet-stream`) and accompanying email body. Irrelevant settings are disabled and preserved when switching recipes. These are literal values, escaped when generating Java, not executable expressions. Naming files dynamically or producing JSON/XML from domain objects belongs in a custom converter.

Nulls and unsupported content raise `TransformationException`. Maps are not implicitly converted using `toString()`, and JMS ObjectMessage deserialization is not attempted. Malformed text is rejected rather than replaced. JMS byte messages are read in a loop, including partial reads, and their read cursor is reset. Supplied JMS and local-file recipes are intended for small messages, with a 16 MiB check; use custom streaming logic for large files.

## Metadata and maintenance

`ConversionRecipeMeta` retains a stable `id`, source and target Java types, display name, help and root template. Composed recipes additionally declare `extractionTemplate`, `constructionTemplate` and `configurationProperties`. Configuration fields use normal component property metadata and set `affectsUserImplementedClass=true`. Multiple recipes may share a source/target pair; IDs must remain unique.

The extractor supplies `Object body` and may update `filename`; the constructor returns the declared target. Templates may reuse these steps for other producers that accept the same payload contract. Producer names are not hardcoded in the matcher. Existing producer `expectedInputTypes` and property-driven input declarations provide the contracts; file and attachment constructors enforce the filename invariant.

Matching is conservative and offline: declared type names, not PSI class loading or arbitrary inheritance guesses. Historic `(auto-converted)` display annotations are normalized for compatibility, but recipe metadata contains Java types only. Suggestions do not cross branching routers or assume that every branch has the same contract. All recipes remain available for deliberate type changes.

The offline AI component catalogue includes recipe metadata and configuration choices after regeneration. Existing recipe IDs are preserved. Existing developer-owned Java is not rewritten merely by installing the plugin. Recipes use dependencies already supplied by the selected Ikasan pack; a future recipe requiring another library must also provide that dependency through the pack's build templates.

## Verification

`./gradlew test validateMetaPacks buildPlugin` covers template rendering, incompatible saved selections, escaping, draft-only editing, every recipe's type-field validation, and both meta-packs.

To compile and exercise generated classes against locally cached real Ikasan dependencies:

```sh
STUDIO_RECIPE_EXPORT=/tmp/studio-recipe-sources ./gradlew test --tests '*ComposedConversionRecipeTest' --rerun-tasks
python3 scripts/verify-conversion-recipes.py /tmp/studio-recipe-sources
```

The script requires `java`, `javac` and the corresponding artifacts in `~/.m2/repository`; it never downloads dependencies. It checks every recipe, filename/content preservation, email attachments, partial JMS byte reads, map forwarding, nulls, unsupported objects, malformed text and multi-file rejection. Generated code is compiled with `--release 11` for 3.3.9 and `--release 17` for 4.1.6.

Before release, exercise insertion in a live IntelliJ editor, selection changes followed by Cancel/Apply, the regeneration confirmation, and keyboard navigation in both themes. Automated widget checks do not replace that IDE review.
