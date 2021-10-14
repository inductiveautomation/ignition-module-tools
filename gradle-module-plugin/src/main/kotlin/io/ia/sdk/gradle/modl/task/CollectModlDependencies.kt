package io.ia.sdk.gradle.modl.task

import io.ia.sdk.gradle.modl.PLUGIN_TASK_GROUP
import io.ia.sdk.gradle.modl.api.Constants.ARTIFACT_DIR
import io.ia.sdk.gradle.modl.api.Constants.MODULE_DEPENDENCY_CONFIGURATION
import io.ia.sdk.gradle.modl.model.Artifact
import io.ia.sdk.gradle.modl.model.ArtifactManifest
import io.ia.sdk.gradle.modl.model.FileArtifact
import io.ia.sdk.gradle.modl.model.IgnitionScope
import io.ia.sdk.gradle.modl.model.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.FileNotFoundException
import javax.inject.Inject

/**
 * Task which collects the artifacts produced by a single project, and moves them to the appropriate scoped folder
 * in the build/module/ directory.
 *
 * This task should be registered to each project associated with the module, if that project applies the java-library
 * gradle plugin (e.g. - if it produces jar artifacts)
 */
open class CollectModlDependencies @Inject constructor(objects: ObjectFactory, layout: ProjectLayout) : DefaultTask() {
    companion object {
        const val ID = "collectModlDependencies"
        const val JSON_FILENAME = "artifacts.json"
    }

    init {
        group = PLUGIN_TASK_GROUP
        description = """
    |Collects modlDependency artifacts (jars)  into the ./build/artifacts of the local gradle project (including the 
    |  jar produce by the subproject). I addition, writes a json file containing meta-information about the 
    |  dependencies.  This file is used by the 'root' project when assembling the final module structure""".trimMargin()
    }

    @get:Input
    val projectScopes: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)

    @get:Input
    val ignitionScope: IgnitionScope by lazy {
        val projectPath = project.path
        val match = projectScopes.get().filter { it.key == projectPath }

        project.logger.info("${project.path}:$name - Evaluating scope $match")

        when (match.size) {
            0 -> IgnitionScope.NONE
            else -> IgnitionScope.forShorthand(match.getValue(projectPath))
        }
    }

    @get:Input
    val moduleVersion: Property<String> = objects.property(String::class.java)

    @InputFiles
    fun getModlDependencies(): Configuration {
        return project.configurations.getByName(MODULE_DEPENDENCY_CONFIGURATION)
    }

    @get:OutputDirectory
    val artifactOutputDir: DirectoryProperty = objects.directoryProperty().convention(
        layout.buildDirectory.dir(ARTIFACT_DIR)
    )

    @get:OutputFile
    val manifestFile: Provider<RegularFile> =
        layout.buildDirectory.file("$ARTIFACT_DIR/$JSON_FILENAME")

    @TaskAction
    fun execute() {
        val fileArtifacts = deriveArtifacts()
        copyArtifacts(fileArtifacts)

        // named artifacts are used in the xml, and we desire a versioned jar for the main output
        val namedArtifacts = fileArtifacts.map {
            Artifact(it)
        }
        val manifest = ArtifactManifest(project.path, project.name, ignitionScope.code, namedArtifacts)
        val manifestContent = manifest.toJson()
        val manifestFile = manifestFile.get().asFile
        manifestFile.writeText(manifestContent, Charsets.UTF_8)
    }

    private fun buildArtifactsFromArtifactView(config: Configuration): List<FileArtifact> {
        return config.incoming.artifactView {}
            .artifacts
            .artifacts
            .filterIsInstance<ResolvedArtifactResult>()
            .map {
                val file = it.file
                val id = it.id
                FileArtifact(id.displayName, file)
            }.apply {
                logger.info("Resolved the following artifacts as dependencies of ${project.path} '${config.name}':")
                this.forEach { logger.info("    ${it.id} - ${it.jarFile}") }
            }
    }

    /**
     * Builds a list of FileArtifacts, including the main output of the jar task for this project.  The files
     * in this list are those being staged for inclusion in the module.
     */
    private fun deriveArtifacts(): List<FileArtifact> {
        val mainJar = project.tasks.getByName("jar").outputs.files.toList().first()
        if (!mainJar.exists()) throw FileNotFoundException("Could not identify jar task output file $mainJar")

        val mainArtifact = FileArtifact("${project.group}:${project.name}:${moduleVersion.get()}", mainJar)
        val modlDeps = buildArtifactsFromArtifactView(getModlDependencies())

        return modlDeps + mainArtifact
    }

    private fun copyArtifacts(artifacts: List<FileArtifact>) {
        project.logger.info("Copying ${project.path} artifacts to ${artifactOutputDir.get()}")
        project.copy { copySpec ->
            copySpec.from(artifacts.map { it.jarFile })
            copySpec.into(artifactOutputDir.get())
        }
    }
}
