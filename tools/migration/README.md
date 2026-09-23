# Ikasan Studio offline migration tools

These tools are bundled with Ikasan Studio. Export them using
**Tools → Ikasan Studio → Export Offline Migration Tools…**. No additional download is
needed to export or perform migration. The `lib/` directory must stay alongside `bin/`.

Requirements: Java 17+ for migration; Python 3.9+ and Maven for verification. Maven needs
your application's dependencies in its local cache or an accessible corporate repository.
Your own tests determine which services must be available and what behaviour is verified.

From your Studio project's directory (replace `/path/to/tools` with this folder):

```sh
python3 /path/to/tools/bin/studio_upgrade.py verify . --report migration-before
/path/to/tools/bin/studio-cli preview --project . --to V4.1.6 --plan migration-plan.json
# Review the findings and migration-plan.json.diff. Save and close the project in IntelliJ.
/path/to/tools/bin/studio-cli apply --plan migration-plan.json
python3 /path/to/tools/bin/studio_upgrade.py verify . --report migration-after
python3 /path/to/tools/bin/studio_upgrade.py compare migration-before/report.json migration-after/report.json --plan migration-plan.json --report migration-comparison
```

Use `studio-cli.bat` on Windows. Set `JAVA_HOME` appropriately for each Maven build.
You can also migrate through Studio's UI and use only the Python before/after verifier.
Projects with no successful executed tests are reported as **INCOMPLETE**.

Read [CommandLineMigration.md](CommandLineMigration.md) for profiles, report locations,
recovery snapshots, limitations and the IDE-based workflow. Migration does not overwrite
`user/` code; after migration, review API compatibility, reload Maven and select the target
JDK in IntelliJ. Existing report directories and export destinations are never replaced.
