plugins {
    // Provision the JDK that matches the Kotlin toolchain (26) when it is not installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "sml-develop"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
