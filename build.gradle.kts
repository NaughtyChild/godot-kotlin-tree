import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType
import java.io.FileInputStream
import java.util.*

val localProperties = loadLocalProps()

version = "2.1.0"
group = "io.github.nodetree"
description = "A type-safe Godot node tree representation in Kotlin"

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.25"
    id("com.gradle.plugin-publish") version "1.2.1"
    id("com.adarshr.test-logger") version "4.0.0"
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation(kotlin("test"))
}

configure<TestLoggerExtension> {
    theme = ThemeType.MOCHA
    showCauses = true
}

@Suppress("UnstableApiUsage")
gradlePlugin {
    website = "https://github.com/tomwyr/godot-kotlin-tree"
    vcsUrl = "https://github.com/tomwyr/godot-kotlin-tree.git"

    bindProp("gradle.publish.key")
    bindProp("gradle.publish.secret")

    plugins {
        create("godot-kotlin-tree") {
            id = "io.github.nodetree.godot-kotlin-tree"
            displayName = "Godot Kotlin Tree"
            description = "A type-safe Godot node tree representation in Kotlin"
            tags = listOf("godot", "kotlin", "node", "tree")
            implementationClass = "com.tomwyr.GodotKotlinTree"
        }
    }
}

fun bindProp(key: String) {
    val value = localProperties[key] as? String ?: throw Exception("Expected local property $key not set")
    System.setProperty(key, value)
}

fun loadLocalProps() = Properties().apply {
    load(FileInputStream(rootProject.file("local.properties")))
}
