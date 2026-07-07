import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.io.File

class TsDocExportPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        if(!target.plugins.hasPlugin("com.google.devtools.ksp")) {
            throw GradleException("KSP is not configured. Please install KSP.")
        }
        val extension = target.extensions.create("kDocExport", AggregateExtension::class.java)

        val kdocElements = target.configurations.create("kdocElements") {
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes {
                attribute(Consts.kdocElementAttribute, Consts.KDOC_JSON_ELEMENT)
            }
        }

        val kspOutputFile = target.layout.buildDirectory.file("generated/ksp/js/jsMain/resources/extracted-kdocs.json")
        target.artifacts.add(kdocElements.name, kspOutputFile) {
            builtBy(this)
        }

        val kdocClasspath = target.configurations.create("kdocClasspath") {
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes {
                attribute(Consts.kdocElementAttribute, Consts.KDOC_JSON_ELEMENT)
            }
        }

        val sourceConfigurations = listOf("commonMainImplementation", "commonMainApi")
        sourceConfigurations.forEach { sourceName ->
            target.configurations.matching { it.name == sourceName }.configureEach {
                dependencies.configureEach {
                    if (this is ProjectDependency) {
                        target.dependencies.add(kdocClasspath.name, this)
                    }
                }
            }
        }
        val aggregationTask = registerAggregationTask(
            project = target,
            kdocClasspath = kdocClasspath,
            extension = extension
        )
        registerEmbedderTask(target, aggregationTask)
    }
}

private fun registerAggregationTask(
    project: Project,
    kdocClasspath: Configuration,
    extension: AggregateExtension
): TaskProvider<AggregateKDocTask> {
    return project.tasks.register<AggregateKDocTask>("aggregateKDoc") {
        onlyIf { extension.embedKdoc.get() }
        dependsOn("kspKotlinJs")
        val jsonArtifacts = kdocClasspath.incoming.artifactView {
            lenient(true)
        }.files

        val localJson = project.layout.buildDirectory.file("generated/ksp/js/jsMain/resources/extracted-kdocs.json")

        this.kDocFiles.from(jsonArtifacts, localJson)
        this.kDocAggregatedOutput.set(project.layout.buildDirectory.file("generated/kDocExport/aggregated-kdocs.json"))
    }
}

private fun <T : Task> registerEmbedderTask(
    project: Project,
    dependency: TaskProvider<T>
): TaskProvider<TsDocEmbedderTask> {
    with(project) {
        return tasks.register<TsDocEmbedderTask>("embedTypeScriptDocs") {
            val jsProdLibTask = "jsNodeProductionLibraryDistribution"
            dependsOn(dependency, jsProdLibTask)
            val kspJsonFile = project.layout.buildDirectory.file("generated/kDocExport/aggregated-kdocs.json")
            kDocManifest.set(kspJsonFile)
            val searchDir = project.layout.buildDirectory.dir("dist/js/productionLibrary")
            jsLibraryDirectory.set(searchDir)
            finalDtsOutputDirectory.set(project.layout.buildDirectory.dir("dist/js/productionLibrary"))
        }
    }
}
