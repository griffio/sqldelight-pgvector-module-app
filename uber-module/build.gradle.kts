plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.grammarKitComposer)
    id("maven-publish")
    id("org.jreleaser") version "1.18.0"
}

version = "0.0.1"
group = "io.github.griffio"

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://cache-redirector.jetbrains.com/download-pgp-verifier")
    // Grazie
    maven("https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/grazi/grazie-platform-public")
}

grammarKit {
    intellijRelease.set(libs.versions.intellij)
}

dependencies {
    implementation(libs.sqldelight.dialect.api)
    implementation(libs.sqldelight.postgresql.dialect)
    implementation(libs.sqldelight.compiler.env)
    implementation("net.postgis:postgis-jdbc:2024.1.0")
}
