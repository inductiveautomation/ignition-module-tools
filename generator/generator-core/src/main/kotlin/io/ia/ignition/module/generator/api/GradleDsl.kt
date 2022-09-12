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

    fun mapAssociator(): String {
        return when (this) {
            GROOVY -> ":"
            KOTLIN -> "to"
        }
    }

    /**
     * Builds the string content of an extra property on a buildscript.
     */
    fun extraPropertyString(propertyName: String, value: String): String {
        val writtenValue = "\"$propertyName\""
        return when (this) {
            GROOVY -> "    $propertyName = $writtenValue"
            KOTLIN -> "val $propertyName: String by extra($writtenValue)"
        }
    }

    fun dependencyBlock(): String {
        val block = """
                |dependencies {
                |    // <DEPENDENCIES>
                |}
                |
            """.trimMargin()

        // dependency config syntax is currently the same for groovy and kts
        return when (this) {
            GROOVY -> block
            KOTLIN -> block
        }
    }


}
