import org.gradle.kotlin.dsl.testImplementation

plugins {
    kotlin("jvm") version "2.2.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.kurt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktorVersion = "3.2.0"
val coroutinesVersion = "1.10.2"

dependencies {
    // ktor
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    // kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.0")
    // json
    implementation("com.beust:klaxon:5.5")
    // logging
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    // test
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.17")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
    testImplementation("io.ktor:ktor-server-test-host:${ktorVersion}")
}

tasks.test {
    useJUnitPlatform()
}

val javaVersion = 21

kotlin {
    jvmToolchain(javaVersion)
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

tasks.shadowJar {
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = "org.kurt.MainKt"
    }
}