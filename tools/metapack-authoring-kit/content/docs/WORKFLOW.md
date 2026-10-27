# Work entirely inside the VDI

## 1. Establish the baseline

Run the unmodified verifier first. Record build-receipt.json, the SHA-256 and the installed Studio
build. The public example targets Ikasan 4.1.6/JDK 17. Confirm the shared library actually targets
that release. Choose a stable internal pack ID/coordinates before creating real Studio projects.
Keep upstream baseline revision and intentional changes in an internal BASELINE.md.

Copy the kit into an internal repository using your approved tools if desired; GitHub access is not
required to build it. Do not commit dependency repositories, work/, target/, credentials or private
artifacts to a public repository. The public starter's sample runtime is a teaching artifact only.

## 2. Inventory one representative shared component

Use prompt 01. Supply the component's local source and existing usage/configuration examples only
as allowed by work policy. If only binaries are available, inspect approved JARs locally with javap
and use available internal documentation. Do not guess default values from bytecode method names
or an interface. Include inherited members and the actual configuration class exposed by REST.

Use COMPONENTS.csv and PROPERTIES.csv as worksheets. Classify how the component is created:
public constructor, factory, builder or Spring bean. Separately classify configuration-bean setters
and constructor dependencies. Record actual input/output types and life-cycle/transaction semantics.
Mark unknown construction/default/type evidence as unresolved and stop that component's generation.

## 3. Implement and test one component

Use prompts 02 and 03. The maintained starter shows direct runtime construction, externalised string
configuration, a complete copied baseline, resource-only packaging and installer validation. Shared
classes that need more complicated wiring may need a dedicated pack template or a small internal
runtime adapter. Never change the base builder name and assume that creates the subclass.

Add the actual private library dependency to the generated application's descriptor and the authoring
project's test classpath. Configure its version/BOM handling explicitly. Replace sample coordinates
and remove public demo components only after the first internal example is independently verified.

Run verify.py --build-only. It still runs offline. A newly introduced library or Maven plugin requires
explicit approved provisioning into work/maven-repository. Do not weaken tests or change expected
business output to get past missing dependencies. A copied base pack requires ongoing upstream review.

## 4. Prove import and generated application behaviour

Use prompt 04. Create synthetic module/configuration REST fixtures using the real class names but
no production values. Check exact class matching, field mappings, null/omitted values and unknown
fields. Keep Studio property aliases distinct from genuine REST name differences.

Generate a representative application's POM and factories. Compile against the real internal library,
then use local test transports or approved work endpoints to verify first delivery, idle readiness,
second delivery, failure/rollback behaviour where relevant, stop and cleanup. Contract validation is
not runtime certification. A MANUAL flow awaiting an endpoint is incomplete, not a passing demo.

## 5. Release internally

Use prompt 05. Review the JAR to confirm there are no implementation classes or unrelated packs.
Verify the actual archive using the inherited archive test, install it through Tools → Ikasan Studio
→ Install Meta-Pack…, restart the IDE and select the custom pack in a scratch project. Test managed
updates/revision locks before adopting a new revision. Increment packVersion for every release;
retain previous JARs and record hashes. Runtime library and pack are separate internal deliverables.

Keep inventory evidence, fixtures, adapted descriptors, generated outputs, actual runtime reports and
the private pack inside work. External Studio maintenance needs only a synthetic reproduction using
public classes and synthetic values if an engine limitation is found—not screenshots of private code.
