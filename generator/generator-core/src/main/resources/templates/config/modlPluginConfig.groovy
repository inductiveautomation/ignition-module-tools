
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
     *  Example entry: [ ":gateway": "G", ":common": "GC", ":vision-client": "C" ]
     */
    projectScopes = [
    <PROJECT_SCOPE_CONFIG>
    ]

    /*
     * Add your module dependencies here, following the examples, with scope being one or more of G, C or D,
     * for (G)ateway, (D)esigner, Vision (C)lient.
     *
     * Example Value:
     * moduleDependencies = [
           "com.inductiveautomation.vision": "CD",
           "com.inductiveautomation.opcua": "G"
     * ]
     */
    moduleDependencies = [ : ]

    /*
     * Add required module dependencies here, following the examples, with scope being one or more of G, C or D,
     * for (G)ateway, (D)esigner, Vision (C)lient.
     *
     * Example:
     * requiredModuleDependencies = [
     *    moduleId("com.inductiveautomation.vision") {
     *        it.scope = "GCD"
     *        it.required = true
     *    }
     * ]
     *
     * If any of module's required module dependencies are not present, the
     * gateway will fault on loading the module.
     *
     * NOTE: For modules targeting Ignition 8.3 and later. Use `moduleDependencies` for 8.1 and earlier.
     * This property will only add the "required" flag if {requiredIgnitionVersion} is at least 8.3
     *
     */
    requiredModuleDependencies = [ ]

    /*
     * Map of fully qualified hook class to the shorthand scope.  Only one scope per hook class.
     *
     * Example entry: "com.myorganization.vectorizer.VectorizerDesignerHook": "D"
     */
    hooks = [
    <HOOK_CLASS_CONFIG>
    ]

    applyInductiveArtifactRepo = true

    /*
     * Optional 'documentation' settings.  Supply the files that would be desired to end up in the 'doc' dir of the
     * assembled module, and specify the path to the index.html file inside that folder. In this commented-out
     * example, the html files being collected are located in the module root project in `src/docs/`
     */
    // the files to collect into the documentation dir, with example implementation
    // documentationFiles.from(project.file("src/docs/"))

    // the path from the root documentation dir to the index file.
    // documentationIndex.set("index.html")

    /*
     * Optional unsigned modl settings. If true, modl signing will be skipped. This is not for production and should
     * be used merely for development testing
     */
    //<SKIP_MODULE_SIGNING>
}
