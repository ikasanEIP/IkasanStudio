# Flow tests

These are developer-owned scaffolds using the actual generated application.
Flow tests live in org.ikasan.studio.flowtests; module wiring lives in its support package.
Reusable fixtures, assertions and local test servers live in support.utils.
Utilities are created only when missing and are preserved during shared module setup refreshes.
Model edits do not require utility regeneration; Studio fixes or Ikasan upgrades may require reviewed updates.
Existing helpers in the old package are retained. Port custom setup before adopting the new support package.
Scenario tests use IkasanFlowTestRule; continuous-source observation tests use a bounded counting listener.
Standard scenarios surface component failures with the flow/component, incoming payload type and original cause.
This is a success-scenario check; deliberately expected failure/recovery paths need custom observation.
JMS input normally starts with text. An initial ObjectMessageToObjectConverter selects sendObject and
object payloads in createInputOutputBatches: populate its TODO with serializable business objects.
A downstream declared input class is a fixture hint, not automatically constructed business data.
Existing utils/JmsFlowTestSupport.java is preserved. If it lacks sendObject, archive it outside the .java
extension and generate again to recreate the missing helper; review/reapply customisations.
New tests run immediately, starting the configured application and fixtures; no review flag is required.
Direct self-generating-source/discard-sink observation tests have only TODO 1–2: review settings and observation expectations.
They check any meta-pack initial payload sequence, count later events, and stop the test flow only during teardown.
Other scenarios follow TODO 1–4 in the Java test: define input collections per batch, select producer outputs and expected results,
review component-path expectations, and review shared test connections. Tests run immediately; use failures to guide completion of the TODOs.
Configure shared test connections in src/test/resources/module-test.properties (UTF-8).
Studio flow/component renames update recognised test names, property references and fixture directories.
Business assertions, expected values and custom test class names are preserved.
Flow renames also rename test classes/files still using the generated name, updating Java references
and supported run configurations. Conflicting names block the rename.
Review any custom references reported by Studio, then rebuild and rerun existing tests.
For remaining stale settings after custom or external refactoring, use Refresh test properties (archive existing) in Generate Flow Tests
to rebuild settings from the model while keeping business tests and fixtures.
Review custom settings in the properties backup before running tests.
For plain FTP endpoints, set test.ftp.enabled=true to start a disposable loopback FTP server per test.
Local FTP consumers use test.ftp.consumer.min-age-seconds=0 so complete fixtures are immediately discoverable.
This overrides all module FTP connections and directories; inspect/seed context.getBean(LocalFtpTestServer.class).root().
Closing the context stops the server; JUnit then removes its temporary files after the test. FTPS requires a separate service.
The Generate Flow Test dialog can enable local FTP and regenerate shared setup, archiving existing files.
Unchecked leaves settings unchanged. Selecting FTP preserves unrelated properties and custom credentials.
For SFTP endpoints, select the local test SFTP option or set test.sftp.enabled=true.
This starts an embedded SSH server with a generated trusted host key and separate temporary endpoint directories.
No installed SSH service or Docker is needed. test.sftp.consumer.min-age-seconds defaults to zero.
localSftpDirectory(context, flowName, componentName) exposes each endpoint directory for input/delivery assertions.
For email endpoints, the local SMTP option enables test.smtp.enabled=true and a fresh embedded inbox.
verifyReceivedOutput checks real SMTP delivery against expected email-body resources.
Review multiple-recipient/attachment assertions using localSmtpServer(context).receivedMessages().
SMTP configuration overrides affect only the test application's mail producers; mail is never forwarded.
Skip Existing preserves existing support and still creates missing flow tests and resources.
Archive and regenerate shared module setup when endpoint mappings change.
To adopt a utility fix, back up the affected utility and remove it before generating again.
Missing GreenMail and FTP/MINA dependencies are added to an existing test POM with a backup; other contents are preserved.
ModuleFlowTestSupport.java loads that file and enforces isolated H2 and test-only startup settings.
Missing sample-consumer fixture-input defaults are added with a backup; explicit values are preserved.
Set a fixture-input flag false to retain polling across regeneration; adapt the input scenario accordingly.
Absent settings retain application defaults; review them before starting the whole module context.
Each scenario opens and closes a fresh context; contexts and service state are not shared across tests.
Override prepareFixtures(context) to initialise instance fixtures after Spring starts, before the flow starts.
cleanupFixtures(context) runs before Spring closes, even after partial setup or test failure.
Cleanup failures are suppressed onto the original failure; tolerate partially initialised fixtures.
Standard tests use runBatches with createInputOutputBatches, supplyInput and an optional receiver callback.
The number of definitions is the batch count; each input list is submitted in the same running flow.
Expected output values are grouped by producer and ordered within each producer, not across branches.
Business objects must implement equals and matching hashCode. String expectations use formatOutputText.
An empty output list means no delivery during the observation window; omitted producers are ignored.
Outputs need not match input counts. Customise path expectations for routing, filtering and splitting.
Increase test.scenario.timeout-seconds for large/slow collections; the default total limit is 60 + 10 * delivery timeout.
Legacy runTest calls continue to run two batches.
verifyFlow supplies the common lifecycle and two-delivery checks; verifyScenario supports
deliberate rejection/branch scenarios with explicit expectations and output/absence assertions.
For JMS queue scaffolds configure test.jms.broker-url and the flow's matching isolated broker/destinations
in module-test.properties. ModuleJmsTestConfig uses ActiveMQ; adapt it for another provider.
Test configuration classes can be supplied through testConfigurationClasses(); retrieve sample beans
from the context explicitly (these tests do not use SpringRunner field injection).
Flow-specific input overrides remain in the individual test. The support file is created if missing,
otherwise preserved unless you explicitly select archive and regenerate shared setup.
Model-dependent connections and flow names are in generated/src/main/resources/studio-flow-test-wiring.json.
Normal code generation refreshes that file automatically. Shared support needs explicit refresh only
for a support schema or meta-pack upgrade. Business test names and expected results remain yours to review.
Consult ../LOCAL_TEST_ENVIRONMENT.md for test services. Never point tests at production systems.
Test-only MANUAL startup does not change the model or normal application startup settings.

Run from the project root: `mvn -pl user-flow-tests -am test`.
For one test: `mvn -pl user-flow-tests -am -Dtest=YourFlowTest -Dsurefire.failIfNoSpecifiedTests=false test`.
Override assertOutput(Object actual, int batch) for business-object field assertions and call
runTest(PRODUCER_NAME); this bypasses formatOutputText conversion.
With text assertions, false uses toString(), not object equality.
DECODE_OUTPUT_CONTENT_AS_TEXT enables content comparison for Ikasan Payload, bytes, files/paths/file lists and JMS TextMessage.
It uses UTF-8, rejects unsupported types and never acknowledges/consumes JMS messages; override formatOutputText for other formats.
Set it false to retain String.valueOf. Receiver delivery assertions remain separate.
FIRST_BATCH_INPUT and SECOND_BATCH_INPUT define scenario data independently of expected outputs.
Resource-based scenarios create missing first.txt and second.txt sample fixtures under src/test/resources/<flow>/<component>/.
Spaces in directory names become underscores. Existing files are preserved, including when regenerating tests.
Replace sample contents with meaningful input and independent expected outputs before enabling the test.
Shared assertFileContents / assertDeliveredFileContents wait for exact UTF-8 file delivery and contents.
Isolated FTP tests check all accumulated output files using localFtpDirectory(context): one new file per batch.
Adapt assertions for overwrites/checksums; unknown remote receiver locations retain explicit guards.
For SFTP/remote FTP, inspect the test server filesystem or download files first; these helpers do not access remote paths.
Assert actual delivery at external receivers as well as observed flow payloads. For routers, assert
every expected branch and absence of unwanted deliveries. For exclusions, assert stored exclusions
and delivery of subsequent valid input. Component invocation alone proves none of these.
Scheduled tests explicitly trigger the consumer; separately verify real scheduling and pacing.
Isolated test flows stop during teardown; production/demo flows must stay running between batches.

Migration updates the inherited version.ikasan dependency. Studio never regenerates these tests.
Run before and after migration; review framework API changes if a future major version needs them.
Keep assertions unchanged when comparing behaviour. Add application-specific timeouts and reliable
cleanup for any local services you start. A stuck JVM is a test/shutdown failure, not a passing run.
