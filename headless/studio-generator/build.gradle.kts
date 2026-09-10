plugins { `java-library` }

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.22.1")
    api("org.apache.maven:maven-model:3.9.9")
    // Export the patched Plexus dependency to consumers, including Maven consumers.
    api("org.codehaus.plexus:plexus-utils:3.6.1") {
        because("Fix CVE-2025-67030 while retaining the Maven model's Plexus 3.x API")
    }
    api("org.freemarker:freemarker:2.3.35")
    implementation("commons-io:commons-io:2.22.0")
    implementation("org.slf4j:slf4j-api:2.0.19")
    compileOnly("org.projectlombok:lombok:1.18.48")
    annotationProcessor("org.projectlombok:lombok:1.18.48")
    testImplementation("com.tngtech.archunit:archunit:1.5.0")
}

// Production Java sources live in this module; the plugin consumes its JAR.
sourceSets {
    main {
        resources.setSrcDirs(listOf("../../src/main/resources"))
        resources.include("studio/metapack/schema/**")
    }
    test {
        java.srcDir("../../src/test/java")
        java.include("org/ikasan/studio/testing/engine/**")
        resources.srcDir("../../src/test/resources")
        resources.include("studio/metapack/Engine*/**", "studio/metapack/FailureInjection/**",
            "studio/metapack/TestV1/**", "studio/validation/**")
    }
}
