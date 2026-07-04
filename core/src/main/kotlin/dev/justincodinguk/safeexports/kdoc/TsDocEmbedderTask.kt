package dev.justincodinguk.safeexports.kdoc

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class TsDocEmbedderTask : DefaultTask() {

    @get:InputFile
    abstract val kDocManifest: RegularFileProperty

    @get:InputFile
    abstract val generatedDtsFile: RegularFileProperty

    @get:OutputFile
    abstract val finalDtsFile: RegularFileProperty

    @TaskAction
    fun embed() {
        val kDocs = kDocManifest.get().asFile
        val parser = JsonSlurper()

        @Suppress("UNCHECKED_CAST")
        val kDocMap = parser.parse(kDocs) as Map<String, String>

        val generatedDts = generatedDtsFile.get().asFile
        val outputFile = finalDtsFile.get().asFile

        if(!outputFile.exists()) outputFile.createNewFile()

        generatedDts.bufferedReader().use { reader ->
            outputFile.bufferedWriter().use { writer ->
                val line = reader.readLine()
                if(line.startsWith("export declare class")) {
                    val className = line.substring("export declare class".length).substringBefore(" ")
                    if(kDocMap.contains(className)) {
                        writer.write(kDocMap[className]!!)
                    }
                }
                writer.write(line)
            }
        }
    }
}