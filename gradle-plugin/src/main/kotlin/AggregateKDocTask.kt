import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class AggregateKDocTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val kDocFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val kDocAggregatedOutput: RegularFileProperty

    @TaskAction
    fun aggregate() {
        val slurper = JsonSlurper()

        val root = mutableMapOf<String, String>()
        kDocFiles.files.forEachIndexed { index, file ->
            val map2 = slurper.parse(file) as Map<String, String>
            root.putAll(map2)
        }

        val outputFile = kDocAggregatedOutput.get().asFile
        if (outputFile.parentFile != null && !outputFile.parentFile.exists()) outputFile.parentFile.mkdirs()
        outputFile.createNewFile()
        outputFile.writeText(JsonBuilder(root).toPrettyString())
    }
}