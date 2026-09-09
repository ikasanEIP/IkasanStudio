plugins { `java-library` }
dependencies {
    testImplementation(project(":studio-test-kit"))
    testImplementation("commons-io:commons-io:2.22.0")
}
sourceSets {
    main {
        resources.setSrcDirs(listOf("../../src/main/resources"))
        resources.include("studio/metapack/**")
        resources.exclude("studio/metapack/schema/**")
    }
    test {
        java.srcDir("../../src/test/java")
        java.include("org/ikasan/studio/testing/packs/**", "org/ikasan/studio/core/TestFixtures.java",
            "org/ikasan/studio/core/generator/TestUtils.java", "org/ikasan/studio/SharedResourceExtension.java",
            "org/ikasan/studio/testkit/**")
        resources.srcDir("../../src/test/resources")
        resources.include("studio/templates/**", "studio/metapack/TestV*/**")
    }
}
