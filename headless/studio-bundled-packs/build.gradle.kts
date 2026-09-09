plugins { `java-library` }
dependencies {
    api(project(":studio-pack-v3"))
    api(project(":studio-pack-v4"))
    testImplementation(project(":studio-test-kit"))
    testImplementation("commons-io:commons-io:2.22.0")
}
sourceSets {
    test {
        java.srcDir("../../src/test/java")
        java.include("org/ikasan/studio/testing/packs/**", "org/ikasan/studio/core/TestFixtures.java",
            "org/ikasan/studio/core/generator/TestUtils.java", "org/ikasan/studio/SharedResourceExtension.java",
            "org/ikasan/studio/testkit/**")
        resources.srcDir("../../src/test/resources")
        resources.include("studio/templates/**", "studio/metapack/TestV*/**")
    }
}

// Isolated provider classpaths let the generated helper be compiled and exercised against
// both javax.jms (V3) and jakarta.jms (V4), without leaking broker dependencies into the kit.
for ((suffix, packVersion) in mapOf("v3" to "3.3.9", "v4" to "4.1.6")) {
    val providerClasspath = configurations.create("activeMqTestRuntime$suffix") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
    dependencies.add(providerClasspath.name, dependencies.platform("org.ikasan:ikasan-eip-standalone-bom:$packVersion"))
    dependencies.add(providerClasspath.name, "org.apache.activemq:activemq-client")
    tasks.test {
        inputs.files(providerClasspath).withPropertyName("activeMq$suffix").withNormalizer(ClasspathNormalizer::class.java)
        jvmArgumentProviders.add(CommandLineArgumentProvider {
            listOf("-Dstudio.activemq.$suffix.classpath=${providerClasspath.asPath}")
        })
    }
}
