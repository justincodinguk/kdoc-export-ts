plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "dev.justincodinguk.safeexports"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}