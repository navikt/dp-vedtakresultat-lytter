import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version("1.3.40")
    application
    id("com.diffplug.gradle.spotless") version "3.23.0"
    id("com.github.johnrengelman.shadow") version "4.0.3"
}

repositories {
    jcenter()
    maven("http://packages.confluent.io/maven")
}

val assertjVersion = "3.11.1"
val avroVersion = "1.9.0"
val confluentVersion = "5.2.1"
val junitVersion = "5.4.1"
val kafkaVersion = "2.2.1"
val konfigVersion = "1.6.10.0"
val kotlinLoggingVersion = "1.6.22"
val ktorVersion = "1.2.0"
val log4j2Version = "2.11.1"
val prometheusVersion = "0.6.0"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    // Http Server
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-metrics-micrometer:$ktorVersion")

    // Miljøkonfigurasjon
    implementation("com.natpryce:konfig:$konfigVersion")

    // Logging
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    implementation("com.vlkan.log4j2:log4j2-logstash-layout-fatjar:0.15")

    // Kafka
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")

    // Schema handling
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")

    // Metrics
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:1.1.5")

    // Test related dependencies
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("io.ktor:ktor-client:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}

dependencyLocking {
    lockAllConfigurations()
}

tasks.register("resolveAndLockDependencies") {
    doFirst {
        require(gradle.startParameter.isWriteDependencyLocks)
    }
    doLast {
        configurations.filter {
            it.isCanBeResolved
        }.forEach { it.resolve() }
    }
}

application {
    applicationName = "dp-vedtakresultat-lytter"
    mainClassName = "no.nav.dp.vedtakslytter.VedtakslytterKt"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        events("passed", "skipped", "failed")
    }
}

tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget = "1.8" }

spotless {
    kotlin {
        ktlint("0.33.0")
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("0.33.0")
    }
}
