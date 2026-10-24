import xyz.jpenilla.runpaper.task.RunServer

plugins {
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

group = "dev.piotrulla"
version = "1.0.0"

val packetEventsVersion: String = libs.versions.packetevents.get()
val packetEventsPluginUrl = "https://github.com/retrooper/packetevents/releases/download/v$packetEventsVersion/packetevents-spigot-$packetEventsVersion.jar"

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.packetevents.spigot)
    implementation(project(":"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(libs.versions.java.compile.get()))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(libs.versions.java.compile.get().toInt())
}

tasks.processResources {
    val pluginVersion = project.version.toString()
    inputs.property("version", pluginVersion)
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
}

fun RunServer.configureWaypointServer(minecraftVersion: String, javaVersion: String) {
    minecraftVersion(minecraftVersion)
    runDirectory.set(layout.projectDirectory.dir("run/$minecraftVersion"))
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    })
    downloadPlugins {
        url(packetEventsPluginUrl)
    }
}

tasks.runServer {
    this.configureWaypointServer(libs.versions.minecraft.modern.get(), libs.versions.java.runtime.modern.get())
}

tasks.register<RunServer>("runServerLegacy") {
    group = "run paper"
    description = "Runs Paper ${libs.versions.minecraft.legacy.get()} with the example plugin (armor stand fallback renderer)"
    pluginJars(tasks.shadowJar.flatMap { it.archiveFile })
    this.configureWaypointServer(libs.versions.minecraft.legacy.get(), libs.versions.java.compile.get())
}
