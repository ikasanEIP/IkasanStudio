# Example Ikasan Studio meta-pack

This Java 17/Maven project builds `ExampleIkasan4`, a complete pack for Ikasan 4.1.6,
and a separate runtime library. Copy this directory or generate it using
`org.ikasan.studio:ikasan-studio-metapack-archetype:0.1.0-SNAPSHOT`.

## Build

Studio artifacts are not yet published to a remote Maven repository. From a Studio
checkout, first run `./gradlew -p headless publishToMavenLocal`. Then, from this directory:

```sh
mvn clean verify
mvn install
```

The pack's test dependency on Ikasan 4.1.6 transitively requires
`me.snowdrop:narayana-spring-boot-starter:3.0.0.redhat-00042`, available from
Red Hat's Early Access Maven repository rather than Maven Central. The pack POM
declares both repositories (releases only), so clean builds and projects copied
from the archetype do not depend on a developer's `settings.xml`. Network access
to both repositories is required for the first build.

`example-runtime` contains SampleTextConverter, UppercaseConverter and LoggingProducer.
`example-pack` contains descriptors, icons, manifest and contract tests. Its build unpacks
the pinned `studio-pack-v4:0.1.1-SNAPSHOT` baseline, then adds the example resources under
its own ID. This is build-time composition: the output JAR contains the complete pack;
Studio has no runtime inheritance or overlay mechanism. Templates are copied without
filtering, preserving FreeMarker syntax. No Java classes or runtime dependencies are
bundled into the pack JAR.

Tests validate the complete descriptors, round-trip a configured sample flow, render
its custom factory and validate the actual packaged JAR with Studio's installer.
`example-pack/target/samples/model.json` is the generated sample model. The sample flow is:

```text
Scheduled Consumer (every five seconds)
  → Example Sample Text Converter
  → Example Uppercase Converter
  → Example Logging Producer
```

The standard scheduled consumer owns pacing and stop/start behaviour. There is no finite
custom source that stops after a batch. Its Quartz tick becomes a greeting before the
string conversion. Pack tests do not certify application startup or delivery. When
checking a running application, verify first delivery, readiness between ticks and
later delivery in the same application, then explicitly stop the flow.

## Try it in Studio

1. Run `mvn install` so generated applications can resolve `example-runtime` locally.
2. Use **Tools → Ikasan Studio → Install Meta-Pack…** and select
   `example-pack/target/example-pack-0.1.0-SNAPSHOT.jar`. Restart the IDE.
3. Create a Studio application with the separate Studio project archetype and select
   `ExampleIkasan4`. Recreate the sample flow above or use the generated sample model
   as its `generated/src/main/model/model.json` before opening Studio.
4. Generate code, build the application with Maven and use **Run module**. Check the
   repeated uppercase greeting in application logs; use **Console** for flow state.

The component descriptors add the runtime dependency to generated Maven output. Installing
only the pack does not install the runtime library into Maven or into the IDE.

## Make it your own

The archetype customises the root Maven coordinates. Child artifact coordinates, Java
namespace and pack ID deliberately start as the same working example. Before publishing,
rename `org.example.metapack`, `example-runtime`, `example-pack` and `ExampleIkasan4`
consistently in POMs, Java packages, resource directories, descriptors and tests. Update
the archive test's filename if artifact coordinates change. Keep the runtime dependency
in descriptors aligned with the manifest's compatibility override and runtime POM.
Give each released pack revision a new `packVersion`; do not replace published bytes.

Review baseline updates explicitly, including security overrides. SNAPSHOT coordinates
are development bootstrap versions, not immutable releases. Record the Studio commit
and exact baseline revision when releasing your pack; publish your runtime library
alongside it. Keep developer implementation classes out of the resource-only pack.

The example manifest declares generator API **5**, matching the copied V4 baseline.
When adopting a newer baseline, review its generator API requirement as well as its
templates and descriptors. The contract tests reject an example declaration below
the copied baseline, even if a particular sample flow does not use every new feature.
