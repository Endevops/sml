val exposed_version: String by project
val h2_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val sqlite_version: String by project

plugins {
    kotlin("jvm") version "2.3.0"
    id("io.ktor.plugin") version "3.3.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0"

    // Static analysis tools (report-only by default)
    id("org.jlleitschuh.gradle.ktlint") version "11.6.0"
}

// ktlint configuration (keep report-only; do not fail build by default)
ktlint {
    verbose.set(false)
    android.set(false)
    outputToConsole.set(true)
    enableExperimentalRules.set(false)
    ignoreFailures.set(true) // report-only
}

group = "be.endevops"
version = "0.0.1"

application {
    mainClass = "be.endevops.ApplicationKt"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-auto-head-response")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.xerial:sqlite-jdbc:$sqlite_version")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-di")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("dnsjava:dnsjava:3.6.3")
    implementation("tools.jackson.dataformat:jackson-dataformat-xml:3.0.3")
    implementation("tools.jackson.module:jackson-module-kotlin:3.0.3")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
