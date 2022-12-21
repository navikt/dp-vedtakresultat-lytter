import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Kotlin.version
    application
    id(Spotless.spotless) version Spotless.version
    id(Shadow.shadow) version Shadow.version
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://jitpack.io")
}

configurations {
    "implementation" {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "ch.qos.logback", module = "logback-classic")
    }
    "testImplementation" {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "ch.qos.logback", module = "logback-classic")
    }
}
dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    // Http Server
    implementation(Ktor.serverNetty)
    implementation(Ktor.micrometerMetrics)

    // Json (de)serialisering
    implementation(Jackson.kotlin)
    implementation(Jackson.jsr310)

    // Unik id
    implementation(Ulid.ulid)

    // Miljøkonfigurasjon
    implementation(Konfig.konfig)

    // Logging
    implementation(Kotlin.Logging.kotlinLogging)
    implementation(Log4j2.api)
    implementation(Log4j2.core)
    implementation(Log4j2.slf4j)
    implementation(Log4j2.library("layout-template-json"))

    // Kafka
    implementation(Kafka.clients)

    // Schema handling
    implementation(Avro.avro)
    implementation(Kafka.Confluent.avroStreamSerdes)

    // Metrics
    implementation(Prometheus.hotspot)
    implementation(Prometheus.common)
    implementation(Prometheus.log4j2)
    implementation(Micrometer.prometheusRegistry)

    // Test related dependencies
    testImplementation(kotlin("test-junit5"))
    testImplementation(Junit5.engine)
    testImplementation(KoTest.runner)
    testImplementation(Mockk.mockk)
}

configurations {
    this.all {
        exclude(group = "ch.qos.logback")
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
        ktlint(Ktlint.version)
    }
    kotlinGradle {
        target("*.gradle.kts", "buildSrc/**/*.kt*")
        ktlint(Ktlint.version)
    }
}

tasks.named("shadowJar") {
    dependsOn("test")
}

tasks.named("compileKotlin") {
    dependsOn("spotlessCheck")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer::class.java)
}
