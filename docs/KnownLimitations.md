# Known limitations

Current candidate, reviewed 14 September 2026:

- Bundled packs cover Ikasan V3.3.9 and V4.1.6. Other versions and future IntelliJ builds are not implicitly verified. See [Supported versions](SupportedVersions.md).
- Type warnings and converter suggestions use declared metadata. They do not prove runtime compatibility, inspect arbitrary business logic or infer all Java inheritance relationships. Suggestions do not cross branching routers. See [Type guidance](TypeGuidance.md).
- Recipes cover explicit payload conversions, not arbitrary business mappings. Supplied JMS/local-file recipes are intended for small messages and enforce a 16 MiB limit; local-file recipes require a single-file batch. See [Converter recipes](ConversionRecipes.md).
- The FTP harness is plain FTP only. Mail testing depends on a separately downloaded MailHog executable and a shared inbox port. JMS readers consume/divert messages and still require a broker. Synthetic injection bypasses acquisition rules. See [Harnesses](Harnesses.md).
- Generated application-specific stubs require implementation. Existing developer code is not automatically made compatible by changing Ikasan versions. Debug copying is best-effort and can retain shared state.
- Automatic model backups retain only three saved predecessors. They do not replace version control or restore external-system state.
- The automated release checks do not establish clean install/upgrade/uninstall behaviour on every operating system. The current [release audit](ReleaseAudit-2026-09-14.md) records outstanding interactive checks, dependency-notice review and API deprecations.

Report reproducible problems with the candidate/IDE/pack versions and reviewed [diagnostics](DiagnosticsAndPrivacy.md). This page describes known boundaries, not a guarantee that no other defects exist.
