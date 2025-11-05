import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    alias(libs.plugins.architectury.loom) apply false
    alias(libs.plugins.architectury.plugin)
    alias(libs.plugins.shadowJar) apply false
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
}

architectury {
    minecraft = libs.versions.minecraft.get()
}

allprojects {
    group = property("maven_group").toString()
    version = property("mod_version").toString()
}

configure(listOf(project(":commonMod"), project(":fabric"), project(":neoforge"))) {
    apply(plugin = "dev.architectury.loom")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")
    apply(plugin = "kotlin")

    val loom = project.extensions.getByName<LoomGradleExtensionAPI>("loom")

    base {
        archivesName.set("${property("archives_name")}-${project.name}")
    }

    dependencies {
        "minecraft"(rootProject.libs.minecraft)
        "mappings"(loom.officialMojangMappings())
    }

    java {
        withSourcesJar()

        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release = 21
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                artifactId = base.archivesName.get()
                from(components["java"])
            }
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}