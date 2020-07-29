package io.ia.ignition.module.generator.api

enum class GradleDsl {
    GROOVY,
    KOTLIN;

    fun buildScriptFilename(): String {
        return when (this) {
            GROOVY -> "build.gradle"
            KOTLIN -> "build.gradle.kts"
        }
    }

    fun settingsFilename(): String {
        return when (this) {
            GROOVY -> "settings.gradle"
            KOTLIN -> "templates/settings.gradle.kts"
        }
    }
}
