# Ignition Module Plugin for Gradle

The Ignition platform is an open/pluggable JVM based system that uses Ignition Modules to add functionality.  As documented in the [Ignition SDK Programmer's Guide](https://docs.inductiveautomation.com/display/SE/Ignition+SDK+Programmers+Guide), an Ignition Module consists of an xml manifest, jar files, and additional resources and meta-information.  

The Ignition Module Plugin for Gradle lets module developers use the [Gradle](https://www.gradle.org) build tool to create and sign functional modules (_.modl_ ) through a convenient DSL-based configuration model.



## Usage

The easiest way to get started with this plugin is to create a new module project using the Ignition Module Generator in this repository.

1. Apply the plugin to your `build.gradle`, or in the case of a multi-project build, to the root or parent project
.   *Note* that you should only apply the plugin to a single parent project in a multi-scope structure (e.g., one
 where you have separate source directories for `gateway` and `designer` code, for instance).

2. Configure your module through the `ignitionModule` configuration DSL.  See DSL properties section below for details. 

3. Configure your signing settings, either in a gradle.properties file, or as commandline flags.  The required properties are defined in constants.kt, and used in the SignModule task.  You may mix and match flags and properties (and flags will override properties), as long as all required values are configured.  The only requirement is that option flags _must_ follow the gradle command to which they apply, which is the 'signModule' task in this case.   The flags/properties are as follows, with usage examples:
   >Note: builds prior to v0.1.0-SNAPSHOT-6 used a separate property file called signing.properties.  Builds after that use gradle.properties files instead.   
   
   | Flag  | Usage  | gradle.properties entry | 
   |-------|--------|-------------------------|
   | certAlias  | gradlew signModule --certAlias=someAlias  | ignition.signing.certAlias=someAlias  |
   | certFile  | gradlew signModule --certFile=/path/to/cert  | ignition.signing.certFile=/path/to/cert  |
   | certPassword  | gradlew signModule --certPassword=mysecret  | ignition.signing.certFile=mysecret  |
   | keystoreFile  | gradlew signModule --keystoreFile=/path/to/keystore  | ignition.signing.keystoreFile=/path/to/keystore  |
   | keystorePassword  | gradlew signModule --keystorePassword=mysecret  | ignition.signing.keystoreFile=mysecret  |


4. When depending on artifacts (dependencies) from the Ignition SDK, they should be specified as `compileOnly` dependencies as they will be provided by the Ignition platform at runtime.  Otherwise, your dependencies should be specified in accordance with the best practices described in Gradle's `java-library` plugin documentation, which is available [here](https://docs.gradle.org/current/userguide/java_library_plugin.html).  

Dependencies marked with either `modlApi` or `modlImplementation` in any subproject of your module will be collected and included in the final modl file.  Note that currently there is no distinction between those configurations with respect to the Ignition Platform itself - however, all other implications apply as documented by the Gradle java-library plugin (e.g. - publishing, artifact uploading, transitive dependency handling, etc).  Test-only dependencies should not be marked with these `modl` configurations.

### `ignitionModule` DSL Properties

Configuration for a module occurs through the `ignitionModule` extension DSL.  See the source code `ModuleSettings.kt` for all options and descriptions.  Example configuration:  

```
ignitionModule {
    /*
     * Human readable name of the module, as will be displayed on the gateway status page
     */
    name = "Starlink Driver"

    /*
     * Name of the '.modl' file to be created, without file extension.
     */
    fileName = "starlink-driver"
    /*
     * Unique identifier for the module.  Reverse domain convention is recommended (e.g.: com.mycompany.charting-module)
     */
    id = "net.starlink.driver"

    moduleVersion = version   // common to refer to the version of the gradle project like this

    moduleDescription = "A short sentence describing what it does, but not much longer than this."
    /*
     * Minimum version of Ignition required for the module to function correctly.  This typically won't change over
     * the course of a major Ignition (7.9, 8.0, etc) version, except for when the Ignition Platform adds/changes APIs
     * used by the module.
     */
    requiredIgnitionVersion = "8.0.10"
    /*
     *  This is a map of String: String, where the 'key' represents the fully qualified path to the project
     *  (using colon-separated gradle project path syntax), and the value is the shorthand Scope string.
     *  Example entry: [ ":gateway": "G", ":common": "GC", ":vision-client": "C" ]
     */
    projectScopes = [
        ":gateway" : "G"
    ]

    /*
     * Add your module dependencies here, following the examples, with scope being one or more of G, C or D,
     * for (G)ateway, (D)esigner, Vision (C)lient.
     *
     * Example Value:
     * moduleDependencies = [
           "com.inductiveautomation.opcua": "G"
     * ]
     */
    moduleDependencies = [ : ]   // syntax for initializing an empty map in groovy

    /*
     * Map of fully qualified hook class to the shorthand scope.  Only one scope per hook class.
     *
     * Example entry: "com.myorganization.vectorizer.VectorizerDesignerHook": "D"
     */
    hooks = [
        "net.starlink.driver.gateway.GatewayHook": "G"
    ]
}

```

 # Tasks
 
 To see the tasks available, run the `tasks` gradle command, or `tasks --all` to see all possible tasks..  

# Pre-Release API Changes

* v0.1.0-SNPASHOT-6 - changed how credentials and files are specified for signing and publication.  The keys are the same, but properties are now expected to exist in a gradle.properties file, or to be specified as runtime flags as described in the Usage section above.
