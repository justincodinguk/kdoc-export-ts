plugins {
    kotlin("jvm")
    `kotlin-dsl`
}

group = "dev.justincodinguk.safeexports"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.0")
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
            implementationClass = "dev.justincodinguk.safeexports.kdoc.TsDocExportPlugin"
        }
    }
}