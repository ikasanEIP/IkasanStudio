# Known limitations

These are the public beta's documented feature boundaries. They are not a guarantee that no other defects exist. Review them before using Studio on an existing project, and keep your model and developer code in version control.

## Compatibility and generation

- [Runtime JSON import](IkasanRuntimeJson.md) accepts core Ikasan/Dashboard topology and separate configuration exports. They do not contain all source code or Spring wiring. Ambiguous variants require a choice; only explicitly supported configuration mappings are applied. Originals are retained for review. New saves use a three-document container; older Studio builds cannot read it. Restore the earlier project revision before downgrading Studio.

- Bundled packs cover Ikasan V3.3.9 (Java 11) and V4.1.6 (Java 17). Other Ikasan versions and future IntelliJ builds are not implicitly verified. See [Supported versions](SupportedVersions.md).
- Generated application-specific stubs require business implementation. Type warnings and converter suggestions use declared metadata; they do not prove runtime compatibility, inspect arbitrary business logic or infer all Java inheritance relationships. Suggestions do not cross branching routers. See [Type guidance](TypeGuidance.md).
- Recipes cover explicit payload conversions, not arbitrary business mappings. Supplied JMS/local-file recipes are intended for small messages and enforce a 16 MiB limit; local-file recipes require a single-file batch. See [Converter recipes](ConversionRecipes.md).
- Migration updates supported model/configuration structures and can apply selected recommended replacements. It does not automatically make arbitrary developer code compatible. Review changes, compile and run business tests. See [Ikasan version migration](IkasanVersionMigration.md).

## Testing and development services

- Generated verification tests check what can be established from the model; they do not establish business correctness or external delivery. Developer-owned flow tests need meaningful fixtures, expectations and review. Preserve the before-upgrade baseline until comparison is complete. See [Generated verification](GeneratedVerification.md) and [Flow testing](IkasanFlowTesting.md).
- The interactive FTP harness is plain FTP only. The interactive mail harness downloads MailHog and uses a shared inbox port. JMS readers require a broker and consume/divert messages. Synthetic injection bypasses source acquisition rules. These IDE tools differ from generated JUnit fixtures, which can start local test FTP, SFTP and SMTP servers without pre-existing external servers. See [Harnesses](Harnesses.md).
- A producer invocation does not by itself prove that a file or message arrived. Verify the received result where delivery matters. Debug payload copying is best-effort and can retain shared state.
- Ordinary flows should remain running while idle and accept later input. An unexpected stop after the first batch is a failure to investigate, not proof of successful long-lived operation.

## Recovery, privacy and release evidence

- Automatic model backups retain only three saved predecessors. Backups and migration snapshots do not replace version control or restore external-system state, such as messages already sent.
- AI integration is optional. Connected clients can access model data through the enabled bridge and have their own data-handling policies. Models, properties, logs and test payloads can contain sensitive information; review anything shared. See [Diagnostics and privacy](DiagnosticsAndPrivacy.md).
- Automated checks do not establish clean installation, upgrade and uninstall on every operating system. The [14 September release audit](ReleaseAudit-2026-09-14.md) records historical results and outstanding checks for that candidate only. Each beta needs fresh [release verification](ReleaseCandidateVerification.md).

## Report a problem

Use the [bug report form](https://github.com/IkasanEIP/IkasanStudio/issues/new?template=bug_report.yml). Include:

- Ikasan Studio version, IntelliJ IDEA version/edition and full build number.
- Operating system and selected Ikasan version (or “not selected” if setup failed first).
- Reproduction steps, expected behaviour and actual behaviour.
- Relevant screenshots or reviewed [diagnostics](DiagnosticsAndPrivacy.md), with credentials and customer data removed.

For security vulnerabilities, follow [SECURITY.md](../SECURITY.md) instead of opening a public issue. Studio's source is licensed under the [BSD 3-Clause License](../LICENSE.txt); third-party components retain their own licences.
