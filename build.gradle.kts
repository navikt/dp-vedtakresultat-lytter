import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Kotlin.version
    application
    id(Spotless.spotless) version Spotless.version
    id(Shadow.shadow) version Shadow.version
}

repositories {
    jcenter()
    maven("http://packages.confluent.io/maven/")
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
    implementation(Ktor.auth)
    implementation(Ktor.micrometerMetrics)

    // Http Klient
    implementation(Fuel.fuel)
    implementation(Fuel.library("coroutines"))
    implementation(Fuel.fuelMoshi)
    implementation(Dagpenger.Biblioteker.ktorUtils)

    // Json (de)serialisering
    implementation(Moshi.moshi)
    implementation(Moshi.moshiKotlin)
    implementation(Moshi.moshiAdapters)

    // Unik id
    implementation(Ulid.ulid)

    // Miljøkonfigurasjon
    implementation(Konfig.konfig)

    // Logging
    implementation(Kotlin.Logging.kotlinLogging)
    implementation(Log4j2.api)
    implementation(Log4j2.core)
    implementation(Log4j2.slf4j)
    implementation(Log4j2.Logstash.logstashLayout)

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
    testImplementation(Assertj.core)
    testImplementation(Ktor.library("client"))
    testImplementation(Ktor.ktorTest)
    testImplementation(KafkaEmbedded.env)
    testImplementation(Mockk.mockk)
    testImplementation(Wiremock.standalone)
}

configurations {
    this.all {
        exclude(group = "ch.qos.logback")
    }
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
