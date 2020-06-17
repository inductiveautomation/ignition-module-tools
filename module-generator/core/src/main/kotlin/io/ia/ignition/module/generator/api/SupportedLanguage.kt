package io.ia.ignition.module.generator.api

enum class SupportedLanguage {
    JAVA,
    KOTLIN;

    fun sourceCodeFileExtension(): String {
        return when (this) {
            JAVA -> "java"
            KOTLIN -> "kt"
        }
    }

    fun commonName(): String {
        return when (this) {
            JAVA -> JAVA.name.toLowerCase()
            KOTLIN -> KOTLIN.name.toLowerCase()
        }
    }
}
