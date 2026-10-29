# Headless generator and pack test kit

This standalone Gradle build uses Java 17 and Maven libraries only. Running it does not configure the root IntelliJ plugin build, download an IntelliJ SDK, launch an IDE, or require the ancillary IDE mediator artifact.

## Modules

| Artifact | Responsibility | Production dependencies |
| --- | --- | --- |
| `studio-cli` | Standalone migration preview/apply distribution, with project verification script | Generator and bundled packs |
| `org.ikasan.studio:studio-generator:0.1.0-SNAPSHOT` | Core model, JSON IO, metadata validation, migration and FreeMarker generators; shared schema resources | Jackson, JavaParser, Maven model, FreeMarker, Commons IO, SLF4J |
| `org.ikasan.studio:studio-test-kit:0.1.0-SNAPSHOT` | Pack validation/rendering helpers and a reusable JUnit Jupiter contract | Generator and JUnit Jupiter API |
| `org.ikasan.studio:studio-pack-v3:0.1.0-SNAPSHOT` | V3.3.9 descriptors, templates and images | None |
| `org.ikasan.studio:studio-pack-v4:0.1.0-SNAPSHOT` | V4.1.6 descriptors, templates and images | None |
| `org.ikasan.studio:studio-pack-v5-release:1.0.0` | V5.1.0 released descriptors, templates and images; generated applications use JDK 25 | None |
| `org.ikasan.studio:studio-bundled-packs:0.1.0-SNAPSHOT` | Bundle selecting exact official pack revisions | All three released pack artifacts |

These are initial development artifact versions, independent of the plugin version. Nothing is automatically published remotely. Each official pack takes its artifact version from its own `metapack.json`. The bundled-packs artifact has no pack resources; its dependency metadata selects all three exact released pack revisions. See [Independent pack releases](../docs/IndependentMetaPackArtifacts.md) for versioning and release commands.

The generator has no dependency on either the test kit or bundled packs. The test kit has no production dependency on bundled packs. Consumers supply their own pack resources on the application classpath using the existing `studio/metapack/<id>/...` structure. Resource discovery merges directories across engine and pack JARs.

With `-PstudioIncludeSnapshotPacks=true`, `studio-bundled-packs` also selects the
[Ikasan 5 development pack](studio-pack-v5/README.md) for fresh IDE sandbox launches.
Its Java 26 runtime verification and snapshot readiness gates remain separate from
released V3/V4/V5 compatibility claims. V5 migration choices remain disabled.

## Source ownership

Production Java sources have their own module source root so IntelliJ and Gradle agree on ownership. Resources and existing tests still use their repository locations:

- `studio-generator` owns `headless/studio-generator/src/main/java/`, containing `org/ikasan/studio/core/**` and `StudioRuntimeException.java`, and packages `studio/metapack/schema/**`.
- `studio-pack-v3` and `studio-pack-v4` each package only their own directory under `src/main/resources/studio/metapack/`. `studio-pack-v5-release` owns its V5.1.0 resources under its own module. `studio-bundled-packs` selects all three and hosts the combined official-pack regression suite.
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

# Each pack in isolation, with the generator/test kit but without the other pack
./gradlew -p headless :studio-pack-v3:test
./gradlew -p headless :studio-pack-v4:test

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

See the [custom-pack handover](../docs/CustomMetaPackHandover.md) for a complete standalone project layout and delivery limitations.

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

The current contract applies to complete packs that supply module and flow templates. Component-extension composition is not implemented. The IntelliJ install action accepts complete custom pack JARs and activates them after restart; see the [handover](../docs/CustomMetaPackHandover.md#install-into-studio).
The [command-line migration utility](../docs/CommandLineMigration.md) provides preview/apply
and reusable before/after project verification; build it with `:studio-cli:distZip`. The kit validates/renders using the same engine as the plugin; it does not compile or start the resulting Ikasan application. Add generated-project compilation against exact BOMs and startup/integration checks before treating a pack as certified.

The generator and metadata remain compatible with their existing public Java packages. The root build still requires IntelliJ and the ancillary mediator for IDE functionality. To package while another sandbox IDE is running, select an isolated sandbox without closing the user's IDE:

```sh
./gradlew buildPlugin -PstudioSandboxDirectory=build/headless-verification-sandbox
```

## External pack host integration

`MetaPackResources` routes descriptors, templates and icons to a session-fixed resource archive for each installed ID. It does not add those JARs to the Java classpath. A host can supply an installation directory through the Java service-provider interface `MetaPackDirectoryProvider`; Studio supplies its IDE configuration directory. Plain headless consumers without a provider continue using classpath packs.

`MetaPackInstaller.validate(MetaPackArchive.open(path))` inspects a complete external JAR without installing it. `install(source, directory, checkCancelled)` validates a staged copy and publishes it for the next session; it rejects existing IDs. Candidate resource lookup is confined to the inspection thread and is restored after failure. Hosts must run discovery, validation and installation outside UI threads. Inspection is synchronous: do not start asynchronous generation from its callback.

`ExternalMetaPackTest` in `studio-test-kit` tests installation and fresh-session discovery using built artifacts, including rendering and icon lookup without IntelliJ. `MetaPackArchiveTest` covers archive/resource boundaries. These checks do not establish an interactive IDE workflow or runtime certification.


`MetaPackStore` schedules external updates/removal for activation at the next session's discovery and retains immutable artifact history. Management callers can review artifact fingerprints, cancel pending work and restore retained JARs. It uses an installation-directory lock, checks before/after artifact hashes and keeps session resources unchanged. `ExternalMetaPackTest` exercises the lifecycle using isolated session classloaders.

Saved model documents now include `studio.metaPackRevision` when the pack supplies revision metadata. `IkasanModelDocuments.toLegacy` rejects a mismatch before restoring component properties. Legacy/unlocked models remain readable. `MetaPackRevisionLock.preview/apply` provides deliberate adoption with full candidate-model validation, stale-review checks and the existing atomic/rotating-backup writer. See the [handover](../docs/CustomMetaPackHandover.md#project-revision-locks-and-deliberate-adoption) for workflow and the one-active-revision limitation.


## Portable project-pack exports

The IntelliJ exporter augments the CLI distribution with the saved project’s required custom artifact, preserving the two bundled pack JARs. `OfflinePackBundle` writes/verifies `meta-packs.json` (ID, artifact revision, relative file and SHA-256) and the resource-only `meta-packs/` directory. The CLI supplies a read-only `MetaPackDirectoryProvider` resolved relative to its own distribution; it neither reads an IDE pack store nor activates pending management operations.

`studio-cli validate --project PATH` checks the model and its pack revision. `render --project PATH --output NEW_DIRECTORY` uses `ProjectArtifactRenderer`, shared with IDE migration rendering, to produce a fresh inspection preview outside the project. It does not copy developer code or run tests. The existing migration wrapper delegates to the same renderer and retains its supported version-pair restrictions. See [portable custom packs](../docs/CommandLineMigration.md#portable-custom-packs-validation-and-rendering).
