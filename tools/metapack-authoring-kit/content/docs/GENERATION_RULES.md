# What the pack should describe

A component descriptor describes a concrete palette entry. It is not a place to implement the
shared library's business rules. The library continues to own that behaviour; Studio configures it.
Read schemas/component.schema.json and reference/METAPACK.md alongside the public example.

| Evidence from the library | Pack treatment |
| --- | --- |
| Actual concrete runtime class | implementingClass; use the runtime class, not the builder class |
| Builder really constructs that concrete class | Existing builder template can be used if signatures match |
| Usable direct constructor and supported setters | Direct construction, as in Example Uppercase Converter |
| Constructor arguments, Spring references or unusual configuration wiring | Dedicated supported template or separate internal runtime adapter; no guessed schema fields |
| Scalar configuration parameter | Verified propertyDataType, setter/config binding and external property label |
| List<String> or Map<String,String> | Supported typed collection metadata; test empty, escaping and external overrides |
| Arbitrary nested objects, expressions or non-string map entries | Explicit configuration bean/adapter or documented unsupported shape; do not pretend it is a string map |
| Existing implementation referenced by runtime import | Preserve it; do not generate a replacement business stub |
| Runtime Maven library | jarDependencies on generated output, plus explicit authoring test dependencies |
| Deployment credentials | Secret/password treatment and synthetic fixture placeholders; no real values in the pack |

For REST-aware components, runtimeConfigurationProperties declares verified bindings.
runtimeConfigurationClass must match the reported configuration class, or it is derived from the
configuration property's usageDataType. runtimeConfigurationMappings is REST name → Studio name,
only for actual discrepancies. Do not fuzzy-match names. runtimeConfigurationDefaults suppresses
review only when the same configuration class and a verified typed default match. Inherited builder
overrides matter: a Java field initializer alone may not describe the constructed component's default.

A missing value member or explicit JSON null is unset. Preserve it without inventing 0, false or an
empty string. Conversely false and 0 are real configured values. Unknown values remain provenance
and actionable review; copying configuration into provenance does not mean it was applied.

Use the starter contract and archive tests as references, not their passing results as certification of
new components. Include an assertion on the constructed runtime class and meaningful property values,
not merely a generated source substring. Keep production/shared classes outside the resource-only JAR.

The public kit does not contain a universal Java-to-meta-pack extractor. Copilot inventories and
implements against explicit evidence, one component at a time. A REST export alone cannot reveal
constructors, required injected dependencies, every default or all builder behaviour.
