import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.register
import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.shadowJar)
    alias(libs.plugins.kotlin.plugin.serialization)
    kotlin("jvm")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

configurations {
    create("common")

    named("common") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }

    named("compileClasspath").configure {
        extendsFrom(configurations["common"])
    }
    named("runtimeClasspath").configure {
        extendsFrom(configurations["common"])
    }
    named("developmentFabric").configure {
        extendsFrom(configurations["common"])
    }

    create("shadowBundle")

    named("shadowBundle") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)

    implementation(kotlin("stdlib"))
    "shadowBundle"(kotlin("stdlib"))

    modCompileOnly(fileTree(mapOf("dir" to "../libs", "include" to listOf("*.jar"))))

    include(libs.adventure.platform.fabric)
    modImplementation(libs.adventure.platform.fabric)

    implementation(libs.adventure.minimessage)
    "shadowBundle"(libs.adventure.minimessage)
    implementation(libs.adventure.legacy)
    "shadowBundle"(libs.adventure.legacy)
    implementation(libs.kotlin.serializationJson)
    "shadowBundle"(libs.kotlin.serializationJson)
    implementation(libs.okhttp)
    "shadowBundle"(libs.okhttp)
    implementation(libs.kotlinx.datetime)
    "shadowBundle"(libs.kotlinx.datetime)

    "common"(project(path = ":commonMod", configuration = "namedElements")) { isTransitive = false }
    "shadowBundle"(project(path = ":commonMod", configuration = "transformProductionFabric")) { isTransitive = false }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}

val generateConfigTrue = tasks.register("generateConfigTrue") {
    group = "build"
    description = "Generates config.properties with AI_ENABLED=true"

    val propsFile = project.layout.buildDirectory.file("generated-resources/true/wynnscribe.config.properties")
    outputs.file(propsFile)

    doLast {
        propsFile.get().asFile.apply {
            parentFile.mkdirs()
            Properties().apply {
                setProperty("AI_ENABLED", "true")
                outputStream().use { fos ->
                    store(fos, "Generated config")
                }
            }
        }
    }
}

val generateConfigFalse = tasks.register("generateConfigFalse") {
    group = "build"
    description = "Generates config.properties with AI_ENABLED=false"

    val propsFile = project.layout.buildDirectory.file("generated-resources/false/wynnscribe.config.properties")
    outputs.file(propsFile)

    doLast {
        propsFile.get().asFile.apply {
            parentFile.mkdirs()
            Properties().apply {
                setProperty("AI_ENABLED", "false")
                outputStream().use { fos ->
                    store(fos, "Generated config")
                }
            }
        }
    }
}

tasks.register<ShadowJar>("shadowJarTrue") {
    group = "build"
    description = "Builds the 'true' shadow JAR (pre-remap)"

    configurations = listOf(project.configurations.getByName("shadowBundle"))
    relocate("kotlinx", "com.wynnscribe.libs.kotlinx")
    relocate("kotlin", "com.wynnscribe.libs.kotlin")
    relocate("okhttp", "com.wynnscribe.libs.okhttp")
    relocate("okio", "com.wynnscribe.libs.okio")

    archiveClassifier.set("ai-dev-shadow")

    dependsOn(tasks.named("classes"), tasks.named("processResources"), generateConfigFalse)

    val mainSourceSet = sourceSets.main.get()
    from(mainSourceSet.output.classesDirs)
    from(mainSourceSet.output.resourcesDir)
    from(generateConfigTrue.get().outputs.files) {
        into("")
    }
}

tasks.register<ShadowJar>("shadowJarFalse") {
    group = "build"
    description = "Builds the 'false' shadow JAR (pre-remap)"

    configurations = listOf(project.configurations.getByName("shadowBundle"))
    relocate("kotlinx", "com.wynnscribe.libs.kotlinx")
    relocate("kotlin", "com.wynnscribe.libs.kotlin")
    relocate("okhttp", "com.wynnscribe.libs.okhttp")
    relocate("okio", "com.wynnscribe.libs.okio")

    archiveClassifier.set("dev-shadow")

    dependsOn(tasks.named("classes"), tasks.named("processResources"), generateConfigTrue)

    val mainSourceSet = sourceSets.main.get()
    from(mainSourceSet.output.classesDirs)
    from(mainSourceSet.output.resourcesDir)
    from(generateConfigFalse.get().outputs.files) {
        into("")
    }
}

tasks.register<net.fabricmc.loom.task.RemapJarTask>("remapJarTrue") {
    group = "build"
    description = "Remaps the 'true' shadow JAR."

    inputFile.set(tasks.named("shadowJarTrue").flatMap { (it as ShadowJar).archiveFile })
    archiveClassifier.set("ai")
    addNestedDependencies.set(true)
}

tasks.register<net.fabricmc.loom.task.RemapJarTask>("remapJarFalse") {
    group = "build"
    description = "Remaps the 'false' shadow JAR."

    inputFile.set(tasks.named("shadowJarFalse").flatMap { (it as ShadowJar).archiveFile })
    archiveClassifier.set("")
    addNestedDependencies.set(true)
}


tasks.register("buildAllShadowJars") {
    group = "build"
    description = "Builds both true and false (remapped) shadow JARs."

    dependsOn(tasks.named("remapJarTrue"), tasks.named("remapJarFalse"))
}

tasks.named("shadowJar") {
    enabled = false
}

tasks.named("remapJar") {
    dependsOn.clear()
    enabled = false
}