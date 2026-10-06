plugins { `java-library` }

abstract class ActiveMqTestArguments : CommandLineArgumentProvider {
    @get:org.gradle.api.tasks.Input
    abstract val variant: Property<String>

    @get:org.gradle.api.tasks.Classpath
    abstract val providerFiles: ConfigurableFileCollection

    override fun asArguments() = listOf("-Dstudio.activemq.${variant.get()}.classpath=${providerFiles.asPath}")
}

dependencies {
    api(project(":studio-pack-v3"))
    api(project(":studio-pack-v4"))
    testImplementation(project(":studio-test-kit"))
    testImplementation("commons-io:commons-io:2.22.0")
    // The shared SMTP fixture compiles and runs the same javax.mail helper shipped by both packs.
    // Keep aligned with flowTestPomTemplate_en.ftl and the root plugin test dependencies.
    testImplementation("com.icegreen:greenmail:1.6.15")
    testImplementation("com.sun.mail:jakarta.mail:1.6.8")
    testImplementation("junit:junit:4.13.2")
    // The same shared source set also compiles and exercises the local FTP fixture.
    testImplementation("org.apache.ftpserver:ftpserver-core:1.2.1")
    constraints {
        testImplementation("org.apache.mina:mina-core:2.2.9")
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
    // Use the same explicit security overrides as generated JMS applications.
    val manifest = groovy.json.JsonSlurper().parse(
        file("../../src/main/resources/studio/metapack/V$packVersion/metapack.json")) as Map<*, *>
    val overrides = manifest["compatibilityOverrides"] as List<*>
    val client = overrides.map { it as Map<*, *> }.single {
        it["groupId"] == "org.apache.activemq" && it["artifactId"] == "activemq-client"
    }
    dependencies.add(providerClasspath.name, "${client["groupId"]}:${client["artifactId"]}:${client["version"]}")
    val arguments = objects.newInstance<ActiveMqTestArguments>().apply {
        variant.set(suffix)
        providerFiles.from(providerClasspath)
    }
    tasks.test {
        jvmArgumentProviders.add(arguments)
    }
}
