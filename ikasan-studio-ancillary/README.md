# Ikasan Studio ancillary projects

These Maven projects have a release lifecycle separate from the Gradle IntelliJ plugin.

- `ikasan-studio-project-archetype` creates an application to design with Studio.
- [example-metapack](example-metapack/README.md) is a working, standalone Java 17/Maven
  pack-authoring project with a separate runtime library and a scheduled sample flow.
- `ikasan-studio-metapack-archetype` generates that authoring project. Its build copies
  the maintained example directly; FreeMarker templates and Java sources are unfiltered.

## Build the meta-pack starter

From the Studio repository root:

```sh
./gradlew -p headless publishToMavenLocal
mvn -f ikasan-studio-ancillary/example-metapack/pom.xml clean verify
mvn -f ikasan-studio-ancillary/ikasan-studio-metapack-archetype/pom.xml clean install
```

The archetype build generates a project in its integration test and runs `mvn verify`
against it, including installer validation of the generated pack JAR. Building the new
archetype does not require rebuilding or releasing the application archetype.

Generate a project from any working directory:

```sh
mvn org.apache.maven.plugins:maven-archetype-plugin:3.3.1:generate \
  -DarchetypeGroupId=org.ikasan.studio \
  -DarchetypeArtifactId=ikasan-studio-metapack-archetype \
  -DarchetypeVersion=0.1.0-SNAPSHOT \
  -DgroupId=com.acme -DartifactId=acme-metapack \
  -Dversion=0.1.0-SNAPSHOT -Dpackage=com.acme -DinteractiveMode=false
cd acme-metapack
mvn clean verify
```

Read the generated README before renaming or publishing: the root coordinates are
customised, while the child modules, component namespace and pack ID retain the working
example names. Studio dependencies currently require the local publication step above.
No remote publication or Maven Central release is configured for the new artifacts.

For the complete authoring contract and lifecycle, see the
[custom pack handover](../docs/CustomMetaPackHandover.md) and
[independent artifacts guide](../docs/IndependentMetaPackArtifacts.md).
