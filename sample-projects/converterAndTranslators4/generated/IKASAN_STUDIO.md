# Working with Ikasan Studio using an AI coding assistant

This is an offline contract for both developers and coding agents. It does not require an MCP
server, network access, or an external AI service. The selected meta-pack is `V4.1.6`.

## Sources of truth

- `src/main/model/model.json` (relative to this generated module) is the editable Studio model.
- New saves contain three documents: `module` (Ikasan topology), `configuration` (Ikasan records
  linked by configurationId), and `studio` (meta-pack and extra generation settings).
  Edit each value in its owning document; do not copy topology or configuration values into
  `studio.properties`. `module.version` is the application version; `studio.metaPack` selects
  the generator. Old flat models remain readable and convert on the next save.
- `src/main/model/model.schema.json` describes both supported JSON shapes.
- `src/main/model/component-catalogue.json` is generated from the selected meta-pack and lists
  valid component keys, roles, properties, defaults, types and choices.
- Java, Maven and configuration files in `generated/` are Studio-owned derived output.
- Files in `user/` are developer-owned. Complete newly generated, unmodified stubs when working
  behaviour is part of the requested task; preserve existing logic and confirm before replacing it.

## Integration workflow skill

For building, changing, troubleshooting or reviewing Studio application flows, read
.agents/skills/ikasan-integration-workflow/SKILL.md when present and use its workflow.
Clients with skill discovery may select ikasan-integration-workflow automatically; otherwise
read the file explicitly. Review-only tasks remain read-only. The skill supplements these
instructions and the current generated contract; it does not grant extra permissions.

The project-local skill and Claude discovery entry are created only if missing. Existing
customisations are preserved. Skill loading depends on the AI client; a model name alone does
not establish skill support. Clients with file access can read the shared SKILL.md directly.

## Local test configuration

Before every task needing test settings, reread LOCAL_TEST_ENVIRONMENT.md in the project root.
For local FTP/SMTP, read the Studio local test harnesses section in generated/IKASAN_STUDIO.md.
A closed port may mean the developer has not started the harness; ask for that startup step.
Accept a simple name=value block: supplied values override selected catalogue defaults. Blank
or omitted optional settings keep existing values on existing components, defaults on new ones,
or remain unset if no default exists. Ask only for missing required details, not every property.
Filled-in local settings need no READY flag. Respect explicit NOT_CONFIGURED/NOT_USED statuses.
Local test passwords may be supplied literally and used in the necessary Studio proposals/model
configuration; do not echo them in chat or logs. Environment references are optional and need
supported runtime binding, not literal copying of env: names into properties. Use key filenames,
never private-key contents. sftp.directory / ftp.directory map to producer outputDirectory and
consumer sourceDirectory; confirm an existing server-visible directory. Choose one supported
authentication mode, check conditional requirements, and preserve applicable optional defaults.
Follow the requested test scope and preserve unrelated data. Verify actual delivery and continued
readiness, not just compilation. This guidance permits the developer's literal local-test values
even if an older generated guide/skill suggested credential references only. Preserve this file;
if absent, Studio creates a template during project onboarding or generation.

Review and update LOCAL_TEST_ENVIRONMENT.md as a developer startup task. Add
/LOCAL_TEST_ENVIRONMENT.md to the root .gitignore before filling it with machine-specific
settings; Studio leaves existing ignore rules unchanged. Presence or READY status does not grant
extra permissions or guarantee availability. Check service health and report prerequisites.
The file is guidance, not an executable script or automatically loaded application config.

## Understanding and researching Ikasan

Ikasan is a Java enterprise integration platform (ESB). A module contains flows; a consumer
receives events, processing components transform/filter/route them, and producers deliver results.
Builders wire the components into flows. Transactions, recovery, exclusion and replay determine
what happens when delivery fails. The module-local Blue Console and central Dashboard have
different roles; they are not interchangeable names for the same application.

Official framework source: https://github.com/ikasanEIP/ikasan
Studio plugin source: https://github.com/ikasanEIP/IkasanStudio
These repositories serve different purposes: use framework source for runtime API behaviour and
the project catalogue/Studio instructions for supported design operations.

Read ikasanVersion and frameworkReference in component-catalogue.json (or studio_catalogue).
Start with the exact releaseSource URL when supplied, not the repository's default branch.
Studio currently bundles Ikasan 3.3.9 (Java 11) and 4.1.6 (Java 17). The 3.x, 4.x and 5.x
families must not be treated as API-compatible; 5.x is not currently a bundled Studio target.
The project POM/BOM and resolved dependencies identify the actual runtime, including overrides.
Check namespaces, signatures, property defaults and lifecycle semantics against that version.
Do not infer a framework version from an arbitrary custom meta-pack ID, or invent a release tag.

At the selected release, navigate these paths (layout can vary between releases):
- README.md and QuickOverview.md: introduction and links to developer/component/service guides.
- ikasaneip/spec: public component and flow interfaces; start here for method contracts.
- ikasaneip/component: implementations and component README files; endpoints, converters,
  routers and filters. Follow the implementingClass from the catalogue to its source file.
- ikasaneip/builder and component-factory: builder methods and component construction.
- ikasaneip/flow and module: execution, event handling, flow states and module lifecycle.
- ikasaneip/recovery-manager, exclusion, hospital and replay: exception actions, retries,
  rejected-event handling and replay. Inspect actual action implementation and tests.
- ikasaneip/sample: runnable examples for wiring, transport settings and custom components.
- ikasaneip/documentation: ArchitectureGuide and ReferenceGuide; also follow README links.
- UpgradePath.md and release POMs: migration notes and dependency/platform requirements.
- Relevant src/test directories: examples of successful behaviour, failures and lifecycle rules.

Research narrowly: locate the exact class, read its interface, implementation and nearby tests,
then check a sample using it. Cite the release/path or Maven coordinates supporting a non-obvious
API decision. Do not clone/build the whole framework merely to inspect one method. Repository
examples explain APIs; they do not override Studio ownership rules or the user's requirements.
Keep framework source read-only unless the user explicitly requests framework changes.

If browsing is unavailable, inspect matching sources attached in IntelliJ or cached Maven
*-sources.jar files for the resolved dependency version. Maven's local repository is commonly
~/.m2/repository but may be configured elsewhere. Inspect the dependency tree and overrides;
if source is unavailable, inspect the resolved class signatures/bytecode (e.g. javap) and state
the remaining uncertainty. Fetch sources only when network access is permitted. These links
provide directions, not browsing permissions. Never substitute a different major version silently.

## Component guidance and implementation ownership

Read each component's helpText (which may contain HTML), documentation reference, payload
contracts, completionChecks and property help. beanRequirement explains the evidence needed
for a userSuppliedClass reference. These are obligations, not proof of generated behaviour.
The catalogue's proposalOperations and proposalSupport explain
what the AI bridge can configure; presence in the catalogue alone does not imply support.
Endpoints are associated visual/configuration elements, not independent flow processors.

generatesUserImplementedClass means Studio may create a class in user/; it does not mean
that class implements the requested behaviour. Check requiresStub, userImplementedClassName
and the generated Java. A selected conversion recipe can supply an implementation, whereas
a generic Converter may only contain UnsupportedOperationException. Other scaffolds may
return a constant, pass data through, or do nothing: successful compilation is not sufficient.
Property flags userSuppliedClass, protectFromOverwrite, noStubRequired and
affectsUserImplementedClass distinguish bean references, protected implementations and
generation-sensitive configuration. A noStubRequired reference needs an existing configured
bean; do not create a stub merely because a property is marked userSuppliedClass.

Inspect user/ files and existing diffs before editing. A request for working functionality
authorises focused implementation of newly generated, unmodified stubs for that task.
It does not authorise replacing existing developer logic or enabling Studio's overwrite flag.
Make Java implementation edits in user/ using the AI client's normal code-editing tools;
use Studio proposals for model changes. Preserve Spring bean names, interfaces and method
signatures. If code is already customised or intent is uncertain, ask before replacing it.

## Proposal names

New flow and component names must match [A-Za-z][A-Za-z0-9_ ]{0,79}: start with a letter, then letters, digits, spaces or underscores, maximum 80 characters. For numbered flows use Flow01 or Demo01, never a leading number. Existing flow/component references must use their exact saved names.

## Defaults and model property changes

A catalogue default is a literal value; defaultExpression is an internal derivation rule,
not a Java class name or a value to copy into a proposal. Omit derived properties on creation
and let Studio resolve them, or provide explicit, meaningful Java class names. Different
implementations in the same flow need distinct class names. Display renaming does not rename
an existing implementation class. Check setPropertySupported and editGuidance before editing:
addComponent/replaceComponent accept initial implementation settings, but setProperty cannot
normally change implementation-sensitive properties. Do not repeatedly retry a forbidden edit.
Exception: an unresolved __fieldName: value in userImplementedClassName can be repaired with
setProperty and a valid concrete Java class name; this does not rename existing valid classes.

## Cross-flow transport connections and ordering

A producer hands off to a consumer through transport configuration, not a cross-flow connect
operation. connect only orders components inside one flow route. Plan related flows together and
add them in the desired canvas order; numbering names does not reorder existing flows. The proposal
API has no flow-reordering operation: use Studio to reorder existing flows, never delete and
recreate them just to change their position.

- JMS: match destinationJndiName, connectionFactoryName, connectionFactoryJndiPropertyProviderUrl
  and pubSubDomain. Check the broker, JNDI factory, message type, autoContentConversion and trusted
  object packages as well. Matching labels alone do not prove messages can be delivered.
- FTP/FTPS: match protocol/security mode, host, port and producer outputDirectory to consumer
  sourceDirectory. Match generated filenames to the consumer filenamePattern, and verify
  credentials, permissions, polling and post-consumption handling. File-transfer recipes default to
  unique .dat filenames when no input filename or recipeFilename is supplied; a .txt-only
  consumer will miss them. A fixed recipeFilename can collide on repeated deliveries. A custom source directory
  factory may change the actual location. Avoid endless reprocessing or overwriting files.
- SFTP: apply the same directory/filename checks with SSH authentication and host-key validation.
  SFTP is a separate protocol from FTP/FTPS. Studio currently draws shared-endpoint connectors
  for JMS and FTP/FTPS, not SFTP; matching SFTP configuration still permits a runtime hand-off.
Shared-endpoint lines indicate inferred configuration matches, not verified delivery. Check
payload types at both ends and report external services still needing setup or runtime tests.

## Generation failures and recovery

Model application and Java generation are separate outcomes. After every proposal, wait for
generation to finish before dependent work. A generation_failed status or Studio error is a
real failure, not a routine progress message to ignore until all proposals finish. Stop dependent
submissions, inspect the error, reread the current model/revision, and prepare a focused repair
using supported operations. Do not resubmit the whole creation batch into an already updated model.
For file proposals, recompute the saved model SHA-256 and use a new proposal filename.
Tell the developer whether you are repairing the failure or need their action; do not ask them
to click Apply unless Studio actually requires review. Verify that expected flow factories and
user classes exist after successful generation. Compiling an old skeleton does not validate new
flows. Keep compilation, runtime delivery and visual coverage claims separate.

## Payload examples and recipe selection

Copy all three properties from recipeConfigurations into the converter proposal:
conversionRecipeId, fromType and toType. For addComponent, omitted types are filled from the
selected recipe. Explicitly supplied types must match. Editing an existing component does not
silently change its types; supply all required property changes in the same proposal.
Use recipeConfigurations as ready-to-use property examples, and read the corresponding
conversionRecipes entry for preconditions and configuration. Match the entire source and
target type, including collection element types and the meta-pack's javax/jakarta namespace.
Never fix a rejected recipe by merely removing its ID and then claiming the converter is done.
Either choose a matching recipe or implement and test the custom conversion in user/.

- Local File Consumer emits List<File>. Default List Splitter emits individual File values.
  After splitting, a File-to-String converter must read each file using an explicit charset
  (for example UTF-8: a file containing hello produces the String hello). Test read failures.
  single-local-file-to-string instead accepts List<File> containing exactly one file; it is
  not suitable after a splitter, or for silently ignoring the rest of a batch.
- JMS Message-to-ObjectMessage is a type check, not a universal cast. A custom converter
  must accept an ObjectMessage and reject other message kinds with a clear transformation
  failure. Test an ObjectMessage and a non-object message. Use the selected meta-pack's
  interfaces and catalogue recipes; do not mix javax.jms and jakarta.jms.
- A Translator changes a mutable payload in place. Use a Converter when replacing a value
  such as String. Give filters, brokers, splitters and producers actual demonstration
  behaviour and tests rather than leaving their default pass-through or no-op scaffolds.

## Demonstration scope and completion checklist

For requests to build a flow, integration or demonstration, aim for working behaviour by default.
Stop at a diagram or scaffolding only when the user explicitly requests that scope. Do not choose
visual-only scope merely because it is easier or external services are unavailable. Complete the
locally implementable behaviour and tests, then report the remaining external setup separately.
For underspecified demonstrations, choose simple, deterministic sample behaviour and state your
assumptions (for example a sample order with fixed fields). Ask only when a missing decision
materially changes the required behaviour, destination, data ownership or safety. Do not invent
production business rules, credentials or permission to send messages to real external systems.
For an every-component request, compare the selected catalogue's component keys with the
resulting model. Include supported routers and explicit exception policies when requested; report
associated endpoints separately from executable components. Do not omit missing work silently.

ESB lifecycle requirement: flows are long-lived services, including demonstration flows.
**DEMO ACCEPTANCE RULE: DO NOT LEAVE DEMO FLOWS STOPPED.** While the module is running,
every requested normal demo flow must run and remain ready for later input. A stopped flow does
no work. Quietly disabling it hides missing setup and makes developers waste time finding why
nothing happens; a visible startup error exposes the problem. Do not call the demo complete
while any required flow is stopped or in error, even if that state is explained in a README.
Resolve the cause or report the demo as BLOCKED/INCOMPLETE with the exact remaining action.
After a sample batch, they must remain running and ready for later input. An unexplained Stopped
state is a failed acceptance check even if the first batch was delivered. Stop only for an explicit
operator action, application shutdown or a configured failure policy; an explicitly requested
finite batch is an exception, not the default. Do not reclassify unintended stopping as success
by documenting it in a README or adding a completion notification.

An Event Generating Consumer terminates when its provider returns null: null is not an idle signal.
Do not exhaust a short list and return null in a normal ESB demo. Prefer scheduled, polling or
listener-based input with correct idle behaviour. Where catalogue coverage needs an Event
Generating Consumer, use its built-in provider unless the requested behaviour requires a custom
payload provider. Add a paced provider only when the developer explicitly requests one; catalogue
coverage alone does not require it. Custom consumers/providers must honour cancellation and the
selected framework version's lifecycle contract. Verify rollback, interruption, stop/start and
worker cleanup. Never fake isRunning, swallow failures, busy-spin or flood logs/messages just to
keep the display green. Give repeated events distinct IDs unless deliberately testing replay.
Keep bounded fixtures in automated tests; tests should stop the application during teardown,
rather than making the normal application self-terminate after its fixtures.

Acceptance check: observe successful delivery, allow an idle interval, assert that each ready
flow remains running, then send another batch through the same running application and verify
delivery without resetting a bean, restarting the flow or restarting the application. If a paced
generator was explicitly requested, observe at least two separated batches and continued readiness.
Verify that exclusion
or another recoverable policy does not stop the flow and that subsequent valid input is processed.

When missing external configuration blocks part of the brief, finish independent paths and
ask a focused question listing the exact remaining settings. Maintain a requested-versus-delivered
inventory; do not silently narrow an every-component request to the currently convenient types.
Do not replace a requested transport with a scheduled reminder and logging producer just to
obtain a Running state. A reminder is not an SFTP/FTP implementation or component coverage.
Retain real requested transport components where validation permits; otherwise report the
blocked design explicitly and ask for the required settings before claiming completion.
Offer a clearly labelled incomplete design only if Studio's validation permits it; do not invent
credentials or claim unavailable transports are configured. Explain how to complete and test them.
Keep normal ESB flows AUTOMATIC, including paths whose external services are unavailable.
Do not set MANUAL or DISABLED to suppress startup errors or make a partial demo appear healthy.
Resolve supplied configuration and service issues within the authorised scope; otherwise report
the exact blocker and ask for the missing setup. Do not invent credentials or destinations.
Startup/error states must remain visible and count as incomplete acceptance checks. Only change
a flow to MANUAL or DISABLED when the developer explicitly requests it; preserve existing
operator choices. Existing database startup
controls may override new properties: inspect the effective runtime setting and use supported
operator controls. Do not globally force allowDbOverwrite or erase runtime data to conceal this.

For each requested functional path:
1. Confirm Studio applied the model and generation completed; reread the saved model and files.
2. Inspect the relevant user/ classes for throwing stubs, TODO implementations, constant dummy
   results and no-op methods. Implement the required behaviour within the ownership rules above.
   Explanatory TODO comments alone do not prove that a component is unfinished.
3. Trace a representative payload from consumer through every transformation to producer and,
   where applicable, the receiving flow. Check payload types, actual output values, filename
   filters and transport settings. Locate every required implementation and Spring bean in code
   or dependency configuration: a model reference or successful compilation is not evidence that
   a bean exists. Check its type, registration, name/qualifier and component-scan coverage.
   If a required custom bean was not generated, inspect the selected version's interface and
   create the missing implementation and registration in user/ within the requested scope.
   Do not create competing beans or replace existing implementations. For external noStubRequired
   beans, verify the actual configured provider rather than inventing a placeholder.
4. List external requirements: input files/directories, FTP/SFTP servers, JMS brokers, SMTP,
   databases and credentials as applicable. Prefer supported local test facilities for demos,
   verify they are configured, and never claim that an unavailable service was exercised.
5. Compile and run relevant tests, including representative success and failure payloads.
   If authorised to run the module, verify startup and sample messages; otherwise report that
   runtime behaviour remains unverified. Do not send real external messages without authorisation.
6. Report model application, implementation, compilation, tests and runtime verification
   separately. Cite the actual implementation files and test results supporting completion.
   A build with no tests only proves compilation; do not call it behaviour verification.
   List incomplete classes, unsupported components and remaining setup explicitly. Continue
   implementing unblocked parts rather than treating the list of gaps as task completion.

## Communicate testing progress and blockers

Before each test or verification step, send a short message stating what you are testing
and what result will count as success.

If that step fails, stalls, encounters an approval restriction, or requires unexpected
investigation, communicate this immediately—before starting further diagnosis or retries.
Explain what happened, its impact on completion, and your next action.

During extended investigation, provide a meaningful update at least every 60 seconds.
Distinguish problems in the application from problems with testing tools, access, or permissions.

These updates are informational and do not require user confirmation.
Continue authorized work autonomously.

## Efficient implementation and verification

For a small demonstration, keep the analyst's brief focused on behaviour; use this contract
for implementation discipline. Before proposing changes, confirm Studio has generated
IKASAN_STUDIO.md, component-catalogue.json and the saved model. If these are missing or Studio
reports not ready, ask the developer to finish module configuration/generation and wait for it
to complete. Do not invent a contract or repeatedly submit proposals against an unready project.

Read the catalogue selectively. First inspect its version, proposal operations and supported
keys, then extract only the selected components, relevant properties and recipeConfigurations
using a JSON query/script. Avoid dumping the entire catalogue or repeatedly fetching unchanged
content. With MCP, request studio_catalogue with componentKeys: [] for availableComponentKeys,
then componentKeys: [exact selected keys] for their complete definitions. Omit componentKeys
only when the full catalogue is needed. File-based clients should filter the local JSON.
Broaden inspection only when the brief, a dependency or an error requires it. For small examples,
verify the completed paths together in one bounded application run where practical; rerun only
the relevant checks after changes. Do not replace actual component/delivery tests with helper tests.

For reusable flow tests, consult catalogue frameworkReference.flowTesting. Prefer the matching
Ikasan test framework with actual application flows and payload assertions. Keep tests in user/;
confirm they execute, not merely compile. A test-only context may stop on teardown, but the
developer's running module must remain available. Reference examples are starting points,
not evidence that this application's requested behaviour has been verified.

Check version-specific behaviour before adding optional customisation:
- Ikasan 3.3.9 Logging Producer: configuring replacementText and regExpPattern through builder
  setters may leave the compiled pattern uninitialised. LogProducer compiles the regex inside
  setConfiguration(), while the builder's regex setter mutates getConfiguration() directly.
  Do not rely on replacement labels without observing actual output. Prefer plain payload
  logging with meaningful payload toString(), or a scoped user implementation using SLF4J when
  labels are required. Do not rewrite generated factories or add custom code solely for decoration.
- Ikasan 3.3.9 context-close verification has exposed waiting JMS listener workers and an
  event-consumer executor after Spring context closure. Treat this as a documented investigation
  lead, not a blanket exemption or a proven diagnosis for a different application/version.
  Distinguish context.close() checks from normal process shutdown; verify the shutdown path
  actually used. If reproduced, preserve evidence, record shutdown as failed separately from
  delivery, and avoid repeatedly investigating the same unchanged symptom. A forced exit is
  not a successful graceful shutdown. Keep real application failures visible.

Before constructing several related flows, read LOCAL_TEST_ENVIRONMENT.md once for this task
and check the supplied services needed by the brief. Identify unavailable services early; do
not repeatedly try the same unavailable endpoint. Keep working on independent requirements.
Check payload types, mandatory authentication and transaction requirements before submitting
a coherent proposal. For raw JMS object conversion, validate/narrow Message to ObjectMessage
before a converter that requires ObjectMessage; match the selected version's JMS namespace.
For JMS redelivery/exclusion examples, configure the required transactional consumer and verify
persisted exclusion plus subsequent valid delivery, not only a thrown exception in a unit test.
For periodic samples, a Scheduled Consumer plus a Converter is usually sufficient. A custom
messageProvider is also supported: complete its developer-owned invoke(JobExecutionContext)
method and check the registered bean name rather than replacing the consumer to avoid a stub.

Verify one representative implementation of each distinct pattern before repeating it. Batch
related corrections, wait for application/generation results, and rerun affected checks first.
Once those pass, run one final module-level verification covering all acceptance requirements.
Repeat broad testing only after changes or new failures justify it. Use bounded waits and stop
when acceptance evidence is complete; report remaining external setup separately. Do not add
compiler exclusions or reflection-based lifecycle workarounds just to hide a generator/framework
defect: identify the cause, prefer supported APIs, and report any necessary workaround explicitly.

## Runtime verification of functional paths

Component tests and an isolated embedded transport test do not exercise the generated application.
Use staged verification: compilation, component behaviour, generated application startup, then
end-to-end delivery. Report which stages actually ran; do not infer later stages from earlier ones.
When local execution is within the user's authorised task, carry out the available stages rather
than stopping at a recommendation to test. Otherwise provide reproducible commands and prerequisites.

Start with a self-contained path using local files or an embedded transport, where supported by
the selected version. Load the actual generated application configuration and flow factories;
a test that scans only user/ beans does not verify the generated Spring wiring. Check required
transaction/configuration beans, flow startup state and startup errors. Do not work around wiring
failures with substitute production beans merely to make a test pass.

Before launch, inspect every automatically starting flow and its destinations. Report unavailable
external services and resolve authorised setup without disabling flows. Bounded tests may isolate
dependencies, but must not change the saved startup policy or substitute for normal-launch
verification. Never suppress normal startup failures with a profile that disables affected flows.

Verify the developer's normal launch path as a separate acceptance check: Studio Run module,
its generated Application entry point, actual profiles, working directory and persisted startup
controls. A custom launcher that forces every flow MANUAL then starts selected flows verifies
only that launcher. It does not establish that Run module is ready. If IDE access is unavailable,
exercise the equivalent launch configuration and explicitly mark the interactive check unverified.
Compare every flow's expected and observed state after startup and sample delivery; investigate
unexpected Stopped, error and recovery states. Distinguish deliberate completion, waiting for
setup and failure. Prefer Studio's supported harnesses or documented local service setup; do not
hide prerequisites in a test script that the normal launcher never runs. Use free/ephemeral ports
for isolated tests where configurable; report port conflicts without stopping another project.

For each paired path, send a distinctive sample through the real sender and receiving flow.
Assert the expected received value or observable output, not only matching endpoint settings,
a connector line or a log saying the application started. Use bounded waits and identify the
failing stage on timeout. Test a relevant rejection/rollback path where applicable. Record the
working directory, input, expected output and command so the developer can repeat the check.
Verification must use its own inputs and endpoints. Override saved absolute file paths in the
test configuration when using a temporary checkout; otherwise the receiver may scan the original
project while the test writes elsewhere. Assert the effective input location. Prefer portable
demo paths with an explicit working directory, while keeping SSH identity paths user-specific.

For file-producing implementations, do not expose a partially written final file. Write and close
a unique temporary file in the destination filesystem, then publish it using a supported atomic
operation. Confirm filesystem and concurrent-writer semantics; do not assume ATOMIC_MOVE preserves
an existing target or silently replace developer files. For remote transfers, inspect the selected
transport's staging/rename support rather than assuming local filesystem guarantees apply.
Test interrupted publication and retry, not only duplicate successful writes. Define how matching
completed output, conflicting output and abandoned temporary files are handled. Keep input eligible
for retry on failure and distinguish committed delivery from a later input-archiving failure.
Idempotent filenames alone do not establish crash recovery or exactly-once delivery.

## Studio local test harnesses

Studio provides local FTP and SMTP test servers which the developer starts explicitly in the IDE.
A closed local port during implementation may simply mean the harness has not been started yet;
it does not prove the supplied settings are wrong or that infrastructure must be installed.
Check these settings and the intended harness before changing endpoints or proposing another server.

- FTP: right-click a supported local FTP Consumer or Producer and select **Start Test FTP Server**,
  then **Show Test FTP Server Details** for the actual address, credentials and test directory.
  Use **Show Test FTP Directory** to inspect files. The existing server-visible root is `/`;
  any different directory must exist inside that root. Do not infer credentials from port numbers.
- SMTP: right-click an Email Producer or its endpoint and select **Start Test Mail Server**,
  then **Show Test Mail Server Details** for the SMTP address and web inbox. Studio uses MailHog;
  the first start may require a download. Check Terminal output/notifications if startup fails.
- These controls start test servers, not the application. Start the required harnesses before
  **Run module**. Harness settings/details and supplied values determine ports; do not assume
  that a listener on a familiar port is the intended server.
- The FTP harness is plain FTP, not SFTP or FTPS. SFTP needs the configured external/local SSH
  server. A JMS test reader needs a functioning JMS provider; it does not start a broker.

If you cannot operate the IDE controls, ask the developer to start the named harness and confirm
its details. Continue independent implementation, then verify real delivery once it is available.
Keep demo flows AUTOMATIC: do not disable them because their harness is not running. Report the
specific harness startup step as pending; do not claim end-to-end verification or demo completion.
Synthetic message injection does not verify FTP/SFTP acquisition or remote service connectivity.

For external transports, prefer isolated local test services or the supported Studio harnesses.
Check protocol, ports, directories, permissions and host-key requirements before delivery tests.
For bundled FTP harness demos, inspect its details and use the existing session root / for
both producer and consumer unless a custom subdirectory is actually provisioned. Verify listing,
write and rename access before starting either flow; a producer's create-directory option is not
a startup-order guarantee. A failed change-directory operation can mean a missing path, incorrect
session-root assumption or insufficient permissions, not an empty input queue.
For SFTP, ask which existing identity to use. Keys commonly live under the module user's home
.ssh directory, but filenames, formats and server authorization must be confirmed. Use resolved
paths, not an assumed project-local key or unexpanded ~. Never read private key contents into
the chat or copy them into the repository. Independently verify the server host fingerprint.
Distinguish configured, reachable and delivery-tested services. Do not invent credentials or
bypass SSH/TLS verification. Unavailable services should not block unrelated local verification.
Stop only test processes started by this task and clean up only its own temporary inputs/outputs.

Verify shutdown separately from delivery: stop started flows, close the application context and
wait a bounded time for owned workers and resources to terminate. Where relevant, test stop/start
on the same consumer instance and ensure no old worker continues delivering after stop returns.
Observe normal child-process exit from a supervising process. System.exit(0), forced termination
or an outer timeout is not evidence of clean shutdown. A delivery check may pass while shutdown
fails; report both results, preserve diagnostics and use a nonzero verification exit status for
failed assertions or cleanup. Do not hide leaked executors merely to print a successful result.

End with a compact per-path record: implemented behaviour, tests run, startup/delivery evidence,
external prerequisites and remaining blockers. Include unsupported catalogue types separately;
full coverage of proposal-supported types is not full catalogue coverage.

## Module completeness checks are not runtime proof

Read implementationReadiness in studio_snapshot, or generated/implementation-readiness.json
after generation. Studio's module menu offers Validate Module to refresh saved-source
findings and open affected files. Reports inspect primary user implementation files only; save
edits before refreshing. Reports can become stale after edits and do not cover helper/provider beans.
Resolve missing source and known throwing stubs. Review generated TODO warnings against actual
method bodies; a leftover comment alone is not proof of missing logic. Do not merely remove markers
to clear the report. No findings, applied status, compilation and running flow state are not proof
of working behaviour. Exercise the actual component methods with representative input and verify
delivery through every requested route, including error/exclusion paths. Tests of helpers alone
do not establish that the flow invokes them. Report untested paths as unverified, not complete.

## Live Studio workflow

Use Tools -> Ikasan Studio -> Connect AI to Ikasan Studio for IntelliJ MCP or the bundled Java adapter.
Read studio_snapshot and studio_catalogue, then submit studio_propose with the returned revision
and structured operations. Studio validates every proposal. Validated supported changes apply automatically
unless Settings -> Tools -> Ikasan Studio -> Always ask for approval is enabled (default: off),
or Confirm deletes is enabled (default: on) and the proposal deletes flows/components or replaces components.
Potential developer-owned code replacement always requires review and Apply. Both routes create one undoable change.
Check studio_proposal_status: request Apply only for awaiting_review, wait while generating,
and continue only after applied. Report generation_failed instead of assuming success. If the chat turn ended while
awaiting review, the developer must tell the AI to continue; Apply does not restart an idle chat.
While Studio is open, do not edit model.json on disk.

## Proposal file workflow (no MCP required)

After submitting a proposal file, read its adjacent `<proposal-filename>.result.json`.
Match proposalSha256 to the SHA-256 of the exact proposal bytes before trusting it.
Status awaiting_review means ask for developer review; generating means wait; applied means
generation completed and you must now verify the updated files. For rejected, read message,
current model and catalogue, correct the problem and submit a fresh uniquely named proposal
with an up-to-date baseModelSha256. Attempt at most two corrections per request, then explain
the blocker and the Studio UI fallback. For generation_failed, inspect generation diagnostics
and current model before proceeding: changes may already be present. Respect cancelled/undone;
do not automatically resubmit. Missing feedback is not success: use Review Latest AI Proposal
or ask the developer to copy the rejection feedback. Do not poll indefinitely or bypass Studio.

1. Keep Studio and IntelliJ open. Read the saved model and component catalogue.
2. Calculate SHA-256 of the exact bytes of generated/src/main/model/model.json, for example
   `sha256sum generated/src/main/model/model.json`. Do not reformat the model before hashing.
3. Write a UTF-8 file such as ai-proposals/rename-consumer.studio-proposal.json, outside generated/.
   Create ai-proposals/ in the project root if needed. Use a new filename for each proposal;
   preserve previous proposals. Write a temporary file without the .studio-proposal.json suffix,
   then rename it to the final filename when complete so Studio does not read a partial file.
   Use this format, replacing the digest and names with values from the current project:

   ```json
   {
     "formatVersion": 1,
     "baseModelSha256": "<64-character SHA-256 of the saved model>",
     "operations": [
       {"type": "renameComponent", "flow": "flow1", "component": "my egc",
        "name": "EventGeneratingConsumer"}
     ]
   }
   ```

4. Studio detects new or updated files directly in ai-proposals/. Validated supported proposals can
   apply automatically unless Confirm deletes requires review (enabled by default for deletions and replacements) or Always ask for approval is enabled or developer-owned code could be replaced. Confirm the saved model has
   changed before continuing; writing a proposal is never proof of application.
   Other proposals show AI proposal ready.
   Ask the developer to click Review on that notification, or Tools -> Ikasan Studio -> Review Latest AI Proposal,
   then review the operations and click Apply. The menu selects the most recently modified file.
   Existing files do not trigger notifications on IDE restart; use the menu to review them.
   Tools -> Ikasan Studio -> Import AI Proposal into Ikasan Studio remains available for selecting any proposal.
   Importing does not enable an MCP server or give the AI live access.
5. Studio checks the digest, compares the saved model with its live design, validates operations,
   and rejects stale proposals. If rejected, save any intended changes through Studio and prepare
   a fresh proposal from the updated model. Never edit model.json to make a proposal pass.
6. After Apply and generation complete, ask the AI to read the updated files and compile/test.
   A saved proposal file is not an applied change. The model changes support Undo in Studio.

## Supported proposal operations

Both MCP and file proposals use the same operations (1 to 100 per proposal):

- deleteFlow: type, flow (existing name). Removes the entire flow and its attached test harnesses; retains developer-owned source files.
- addFlow: type, flow (new name). May create an empty design placeholder; do not invent components.
  Flows can be built incrementally across proposals. An incomplete flow is a design in progress,
  not a rejection; it needs a consumer and producer before it can run.
- addComponent: type, flow, key (exact catalogue key), name, optional properties object and route path.
  route is an array of branch names, e.g. ["Accepted"] or ["Accepted", "Audit"]. Omit for root.
- deleteComponent: type, flow, component (current name). Removes only the design component;
  retains developer-owned source files. Remaining components stay connected within their route. Populated routers cannot be deleted/replaced.
- replaceComponent: type, flow, component (current name), key (new catalogue key), name,
  optional properties object. Preserves position; consumers must be replaced with consumers.
  Uses the new type's defaults plus the supplied properties, not the old type's configuration.
  Retains developer-owned source files. Prefer this for changing an existing consumer's type.
- setProperty: type, flow, component (current name), property, value (scalar or null).
  Protected user-supplied bean references such as endpointEventProvider may refer to existing
  classes. Implementation regeneration remains a Studio operation.
- renameComponent: type, flow, component (current name), name (new name).
  Subsequent operations use the new name. Studio updates route transitions; it does not
  rename Java implementation classes or edit developer-owned source files.
- connect: type, flow, optional route path, order (all executable components in that route exactly
  once; include consumer first only for root). Do not list associated endpoint decorations.
- setFlowProperty: type, flow, property, value (scalar or null to clear). Keep flowStartupType=AUTOMATIC
  unless the developer explicitly requests another startup policy. Names, structural fields and implementation properties are excluded.
- configureRoutes: type, flow, component (router name), names (2 to 32 branch names).
  Names start with a letter and contain only letters/digits. Retain names of populated branches;
  empty branches may be changed. Add components to each branch using the route path.
- setExceptionResolution: type, flow, exception (fully qualified class, optional .class suffix),
  action (from exceptionActions in the catalogue), optional properties object. Adds or updates
  the rule for that exception, preserving other rules. Read exact action parameter names/defaults.

Linear and branched route trees are supported. A router terminates its parent route; each branch
should end in a producer or another router. Shared downstream merges are not supported.
Flow renaming and migration still require Studio. Do not alter transitions on disk.

For routing requests, identify an explicit predicate and named destinations. A Single Recipient
Router returns exactly one configured route name; a Multi Recipient Router returns a list of
configured names. Complete the generated user/ logic, preserve existing code, and test each branch,
fallback and fan-out. Treat shared mutable payloads carefully; do not assume fan-out copies them.
Configure branch names before implementing the router; later changes require checking its code.

Exception rules are flow-wide, not ordinary connected processors. Ask for the failure type and
intended action when business consequences are unclear. Do not silently ignore errors or choose
infinite retries. For a demonstration, propose an explicit narrowly scoped failure and policy,
e.g. TransformationException -> excludeEvent, and verify exclusion and later valid delivery.
Class-name syntax is validated, but compile and test against the selected version to establish
that the class is a Throwable, which exception the flow resolves, and the actual action taken.
Use catalogue action guidance: legacy retry property interval is a retry COUNT, not a delay.
Cron syntax receives a structural check; validate its runtime scheduling meaning in tests.
Preserve unknown fields; use catalogue keys and property validation rules. Names must be unique
within their scope, including their generated Java names. Complete flows require a consumer.

To change Ikasan versions, use Studio's Migrate action. It previews the model, generated
code and Maven changes and saves a recovery snapshot before applying the conversion.

JSON Schema checks structure, but Studio performs additional semantic checks including
component lookup, required properties, transitions, route integrity and meta-pack compatibility.
A syntactically valid JSON document is not necessarily a valid Ikasan module.
