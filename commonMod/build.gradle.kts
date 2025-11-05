plugins {
    alias(libs.plugins.kotlin.plugin.serialization)
}

architectury {
    common("${property("enabled_platforms")}".split(","))
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    modImplementation(libs.fabric.loader)
    compileOnly(libs.adventure.platform.shared)
    modCompileOnly(fileTree(mapOf("dir" to "../libs", "include" to listOf("*.jar"))))

    compileOnly(libs.adventure.minimessage)
    compileOnly(libs.adventure.legacy)
    implementation(libs.kotlin.serializationJson)
    compileOnly(libs.okhttp)
    compileOnly(libs.kotlinx.datetime)
}