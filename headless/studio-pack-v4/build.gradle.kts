import groovy.json.JsonSlurper

plugins { `java-library` }

// The manifest is the single source of truth for this artifact's independent revision.
val metadata = JsonSlurper().parseText(providers.fileContents(layout.projectDirectory.file(
    "../../src/main/resources/studio/metapack/V4.1.6/metapack.json")).asText.get()) as Map<*, *>
version = metadata["packVersion"] as String

sourceSets.main {
    resources.setSrcDirs(listOf("../../src/main/resources"))
    resources.include("studio/metapack/V4.1.6/**")
}
dependencies { testImplementation(project(":studio-test-kit")) }
