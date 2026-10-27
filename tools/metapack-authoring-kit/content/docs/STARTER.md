# SharedIkasan4 — public starter for internal adaptation

Read [the kit README](../README.md) first. This starter was assembled from Studio's maintained Maven
example and already has its required public artifacts in the bundled repository. You do **not** need
the Studio checkout or a publishToMavenLocal step on the VDI.

From the kit root, run `python verify.py --maven /path/to/mvn`. After intentional edits use
`python verify.py --build-only --maven /path/to/mvn`. Both builds are offline. If Maven is configured
in IntelliJ, select the kit's `work/maven-repository` as the IDE's local Maven repository.

The Maven reactor retains these public example coordinates until you choose your internal names:

- `org.example.metapack:example-metapack:0.1.0-SNAPSHOT` — reactor.
- `org.example.metapack:example-runtime:0.1.0-SNAPSHOT` — public teaching implementations.
- `org.example.metapack:example-pack:0.1.0-SNAPSHOT` — complete resource-only SharedIkasan4 pack.

The scheduled sample flow is Every five seconds → Example Sample Text Converter → Example Uppercase
Converter → Example Logging Producer. Its model is generated at `example-pack/target/samples/model.json`.
The kit also contains a prebuilt public sample under `../samples/`. No private components are present.

The pack build copies the pinned V4 baseline and adds the descriptors under its distinct ID. This
is build-time assembly, not runtime inheritance. Tests validate metadata, round-trip a model, compile
the generated factory and validate the actual packaged JAR through Studio's installer contract.

Before private/production adoption, choose internal Maven coordinates, a stable pack ID and a released
revision. Update POMs, resource paths, descriptors, manifests and Java/archive tests consistently.
Replace the public example runtime dependencies with the actual shared library and adapt one component
at a time using the kit's numbered prompts. Keep private source and the resulting pack inside work.

Application delivery must be verified separately; a passing authoring build does not prove all runtime
transports or additional generated-application dependencies are available offline.
