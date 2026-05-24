pluginManagement {
    includeBuild("F:/dev/godot/godotTree")

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    // to automatically download the toolchain jdk if missing
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0" // https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver-convention
}

rootProject.name = "example"
