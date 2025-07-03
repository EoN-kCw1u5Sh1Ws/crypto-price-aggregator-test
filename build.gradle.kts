import org.gradle.kotlin.dsl.testImplementation

plugins {
    kotlin("jvm") version "2.2.0"
    id("org.graalvm.buildtools.native") version "0.10.3"
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

graalvmNative {
    toolchainDetection.set(true)
    binaries {
        named("main") {
            imageName.set("crypto-price-aggregator")
            mainClass.set("org.kurt.MainKt")
            buildArgs.addAll(
                "--report-unsupported-elements-at-runtime",
                "--no-fallback",
                "--report-unsupported-elements-at-runtime",
                // Initializes runtime classes during compilation rather than at application startup.
                // Improves startup time and reduces runtime overhead, prevents configuration issues at runtime.
                "--initialize-at-build-time=kotlin",
                "--initialize-at-build-time=kotlinx.coroutines",
                "--initialize-at-build-time=ch.qos.logback",
                "-H:+AddAllCharsets",
                "--enable-url-protocols=http,https"
            )
        }
    }
}