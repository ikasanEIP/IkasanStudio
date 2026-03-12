import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease

plugins {
    id("java") // Java support
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    id("idea")
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Configure project's dependencies
repositories {
    mavenLocal() // This tells Gradle to use the local Maven repository (~/.m2/repository)
    mavenCentral()
    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    testImplementation(libs.junit)
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:4.0.0-M1")
    implementation("org.freemarker:freemarker:2.3.34")
    implementation("org.ikasan.studio:ikasan-studio-ide-mediator:1.0.2")
    testImplementation("org.freemarker:freemarker:2.3.34")
    testImplementation("org.mockito:mockito-core:5.21.0")
    implementation("org.mockito:mockito-inline:5.2.0")
    implementation("net.sourceforge.fmpp:fmpp:0.9.16")

    testImplementation("ch.qos.logback:logback-classic:1.5.32")

    testImplementation("org.ikasan:ikasan-test:4.1.4")
    compileOnly ("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    testCompileOnly("org.projectlombok:lombok:1.18.42")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.42")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }
}

// Source set for UI visual test harnesses that require human inspection.
// Lives in src/testHarness/java so it is never on the :test task's classpath.
// IntelliJ IDEA associates right-click → Run with :runHarness because that task
// declares testClassesDirs pointing at this source set's output directories.
sourceSets {
    create("testHarness") {
        java.srcDir("src/testHarness/java")
        // Needs main output (production UI/core classes), test output (TestFixtures, etc.),
        // and the full test compile/runtime dependency graph (IntelliJ Platform, JUnit, etc.)
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output +
                configurations["testCompileClasspath"]
        runtimeClasspath += output + configurations["testRuntimeClasspath"]
    }
}

// Tell IntelliJ IDEA that src/testHarness/java is a test source root so it shows
// the green test-class gutter icons and allows right-click → Run on individual methods.
idea {
    module {
        testSources.from(sourceSets["testHarness"].java.srcDirs)
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            select {
//                types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
                channels = listOf(ProductRelease.Channel.RELEASE)
                sinceBuild = "2025.3"
                untilBuild = "2025.3"
            }
        }
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }

    // Standard test task — only scans src/test/java output. PanelHarnessTest is in
    // src/testHarness/java so it is never on this task's classpath and never runs here.
    test {
        useJUnitPlatform()
    }

    // Task to run the UI visual harnesses. testClassesDirs is set to the testHarness
    // source set output so IntelliJ IDEA knows to delegate right-click → Run on harness
    // classes to this task rather than :test.
    register<Test>("runHarness") {
        group = "verification"
        description = "Runs the UI component test harnesses."
        testClassesDirs = sourceSets["testHarness"].output.classesDirs
        // The IntelliJ Platform Gradle Plugin only fully configures the standard :test task
        // (classpath, JVM args, sandbox). Custom Test tasks get none of that automatically.
        // We depend on prepareTest so the sandbox is created, then copy everything from :test
        // at execution time and append the compiled testHarness classes.
        dependsOn("prepareTest")
        useJUnitPlatform()
        // ConfigurableFileCollection is configuration-cache serializable; SourceSetOutput is not.
        val testHarnessOutput: ConfigurableFileCollection = objects.fileCollection()
            .from(sourceSets["testHarness"].output)
        // This task reads :test's classpath/JVM args at execution time, which is inherently
        // incompatible with configuration caching. This only affects ./gradlew runHarness;
        // ./gradlew test continues to benefit from the configuration cache normally.
        notCompatibleWithConfigurationCache("runHarness inherits :test classpath and JVM args at execution time")
        doFirst {
            // Inside doFirst the implicit receiver is Task, not Test — cast explicitly.
            val runHarness = this as Test
            val testTask = project.tasks.named("test", Test::class.java).get()
            runHarness.classpath = testTask.classpath + testHarnessOutput
            // jvmArgs only captures eagerly-set args. The IntelliJ Platform Gradle Plugin
            // adds --add-opens and other module flags via jvmArgumentProviders (lazy providers).
            // Resolve both sources so the forked test JVM has the full set of flags.
            val allJvmArgs = (testTask.jvmArgs ?: emptyList()) +
                testTask.jvmArgumentProviders.flatMap { it.asArguments() }
            runHarness.setJvmArgs(allJvmArgs)
            runHarness.systemProperties.putAll(testTask.systemProperties)
            runHarness.environment.putAll(testTask.environment)
        }
    }

    // Task to run ALL tests including UI tests
    register<Test>("allTests") {
        description = "Runs all tests including UI visual tests"
        group = "verification"

        useJUnitPlatform()
        ignoreFailures = true  // Accept ThreadLeakTracker failures from UI tests
    }
}


intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                    )
                }
            }

            plugins {
                robotServerPlugin()
            }
        }
    }
}
