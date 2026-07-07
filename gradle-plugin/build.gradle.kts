plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "dev.justincodinguk.safeexports"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.0")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("kdoc-export-ts") {
            id = "dev.justincodinguk.safeexports.kdoc-export-ts"
            implementationClass = "TsDocExportPlugin"
        }
    }
}