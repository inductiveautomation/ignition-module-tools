package io.ia.sdk.gradle.modl.task

import io.ia.sdk.gradle.modl.PLUGIN_TASK_GROUP
import io.ia.sdk.gradle.modl.model.ArtifactManifest
import io.ia.sdk.gradle.modl.model.artifactManifestFromJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.xml
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject

open class WriteModuleXml @Inject constructor(_objects: ObjectFactory) : DefaultTask() {

    companion object {
        const val ID = "writeModuleXml"
    }

    init {
        this.group = PLUGIN_TASK_GROUP
        this.description = "Writes the module.xml based on values derived from plugin configuration"
    }

    @get:Input
    val freeModule: Property<Boolean> = _objects.property(Boolean::class.javaObjectType)

    @get:Input
    val moduleDescription: Property<String> = _objects.property(String::class.java)

    @get:Input
    val moduleId: Property<String> = _objects.property(String::class.java)

    @get:Input
    val moduleName: Property<String> = _objects.property(String::class.java)

    @get:Input
    val moduleVersion: Property<String> = _objects.property(String::class.java)

    @get:Input
    val requiredIgnitionVersion: Property<String> = _objects.property(String::class.java)

    /**
     * Hook classes, as provided by the module settings extension object.  class Reference : scope
     */
    @get:Input
    val hookClasses: MapProperty<String, String> = _objects.mapProperty(String::class.java, String::class.java)

    /**
     * Manifests files generated by the [CollectModlDependencies.manifestFile] output file property
     */
    @get:InputFiles
    val artifactManifests: SetProperty<RegularFile> = _objects.setProperty(RegularFile::class.java)

    @get:Input
    val requireFromPlatform: MapProperty<String, String> =
        _objects.mapProperty(String::class.java, String::class.java)

    @get:Input
    val requiredFrameworkVersion: Property<String> = _objects.property(String::class.java)

    /**
     * Path to license file, optionally provided by user.
     */
    @get:Optional
    @get:Input
    val license: Property<String> = _objects.property(String::class.java)

    /**
     * Map of <moduleId : scope>
     */
    @get:Input
    val moduleDependencies: MapProperty<String, String> =
        _objects.mapProperty(String::class.java, String::class.java)

    @get:Input
    @get:Optional
    val docIndexPath: Property<String> = _objects.property(String::class.java)

    @OutputFile
    fun getModuleXmlFile(): File {
        return project.file("${project.buildDir}/moduleContent/module.xml")
    }

    init {
        this.description = "Writes the module.xml file for the module, using the settings applied to the " +
            "'ignitionModule' block of the build script."
        this.group = "Ignition Module"
    }

    @TaskAction
    fun execute() {

        val xml = buildXml()
        val fileToWrite = getModuleXmlFile()
        project.logger.debug("Beginning to write to '${fileToWrite.absolutePath}' with content:\n$xml")

        writeXml(fileToWrite, xml)
    }

    private fun buildXml(): String {
        val modules = xml("modules", "UTF-8") {
            "module" {
                "name" { -moduleName.get() }

                "id" { -moduleId.get() }

                "version" { -moduleVersion.get() }

                "description" { -moduleDescription.get() }

                if (license.isPresent && license.get().isNotEmpty()) {
                    val licenseFile: File = project.file(license.get())

                    if (licenseFile.exists()) {
                        "license" {
                            -licenseFile.name
                        }
                    } else {
                        throw FileNotFoundException("Could not find license file ${licenseFile.absolutePath}")
                    }
                }

                if (requiredIgnitionVersion.isPresent) {
                    "requiredIgnitionVersion" {
                        -requiredIgnitionVersion.get()
                    }
                }

                "freeModule" {
                    -freeModule.get().toString()
                }

                if (docIndexPath.isPresent) {
                    "documentation" { -docIndexPath.get() }
                }

                hookClasses.get().forEach { classReference, scope ->
                    "hook" {
                        attribute("scope", scope)
                        -classReference
                    }
                }

                moduleDependencies.get().forEach { moduleId, scope ->
                    "depends" {
                        attribute("scope", scope)
                        -moduleId
                    }
                }

                requireFromPlatform.get().forEach { libraryName, scope ->
                    "require" {
                        attribute("scope", scope)
                        -libraryName
                    }
                }

                if (requiredFrameworkVersion.isPresent && requiredIgnitionVersion.get().isNotEmpty()) {
                    "requiredFrameworkVersion" {
                        -requiredFrameworkVersion.get()
                    }
                }

                manifests().groupBy { it.scope }
                    .forEach { (scope, manifests) ->
                        manifests.flatMap { it.artifacts }
                            .distinctBy { it.jarName }
                            .forEach { artifact ->
                                "jar" {
                                    attribute("scope", scope)
                                    -artifact.jarName
                                }
                            }
                    }
            }
        }

        return modules.toString(PrintOptions(pretty = true, singleLineTextElements = true, useSelfClosingTags = false))
    }

    private fun manifests(): List<ArtifactManifest> {

        return artifactManifests.get().map { manifest ->
            artifactManifestFromJson(manifest.asFile.readText(Charsets.UTF_8))
        }
    }

    fun writeXml(outputFile: File, moduleXml: String) {
        outputFile.writeText(moduleXml)
    }
}
