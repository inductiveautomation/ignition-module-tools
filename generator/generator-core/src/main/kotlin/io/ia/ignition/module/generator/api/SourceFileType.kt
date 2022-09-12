package io.ia.ignition.module.generator.api

enum class SourceFileType {
    JAVA,
    KOTLIN,
    PROPERTIES,
    TOML;

    fun sourceCodeFileExtension(): String {
        return when (this) {
            JAVA -> "java"
            KOTLIN -> "kt"
            TOML -> "toml"
            PROPERTIES -> "properties"
        }
    }

    fun commonName(): String {
        return when (this) {
            JAVA -> JAVA.name.lowercase()
            KOTLIN -> KOTLIN.name.lowercase()
            TOML -> TOML.name.lowercase()
            PROPERTIES -> "properties file"
        }
    }
}
