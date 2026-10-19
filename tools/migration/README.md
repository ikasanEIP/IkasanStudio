# Ikasan Studio offline migration tools

These tools are bundled with Ikasan Studio. Export them using
**Tools → Ikasan Studio → Export Offline Migration Tools…**. No additional download is
needed to export or perform migration. Keep `lib/`, `meta-packs/` and `meta-packs.json` alongside `bin/` in an IDE export. A configured project’s exact custom pack is included automatically, using the saved model selection.

Requirements: Java 17+ for migration; Python 3.9+ and Maven for verification. Maven needs
your application's dependencies in its local cache or an accessible corporate repository.
Your own tests determine which services must be available and what behaviour is verified.

For bundled or exported custom packs, first validate the model or inspect generated output in a fresh directory outside the project:

```sh
/path/to/tools/bin/studio-cli validate --project /path/to/project
/path/to/tools/bin/studio-cli render --project /path/to/project --output /path/to/new-preview
```

These commands check metadata/revision locks and render an inspection preview; they do not run application tests or rewrite the project. Pack checksums and IDs/revisions are verified automatically. Custom JARs are resources, not executable classpath dependencies. Missing or altered artifacts require a fresh export. The tools folder is relocatable and needs no IntelliJ installation. Application dependencies and external services must still be supplied separately. Custom packs do not widen the supported V3.3.9 ↔ V4.1.6 migration paths.

From your Studio project's directory (replace `/path/to/tools` with this folder):

```sh
python3 /path/to/tools/bin/studio_upgrade.py verify . --report migration-before
/path/to/tools/bin/studio-cli preview --project . --to V4.1.6 --plan migration-plan.json --update-user-imports true
# Review the findings and migration-plan.json.diff. Save and close the project in IntelliJ.
/path/to/tools/bin/studio-cli apply --plan migration-plan.json
python3 /path/to/tools/bin/studio_upgrade.py verify . --report migration-after
python3 /path/to/tools/bin/studio_upgrade.py compare migration-before/report.json migration-after/report.json --plan migration-plan.json --report migration-comparison
```

Use `studio-cli.bat` on Windows. Set `JAVA_HOME` appropriately for each Maven build.
You can also migrate through Studio's UI and use only the Python before/after verifier.
The **Apply recommended migration search and replace (review changes before applying)**
option is selected by default in Studio; it currently updates compatible Java imports only.
Projects with no successful executed tests are reported as **INCOMPLETE**.

Read [Command-line migration guide](https://github.com/IkasanEIP/IkasanStudio/blob/main/docs/CommandLineMigration.md) for profiles, report locations,
recovery snapshots, limitations and the IDE-based workflow. The standard command above
includes compatible Java import updates in `user/src/main/java`; review them in the preview
diff before applying. Other developer code and tests are preserved. After migration, review
remaining API compatibility, reload Maven and select the target JDK in IntelliJ. Existing report directories and export destinations are never replaced.
