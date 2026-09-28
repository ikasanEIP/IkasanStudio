plugins { `java-library` }
dependencies {
    api(project(":studio-pack-v3"))
    api(project(":studio-pack-v4"))
    testImplementation(project(":studio-test-kit"))
    testImplementation("commons-io:commons-io:2.22.0")
    // The shared local FTP/SMTP flow-test fixture tests compile against the same libraries as the plugin's test suite.
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.icegreen:greenmail:1.6.15")
    testImplementation("org.apache.ftpserver:ftpserver-core:1.2.1") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    constraints {
        testImplementation("org.apache.mina:mina-core:2.2.9") {
            because("CVE-2026-41635 / CVE-2026-41409 - fixes deserialization RCE in mina-core < 2.2.9")
        }
    }
}
// Give this module its own source root. Sharing the plugin's entire test root with
// include filters makes IntelliJ assign unrelated plugin tests to this module.
val sharedTestSources = tasks.register<Sync>("syncSharedTestSources") {
    from("../../src/test/java") {
        include("org/ikasan/studio/testing/packs/**", "org/ikasan/studio/core/TestFixtures.java",
            "org/ikasan/studio/core/generator/TestUtils.java", "org/ikasan/studio/SharedResourceExtension.java",
            "org/ikasan/studio/testkit/**")
    }
    into(layout.buildDirectory.dir("generated/sources/sharedTest/java"))
}

sourceSets {
    test {
        java.srcDir(sharedTestSources)
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
