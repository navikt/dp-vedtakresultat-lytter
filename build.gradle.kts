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
    implementation("com.github.navikt.tbd-libs:naisful-app:2025.03.30-14.11-a91ce546")

    // Json (de)serialisering
    implementation(libs.bundles.jackson)

    // Unik id
    implementation("de.huxhorn.sulky:de.huxhorn.sulky.ulid:8.3.0")

    // Miljøkonfigurasjon
    implementation(libs.konfig)

    // Logging
    val log4j2Version = "2.24.3"
    implementation(libs.kotlin.logging)
    implementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-layout-template-json:$log4j2Version")

    // Kafka
    implementation("org.apache.kafka:kafka-clients:3.7.1")

    // Schema handling
    implementation("org.apache.avro:avro:1.11.4")
    implementation("io.confluent:kafka-streams-avro-serde:7.3.0")

    // Metrics
    val version = "0.16.0"
    implementation("io.prometheus:simpleclient_common:$version")
    implementation("io.prometheus:simpleclient_hotspot:$version")
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.5")

    // Test related dependencies
    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertions.core)
}

application {
    mainClass.set("no.nav.dp.vedtakslytter.VedtakslytterKt")
}
