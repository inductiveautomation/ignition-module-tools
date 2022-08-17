// TODO -- this needs to be implemented for kotlinscript
plugins {
    <ROOT_PLUGIN_CONFIGURATION>
}

ext {
    sdk_version = "8.0.10"
}

allprojects {
    version = "0.0.1-SNAPSHOT"
}

ignitionModule {
    /*
     * Human readable name of the module, as will be displayed on the gateway status page
     */
    name = "<MODULE_NAME>"

    /*
     * Name of the '.modl' file to be created, without file extension.
     */
    fileName = "<MODULE_FILENAME>"
    /*
     * Unique identifier for the module.  Reverse domain convention is recommended (e.g.: com.mycompany.charting-module)
     */
    id = "<MODULE_ID>"

    /*
     * Version of the module.  Here being set to the same version that gradle uses, up above in this file.
     */
    moduleVersion = version

    moduleDescription = "A short sentence describing what it does, but not much longer than this."

    /*
     * Minimum version of Ignition required for the module to function correctly.  This typically won't change over
     * the course of a major Ignition (7.9, 8.0, etc) version, except for when the Ignition Platform adds/changes APIs
     * used by the module.
     */
    requiredIgnitionVersion = "8.0.10"
    /*
     *  This is a map of String: String, where the 'key' represents the fully qualified path to the project
     *  (using gradle path syntax), and the value is the shorthand Scope string.
     *  Example entry: listOf( ":gateway" to "G", ":common" to "GC", ":vision-client" to "C" )
     */
    projectScopes = listOf(
        <PROJECT_SCOPE_CONFIG>
    )

    /*
     * Add your module dependencies here, following the examples, with scope being one or more of G, C or D,
     * for (G)ateway, (D)esigner, Vision (C)lient.
     * Example:
     * moduleDependencies = mapOf(
     *    "com.inductiveautomation.vision" to "CD",
     *    "com.inductiveautomation.opcua" to "G"
     *  )
     */
    moduleDependencies = mapOf()

    /*
     * Map of fully qualified hook class to the shorthand scope.  Only one scope per hook class.
     *
     * Example entry: "com.myorganization.vectorizer.VectorizerDesignerHook" to "D"
     */
    hooks = mapOf(
            <HOOK_CLASS_CONFIG>
    )

    /*
     * Optional unsigned modl settings. If true, modl signing will be skipped. This is not for production and should
     * be used merely for development testing
     */
    // skipModlSigning.set(false)
}
