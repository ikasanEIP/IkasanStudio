plugins { base }

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    group = "org.ikasan.studio"
    version = "0.1.0-SNAPSHOT"
    repositories { mavenLocal(); mavenCentral() }
    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
        withSourcesJar()
    }
    tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
    dependencies {
        "testImplementation"(platform("org.junit:junit-bom:6.1.3"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testImplementation"("org.assertj:assertj-core:4.0.0-M1")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        systemProperty("java.awt.headless", "true")
        workingDir = rootProject.projectDir.parentFile
    }
    extensions.configure<PublishingExtension> {
        publications.create<MavenPublication>("library") { from(components["java"]) }
    }
}
