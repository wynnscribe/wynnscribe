plugins {
    alias(libs.plugins.shadowJar)
    alias(libs.plugins.kotlin.plugin.serialization)
    kotlin("jvm")
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

configurations {
    create("common") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
    named("compileClasspath").configure {
        extendsFrom(configurations["common"])
    }
    named("runtimeClasspath").configure {
        extendsFrom(configurations["common"])
    }
    named("developmentNeoForge").configure {
        extendsFrom(configurations["common"])
    }
    create("shadowBundle") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
}

repositories {
    maven("https://maven.neoforged.net/releases")
    maven("https://jitpack.io")
}

dependencies {
    "neoForge"(libs.neoForge)
    modImplementation(libs.architectury.neoForge)
    modImplementation(libs.adventure.platform.neoforge)
    "shadowBundle"(libs.adventure.platform.neoforge)
    implementation(kotlin("stdlib"))

    implementation(libs.adventure.minimessage)
    implementation(libs.adventure.legacy)
    implementation(libs.kotlin.serializationJson)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.datetime)

    "common"(project(path = ":commonMod", configuration = "namedElements")) { isTransitive = false }
    "shadowBundle"(project(path = ":commonMod", configuration = "transformProductionNeoForge"))
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(mapOf("version" to project.version))
    }
}



tasks.shadowJar {
    minimize()
    configurations = listOf(project.configurations.getByName("shadowBundle"))
    archiveClassifier.set("dev-shadow")
}

tasks.remapJar {
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
}