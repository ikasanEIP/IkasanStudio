plugins { `java-library` }
dependencies {
    api(project(":studio-generator"))
    api(platform("org.junit:junit-bom:6.1.3"))
    api("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly(project(":studio-bundled-packs"))
}
