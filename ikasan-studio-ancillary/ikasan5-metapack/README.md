# Standalone Ikasan 5.1.0 meta-pack example

This independent Maven project produces `org.ikasan.metapack:ikasan5-metapack:1.0.0`.
Its complete resource-only pack is **ExampleIkasan5**, pack revision **1.0.0**, targeting
Ikasan **5.1.0** and generated-project **JDK 25**. The distinct ID allows it to be installed
alongside Studio's bundled **V5.1.0**, demonstrating external pack loading on the normal plugin.
No snapshot opt-in is needed for this released runtime.

Copy this directory into a separate repository. It has no parent POM, relative source path
into Studio, production Java classes, runtime Studio dependency or dependency on another pack.
All descriptors, icons, migrations and templates live under
`src/main/resources/studio/metapack/ExampleIkasan5/`. This is a full adapter, not an overlay.
Its baseline is the released 5.1.0 pack in `headless/studio-pack-v5-release`, replacing the
old snapshot example from `demo/external-ikasan5-metapack`. Future changes are independent;
advance `packVersion` and the Maven artifact version together when distributing changed bytes.

## Build and tests

Use JDK 17 for pack authoring. Bootstrap the engine/test-kit once from the matching Studio
checkout, or obtain the same artifacts through your approved Maven repository:

```sh
./gradlew -p headless :studio-generator:publishToMavenLocal :studio-test-kit:publishToMavenLocal
```

Then run inside this directory:

```sh
mvn clean verify
```

The resulting `target/ikasan5-metapack-1.0.0.jar` is installable directly. Tests validate the
library, render and round-trip a model, inspect the actual archive and run separate JVM
installation/restart probes with the example resources excluded from the host classpath.
Those probes prove the pack is discovered from the installed JAR, including when snapshots
are disabled. `target/demo-render/model.json` is an importable logical example; use Studio
to generate its full Maven application. Pack tests do not replace application runtime tests.

## Demonstrate external loading

1. Build and launch the normal Studio plugin, preferably using a disposable IDE profile.
2. Confirm **V5.1.0** is available but **ExampleIkasan5** is absent.
3. Choose **Tools → Ikasan Studio → Install Meta-Pack…**, select the example JAR and restart.
4. Select **ExampleIkasan5**, or import `target/demo-render/model.json` into a new Studio project.
5. Set the project SDK, Maven importer/runner and Application Run/Debug JRE to **JDK 25**.
   Generate and test the application against the released 5.1.0 dependencies.
6. Reopen the project to demonstrate persistence. Manage the installed example through
   **Tools → Ikasan Studio → Manage Meta-Packs…**.

The original demonstration branch was designed to show V5 absent before installation. This
released example uses a separate ID instead, so it works with the current plugin without
removing the bundled runtime adapter. Previous snapshot projects and revision locks remain
unchanged; importing an old snapshot model does not automatically select this example.

## Adapt it to your internal library

Use your own pack ID and revision; keep the runtime/BOM and JDK contracts explicit. Add your
component descriptors and generation templates based on verified constructors, builders,
setters and configuration types. A subclass does not imply the base builder constructs it.
Runtime libraries belong in generated project dependencies; the pack JAR remains resource-only.
Keep confidential code and metadata inside your organisation's approved environment.
See the [authoring guide](https://github.com/ikasanEIP/IkasanStudio/blob/main/src/main/resources/studio/metapack/METAPACK.md) and the
[restricted-workstation kit](https://github.com/ikasanEIP/IkasanStudio/blob/main/tools/metapack-authoring-kit/README.md). That kit remains
an Ikasan 4/JDK 17 starter; this directory is the independent released Ikasan 5 example.

The unmodified 5.1.0 runtime has reproduced transaction rollback failures under injected
JMS/FTP/SFTP errors. The pack does not patch those libraries. See the
[release verification and limitations](https://github.com/ikasanEIP/IkasanStudio/blob/main/docs/Ikasan510Verification-2026-10-29.md).
