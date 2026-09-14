plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.5.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

description = "The ultimate™ leaderboards plugin. Allows you to make modern leaderboards easily."

val baseVersion = project.findProperty("version")?.toString() ?: "1.3"
val buildNumber = System.getenv("GITHUB_RUN_NUMBER") ?: project.findProperty("buildNumber")?.toString()
val gitRef = System.getenv("GITHUB_REF") ?: ""
val gitCommit = System.getenv("GITHUB_SHA")?.take(7)

version = when {
    gitRef.startsWith("refs/tags/v") -> gitRef.removePrefix("refs/tags/v")
    gitRef.startsWith("refs/tags/") -> gitRef.removePrefix("refs/tags/")
    !buildNumber.isNullOrEmpty() -> {
        if (!gitCommit.isNullOrEmpty()) "$baseVersion-DEV-b$buildNumber+$gitCommit"
        else "$baseVersion-DEV-b$buildNumber"
    }
    else -> "$baseVersion-LOCAL"
}

tasks.shadowJar {
    archiveClassifier.set("")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.skriptlang.org/releases")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven {
        name = "faststatsReleases"
        url = uri("https://repo.faststats.dev/releases")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.github.SkriptLang:Skript:2.10.2") { isTransitive = false }
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("dev.faststats.metrics:bukkit:0.28.0")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    jar {
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion("1.21.11")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        filteringCharset = "UTF-8"
        val props = mapOf("version" to version, "description" to project.description)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}