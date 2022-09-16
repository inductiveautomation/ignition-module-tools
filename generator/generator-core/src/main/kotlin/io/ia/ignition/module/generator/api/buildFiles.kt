package io.ia.ignition.module.generator.api

enum class BuildFileType {
    BUILDSCRIPT,
    SETTINGS,
    PROPERTY,
    VERSION_CATALOG,
    MISC
}

/**
 * A BuildFile represents a single file that will be generated.
 */
interface BuildFile {

    /**
     * The path and complete filename (including extension) of file that would be created and filled with the results
     * of [renderContents].  The path should be relative to the project or subroject the file is being generated for.
     */
    fun getLocalFilePath(): String

    /**
     * Scope of the build file
     */
    fun getScope(): ProjectScope

    /**
     * Called to build the full string contents of the build file.
     */
    fun renderContents(): String
}
