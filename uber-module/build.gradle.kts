import org.jreleaser.model.Active

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

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "sqldelight-pgvector"
            groupId = project.group.toString()
            version = project.version.toString()

            pom {
                name.set("SQLDelight PgVector Module")
                description.set("SQLDelight module for PgVector support")
                url.set("https://github.com/griffio/sqldelight-pgvector-module-app")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("griffio")
                        name.set("griffio")
                        email.set("griffio.work@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/griffio/sqldelight-pgvector-module-app.git")
                    developerConnection.set("scm:git:ssh://github.com/griffio/sqldelight-pgvector-module-app.git")
                    url.set("https://github.com/griffio/sqldelight-pgvector-module-app")
                }
            }
        }
    }
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

jreleaser {
    gitRootSearch = true
    project {
        description = "SQLDelight module for PgVector support"
        copyright = "2025 griffio"
    }
    deploy {
        signing {
            active = Active.ALWAYS
        }
        maven {
            mavenCentral { // https://jreleaser.org/guide/latest/reference/deploy/maven/maven-central.html
                create("maven-central") {
                    active = Active.ALWAYS
                    applyMavenCentralRules = true
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}

