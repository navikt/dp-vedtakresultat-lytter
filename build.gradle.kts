plugins {
    kotlin("jvm") version("1.3.31")
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
val kafkaVersion = "2.2.1"
val ktorVersion = "1.2.0"
val prometheusVersion = "0.6.0"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    //Http Server
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")

    //Kafka
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")

    // Schema handling
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")

    //Metrics
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")


    // Test related dependencies
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.assertj:assertj-core:$assertjVersion")
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
        events("passed", "skipped", "failed")
    }
}

