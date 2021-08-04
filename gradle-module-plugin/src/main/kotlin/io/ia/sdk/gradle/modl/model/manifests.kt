package io.ia.sdk.gradle.modl.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.Serializable

data class FileArtifact(val id: String, val jarFile: File)

data class Artifact(val id: String, val jarName: String) : Serializable {
    constructor(fileArtifact: FileArtifact) : this(
        fileArtifact.id,
        fileArtifact.jarFile.name
    )
}

data class ArtifactManifest(
    val projectPath: String,
    val projectName: String,
    val scope: String,
    val artifacts: List<Artifact>
) : Serializable

val MOSHI: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

fun artifactManifestFromJson(json: String): ArtifactManifest {
    val adapter = MOSHI.adapter(ArtifactManifest::class.java).indent("    ")
    return adapter.fromJson(json) as ArtifactManifest
}

fun ArtifactManifest.toJson(): String {
    val adapter = MOSHI.adapter(ArtifactManifest::class.java).indent("    ")
    return adapter.toJson(this)
}

data class ChecksumResult(
    val checksum: String,
    val algorithm: String
)

val checksumAdapter: JsonAdapter<ChecksumResult> = MOSHI.adapter(ChecksumResult::class.java)

/**
 * Creates instance of [ChecksumResult] from json string.
 */
fun jsonToChecksumResult(json: String): ChecksumResult {
    return checksumAdapter.fromJson(json) as ChecksumResult
}

/**
 * Creates JSON string of the checksum result.
 */
fun ChecksumResult.toJson(): String {
    return checksumAdapter.toJson(this)
}

data class AssemblyManifest(
    val moduleId: String,
    val moduleName: String,
    val moduleDescription: String,
    val version: String,
    val checksum: ChecksumResult,
    val fileName: String,
    val fileSize: Long,
    val artifacts: Map<String, ArtifactManifest>, // project path to manifest
    val metaInfo: Map<String, String>
)

val adapter = MOSHI.adapter(AssemblyManifest::class.java).indent("    ")

fun AssemblyManifest.toJson(): String {
    return adapter.toJson(this)
}

fun assemblyManifestFromJson(json: String): AssemblyManifest {
    return adapter.fromJson(json) as AssemblyManifest
}
