# Ikasan 5 external meta-pack

A standalone Maven project producing `org.ikasan.metapack:ikasan5-metapack:0.1.0-SNAPSHOT`.
Copy this directory into its own repository to develop it independently. It has no parent
POM, no relative source paths into Studio, no production Java classes and no production
Studio dependencies. The complete V5 descriptors, icons and FreeMarker templates are in
`src/main/resources/studio/metapack/V5.0.1-SNAPSHOT/`.

This is a complete snapshot-runtime adapter, not a component overlay. Its starting
resources were copied from Studio's tested V5 development pack on 21 October 2026.
The Gradle V5 project remains a regression/reference fixture on this branch; it is not
part of the plugin distribution. Future edits here are independent of that fixture.
The runtime's known transaction rollback failures remain unresolved.

## Build

Use JDK 17 or later for this authoring project. Generated Ikasan 5 applications require
**JDK 26**. Run Maven from this directory:

```sh
mvn clean verify
```

The test kit is not yet assumed to be available from a public repository. Bootstrap it
once from the matching Studio checkout (or obtain these artifacts from your team's
Maven repository):

```sh
./gradlew -p headless :studio-generator:publishToMavenLocal :studio-test-kit:publishToMavenLocal
```

That explicitly installs build/test dependencies in your local Maven repository. No
remote publication is configured. The standalone project does not need the Studio
checkout after these dependencies are available. Maven dependencies must be resolved
before an offline build can succeed.

`verify` validates descriptors, round trips/renders a model, checks that no other pack
or IntelliJ is needed, validates the final JAR through Studio's actual installer, and
runs two separate JVM probes. The first host has **no V5 resources** and installs the
JAR; the second discovers it after restart, applies snapshot selection policy and renders
a consumer-to-producer flow from the installed archive. Logs are `target/probe-*.log`;
`target/demo-render/` contains the rendered example and importable `model.json`.
These logical rendering artifacts are a test fixture, not a complete runnable Maven
application. Use Studio to create/generate the application for the interactive demo.

## Demo with Studio

1. On Studio branch `demo/external-ikasan5-metapack`, build/launch the normal **Run Plugin**
   configuration. Its archive contains V3/V4 only, with no hidden V5 payload.
2. Start with a sandbox/profile where V5 has not previously been installed. Enabling
   **Settings → Tools → Ikasan Studio → Allow snapshot meta-packs** should not by itself
   make V5 appear: the pack is absent. Do not delete a real IDE configuration for the demo.
3. Choose **Tools → Ikasan Studio → Install Meta-Pack…** and select
   `target/ikasan5-metapack-0.1.0-SNAPSHOT.jar` from this project.
4. Restart the sandbox IDE. Select **V5.0.1-SNAPSHOT** when configuring a new module,
   or import `target/demo-render/model.json` into a disposable new Studio project.
5. Configure its project SDK, Maven importer/runner and Run/Debug JRE as **JDK 26**.
   Allow Maven resolution, generate the code, then run the chosen demonstration module.
   Show first delivery, idle readiness and later delivery without restarting the module.
6. Reopen the project to show installation and selection persistence. The installed JAR
   lives in the IDE configuration, outside the plugin and application project.

If V5 already appears before installation, that IDE profile has a previous installation
in the normal meta-pack installation directory. This branch does not delete those resources.
The older automatic snapshot-payload directory is no longer read; install the standalone
JAR into the normal directory even if you previously used that activation route. A manually installed pack
can be reviewed through **Manage Meta-Packs…**. For a repeatable before/after demo, use
a fresh IDE sandbox/profile; uninstalling the plugin alone does not clear installed packs.
Existing saved V5 models retain the same pack ID/revision, so their revision locks remain
meaningful. Updates with changed bytes should advance `packVersion` and be reviewed in
**Manage Meta-Packs…**; do not silently replace an immutable revision.

## Map this example to an internal library

- `metapack.json`: unique pack ID, independent pack revision, runtime/BOM and JDK contract.
- `library/`: component descriptions, editable properties, icons and application JAR dependencies.
- `templates/`: code generation adapted to that runtime and your component constructors/configuration.
- `src/test/`: validation, rendering and the same installation/restart boundary used by Studio.

For a library of additional components, start from the smaller existing
`../example-metapack` and `../ikasan-studio-metapack-archetype` projects: they show a
separate runtime library and custom component descriptors. A pack must currently be
complete, with Module, Flow and Exception Resolver definitions; component-only overlays
are not supported. Use a new pack ID for your library rather than replacing `V5.0.1-SNAPSHOT`.
Keep pack production dependencies empty: runtime library dependencies belong in component
metadata/generated application POMs; test dependencies belong in Maven test scope.

See Studio's `docs/CustomMetaPackHandover.md` and
`src/main/resources/studio/metapack/METAPACK.md` for the full authoring contract.
