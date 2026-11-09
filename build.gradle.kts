val exposed_version = property("exposed_version") as String
val kotlin_version = property("kotlin_version") as String
val logback_version = property("logback_version") as String
val sqlite_version = property("sqlite_version") as String

plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
    id("io.ktor.plugin") version "3.6.1"

    // Static analysis tools (report-only by default)
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
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

kotlin {
    jvmToolchain(26)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xannotation-default-target=param-property",
            "-Xreturn-value-checker=check",
        )
    }
}

application {
    mainClass = "be.endevops.ApplicationKt"
}

ktor {}

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
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-di")
    implementation("io.ktor:ktor-server-html-builder")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.xerial:sqlite-jdbc:$sqlite_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("dnsjava:dnsjava:3.7.1")

    implementation("io.github.pdvrieze.xmlutil:serialization:1.0.2")
    implementation("io.github.pdvrieze.xmlutil:serialization-io:1.0.2")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
