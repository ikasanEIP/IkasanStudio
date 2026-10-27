# Custom meta-pack developer handover

Reviewed against the source on 19 October 2026. This is a development handover, not release certification.

## Current position

A teammate can develop and test a complete meta-pack in a separate Java/Gradle repository today, without building or running IntelliJ. The generator and reusable test kit are separate artifacts. The two official packs are independently versioned, resource-only JARs, although their sources and CI remain in the Studio repository.

| Capability | Current implementation |
| --- | --- |
| Separate generator and test kit | `studio-generator` and `studio-test-kit`; plain Java 17 |
| Independent pack artifacts | `studio-pack-v3` and `studio-pack-v4`; revision read from each manifest |
| Separate project consumption | Gradle composite build or locally published Maven artifacts |
| Remote artifact publication | Not configured; requires repository and release setup |
| Custom component overlays / base-pack inheritance | Not implemented; supply a complete pack |
| Installing an external JAR through Studio | Tools → Ikasan Studio → Install Meta-Pack…; activation after IDE restart |
| Project revision guard | Saved documents record the revision; mismatches block loading until restoration or explicit adoption |
| Managed lifecycle | Updates, removal, cancellation and restoring retained revisions through Manage Meta-Packs |
| Concurrent revisions of the same ID | Not implemented; one active revision per ID in each IDE session |
| Offline project tools | Export includes the saved project’s exact custom pack, with a checked revision/checksum inventory and CLI validate/render commands |
| Runtime certification | Must be performed separately; validation/rendering tests do not establish delivery |

The supported bundled packs remain V3.3.9 and V4.1.6. An experimental directory alone does not establish support: it must be wired into packaging, contracts and plugin distribution.

## Recommended scope for the extended tool set

Start by agreeing the exact Ikasan runtime release, Java baseline and custom component Maven coordinates with the Studio maintainer. Use the corresponding official pack as a complete starting point, retaining required shared templates and standard components. Give the custom pack a unique, stable ID such as `AcmeIkasan4`; do not reuse `V4.1.6` or place competing copies of that resource path on the classpath.

There is currently no manifest declaration that means “extend V4.1.6”. Copying a complete pack also creates responsibility for reviewing and carrying forward upstream fixes. Record the source Studio commit and original pack revision in the custom repository, alongside a list of intentional differences. Adding descriptors to a pack is supported; inventing new engine behaviour or unsupported descriptor fields may require coordinated Studio changes.

Use additive component identities. Verify builders, setters, payload types and dependencies against the actual extended tool set. Keep business implementation in runtime libraries or developer-owned stubs; pack JARs should contain metadata, templates and artwork. Generated applications need the runtime libraries in their own Maven dependencies: a dependency of the pack's test project does not add it to generated POMs.

## Restricted-network / VDI authoring kit

The tracked [kit source and build instructions](../tools/metapack-authoring-kit/README.md) assemble a
transferable ZIP for Ikasan 4.1.6/JDK 17 without requiring a Studio/Ikasan/Dashboard checkout on the VDI.
It includes the maintained Maven starter, complete pinned baseline pack, generator/test-kit artifacts and
public API sources, schemas, Copilot instructions/prompts, inventory worksheets and a curated offline
Maven repository. The builder validates an isolated-repository bootstrap and offline rebuild, including
generated-factory compilation and packaged-JAR installer validation. A matching development plugin ZIP
is included by default. Generated ZIPs/logs stay under ignored `build/metapack-authoring-kit/`; the
instructions and reproducible builder are committed under `tools/metapack-authoring-kit/`.

The kit contains no private shared components and does not provision future private dependencies or
certify runtime delivery. Private adaptations and packs stay inside work; use Copilot on private code
only under the organisation's approved processing policy. A local IDE is not a local-inference guarantee.

## Maven example and archetype

For a working starter, read [the ancillary build and archetype instructions](../ikasan-studio-ancillary/README.md)
and [the example README](../ikasan-studio-ancillary/example-metapack/README.md).
The maintained Maven example builds a separate runtime library and a complete
`ExampleIkasan4` resource pack. It includes a scheduled greeting flow, contract tests,
compilation of its generated component factory against Ikasan 4.1.6 and installer
validation of the packaged JAR. The archetype generates the same example and builds it
in an integration test. Root coordinates are configurable; rename the documented
example child coordinates and pack ID before publishing your own pack.

This uses build-time assembly from the pinned official pack artifact, rather than
runtime inheritance. Review and release baseline changes deliberately. Application
startup and continued delivery still require separate runtime verification.

## Standalone project setup

Use a Gradle wrapper, Java 17 and the following layout:

```text
custom-ikasan-pack/
  settings.gradle.kts
  build.gradle.kts
  src/main/resources/studio/metapack/AcmeIkasan4/
    metapack.json
    library/                 # complete category/component descriptors and icons
    templates/               # complete version-specific template set
  src/test/java/com/acme/AcmePackTest.java
  src/test/resources/samples/model.json
```

For initial development, use a local Studio checkout. Pin and record its commit because the current development artifacts use snapshot versions. In `settings.gradle.kts`:

```kotlin
rootProject.name = "acme-ikasan-pack"
includeBuild("/path/to/IkasanStudio_main/headless")
```

In `build.gradle.kts`:

```kotlin
import groovy.json.JsonSlurper

plugins {
    `java-library`
    `maven-publish`
}
group = "com.acme.ikasan"
val manifest = JsonSlurper().parse(file(
    "src/main/resources/studio/metapack/AcmeIkasan4/metapack.json"
)) as Map<*, *>
version = manifest["packVersion"] as String
repositories { mavenCentral() }
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
}
dependencies {
    testImplementation("org.ikasan.studio:studio-test-kit:0.1.0-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
}
publishing {
    publications.create<MavenPublication>("library") { from(components["java"]) }
}
```

The composite supplies the test kit and generator; do not add the official pack bundle unless a test explicitly needs it. The custom pack's own main resources are already on its test classpath. No production generator dependency is needed for a resource-only pack. Install JDK 17 locally for this example; unlike the Studio build, this minimal settings file does not configure automatic toolchain downloads.

Alternatively, run `./gradlew -p headless publishToMavenLocal` in Studio, remove `includeBuild`, and add `mavenLocal()` to the custom project's repositories. This explicitly writes to the local Maven repository. Remote consumption needs separately configured publication; these snapshot coordinates are not a promise of a public artifact service.

## Manifest, metadata and tests

Use schema 2, `generatorApiVersion: 1`, a `packVersion` such as `0.1.0-SNAPSHOT`, and an `id` matching the resource directory. Preserve the exact runtime `ikasanVersion`, Java baseline and BOM contract from the chosen platform. The custom ID does not change the runtime version. Declare justified explicit third-party dependency versions in `compatibilityOverrides` as required by the validator.

Follow [meta-pack authoring](../src/main/resources/studio/metapack/METAPACK.md), the [schemas](../src/main/resources/studio/metapack/schema/) and [compliance requirements](../src/main/resources/studio/metapack/METAPACK_COMPLIANCE.md). Review copied compatibility overrides rather than dropping them or assuming they suit a different runtime.

Create a representative Studio model fixture targeting `AcmeIkasan4`. The model's `version` identifies the pack, not the artifact revision or runtime release. Use a real configured model and include flows exercising custom components; an empty module is weak evidence. Extend `MetaPackContract` using the [working example in the headless guide](../headless/README.md#authoring-a-pack-contract). Do not depend on Studio's internal `TestFixtures`; those are not exported by the kit.

The inherited contract validates metadata and checks model round-trip plus deterministic rendering of application/module configuration, properties and flow/component factories. It does not compile code, render every template, test custom stubs, or start flows. Add reviewed expected-output tests for the custom components and exercise further templates through public generator APIs. Keep expected files independent of the code under test.

From the custom project, run:

```sh
./gradlew test jar generatePomFileForLibraryPublication
jar tf build/libs/acme-ikasan-pack-0.1.0-SNAPSHOT.jar
```

Check that the JAR contains `studio/metapack/AcmeIkasan4/metapack.json`, the full library and templates, with no copied engine classes or unrelated pack resources. Inspect `build/publications/library/pom-default.xml` for unintended production dependencies. Reports are in `build/reports/tests/test/index.html`.

## Acceptance and delivery to Studio

Before handover, generate minimal and representative full applications, resolve the exact BOM, compile/test on the declared Java version and inspect dependency convergence. Test custom components' payload handling, property bindings, user-code preservation and failures. Verify first delivery, idle readiness and later delivery in the same running application without restart. Flows are long-lived services; exhausting demonstration input must not stop them. External prerequisites left unconfigured must be recorded as incomplete paths.

`validateMetaPacks` is a Studio repository task for the packs on that build's test classpath; it is not installed into the standalone project by the example above. For the custom project, use the contract plus explicit BOM-resolution, generated-application and help-link checks. For inclusion in Studio, the maintainer must also run the repository checks with the custom pack actually included; passing the unchanged official suites says nothing about a separate JAR.

### Install into Studio

Use a Studio build containing this installer implementation. The first implementation accepts complete resource-only pack JARs, one pack per JAR, with schema-2 manifests. It rejects classes, service registrations, conflicting IDs, invalid metadata, missing baseline templates and template syntax errors. Standard JAR directory entries are optional.

1. Build and test the custom pack, including generated-application checks above.
2. In IntelliJ with a project open, choose **Tools → Ikasan Studio → Install Meta-Pack…** (also available in Find Action).
3. Select the binary pack JAR, not its sources JAR. Only install packs from trusted authors: templates produce application code.
4. Wait for validation and the installation notification, then restart IntelliJ IDEA.
5. Configure a new Studio module and select the custom pack ID. Existing models retain their original pack ID. Merely installing a pack does not migrate projects.
6. Exercise discovery, palette/icons, properties, generation and closing/reopening the project in the sandbox before distributing the pack to colleagues.

Installation copies the validated JAR into `<IDE configuration directory>/ikasan-studio/meta-packs/<id>.jar`, outside the plugin installation. Plugin updates therefore do not overwrite the pack. Sources are fixed for the IDE session; installation does not change templates used by open projects. Duplicate IDs, including bundled IDs and pending installations, are rejected. Two manually supplied JARs with the same external ID make that ID unavailable, rather than selecting a revision by filename order. Invalid sources produce startup diagnostics and are excluded from the chooser.

### Update, remove or restore an external pack

Open **Tools → Ikasan Studio → Manage Meta-Packs…** with a project open. This lists external installations and their pending operations; bundled packs remain managed by plugin releases.

- **Update from JAR…** validates the candidate and shows the installed and proposed revisions before scheduling it. The pack ID and target Ikasan runtime must remain the same; use a new pack ID and explicit migration for another runtime. The update must have a different `packVersion`. A previously retained revision cannot be reused with different artifact bytes.
- **Remove after Restart…** schedules removal and explains the impact on projects using the pack.
- **Cancel Pending Change** cancels a scheduled update or removal before restart.
- **Restore Previous Revision…** selects a retained JAR and schedules rollback using the same review and validation path. Removed packs with retained backups also appear in this dialog; restoring one installs its retained JAR for activation after restart.

Restart IntelliJ IDEA after scheduling a change. The current session keeps using its current JAR. Before activation, Studio checks the recorded SHA-256 values of the current and candidate files, saves the previous JAR under `.history/<id>/`, then applies the operation. A stale or altered pending artifact leaves the current pack in place and produces a diagnostic. `.pending/` contains operations awaiting activation; `.management.lock` coordinates filesystem changes across IDE instances. Use the UI rather than editing those files or replacing active JARs manually.

Pending changes are processed when pack resources are first initialized in the next session. An IDE configuration shared by several running IDE processes must be restarted consistently; separate configurations remain independent. Filesystem checks catch changes made after review; this is not a mechanism for live pack replacement.

### Project revision locks and deliberate adoption

On the next normal save, a document-format model records `studio.metaPackRevision` alongside `studio.metaPack`. Older documents and legacy models without a lock remain readable and gain a lock when saved. Schema-1 packs without revision metadata remain unpinned. These new lock fields require a Studio build that supports them; older strict document readers may reject them.

If the available pack revision differs from a saved lock, Studio rejects the model before reconstructing its components and keeps normal persistence blocked. It does not substitute the installed revision. This guard also applies to headless document loading. Existing models are not rewritten merely because Studio starts or a pack is installed.

Two recovery routes are available:

1. Restore the required pack revision in **Manage Meta-Packs…**, restart and reload the project. Reinstall an exact supplied artifact if there is no retained copy.
2. After reviewing the pack change, choose **Tools → Ikasan Studio → Adopt Installed Meta-Pack Revision…**. Save open project files and apply/discard pending property edits first. Studio validates the existing model against the installed revision and shows the old/new lock for confirmation. Applying the review changes only the revision field semantically, retains the exact previous file as `model.json.bak.1` and reloads through normal background initialization. A changed source file or invalid model cancels adoption.

Adoption does not regenerate application files. Regenerate and inspect the output, resolve dependencies, build and test against the real runtime before running it. Structural validation cannot establish business/API compatibility. Backups with an unavailable revision remain protected by the same loading guard; restore that pack revision before using such a model backup.

There is still one active revision of a given pack ID per IDE session. Locks prevent an unintended upgrade; they do not let two projects use different revisions of that ID concurrently. Managed updates/removal and revision mismatch protection are implemented; simultaneous revision resolution, project-specific artifact provisioning, component overlays and automatic updates remain future work.

The installation directory is local to an IntelliJ configuration; it is not a public marketplace or an automatic update service. Remote development/Split Mode installation remains unimplemented and unverified. Offline tools export now includes the saved project’s required custom pack; keep its `meta-packs/` folder and `meta-packs.json` together with the launchers and libraries. See [portable pack validation/rendering](CommandLineMigration.md#portable-custom-packs-validation-and-rendering). Migration support remains limited to the two documented official pack IDs.

For an official bundled pack, the existing pack modules and `studio-bundled-packs` remain the packaging pattern. A plugin rebuild is needed when changing the bundled selection, but is no longer needed for each custom pack installation.

The teammate's handover should include the repository and pinned Studio commit, build commands, pack ID/revision/runtime/BOM/Java versions, component inventory, upstream differences, generated-application evidence, external prerequisites and a list of anything requiring engine or UI changes. Independent releases and future installation work are described in [Independent pack artifacts](IndependentMetaPackArtifacts.md).
