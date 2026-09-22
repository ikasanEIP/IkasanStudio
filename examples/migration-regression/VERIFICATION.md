# Verification evidence — 22 September 2026

The final fixture was generated and run against **Ikasan 3.3.9 and 4.1.6** using
Studio's headless migration/rendering engine and the actual resolved Maven dependencies.

| Result | 3.3.9 | 4.1.6 |
| --- | --- | --- |
| Maven compile and actual-flow acceptance | PASS | PASS |
| Core pipeline, two filters, both routers and branch isolation | PASS | PASS |
| Stored exclusion and subsequent valid delivery | PASS | PASS |
| JMS object delivery and XML conversion | PASS | PASS |
| Event source and local-file contents | PASS | PASS |
| FTP and SFTP round trips, duplicate suppression and later files | PASS | PASS |
| Captured SMTP delivery, two messages | PASS | PASS |
| All 12 flows running while idle in the test application | PASS | PASS |
| Model and developer-source unchanged by verification | PASS | PASS |

Each run reported **14 passing checks**. The comparison reported five passing checks:
both runs passed, all **26 developer-source files** retained identical SHA-256 hashes,
model structure/settings matched after supported version/type normalization, and the
same acceptance checks ran. Four controller regression tests also passed, including
rejection of changed user source, unexpected model edits, missing checks and failed runs.

The `serve` command was separately smoke-tested: FTP 2121, SFTP 2222 and SMTP 2525
started on loopback, and were then stopped. No personal testing configuration was used.

During fixture development, the tests caught an input provider advancing on rollback
(instead of retaining the event for exclusion), a local-file test confusing filenames
with file contents, and an SFTP test server offering only legacy RSA signatures.
Those fixture defects were corrected without weakening application-client settings or
changing production Studio code. The server offers modern RSA signatures alongside
the compatibility signature needed by the older client.

Limits: this evidence covers headless generation/migration and real runtime behaviour,
not clicking IntelliJ's migration UI or checking every visual endpoint marker. Spring
context closure returned; independent graceful JVM worker termination is not certified.
The test JVM is managed by Surefire. SFTP coverage uses password authentication, not
production key authentication, and services are isolated local implementations.

Local detailed evidence (under ignored build output):

- `build/final-baseline-report/report.md` and `report.json`
- `build/final-target-report/report.md` and `report.json`
- `build/final-comparison/report.md` and `report.json`

Re-run the README workflow for new candidates; this dated record is not a permanent
claim that future changes or versions pass.
