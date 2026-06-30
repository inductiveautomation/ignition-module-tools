package io.ia.ignition.module.generator.api

enum class SourceFileType {
    JAVA,
    KOTLIN,
    PROPERTIES,
    TOML,
    ;

    fun sourceCodeFileExtension(): String = when (this) {
        JAVA -> "java"
        KOTLIN -> "kt"
        TOML -> "toml"
        PROPERTIES -> "properties"
    }

    fun commonName(): String = when (this) {
        JAVA -> JAVA.name.lowercase()
        KOTLIN -> KOTLIN.name.lowercase()
        TOML -> TOML.name.lowercase()
        PROPERTIES -> "properties file"
    }
}
