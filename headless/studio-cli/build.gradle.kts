plugins { application }
dependencies {
    implementation(project(":studio-generator"))
    runtimeOnly(project(":studio-bundled-packs"))
    testRuntimeOnly(project(":studio-bundled-packs"))
}
application { mainClass.set("org.ikasan.studio.cli.StudioMigrationCli") }
distributions {
    main {
        contents {
            from("../../tools/migration/studio_upgrade.py") { into("bin") }
            from("../../docs/CommandLineMigration.md")
            from("../../tools/migration/README.md")
            from("../../LICENSE.txt")
        }
    }
}

// Stable resource name for the IntelliJ composite build, independent of the CLI version.
tasks.register<Sync>("preparePluginTools") {
    from(tasks.named<Zip>("distZip").flatMap { it.archiveFile })
    into(layout.buildDirectory.dir("plugin-tools"))
    rename { "studio-offline-tools.zip" }
}
