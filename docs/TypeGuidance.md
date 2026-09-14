# Type warnings and converter suggestions

Studio compares the upstream component's declared output with the next component's declared input. A warning means the metadata suggests a mismatch: for example, a JMS message cannot simply be treated as the email payload an Email Producer expects. Inspect the warning and the components' type-related properties before running.

These checks use the selected pack's metadata, including property-dependent declarations such as custom class types and JMS content-conversion settings. They do not execute your Java or prove that an arbitrary custom implementation returns its declared type. Missing or generic metadata can limit the guidance; no warning is not proof of compatibility.

To resolve a mismatch:

1. Check the consumer's content-conversion settings and any declared input/output types against the real payload.
2. Insert or select a generic **Converter** and open **Conversion recipe**. Entries marked **suggested** match the inferred input and downstream declaration. A new converter can preselect the highest-ranked suggestion in its draft; review it, particularly email body versus attachment choices.
3. Review source/target types and recipe settings, then apply with **Update Code**. Existing implementations use the normal regeneration confirmation. Inspect the resulting Java and test representative payloads.
4. If no recipe implements your intended mapping, clear the selection and implement a custom converter. Changing a type label alone does not transform an event.

Matching is offline and based on declared Java type names. It does not infer arbitrary inheritance or assume all router branches have the same contract. Existing saved choices are preserved; an unknown or incompatible saved recipe blocks generation instead of silently generating an empty implementation.

See [Converter recipes](ConversionRecipes.md) for payload coverage, filename behaviour, charset settings and size limits, and [JMS object messages](JmsObjectMessages.md) for serialization and trusted packages.
