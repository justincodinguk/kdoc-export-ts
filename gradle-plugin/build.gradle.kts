import java.util.Properties
import kotlin.apply

plugins {
    `kotlin-dsl`
    id("com.vanniktech.maven.publish")
}

group = "io.github.justincodinguk.safeexports.kdoc-export-ts"
version = "1.0.0"

val sonatypeProperties = Properties().apply {
    val file = rootProject.file("central.sonatype.properties")
    if(file.exists()) {
        file.inputStream().use { load(it) }
    }
}

sonatypeProperties.forEach { (key, value) ->
    project.extensions.extraProperties[key as String] = value
}

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
            id = "io.github.justincodinguk.safeexports.kdoc-export-ts"
            implementationClass = "TsDocExportPlugin"
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set("SafeExports Kotlin/JS KDoc Embedder")
        description.set("A Gradle plugin to embed KDocs into generated TypeScript definitions")
        url.set("https://github.com/justincodinguk/kdoc-export-ts")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("justincodinguk")
                name.set("Justin")
                email.set("coderstudent09@gmail.com")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/justincodinguk/kdoc-export-ts.git")
            developerConnection.set("scm:git:ssh://github.com/justincodinguk/kdoc-export-ts.git")
            url.set("https://github.com/justincodinguk/kdoc-export-ts.git")
        }
    }
}