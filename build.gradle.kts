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
    implementation("com.github.navikt.tbd-libs:naisful-app:2025.06.20-13.05-40af2647")

    // Json (de)serialisering
    implementation(libs.bundles.jackson)

    // Unik id
    implementation("de.huxhorn.sulky:de.huxhorn.sulky.ulid:8.3.0")

    // Miljøkonfigurasjon
    implementation(libs.konfig)

    // Logging
    val log4j2Version = "2.25.0"
    implementation(libs.kotlin.logging)
    implementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-layout-template-json:$log4j2Version")

    // Kafka
    implementation("org.apache.kafka:kafka-clients:7.9.1-ce")

    // Schema handling
    implementation("org.apache.avro:avro:1.12.0")
    implementation("org.apache.avro:avro:1.12.0")
    implementation("io.confluent:kafka-streams-avro-serde:8.0.0")

    // Metrics
    implementation("io.prometheus:prometheus-metrics-core:1.3.10")

    // Test related dependencies
    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertions.core)
}

application {
    mainClass.set("no.nav.dp.vedtakslytter.VedtakslytterKt")
}
