plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.flyway)
    application
}

group = "griffio"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(libs.sqldelight.jdbc.driver)
    implementation(libs.google.truth)
    implementation(libs.pgvector)
    api(libs.sqldelight.postgresql.dialect)
    implementation(libs.postgresql.jdbc.driver)
    testImplementation(kotlin("test"))
}

val migrationsDir = layout.buildDirectory.dir("generated/migrations")

sqldelight {
    databases {
        create("Sample") {
            deriveSchemaFromMigrations.set(true)
            migrationOutputDirectory = migrationsDir
            migrationOutputFileFormat = ".sql"
            packageName.set("griffio.queries")
            dialect(libs.sqldelight.postgresql.dialect)
            module(project(":pgvector-module"))
        }
    }
}

tasks {
    ///sqldelight task generateMainSampleMigrations will output your .sqm files as valid SQL
    // in the output directory, with the output format.
    // Create a dependency from compileKotlin where flyway will have the files available on the classpath
    compileKotlin.configure {
        dependsOn("generateMainSampleMigrations")
    }
}

flyway {
    url = "jdbc:postgresql://localhost:5432/vector"
    user = "postgres"
    password = ""
    locations = arrayOf("filesystem:${migrationsDir.get().asFile}")
    baselineOnMigrate = true
    baselineVersion = "0"
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("griffio.MainKt")
}

// https://documentation.red-gate.com/fd/gradle-task-184127407.html
//:( Without this you may see an error like the following: No database found to handle jdbc:...
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(libs.flyway.database.postgresql)
    }
}
