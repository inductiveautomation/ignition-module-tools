package io.ia.sdk.gradle.modl.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class Artifact(val group: String, val name: String, val version: String, val fileName: String)

data class ArtifactManifest(val projectPath: String, val projectName: String, val scope: String, val artifacts: List<Artifact>)

val MOSHI: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

fun manifestFromJson(json: String): ArtifactManifest {
    val adapter = MOSHI.adapter(ArtifactManifest::class.java)
    return adapter.fromJson(json) as ArtifactManifest
}

fun manifestToJson(manifest: ArtifactManifest): String {
    val adapter = MOSHI.adapter(ArtifactManifest::class.java)
    return adapter.toJson(manifest)
}
