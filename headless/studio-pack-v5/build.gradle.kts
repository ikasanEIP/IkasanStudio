import groovy.json.JsonSlurper

plugins { `java-library` }

// Deliberately not selected by studio-bundled-packs: this runtime is unreleased.
val metadata = JsonSlurper().parseText(providers.fileContents(layout.projectDirectory.file(
    "src/main/resources/studio/metapack/V5.0.1-SNAPSHOT/metapack.json")).asText.get()) as Map<*, *>
version = metadata["packVersion"] as String

dependencies { testImplementation(project(":studio-test-kit")) }

// Export a real generated application for Maven/JDK 26 checks without changing sample projects.
tasks.register<Test>("generateCompatibilityFixture") {
    description = "Render the Ikasan 5 snapshot compilation fixture (does not start flows)."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("*DevelopmentFixtureTest") }
    val destination = layout.buildDirectory.dir("compatibility-fixture")
    outputs.dir(destination)
    systemProperty("studio.v5.fixture", destination.get().asFile.absolutePath)
}
