plugins {
    // Included builds do not inherit the root build's toolchain download repositories.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "studio-headless"
include("studio-generator", "studio-test-kit", "studio-bundled-packs")

include("studio-pack-v3", "studio-pack-v4")
