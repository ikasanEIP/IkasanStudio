# Meta-pack property naming audit — 26 October 2026

Reviewed the V3.3.9, V4.1.6 and V5.0.1-SNAPSHOT component descriptors against the approved local Ikasan 3, 4 and 5 source trees. The audit examined explicit setter-name overrides and checked the names of generated setters against methods present in those reference trees. Configuration getters and the relevant builder implementations were inspected for the corrections below. This is a property-naming review, not a replacement for generated-code compilation or release compatibility verification.

## Corrected Studio property names

| Component | Historical name | Canonical name | Ikasan setter |
| --- | --- | --- | --- |
| Email Producer | `hasAttachments` | `hasAttachment` | `setHasAttachment` |
| SFTP Consumer and Producer | `knownHostFilename` | `knownHostsFilename` | `setKnownHostsFilename` |
| SFTP Producer | `cleanupChunks` | `cleanUpChunks` | `setCleanUpChunks` |

All corrections apply to all three packs. Pack-declared `legacyPropertyAliases` load historical flat models and Studio document containers without losing values. Conflicting historical/canonical values are rejected. Saves use canonical names; existing application-property labels remain unchanged to preserve overrides. The corrected fields also participate in runtime configuration import.

## Intentional differences retained

- `mailhost`: matches the Email Producer builder method `setMailhost`; the configuration getter/REST field is `getMailHost` / `mailHost`. This is an Ikasan API naming difference, not an incorrect generated call. Its REST-to-Studio mapping is declared separately in `runtimeConfigurationMappings`.
- `mailPassword` and `mailRuntimeEnvironment`: descriptive Studio names explicitly mapped to `setPassword` and `setRuntimeEnvironment`.
- `configurationId`: Studio/topology terminology mapped to the runtime setter `setConfiguredResourceId`.
- `ftps`: normal Studio spelling mapped to the acronym-based method `setFTPS`.

## Reference source locations

For each approved version tree under `/home/hidavi/dev/ws/ik3/ikasan3`, `/home/hidavi/dev/ws/ik4/ikasan4` and `/home/hidavi/dev/ws/ik5/ikasan5`:

- `ikasaneip/component/endpoint/email-endpoint/.../EmailProducerConfiguration.java` and `ikasaneip/builder/jar/.../EmailProducerBuilderImpl.java`.
- `ikasaneip/component/endpoint/filetransfer/sftp/.../SftpConsumerConfiguration.java`, `SftpProducerConfiguration.java`, and their consumer/producer builder implementations.

## Setter-name scan coverage

- V3.3.9: 41 component descriptors, 268 non-user-class setter declarations checked; 0 setter names absent from the reference source tree.
- V4.1.6: 41 component descriptors, 268 non-user-class setter declarations checked; 0 setter names absent from the reference source tree.
- V5.0.1-SNAPSHOT: 41 component descriptors, 268 non-user-class setter declarations checked; 0 setter names absent from the reference source tree.

The scan checks name existence across the reference tree; individual component ownership and method semantics require inspection, as performed for the corrections above.


## Remaining REST-to-Studio naming differences

A follow-up inspection of the 23 configuration-bearing component descriptors in each pack,
configuration getters (including inherited getters), and relevant builder implementations found
these mappings consistently across Ikasan 3, 4 and 5. They are now implemented as `runtimeConfigurationMappings`;
`legacyPropertyAliases` handles historical Studio model names and does not implement the REST mappings.

| Component | REST configuration parameter | Studio property | Difference |
| --- | --- | --- | --- |
| Email Producer | `mailHost` | `mailhost` | Capitalisation |
| FTP Consumer and Producer | `FTPS` | `ftps` | Acronym capitalisation |
| Email Producer | `password` | `mailPassword` | Studio-qualified name |
| Email Producer | `runtimeEnvironment` | `mailRuntimeEnvironment` | Studio-qualified name |
| Email Producer | `subject` | `mailSubject` | Studio-qualified name |
| Email Producer | `mailFrom` | `from` | Different API naming |
| Email Producer | `mailTransportProtocol` | `transportProtocol` | Different API naming |

The supplied V5 runtime import snapshot demonstrates `mailHost` and `FTPS` mismatches.
Other fields may be absent from a particular export because their values are unset; that
absence does not establish that the importer supports those mappings.

### Implemented matching contract

- Preserve runtime instance `componentName` verbatim. It is a developer-assigned name,
  not the palette variant name. Resolve component variants from exact component type and
  implementing class, with linked configuration class evidence for shared implementations.
- Match configuration parameters exactly first, then use an explicit runtime-name mapping
  declared by the selected meta-pack/component. Validate the configuration class and value
  type. Reject conflicting/ambiguous assignments instead of silently choosing one.
- Apply the runtime-name mapping consistently when importing, reading/writing native model
  configuration records, and exporting; saved runtime parameter names should remain correct
  for the Ikasan API. Keep historical Studio-name aliases separate.
- Do not automatically apply fuzzy matches or globally lowercase Java property names.
  Similar fields such as `mailSmtpHost` and `mailPopHost` have different meanings. JavaBean
  acronym names can also differ legitimately. Fuzzy candidates could be offered as review
  suggestions, but need not be part of the automatic import path.
- Treat shape differences separately. JMS JNDI maps now import as complete maps. Email
  singular recipient convenience setters and plural recipient lists are different shapes,
  not spelling aliases. A runtime field without any Studio equivalent (for example the
  current FTP Consumer `provideRelativePath`) remains a capability gap requiring review.

These declarations belong to the meta-packs. Runtime import would still require only the
REST responses and installed meta-pack; it would not inspect the original application's
source code or POM.


## Required historical-alias cleanup for the next release

The project owner has requested removal of historical Studio-name aliases in the next release. They remain temporarily available in development builds so projects can be opened and saved with canonical names. REST name mappings are independent, permanent pack contracts only where names differ.

Before publishing the next release:

1. Resave checked-in examples and development projects using canonical names, preserving values and application-property labels.
2. Remove `legacyPropertyAliases` declarations, the metadata/schema/validation support, the `ComponentPropertyAliases` helper and its parser integration. Remove the associated scalar compatibility exception and historical-alias tests; retain canonical-name and REST round-trip tests.
3. Verify the documented upgrade path for older saved models, including rejection/recovery rather than silently losing old settings. Do not describe the new release as accepting historical names once support is removed.
4. Preserve `runtimeConfigurationMappings` and the separate compatibility path for previously Studio-owned values moving into native runtime records. Re-run generation and import/export checks for all three packs.
