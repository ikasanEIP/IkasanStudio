# Application project layout

New projects created with the Studio archetype 1.0.4-SNAPSHOT use the familiar Ikasan deployment structure:

```text
pom.xml                       Parent reactor and shared build settings
jar/
  pom.xml                     Application reactor and component dependencies
  generated/                  Studio-owned Java/configuration and the saved model
  user/                       Developer-owned Java and resources
  user-flow-tests/            Created on demand for developer-owned flow tests
bin/
  ikasan.sh                   Created when the module is configured; developer-owned thereafter
  simple-env.sh               Developer-owned JVM environment settings
  ikasan.bat                  Windows launcher
  simple-env.bat              Developer-owned Windows JVM environment settings
distribution/
  pom.xml                     Deployment assembly and application dependency
  distribution.xml            Editable ZIP contents
```

`jar/generated/src/main/model/model.json` remains the source of truth. Keep it in version control. The application is built from `jar/generated/` and `jar/user/`; this separation prevents regeneration from replacing business implementations. New application and implementation artifacts include the project name (for example, `orders-generated` and `orders-user`), avoiding collisions between projects installed into the same Maven repository.

Existing projects with root-level `generated/` and `user/` remain supported in place. Studio does not move their files, introduce deployment modules, or change their Maven coordinates automatically. Most existing guides use paths relative to the application reactor: for a new project, that reactor is `jar/`; for a legacy project, it is the workspace root.

## Building and running

After configuring the module and generating its code, run `mvn clean verify` from the workspace root using the JDK required by the selected Ikasan version. The distribution module creates `distribution/target/*-dist.zip`. Unzip the archive and run `bin/ikasan.sh` (or `sh bin/ikasan.sh`). On Windows, run `bin\ikasan.bat` and edit `bin\simple-env.bat`. The launcher runs in the foreground so a service manager can own its lifecycle. Application arguments are forwarded unchanged; configure JVM options in `bin/simple-env.sh` and select the JDK with `JAVA_HOME`.

The archive contains `bin/`, external `config/`, and runtime JARs under `lib/`. It includes the application, user implementation JAR and transitive runtime dependencies, excluding test-only dependencies. It uses the selected meta-pack's Ikasan dependencies and Java baseline; it does not copy the latest snapshot archetype's dependency versions into older applications. The launcher uses the normal Java classpath, rather than requiring an additional Spring Boot repackage step.

For flow tests from the workspace root, use `mvn -pl jar/user-flow-tests -am test`. Alternatively, run `mvn -pl user-flow-tests -am test` from `jar/`. Legacy projects continue using the latter command at their workspace root.

IntelliJ Run/Debug, independent H2, the local harness, environment settings and test fixtures use the application reactor as their working directory. Local overrides and runtime data therefore live under `jar/config/`, `jar/temporary-files/`, and other application-relative locations in new projects. Workspace instructions and the AI proposal inbox remain at the workspace root.

## What generation maintains

Studio maintains the parent POM's selected Ikasan/Java contract, required reactor modules, and properties identifying the application artifact. It adds the required application dependency and assembly execution to `distribution/pom.xml`, preserving other dependencies, modules and explicit plugin settings. The selected meta-pack supplies the runtime/BOM and Java settings.

Launcher scripts and `distribution/distribution.xml` are seeded only when missing. Existing files are developer-owned and are not replaced during regeneration. Their default contents use Maven properties instead of fixed application names and versions. Keep custom packaging compatible with changes you make to application coordinates, resources or runtime requirements. The generated POMs are merged as Maven models; comments and formatting may be normalised, so keep important operational instructions in your own documentation.

Generation prepares these updates together with application code, rejects stale or unsaved scaffolding edits, and rolls the batch back on failure. Reviewed V3/V4 migration snapshots include affected parent and distribution settings so restoration can recover the complete version contract. This does not add a supported V5 migration route.

## Developing the archetype locally

Build/install the new archetype with `mvn -f ikasan-studio-ancillary/ikasan-studio-project-archetype/pom.xml install`, then explicitly choose version `1.0.4-SNAPSHOT` when creating a Maven project from it. Existing published archetype versions continue to create their original layout. A local archetype build is separate from a plugin ZIP or Marketplace publication.
