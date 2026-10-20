import groovy.json.JsonSlurper

plugins { `java-library` }

// Bundled on the Ikasan 5 development branch for interactive sandbox testing.
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
    doFirst { delete(destination) }
    systemProperty("studio.v5.fixture", destination.get().asFile.absolutePath)
}

// Deliberately compile-only: these fixtures contain business TODOs and endpoint placeholders.
tasks.register<Test>("generateComponentCoverageFixture") {
    description = "Render every V5 runtime component and its flow test for compilation coverage."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("*ComponentCoverageTest") }
    val destination = layout.buildDirectory.dir("component-coverage-fixture")
    outputs.dir(destination)
    doFirst { delete(destination) }
    systemProperty("studio.v5.components", destination.get().asFile.absolutePath)
}

// Nine isolated apps: real local transport runtime verification is an explicit Maven/JDK 26 gate.
tasks.register<Test>("generateRuntimeTransportFixtures") {
    description = "Render local JMS, SMTP, FTP and SFTP runtime scenarios."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("*RuntimeTransportFixtureTest") }
    val destination = layout.buildDirectory.dir("runtime-transports")
    outputs.dir(destination)
    doFirst { delete(destination) }
    systemProperty("studio.v5.transports", destination.get().asFile.absolutePath)
}

// Intentional transaction failures are separate from success-only generated scenarios.
tasks.register<Test>("generateRollbackFixtures") {
    description = "Render local JMS, FTP and SFTP transaction rollback and retry scenarios."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("*RollbackFixtureTest") }
    val destination = layout.buildDirectory.dir("rollback-fixtures")
    outputs.dir(destination)
    doFirst { delete(destination) }
    systemProperty("studio.v5.rollback", destination.get().asFile.absolutePath)
}

tasks.register<Test>("generateProcessingFixtures") {
    description = "Render live V5 routing, filtering, splitting, translation and Jakarta XML scenarios."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("*ProcessingFixtureTest") }
    val destination = layout.buildDirectory.dir("processing-fixtures")
    outputs.dir(destination)
    doFirst { delete(destination) }
    systemProperty("studio.v5.processing", destination.get().asFile.absolutePath)
}

tasks.register<Test>("generateInputFixtures") {
    description = "Render V5 timer-only and local-file input scenarios, including transparent debug components."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("*InputFixtureTest") }
    val destination = layout.buildDirectory.dir("input-fixtures")
    outputs.dir(destination)
    doFirst { delete(destination) }
    systemProperty("studio.v5.inputs", destination.get().asFile.absolutePath)
}

// Real user/generated/user-flow-tests boundaries, using the checked-in archetype POMs.
val reactorPomFiles = listOf("pom.xml", "user/pom.xml", "generated/pom.xml").map {
    rootProject.file("../ikasan-studio-ancillary/ikasan-studio-project-archetype/src/main/resources/archetype-resources/$it")
}
tasks.named<Test>("test") { inputs.files(reactorPomFiles) }
tasks.register<Test>("generateReactorFixture") {
    description = "Render the actual archetype reactor with a runnable V5 flow test."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("*ReactorFixtureTest") }
    val destination = layout.buildDirectory.dir("reactor-fixture")
    outputs.dir(destination)
    inputs.files(reactorPomFiles)
    doFirst { delete(destination) }
    systemProperty("studio.v5.reactor", destination.get().asFile.absolutePath)
}

// Keep the ordinary pack contracts isolated; only migration rehearsals load older packs.
val migrationPacks = configurations.create("migrationPacks")
dependencies {
    migrationPacks(project(":studio-pack-v3"))
    migrationPacks(project(":studio-pack-v4"))
}
tasks.named<Test>("test") { exclude("**/MigrationFixtureTest.class") }
tasks.register<Test>("generateMigrationFixtures") {
    description = "Rehearse V3/V4 to V5 model migration and export generated applications."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath + migrationPacks
    filter { includeTestsMatching("*MigrationFixtureTest") }
    val destination = layout.buildDirectory.dir("migration-fixtures")
    outputs.dir(destination)
    doFirst { delete(destination) }
    systemProperty("studio.v5.migration", destination.get().asFile.absolutePath)
}
