plugins { application }
repositories { mavenCentral() }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(17)) }
dependencies {
    implementation("org.ikasan.studio:studio-generator:0.1.0-SNAPSHOT")
    runtimeOnly("org.ikasan.studio:studio-bundled-packs:0.1.0-SNAPSHOT")
}
application { mainClass.set("BuildFixture") }
