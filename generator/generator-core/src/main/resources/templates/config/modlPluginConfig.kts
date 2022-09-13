ignitionModule {
    /*
     * Human readable name of the module, as will be displayed on the gateway status page
     */
    name.set("<MODULE_NAME>")

    /*
     * Name of the '.modl' file to be created, without file extension.
     */
    fileName.set("<MODULE_FILENAME>")
    /*
     * Unique identifier for the module.  Reverse domain convention is recommended (e.g.: com.mycompany.charting-module)
     */
    id.set("<MODULE_ID>")

    /*
     * Version of the module.  Here being set to the same version that gradle uses, up above in this file.
     */
    moduleVersion.set("${project.version}")

    moduleDescription.set("A short sentence describing what it does, but not much longer than this.")

    /*
     * Minimum version of Ignition required for the module to function correctly.  This typically won't change over
     * the course of a major Ignition (7.9, 8.0, etc) version, except for when the Ignition Platform adds/changes APIs
     * used by the module.
     */
    requiredIgnitionVersion.set("8.1.11")
    /*
     *  This is a map of String: String, where the 'key' represents the fully qualified path to the project
     *  (using gradle path syntax), and the value is the shorthand Scope string.
     *  Example entry: listOf( ":gateway" to "G", ":common" to "GC", ":vision-client" to "C" )
     */
    projectScopes.set(
        listOf(
        <PROJECT_SCOPE_CONFIG>
    ))

    /*
     * Add your module dependencies here, following the examples, with scope being one or more of G, C or D,
     * for (G)ateway, (D)esigner, Vision (C)lient.
     * Example:
     * moduleDependencies = mapOf(
     *    "CD" to "com.inductiveautomation.vision",
     *    "G" to "com.inductiveautomation.opcua"
     *  )
     */
    moduleDependencies.set(mapOf<String, String>())

    /*
     * Map of fully qualified hook class to the shorthand scope.  Only one scope may apply to a class, and each scope
     * must have no more than single class registered.  You may omit scope registrations if they do not apply.
     *
     * Example entry: "com.myorganization.vectorizer.VectorizerDesignerHook" to "D"
     */
    hooks.set(
        mapOf(
         <HOOK_CLASS_CONFIG>
        )
    )

    /*
     * Optional 'documentation' settings.  Supply the files that would be desired to end up in the 'doc' dir of the
     * assembled module, and specify the path to the index.html file inside that folder. In this commented-out
     * example, the html files being collected are located in the module root project in `src/docs/`
     */
    // the files to collect into the documentation dir, with example implementation
    // documentationFiles.from(project.file("src/docs/"))

    /* The path from the root documentation dir to the index file, or filename if in the root doc dir. */
    // documentationIndex.set("index.html")

    /*
     * Optional unsigned modl settings. If true, modl signing will be skipped. This is not for production and should
     * be used merely for development testing
     */
    // skipModlSigning.set(false)
}
