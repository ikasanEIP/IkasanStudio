Add synthetic REST metadata/configuration fixtures using the exact shared implementation and configuration
class names. Declare verified runtimeConfigurationProperties and discrepancies via runtimeConfigurationMappings.
Test exact class matching, omitted value versus null, false/zero, wrong types, unknown fields and save/reload.
Preserve unknown configuration for review and existing implementation references. Do not fuzzy-match names,
copy real production values into tests or claim constructors/wiring were recovered from REST alone.
Run the contract/build tests, then a representative application's first delivery, idle readiness and later
delivery in one running application, with explicit stop and resource cleanup. Record incomplete prerequisites.
