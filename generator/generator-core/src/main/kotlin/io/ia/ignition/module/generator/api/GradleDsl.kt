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
        val writtenValue = "\"$value\""
        return when (this) {
            GROOVY -> "    $propertyName = $writtenValue"
            KOTLIN -> "val $propertyName: String by extra($writtenValue)"
        }
    }

    /**
     * Returns the string that resolves the sdk version to use for the Ignition sdk dependencies.  Assumes
     * the version has been defined as an 'extra project property' with the key of `sdk_version`.
     */
    fun artifactSdkVersion(): String {
        return when (this) {
            GROOVY -> "${'$'}sdk_version"
            KOTLIN -> "${'$'}{rootProject.extra[\"sdk_version\"]}"
        }
    }

    fun skipSigningConfig(enable: Boolean = true): String {
        return when (this) {
            KOTLIN -> "skipModlSigning.set($enable)"
            GROOVY -> "skipModlSigning = $enable"
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
