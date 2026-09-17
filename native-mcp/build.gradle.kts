plugins {
    id("org.jetbrains.intellij.platform.module")
    id("org.jetbrains.kotlin.jvm")
}
repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}
dependencies {
    intellijPlatform {
        create("IU", "2026.2.2")
        bundledPlugin("com.intellij.mcpServer")
    }
    // Only the root Java API is needed; depending on its JAR would create a packaging cycle.
    compileOnly(files(rootProject.layout.buildDirectory.dir("classes/java/main")))
}
kotlin {
    jvmToolchain(21)
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}
tasks.compileKotlin {
    dependsOn(":compileJava")
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
