# Generated verification baseline

Studio owns the verification package and resources/studio-verification under generated/src/test. Generate/refresh explicitly; running tests never regenerates them.
Commit these files. Refresh replaces verification tests without retaining a backup by default.
Select "Archive existing tests" in the IDE confirmation to preserve the previous test tree, including temporary developer corrections.

This first version verifies flow/component factory signatures and declared user implementation interfaces.
Classes are loaded without initialisation; Spring and external services are not started.
These checks cover structure and interface compatibility only.
Add delivery, routing, exclusion and business scenarios in developer-maintained user-flow-tests.
Their coverage depends on the scenarios implemented; passing structural checks does not establish it.
Maven Surefire reports passed/failed/skipped checks in target/surefire-reports.

Run from the project root: mvn -pl generated -am test
BASELINE_MODEL_SHA256 in GeneratedVerificationSupport identifies the saved model.json at generation.
It hashes UTF-8 model bytes, not Java sources or compiled classes; baseline.properties stores the same hash.
Formatting-only model changes also change the hash. A mismatch warns; assertions still run unchanged.
Before migration run and save target/surefire-reports outside target. Upgrade the application, then run
these same tests again. Save the after reports separately and compare failures and skipped checks.
Only after comparison, optionally use Generate/Refresh Verification Tests to establish a new baseline.
A model-hash warning is expected after migration and is not itself a failed test.
If an API change prevents the old tests running, retain that failure evidence before adapting tests.
Interface incompatibilities are failures to review, never silently rewritten expectations.
Ordinary generation and migration preserve this directory. Business tests remain in user-flow-tests.
