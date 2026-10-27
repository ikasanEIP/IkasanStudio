# Troubleshooting without exporting private material

**Maven/Python/JDK missing:** use approved installations. The kit contains neither their installers
nor permission to bypass the corporate firewall. verify.py requires Maven running on JDK 17.

**Inventory mismatch before any edits:** verify the downloaded ZIP's SHA-256 and extract it again.
Do not overwrite checksums to hide an unexpected change. Hashes detect accidental changes; they are
not a publisher signature. After deliberate starter changes use --build-only; keep the original ZIP.

**Offline dependency/plugin missing:** the public baseline was verified. A private library or new
plugin/version can require additional artifacts. Use the approved internal Maven provisioning route,
pointing it at work/maven-repository. Do not copy Maven settings or credentials back into the public kit.
Maven's error can include internal URLs/coordinates; keep private error logs inside work.

**IDE Maven cannot resolve Studio artifacts:** set the IDE's Maven local repository to the full
path of work/maven-repository. The terminal verifier's private local-repository argument does not
change IntelliJ's global Maven settings. Normal sample application development needs its own complete
application dependencies too; the kit warms only the included authoring/build tests.

**Installer rejects an ID:** use a distinct custom ID. Bundled IDs cannot be overwritten. Installing a
pack is not installing its business library; configure generated-application dependencies separately.

**Revision lock failure:** restore the recorded pack revision or use Studio's reviewed adoption flow.
Do not edit revision strings just to bypass validation. Do not replace released bytes under one revision.

**Factory creates the standard component:** inherited setters do not prove a standard builder can
construct a subclass. Implement the correct direct/factory/adapter construction path and assert its type.

**Metadata asks for unsupported object/map types:** keep them in an explicit configuration bean or add a
small runtime adapter. Do not serialize arbitrary object trees into a string field as a substitute.

**Copilot cannot access source or execute commands:** use the supported Chat mode, attach the public
instructions/schema/example and paste the numbered prompts individually. Run validation yourself in
the terminal. Use private source with Copilot only if your organisation approves that processing.

**Need outside help:** reproduce with public/synthetic classes and values. Keep actual private classes,
exports, credentials and generated private pack on the VDI. Do not OCR private implementation details
or production configuration into an external conversation.
