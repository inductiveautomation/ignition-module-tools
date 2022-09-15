@file:Suppress("UnstableApiUsage")

package io.ia.sdk.gradle.modl.task

import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import com.google.common.io.Files
import io.ia.sdk.gradle.modl.PLUGIN_TASK_GROUP
import io.ia.sdk.gradle.modl.model.ChecksumResult
import io.ia.sdk.gradle.modl.model.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.FileNotFoundException
import java.io.Serializable
import javax.inject.Inject

open class Checksum @Inject constructor(_objects: ObjectFactory, _layout: ProjectLayout) : DefaultTask() {
    companion object {
        const val ID = "checksumModl"
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val modlFile: RegularFileProperty = _objects.fileProperty()

    @get:OutputFile
    val checksumJson: RegularFileProperty = _objects.fileProperty().convention(
        _layout.buildDirectory.file("checksum/checksum.json")
    )

    /**
     * Hash function to use against the module file to get the file's checksum.
     */
    @get:Input
    val hashAlgorithm: Property<HashAlgorithm> = _objects.property(HashAlgorithm::class.java)

    @TaskAction
    fun execute() {
        val module = modlFile.get().asFile

        if (module.exists()) {
            val digest = Files.asByteSource(module).hash(hashImpl(hashAlgorithm.get()))
            val checksumInfo = ChecksumResult(digest.toString(), hashAlgorithm.get().name)
            checksumJson.get().asFile.writeText(checksumInfo.toJson())
        } else {
            throw FileNotFoundException("Could not generate checksum for '$module'")
        }
    }

    init {
        this.group = PLUGIN_TASK_GROUP
        this.description = """
            |Executes a hash function (default SHA256) against the modl file, and emits a json file to the build
            | directory containing the digest.
        """.trimMargin()
    }
}

/**
 * Simple mapping of our internal enum to the implementation, to avoid configuration-breaking changes down the road
 * should we need to break away from Guava or want to add alternatives.
 */
private fun hashImpl(config: HashAlgorithm): HashFunction {
    return when (config) {
        HashAlgorithm.SHA256 -> Hashing.sha256()
        HashAlgorithm.SHA384 -> Hashing.sha384()
        HashAlgorithm.SHA512 -> Hashing.sha512()
    }
}

enum class HashAlgorithm : Serializable {
    SHA256, SHA384, SHA512
}
