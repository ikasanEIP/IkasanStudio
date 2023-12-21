// might not need below
//import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

// might not need below
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
// might not need below
//fun properties(key: String) = project.findProperty(key).toString()
fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)


plugins {

    id("java")  // Java support
    
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
    
    
    // Kotlin support
    //id("org.jetbrains.kotlin.jvm") version "1.5.10"
    
    
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    //id("org.jetbrains.intellij") version "1.0"
    
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    //id("org.jetbrains.changelog") version "1.1.2"
    
    // detekt linter - read more: https://detekt.github.io/detekt/gradle.html
    //id("io.gitlab.arturbosch.detekt") version "1.17.1"
    
    // ktlint linter - read more: https://github.com/JLLeitschuh/ktlint-gradle
    //id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
//    id("java")
//    // Kotlin support
//    id("org.jetbrains.kotlin.jvm") version "1.5.10"
//    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
//    id("org.jetbrains.intellij") version "1.0"
//    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
//    id("org.jetbrains.changelog") version "0.6.2"
//    // detekt linter - read more: https://detekt.github.io/detekt/gradle.html
//    id("io.gitlab.arturbosch.detekt") version "1.17.1"
//    // ktlint linter - read more: https://github.com/JLLeitschuh/ktlint-gradle
//    id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
}

// Import variables from gradle.properties file
//val pluginGroup: String by project
// `pluginName_` variable ends with `_` because of the collision with Kotlin magic getter in the `intellij` closure.
// Read more about the issue: https://github.com/JetBrains/intellij-platform-plugin-template/issues/29
//val pluginName_: String by project
//val pluginVersion: String by project
//val pluginSinceBuild: String by project
//val pluginUntilBuild: String by project
//val pluginVerifierIdeVersions: String by project

//val platformType: String by project
//val platformVersion: String by project
//val platformPlugins: String by project
//val platformDownloadSources: String by project

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

// Configure project's dependencies
repositories {
    mavenCentral()
    //jcenter()
}
dependencies {
//    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.17.1")
// For now, pause the use of V2.2, too many issues with Plugin clashes.
//    api("org.apache.velocity:velocity-engine-core:2.2")
//    implementation("org.apache.velocity:velocity-engine-core:2.2")
//    implementation("org.freemarker:freemarker:2.3.20")
    implementation("org.freemarker:freemarker:2.3.31")
    implementation("org.ikasan:ikasan-eip-standalone:3.1.0")
    implementation("org.ikasan:ikasan-ftp-endpoint:3.1.0")
    implementation("org.ikasan:ikasan-h2-standalone-persistence:3.1.0")
//    testImplementation("org.freemarker:freemarker:2.3.20")
    testImplementation("org.freemarker:freemarker:2.3.31")
//    testImplementation("org.apache.velocity:velocity-engine-core:2.3.20")
    testImplementation("org.ikasan:ikasan-eip-standalone:3.0.1")
    testImplementation("org.ikasan:ikasan-ftp-endpoint:3.1.0")
    testImplementation("org.ikasan:ikasan-jms-spring-arjuna:3.1.0")
    testImplementation("org.ikasan:ikasan-component-converter:3.1.0")
    testImplementation("org.ikasan:ikasan-test-endpoint:3.1.0")
    testImplementation("org.ikasan:ikasan-test:3.1.0")
    compileOnly ("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")
}

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    @Suppress("UnstableApiUsage")
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(17)
        vendor = JvmVendorSpec.JETBRAINS
    }
}


java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName = properties("pluginName")
    version = properties("platformVersion")
    type = properties("platformType")
//    downloadSources = properties("platformDownloadSources")

    //pluginName.set(properties("pluginName"))
    //version.set(properties("platformVersion"))
    //type.set(properties("platformType"))
//    downloadSources.set(properties("platformDownloadSources").toBoolean())
   //updateSinceUntilBuild.set(true)

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins = properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

// Configure detekt plugin.
// Read more: https://detekt.github.io/detekt/kotlindsl.html
//detekt {
//    config = files("./detekt-config.yml")
//    buildUponDefaultConfig = true

//    reports {
//        html.enabled = false
//        xml.enabled = false
//        txt.enabled = false
//    }
//}
// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath = provider { file(".qodana").canonicalPath }
    reportPath = provider { file("build/reports/inspections").canonicalPath }
    saveReport = true
    showReport = environment("QODANA_SHOW_REPORT").map { it.toBoolean() }.getOrElse(false)
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
koverReport {
    defaults {
        xml {
            onCheck = true
        }
    }
}

tasks {
    // Set the compatibility versions to 1.8
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

//    withType<Detekt> {
//        jvmTarget = "1.8"
//    }

    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    patchPluginXml {
        version = properties("pluginVersion")
        sinceBuild = properties("pluginSinceBuild")
        untilBuild = properties("pluginUntilBuild")


        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with (it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    signPlugin {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }


    publishPlugin {
        dependsOn("patchChangelog")
        token = environment("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = properties("pluginVersion").map { listOf(it.split('-').getOrElse(1) { "default" }.split('.').first()) }
    }


    // This seems quite brittle, to test, IJ requires to state the local path of where intellij community source code is installed.
//    test {
// //        systemProperty("idea.home.path", "/dev/ws/intellij-community-193.7288")
//         systemProperty("idea.home.path", intellijRootDir().canonicalPath)
//    }
}
