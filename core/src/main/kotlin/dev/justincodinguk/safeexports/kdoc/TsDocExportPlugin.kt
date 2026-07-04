package dev.justincodinguk.safeexports.kdoc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class TsDocExportPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            tasks.register<TsDocEmbedderTask>("embedTypeScriptDocs") {
                val kspJsonFile = layout.buildDirectory.file("generated/ksp/js/jsMain/resources/kdoc-manifest.json")
                kDocManifest.set(kspJsonFile)
                val searchDir = layout.buildDirectory.dir("dist/js/productionLibrary").get().asFile
                doFirst {
                    val candidateFile = searchDir.walkTopDown()
                        .firstOrNull { it.isFile && (it.name.endsWith(".d.ts") || it.name.endsWith(".d.mts")) }

                    if (candidateFile != null) {
                        generatedDtsFile.set(candidateFile)
                        val outExtension = candidateFile.extension
                        val destination = layout.buildDirectory.file("dist/js/productionLibrary/${candidateFile.nameWithoutExtension}.d.$outExtension").get().asFile
                        finalDtsFile.set(destination)
                        logger.lifecycle("[TsDocExportPlugin] Found declaration file: ${candidateFile.name}.")
                    } else {
                        logger.warn("[TsDocExportPlugin] Could not find any .d.ts or .d.mts file in $searchDir")
                    }
                }
            }
        }
    }
}