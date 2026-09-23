# MigrationRegression

Open this Maven project in IntelliJ with Ikasan Studio installed. The baseline model
is `generated/src/main/model/model.json`; custom implementations live in `user/`.

The reusable verification suite is in `verification/`. The included controller works from this project directory:

```
python3 fixture.py serve .
python3 fixture.py verify .
```

Start services before running the module manually. Automated verification starts its
own isolated services and application. It does not test or stop your currently running
IDE application. See the fixture README in the Studio repository for migration and
before/after comparison commands. Do not edit generated sources to make a test pass.

## Before and after migration

Run these commands **from this project root**. Reports then appear alongside `pom.xml`
in IntelliJ, rather than in the parent directory:

```sh
python3 fixture.py verify . --report migration-before
# Migrate this project in Studio and wait for generation to finish.
python3 fixture.py verify . --report migration-after
python3 fixture.py compare migration-before/report.json migration-after/report.json --report migration-comparison
```

Open `migration-before/report.md`, `migration-after/report.md` and
`migration-comparison/report.md`. Use fresh directory names for another run; existing
evidence is never overwritten. Verification without `--report` also keeps its timestamped
output inside this project, under `acceptance-reports/`.

Both wiretap types decorate **Core Pipeline → Enrich Order**, before and after enrichment.
Tests verify twelve persisted captures and twelve logging-wiretap messages across two
batches. Automated verification enables the `debug` profile; for interactive logging
wiretaps add `--spring.profiles.active=debug` to the Application program arguments.
