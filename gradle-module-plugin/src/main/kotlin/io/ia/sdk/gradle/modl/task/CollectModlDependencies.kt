package io.ia.sdk.gradle.modl.task

import io.ia.sdk.gradle.modl.PLUGIN_TASK_GROUP
import io.ia.sdk.gradle.modl.api.Constants.ARTIFACT_DIR
import io.ia.sdk.gradle.modl.model.Artifact
import io.ia.sdk.gradle.modl.model.ArtifactManifest
import io.ia.sdk.gradle.modl.model.IgnitionScope
import io.ia.sdk.gradle.modl.model.manifestToJson
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DirectoryProperty
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
import javax.inject.Inject

/**
 * Task which collects the artifacts produced by a single project, and moves them to the appropriate scoped folder
 * in the build/module/ directory.
 *
 * This task should be registered to each project associated with the module, if that project applies the java gradle
 * plugin (e.g. - if it produces jar artifacts)
 */
open class CollectModlDependencies @Inject constructor(objects: ObjectFactory) : DefaultTask() {
    companion object {
        const val ID = "collectModlDependencies"
    }

    init {
        group = PLUGIN_TASK_GROUP
        description = """
    |Collects modlApi and modlImplementation artifact (jar) dependencies into the ./build/artifacts of the local
    |  gradle project (including the jar produce by the subproject). I addition, writes a json file containing 
    |  meta-information about the dependencies.  This file is used by the 'root' project when assembling the final
    |  module structure""".trimMargin()
    }

    @Input
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

    @Input
    val moduleVersion: Property<String> = objects.property(String::class.java)

    @InputFiles
    fun getModlApiDeps(): Configuration {
        return project.configurations.getByName("modlApi")
    }

    @InputFiles
    fun getModlImplementationDeps(): Configuration {
        return project.configurations.getByName("modlImplementation")
    }

    @OutputDirectory
    val artifactOutputDir: DirectoryProperty = objects.directoryProperty()

    init {
        artifactOutputDir.set(project.file("${project.buildDir}/$ARTIFACT_DIR"))
    }

    @get:OutputFile
    val manifestFile: Provider<RegularFile> =
        project.layout.buildDirectory.file("$ARTIFACT_DIR/manifest.json")

    @get:Input
    val versionedJarName: String by lazy {
        "${project.name}-${moduleVersion.get()}.jar"
    }

    private fun buildManifest(): ArtifactManifest {
        val apiDeps = getModlApiDeps()
        val implDeps = getModlImplementationDeps()
        val deps = apiDeps.dependencies + implDeps.dependencies
        val mainArtifact = Artifact(project.group.toString(), project.name, moduleVersion.get(), versionedJarName)
        val artifacts = deps.map {
            Artifact(it.group.toString(), it.name, it.version.toString(), "${it.name}-${it.version}.jar")
        } + mainArtifact

        return ArtifactManifest(project.path, project.name, ignitionScope.code, artifacts)
    }

    private fun copyArtifacts() {
        project.logger.info("Copying ${project.path} artifacts to ${artifactOutputDir.get()}")
        project.copy { copySpec ->
            copySpec.from(project.tasks.getByName("jar").outputs.files.toList())
            copySpec.from(getModlApiDeps())
            copySpec.from(getModlImplementationDeps())
            copySpec.into(artifactOutputDir.get())
            copySpec.rename("${project.name}\\.jar", versionedJarName)
        }
    }

    @TaskAction
    fun execute() {
        val manifestContent = manifestToJson(buildManifest())
        val manifestFile = manifestFile.get().asFile
        manifestFile.writeText(manifestContent, Charsets.UTF_8)
        copyArtifacts()
    }
}
