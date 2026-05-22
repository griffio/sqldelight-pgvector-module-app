pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "sqldelight-pgvector-module-app"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            val vSqlDelight = "2.3.2"
            val vIntellij = "231.9392.1"
            version("intellij", vIntellij)
            plugin("kotlin", "org.jetbrains.kotlin.jvm").version("2.3.10")
            plugin("sqldelight", "app.cash.sqldelight").version(vSqlDelight)
            plugin("flyway", "org.flywaydb.flyway").version("12.1.1")
            library("sqldelight-dialect-api", "app.cash.sqldelight:dialect-api:$vSqlDelight")
            library("sqldelight-jdbc-driver", "app.cash.sqldelight:jdbc-driver:$vSqlDelight")
            library("sqldelight-postgresql-dialect", "app.cash.sqldelight:postgresql-dialect:$vSqlDelight")
            library("sqldelight-compiler-env", "app.cash.sqldelight:compiler-env:$vSqlDelight")
            library("postgresql-jdbc-driver", "org.postgresql:postgresql:42.7.11")
            library("flyway-database-postgresql", "org.flywaydb:flyway-database-postgresql:12.1.1")
            library("google-truth", "com.google.truth:truth:1.4.2")
            library("intellij-ide", "com.jetbrains.intellij.platform:ide:$vIntellij")
            library("pgvector", "com.pgvector:pgvector:0.1.6")
            plugin("intellij", "org.jetbrains.intellij.platform").version("2.1.0")
            plugin("grammarKitComposer", "com.alecstrong.grammar.kit.composer").version("0.1.12")
        }
    }
}

include("pgvector-module")
