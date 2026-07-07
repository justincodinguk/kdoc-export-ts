import com.vanniktech.maven.publish.portal.SonatypeCentralPortal
import java.util.Properties

plugins {
    kotlin("jvm")
    `maven-publish`
    id("com.vanniktech.maven.publish")
}

group = "io.github.justincodinguk.safeexports"
val projectArtifactId = "kdoc-export-ts-processor"
version = "1.0.0"

val sonatypeProperties = Properties().apply {
    val file = rootProject.file("central.sonatype.properties")
    if(file.exists()) {
        file.inputStream().use { load(it) }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

sonatypeProperties.forEach { (key, value) ->
    project.extensions.extraProperties[key as String] = value
}

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

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(group.toString(), projectArtifactId, version.toString())
    pom {
        name.set("SafeExports Kotlin/JS KDoc Extractor")
        description.set("KSP Symbol Processor for extracting KDocs")
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
