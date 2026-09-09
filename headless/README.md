# Headless generator and pack test kit

This standalone Gradle build uses Java 17 and Maven libraries only. Running it does not configure the root IntelliJ plugin build, download an IntelliJ SDK, launch an IDE, or require the ancillary IDE mediator artifact.

## Modules

| Artifact | Responsibility | Production dependencies |
| --- | --- | --- |
| `org.ikasan.studio:studio-generator:0.1.0-SNAPSHOT` | Core model, JSON IO, metadata validation, migration and FreeMarker generators; shared schema resources | Jackson, Maven model, FreeMarker, Commons IO, SLF4J |
| `org.ikasan.studio:studio-test-kit:0.1.0-SNAPSHOT` | Pack validation/rendering helpers and a reusable JUnit Jupiter contract | Generator and JUnit Jupiter API |
| `org.ikasan.studio:studio-bundled-packs:0.1.0-SNAPSHOT` | Existing official pack descriptors, templates and images | None |

These are initial development artifact versions, independent of the plugin version. Nothing is automatically published remotely. The bundled pack artifact currently contains the official packs together; independently releasing individual packs is a later packaging step.

The generator has no dependency on either the test kit or bundled packs. The test kit has no production dependency on bundled packs. Consumers supply their own pack resources on the application classpath using the existing `studio/metapack/<id>/...` structure. Resource discovery merges directories across engine and pack JARs.

## Source ownership

Production Java sources have their own module source root so IntelliJ and Gradle agree on ownership. Resources and existing tests still use their repository locations:

- `studio-generator` owns `headless/studio-generator/src/main/java/`, containing `org/ikasan/studio/core/**` and `StudioRuntimeException.java`, and packages `studio/metapack/schema/**`.
- `studio-bundled-packs` packages the remaining `src/main/resources/studio/metapack/**` resources.
- The root IntelliJ build excludes those resources and depends on these artifacts through a composite build in `settings.gradle.kts`.
- The headless engine and pack test tasks reuse the corresponding tests from `src/test/java/org/ikasan/studio/testing`. Official test fixtures stay in the repository's test sources; they are not exported in the test-kit JAR.

This gives one compiled implementation shared by the plugin and headless consumers. Headless tests use the repository root as their working directory for existing source/golden-file checks.

After changing the build structure, reload all Gradle projects in IntelliJ to refresh module dependencies and source roots.

## Build and test

From the repository root, using the existing wrapper:

```sh
# All headless tests; no IntelliJ configuration
./gradlew -p headless test

# Engine contracts, synthetic packs, and verification that IntelliJ/official packs are absent
./gradlew -p headless :studio-generator:test

# V3/V4 golden outputs, dependency/namespace contracts and reusable-contract examples
./gradlew -p headless :studio-bundled-packs:test

# Consumer smoke test using built generator and pack JARs on a plain JVM
./gradlew -p headless :studio-test-kit:test

# Build binary/source JARs and run checks
./gradlew -p headless build

# Inspect generated Maven publication metadata without publishing
./gradlew -p headless generatePomFileForLibraryPublication

# Optional: install artifacts in your local Maven repository for another project
./gradlew -p headless publishToMavenLocal
```

Results are under `headless/<module>/build/reports/tests/test/index.html` and `headless/<module>/build/test-results/test/`. Binary/source JARs are under each module's `build/libs/`. Use `--tests 'fully.qualified.TestName'` on the relevant module's `test` task for a focused test.

`./gradlew test` at the repository root retains the complete plugin test suite and its existing `studioTestSuite` selectors. `./gradlew check` additionally depends on all headless module checks. CI explicitly runs the standalone build as well, ensuring it does not accidentally start depending on IntelliJ configuration.

## Using the kit in another Gradle project

During development, reference this build in the consumer's `settings.gradle.kts`:

```kotlin
includeBuild("/path/to/IkasanStudio_main/headless")
```

The consumer needs Java 17, JUnit Jupiter and a test runtime with its own pack resources:

```kotlin
plugins { java }
repositories { mavenCentral() }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(17)) }
dependencies {
    testImplementation("org.ikasan.studio:studio-test-kit:0.1.0-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Optional when testing the bundled official packs:
    testRuntimeOnly("org.ikasan.studio:studio-bundled-packs:0.1.0-SNAPSHOT")
}
tasks.test { useJUnitPlatform() }
```

Alternatively, use `mavenLocal()` after local publication, or publish to your own configured artifact repository. No remote repository credentials or publication destination are configured here.

## Authoring a pack contract

Extend `org.ikasan.studio.testkit.MetaPackContract` and supply a pack ID plus a configured, representative sample module. A sample loaded from the pack's own JSON fixture is preferable to depending on Studio's internal `TestFixtures`:

```java
class AcmePackTest extends MetaPackContract {
    @Override protected String packId() { return "acme-ikasan-pack"; }

    @Override protected org.ikasan.studio.core.model.ikasan.instance.Module sampleModule()
            throws Exception {
        try (var input = getClass().getResourceAsStream("/samples/model.json")) {
            if (input == null) throw new IllegalStateException("Missing sample model");
            return MetaPackTestKit.readModel(
                    new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8),
                    "samples/model.json");
        }
    }
}
```

Imports for the example are `org.ikasan.studio.testkit.MetaPackContract` and `org.ikasan.studio.testkit.MetaPackTestKit`. The inherited tests check descriptor validation, sample version/identity preservation, model round-trip and deterministic rendering. See `src/test/java/org/ikasan/studio/testkit/V3PackContractTest.java` and `V4PackContractTest.java` for in-repository examples.

`MetaPackTestKit.render(module)` returns an immutable map of logical artifact names to generated text. It renders model JSON, application/module configuration, properties and flow/component factories. It does not write files. Keys such as `flow[0]/Flow.java` identify test artifacts rather than prescribing project filesystem paths. Test user stubs and conversion recipes through the public generator APIs with your own component fixtures and expected output.

## Limits and next steps

The current contract applies to complete packs that supply module and flow templates. Component-extension composition, downloadable pack installation and a CLI are not introduced here. The kit validates/renders using the same engine as the plugin; it does not compile or start the resulting Ikasan application. Add generated-project compilation against exact BOMs and startup/integration checks before treating a pack as certified.

The generator and metadata remain compatible with their existing public Java packages. The root build still requires IntelliJ and the ancillary mediator for IDE functionality. To package while another sandbox IDE is running, select an isolated sandbox without closing the user's IDE:

```sh
./gradlew buildPlugin -PstudioSandboxDirectory=build/headless-verification-sandbox
```
