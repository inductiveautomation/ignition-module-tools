package io.ia.sdk.gradle.modl.task

import io.ia.sdk.gradle.modl.PLUGIN_TASK_GROUP
import io.ia.sdk.gradle.modl.model.ArtifactManifest
import io.ia.sdk.gradle.modl.model.AssemblyManifest
import io.ia.sdk.gradle.modl.model.ChecksumResult
import io.ia.sdk.gradle.modl.model.artifactManifestFromJson
import io.ia.sdk.gradle.modl.model.jsonToChecksumResult
import io.ia.sdk.gradle.modl.model.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

open class ModuleBuildReport @Inject constructor(
    objects: ObjectFactory,
    layout: ProjectLayout,
    providers: ProviderFactory
) : DefaultTask() {

    companion object {
        const val ID = "modlReport"
    }

    /**
     * The name of the json file that contains build information about the module that was assembled.
     */
    @get:Input
    val reportFileName: Property<String> = objects.property(String::class.java).convention("buildResult.json")

    /**
     * The report file that will be created by the task.
     */
    @get:OutputFile
    val report: RegularFileProperty = objects.fileProperty().convention(
        layout.buildDirectory.file(reportFileName)
    )

    /**
     * Map of arbitrary information, derived directly from the Map provided to the
     * [io.ia.sdk.gradle.modl.extension.ModuleSettings] extension object.  These values are written to the json,
     * for convenient use by consumers of the reports, such as build automation/CI or reporting purposes.
     */
    @get:Optional
    @get:Input
    val metaInfo: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)

    /**
     * Json file containing the checksum information.
     */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    val checksumJson: RegularFileProperty = objects.fileProperty()

    /**
     * The signed Ignition Module (modl file)
     */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    val modlFile: RegularFileProperty = objects.fileProperty()

    /**
     * Module ID from the [io.ia.sdk.gradle.modl.extension.ModuleSettings]
     */
    @get:Input
    val moduleId: Property<String> = objects.property(String::class.java)

    /**
     * Module name from [io.ia.sdk.gradle.modl.extension.ModuleSettings.name]
     */
    @get:Input
    val moduleName: Property<String> = objects.property(String::class.java)

    /**
     * Description, from  [io.ia.sdk.gradle.modl.extension.ModuleSettings.moduleDescription]
     */
    @get:Input
    val moduleDescription: Property<String> = objects.property(String::class.java)

    /**
     * Version of the module, based on the [io.ia.sdk.gradle.modl.extension.ModuleSettings.moduleVersion]
     */
    @get:Input
    val moduleVersion: Property<String> = objects.property(String::class.java)

    /**
     * Map of Gradle Project Path to the artifact manifest file for the subproject at that path.  Internal,
     * as this is used to derive the actual inputs.
     */
    @get:Internal
    val childManifests: MapProperty<String, RegularFile> = objects.mapProperty(
        String::class.java,
        RegularFile::class.java
    )

    /**
     * The artifact manifest files, pulled from the values of the child manifests above.  Not directly used, but
     * included as input files to support
     */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    val artifactManifests: Provider<Set<RegularFile>> = childManifests.map { it.values.toSet() }

    @get:Input
    val childManifestJson: Provider<Map<String, ArtifactManifest>> = childManifests.map { children ->
        children.mapValues { artifactManifestFromJson(it.value.asFile.readText()) }
    }

    init {
        group = PLUGIN_TASK_GROUP
        description = "Generates a json file in the root build directory containing module and build metadata."
    }

    @TaskAction
    fun execute() {
        // build manifest
        val checksum = if (checksumJson.isPresent && checksumJson.get().asFile.exists()) {
            checksumJson.get().asFile
        } else throw Exception("Checksum file did not exist at ${checksumJson.get().asFile}")

        val checksumResult: ChecksumResult = jsonToChecksumResult(checksum.readText())

        val fileSize = modlFile.get().asFile.length()

        val manifest = AssemblyManifest(
            moduleId.get(),
            moduleName.get(),
            moduleDescription.get(),
            moduleVersion.get(),
            checksumResult,
            modlFile.get().asFile.name,
            fileSize,
            childManifestJson.get(),
            metaInfo.get()
        )

        if (report.get().asFile.exists()) {
            report.get().asFile.delete()
        }

        report.get().asFile.writeText(manifest.toJson())
    }
}
