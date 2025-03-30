val ktor_version: String by project
val kotlin_version: String by project
val commons_codec_version: String by project
val logback_version: String by project
val koin_version: String by project

plugins {
    kotlin("jvm") version "1.9.23"
    id("io.ktor.plugin") version "2.3.9"
    kotlin("plugin.serialization") version "1.9.23"
}

group = "com.thiagotoazza"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-gson-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")

    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("commons-codec:commons-codec:$commons_codec_version")

    // KOIN (DI)
    implementation("io.insert-koin:koin-ktor:$koin_version")
    implementation("io.insert-koin:koin-logger-slf4j:$koin_version")

    // KOTLIN SERIALIZATION
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // MONGO DATABASE
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.0.0")
    implementation("org.mongodb:bson-kotlinx:5.0.0")

    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
