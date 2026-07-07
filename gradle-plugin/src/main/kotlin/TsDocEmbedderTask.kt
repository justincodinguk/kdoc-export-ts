import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class TsDocEmbedderTask : DefaultTask() {

    @get:InputFile
    abstract val kDocManifest: RegularFileProperty

    @get:InputDirectory
    abstract val jsLibraryDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val finalDtsOutputDirectory: DirectoryProperty

    @TaskAction
    fun embed() {
        val kDocs = kDocManifest.get().asFile
        val parser = JsonSlurper()

        val searchDir = jsLibraryDirectory.get().asFile
        val outputDir = finalDtsOutputDirectory.get().asFile

        val candidateFile = searchDir.walkTopDown()
            .firstOrNull { it.isFile && (it.name.endsWith(".d.ts") || it.name.endsWith(".d.mts")) }

        if (candidateFile == null) {
            logger.error("[TsDocExportPlugin] Could not find any .d.ts or .d.mts file in ${searchDir.absolutePath}")
            return
        }

        val targetBackupFile = File(outputDir, "_${candidateFile.name}")
        val targetFinalFile = File(outputDir, candidateFile.name)

        outputDir.mkdirs()
        candidateFile.copyTo(targetBackupFile, overwrite = true)

        @Suppress("UNCHECKED_CAST")
        val kDocMap = parser.parse(kDocs) as Map<String, String>

        if(targetFinalFile.parentFile!=null && !targetFinalFile.parentFile.exists()) targetFinalFile.parentFile.mkdirs()
        targetFinalFile.createNewFile()
        targetBackupFile.bufferedReader().use { reader ->
            targetFinalFile.bufferedWriter().use { writer ->
                while (true) {
                    val line = reader.readLine() ?: return
                    if(line.startsWith("export declare class ")) {
                        val className = line.substring("export declare class ".length).substringBefore(" ")
                        if(kDocMap.contains(className)) {
                            writer.write(kDocMap[className]!!)
                        }
                        writer.write(line+"\n")
                        var funcLine = reader.readLine() ?: return
                        while (funcLine != "}") {
                            val funcName = funcLine.trim().substringBefore("(")
                            if(kDocMap.contains("$className.$funcName")) {
                                writer.write(kDocMap["$className.$funcName"]!!)
                            }
                            writer.write(funcLine+"\n")
                            funcLine = reader.readLine() ?: return
                        }
                        writer.write(funcLine+"\n")
                    } else writer.write(line+"\n")
                }
            }
        }
    }
}