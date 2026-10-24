plugins {
    `java-library`
    `maven-publish`
}

group = "dev.piotrulla"
version = "1.0.0"

val eternalCodeReleasesUrl = "https://repo.eternalcode.pl/releases"
val eternalCodeSnapshotsUrl = "https://repo.eternalcode.pl/snapshots"
val snapshotSuffix = "-SNAPSHOT"

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.javadoc {
    val javadocOptions = options as StandardJavadocDocletOptions
    javadocOptions.encoding = "UTF-8"
    javadocOptions.addStringOption("Xdoclint:none", "-quiet")
}

publishing {
    repositories {
        mavenLocal()

        maven {
            name = "eternalcode"
            url = uri(if (version.toString().endsWith(snapshotSuffix)) eternalCodeSnapshotsUrl else eternalCodeReleasesUrl)

            credentials {
                username = System.getenv("ETERNAL_CODE_MAVEN_USERNAME")
                password = System.getenv("ETERNAL_CODE_MAVEN_PASSWORD")
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
