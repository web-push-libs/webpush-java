import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    application
    id("com.github.johnrengelman.shadow") version "6.0.0"

    // Used by release.gradle.kts
    `maven-publish`
    signing
    id("io.codearte.nexus-staging") version "0.21.1"
}

group = "nl.martijndwars"
version = "5.1.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // For CLI
    implementation(group = "com.beust", name = "jcommander", version = "1.78")

    // For making async HTTP requests
    implementation(group = "org.apache.httpcomponents", name = "httpasyncclient", version = "4.1.4")

    // For cryptographic operations
    shadow(group = "org.bouncycastle", name = "bcprov-jdk15on", version = "1.64")

    // For creating and signing JWT
    implementation(group = "org.bitbucket.b_c", name = "jose4j", version = "0.7.0")

    // For parsing JSON
    testImplementation(group = "com.google.code.gson", name = "gson", version = "2.8.6")

    // For making HTTP requests
    testImplementation(group = "org.apache.httpcomponents", name = "fluent-hc", version = "4.5.10")

    // For testing, obviously
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = "5.5.2")

    // For running JUnit tests
    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "5.5.2")

    // For turning InputStream to String
    testImplementation(group = "commons-io", name = "commons-io", version = "2.6")

    // For reading the demo vapid keypair from a pem file
    testImplementation(group = "org.bouncycastle", name = "bcpkix-jdk15on", version = "1.64")

    // For verifying Base64Encoder results in unit tests
    testImplementation(group = "com.google.guava", name = "guava", version = "27.0.1-jre")

    // To run a local webserver in the TestCafe tests
    testImplementation(group = "io.undertow", name = "undertow-servlet", version = "2.1.3.Final")
}

tasks.named<Wrapper>("wrapper") {
    gradleVersion = "6.5.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClassName = "nl.martijndwars.webpush.cli.Cli"
}

tasks.named<JavaExec>("run") {
    classpath += files(configurations.shadow)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging {
        events("PASSED", "FAILED", "SKIPPED")
        showStandardStreams = true
        exceptionFormat = FULL
    }
}

if (hasProperty("release")) {
    apply {
        from("release.gradle.kts")
    }
}
