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

    /**
     * Signing properties located in gradle.properties files will be namespaced under this prefix in order to avoid
     * collisions.
     */
    @JvmStatic
    val PROPERTY_NAMESPACE = "ignition.signing"

    /**
     * CLI flag/Property Suffix (see readme) for the certificate alias used when signing the built module.
     */
    const val ALIAS_FLAG: String = "certAlias"

    /**
     * CLI flag/Property suffix (see readme) for the certifcate (file) that is used for signing the built module.
     */
    const val CERT_FILE_FLAG: String = "certFile"

    /**
     * Password for the certificate used to sign the built module.
     */
    const val CERT_PW_FLAG: String = "certPassword"

    /**
     * The keystore file to be used for signing the module
     */
    const val KEYSTORE_FILE_FLAG: String = "keystoreFile"

    /**
     * Keystore password to use in signing the module
     */
    const val KEYSTORE_PW_FLAG: String = "keystorePassword"

    /**
     * Map of CLI option flag `gradle <taskname> --taskProperty=value` to namespaced gradle property equivalent.
     *
     * Examples:
     *
     *
     * | Task Flag    |  Project Property  | Property File (in gradle.properties, or as -P property)|
     * |------------------------------------------------------------|
     *|gradle signModule --certAlias=someAlias | gradle signModule -Pignition.signning.certAlias=someAlias | ignition.signing.certAlias=someAlias|
     */
    val SIGNING_PROPERTIES: Map<String, String> = mapOf(
        ALIAS_FLAG to "$PROPERTY_NAMESPACE.$ALIAS_FLAG",
        CERT_FILE_FLAG to "$PROPERTY_NAMESPACE.$CERT_FILE_FLAG",
        CERT_PW_FLAG to "$PROPERTY_NAMESPACE.$CERT_PW_FLAG",
        KEYSTORE_FILE_FLAG to "$PROPERTY_NAMESPACE.$KEYSTORE_FILE_FLAG",
        KEYSTORE_PW_FLAG to "$PROPERTY_NAMESPACE.$KEYSTORE_PW_FLAG"
    )
}
