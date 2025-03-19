import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("common")
    application
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    // Http Server
    implementation(Ktor2.Server.library("cio"))
    implementation(Ktor2.Server.library("default-headers"))
    implementation(Ktor2.Server.library("metrics-micrometer"))

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

application {
    mainClass.set("no.nav.dp.vedtakslytter.VedtakslytterKt")
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
