plugins {
    id("common")
    application
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

dependencies {

    // Naisful app
    implementation("com.github.navikt.tbd-libs:naisful-app:2025.10.31-14.20-3733c982")

    // Json (de)serialisering
    implementation(libs.bundles.jackson)

    // Unik id
    implementation("de.huxhorn.sulky:de.huxhorn.sulky.ulid:8.3.0")

    // Miljøkonfigurasjon
    implementation(libs.konfig)

    // Logging
    implementation(libs.kotlin.logging)
    implementation("ch.qos.logback:logback-classic:1.5.24")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")

    // Kafka
    implementation("org.apache.kafka:kafka-clients:7.9.1-ce")

    // Schema handling
    implementation("org.apache.avro:avro:1.12.1")
    implementation("org.apache.avro:avro:1.12.1")
    implementation("io.confluent:kafka-streams-avro-serde:7.9.1")

    // Metrics
    implementation("io.prometheus:prometheus-metrics-core:1.4.3")

    // Test related dependencies
    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertions.core)
}

application {
    mainClass.set("no.nav.dp.vedtakslytter.VedtakslytterKt")
}
