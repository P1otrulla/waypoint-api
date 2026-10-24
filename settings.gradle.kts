pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    // auto-downloads JDK 17/21 toolchains if they're not installed locally
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.codemc.io/repository/maven-releases/")
    }
}

rootProject.name = "waypoint-api"

include("example-plugin")
