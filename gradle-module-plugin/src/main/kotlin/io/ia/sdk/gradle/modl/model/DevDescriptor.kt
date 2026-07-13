package io.ia.sdk.gradle.modl.model

import com.squareup.moshi.JsonClass

/**
 * Data model for a dev module descriptor JSON file, used by the gateway to load modules
 * from Gradle build outputs instead of .modl files for IDE classloader isolation.
 */
@JsonClass(generateAdapter = false)
data class DevModuleDescriptor(
    val id: String,
    val name: String,
    val version: String,
    val freeModule: Boolean = false,
    val hooks: Map<String, String> = emptyMap(),
    val moduleDependencies: List<DevModuleDependency> = emptyList(),
    val scopes: Map<String, DevScopeEntry> = emptyMap(),
    val exports: Map<String, List<String>> = emptyMap(),
)

@JsonClass(generateAdapter = false)
data class DevModuleDependency(
    val id: String,
    val scope: String,
    val required: Boolean = false,
)

@JsonClass(generateAdapter = false)
data class DevScopeEntry(
    val classDirs: Set<String> = emptySet(),
    val jars: Set<String> = emptySet(),
)

fun DevModuleDescriptor.toJson(): String {
    val adapter = MOSHI.adapter(DevModuleDescriptor::class.java).indent("  ")
    return adapter.toJson(this)
}
