package io.ia.sdk.gradle.modl.api

object Constants {
    /**
     * Name for directory created in a plugin project's `build` folder
     */
    @JvmStatic
    val LOCAL_PROJECT_WORKDIR: String = "module"

    @JvmStatic
    val ARTIFACT_DIR: String = "artifacts"

    /**
     * "_Working directory_" used by the root module project (where the plugin is applied).  The files and information
     * from a module's build is aggreggated into this directory, the contents of which are ultimately zipped into the
     * .modl file.
     */
    @JvmStatic
    val MODULE_BUILD_WORKDIR = "$LOCAL_PROJECT_WORKDIR/modl"

    @JvmStatic
    val MODULE_API_CONFIGURATION = "modlApi"

    @JvmStatic
    val MODULE_IMPLEMENTATION_CONFIGURATION = "modlImplementation"

    /**
     * Flag checked when setting up the plugin's artifact repository sources.  By default, the plugin adds the Inductive
     * Automation Maven artifact repositories to the project's configuration, allowing the ability to resolve Ignition
     * Java dependencies.  If this is undesirable, executing the build with this flag's value set to 'false' will skip
     * the auto-application of the IA repo.
     *
     * _Excluding the IA repos is generally undesirable, as dependencies are required to compile a module. This flag
     * is provided for troubleshooting/debugging, those using a private artifact repo system that is mirroring or
     * proxying the IA repository, or in the event the Inductive Automation repo URL changes and the incorrect URL is
     * failing the build._
     *
     * Example Usage: `./gradlew assemble -PapplyIaMavenRepository=false`
     * Default value: no default value applied.  Empty value treated as if `applyIaMavenRepository=true` were applied.
     */
    @JvmStatic
    val APPLY_IA_REPOSITORY_FLAG = "applyIaMavenRepository"
}
