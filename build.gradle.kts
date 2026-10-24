plugins {
    `waypoint-publish`
}

dependencies {
    // compile against the oldest supported API so nothing newer sneaks in
    compileOnly(libs.paper.api)
    compileOnly(libs.packetevents.spigot)

    testImplementation(libs.paper.api)
    testImplementation(libs.packetevents.spigot)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(libs.versions.java.compile.get()))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(libs.versions.java.compile.get().toInt())
}

tasks.test {
    useJUnitPlatform()
}
