package io.ia.sdk.gradle.modl.task

import io.ia.sdk.gradle.modl.PLUGIN_TASK_GROUP
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Creates the unsigned .modl file by compressing the contents of the staging folder into a zip file.
 */
open class ZipModule @Inject constructor(objects: ObjectFactory) : DefaultTask() {
    companion object {
        const val ID = "zipModule"
        const val UNSIGNED_EXTENSION = "unsigned.modl"
    }

    @Optional
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    val content: DirectoryProperty = objects.directoryProperty()

    @OutputFile
    val unsignedModule: RegularFileProperty = objects.fileProperty()

    @Input
    val moduleName: Property<String> = objects.property(String::class.java)

    init {
        this.group = PLUGIN_TASK_GROUP
        this.description =
                "Packs the contents of the module folder into a zip archive with a .unsigned.modl file extension"
    }

    @TaskAction
    fun execute() {
        val unsignedFile = unsignedModule.get()
        val contentDir = content.get().asFile

        project.logger.info("Zipping '${contentDir.absolutePath}' into ' ${unsignedFile.asFile.absolutePath}'")
        project.ant.invokeMethod("zip",
                mapOf("basedir" to contentDir, "destfile" to unsignedFile))
    }
}
