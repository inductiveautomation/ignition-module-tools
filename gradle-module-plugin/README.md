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


4. When depending on artifacts (dependencies) from the Ignition SDK, they should be specified as `compileOnly` dependencies as they will be provided by the Ignition platform at runtime.  Dependencies that are applied with the `modlDependency` _Configuration_ in any subproject of your module will be collected and included in the final modl file. Transitive dependencies of `modlDependency` artifacts will also be included, unless specifically excluded using the traditional gradle dependency exclusions.  We suggest reviewing the contents of your modl file to ensure no 'test only' or 'ignition platform' dependencies are included in your module. Gradle's built-in _dependencies_ task can be used to help identify how/why any undesired dependencies are being included (or desired ones missed) in your projects. Test and Compile-time dependencies should be specified in accordance with the best practices described in Gradle's `java` [plugin documentation](https://docs.gradle.org/current/userguide/java_plugin.html#java_plugin). The `java-library` plugin may also be used, but the modl plugin does not result in any additional/altered behavior. 


> Maven Users: If you're familiar with Maven's dependency scopes, you might initially find Gradle's handling of dependencies to be unnecessarily convoluted.  This is a product of Gradle's powerful (but more complex) dependency management.  We suggest reading the gradle docs on [Working With Dependencies](https://docs.gradle.org/current/userguide/core_dependency_management.html), followed by reading the [Java Library Plugin](https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_separation) documentation.  




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
    
    /**
     * Optional map of arbitrary String to String entries.  These will make it into the final _buildResult.json_, but
     * are otherwise unused and have no impact on the module itself.  These values may be useful for adding data to 
     * used by consumers of this build's output.  For instance: CI and publication systems, integrity checking, etc. 
     */
     metaInfo.put("someKey", "Some arbitrary value useful to later use")
     metaInfo.put("publicationUrl", "1.2.3.4:8090")
}

```

 # Tasks
 
 > To see all tasks provided by the plugin, run the `tasks` gradle command, or `tasks --all` to see all possible tasks.


The module plugin exposes a number of tasks that may be run on their own, and some which are bound to lifecycle tasks
provided by Gradle's [Base Plugin](https://docs.gradle.org/current/userguide/base_plugin.html).  Some tasks apply
only to the root project (the project which is applying the plugin), while others are applied to one or more 
subprojects.  The following table is a brief reference:


| Task  | Scope  | Description | 
|-------|--------|-------------------------|
| collectModlDependencies  | root and child projects  | Resolves and collects dependencies from projects with the `java` plugin (including those which apply it, such as the `java-library` plugin) if declared with the 'modlDependency' configuration |
| assembleModlStructure | aggregates assets, dependencies and assembled project jars created by the 'collectModlDependencies' task into the module staging directory |
| writeModuleXml  | root project  | Writes the module.xml file to the staging directory  |
| zipModule  | root project | Compresses the staged module contents into an unsigned zip archive with a .modl file extension  |
| checksumModl  | root project  | Generates a checksum for the signed module, and writes the result to a json file  |
| moduleAssemblyReport  | root project | Writes a json file containing meta information about the module's assembly  |
| signModl | root project | signs the unsigned modl using credentials/certs noted above


# How it Works

This plugin is applied to a single project (the 'root' of the module) that may or may not have child projects.
When the plugin is applied, it will attempt to identify if the root or any subprojects apply the `java`
plugin. For each that does, it will add the _modlDependency_ configurations, so that it may be used
in the project's dependency settings. These special dependencies (and _their_ transitive dependencies) will be resolved 
and collected into the final modl file that is generated. It will create the asset collection tasks and bind them to the
 _assemble_ lifecycle tasks, and ultimately establish task dependencies for the 'root-specific' tasks that create the
module xml, copy files into the appropriate structure, zip the folder into an unsigned modl file, sign it, and report
the result.


# Pre-Release API Changes

* v0.1.0-SNAPSHOT-6 - changed how credentials and files are specified for signing and publication.  The keys are the same, but properties are now expected to exist in a gradle.properties file, or to be specified as runtime flags as described in the Usage section above.
* v0.1.0-SNAPSHOT-12 - added checksum generation and build report tasks.  Split repo into separate builds using gradle build composition to better isolate changes.  
* v0.1.0-SNAPSHOT-15 - fixed dependency collection, renamed 'AssembleModuleAssets' task class and associated task
* v0.1.0-SNAPSHOT-16 - changed modlImplementation configuration to not resolve transitive dependencies
* v0.1.0-SNAPSHOT-17 - modlImplementation and modlApi configurations have been replaced with a single `modlDependency` configuration to simplify dependency marking and avoid confusing differences between compile-time and modl runtime environments, 
