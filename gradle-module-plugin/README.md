# Ignition Module Plugin for Gradle

The Ignition platform is an open/pluggable system that uses Ignition Modules to add functionality.  As documented in the [Ignition SDK Programmer's Guide](https://docs.inductiveautomation.com/display/SE/Ignition+SDK+Programmers+Guide), an Ignition Module consists of an xml manifest, jar files, and additional resources and metainformation.  

This plugin helps build appropriately structured and signed modules for installation into an Ignition Gateway. 

The Ignition Module Plugin for Gradle lets module developers use the [Gradle](https://www.gradle.org) build tool to create and sign modules (_.modl_ ) intended for use 
in Inductive Automation's Ignition Platform.

## Overview


The plugin is intended to support both simple projects with only a single source directory, as well as multi-scope/multi-project builds. Following Gradle best practices, configuration is simple and understandable when following our conventions, but complex customizations are also doable when required.

## Usage

The easiest way to get started with this plugin is to create a new module project using the Ignition Module Generator in this repository, or by cloning an existing gradle project from the [Inductive Automation Example Modules](http://www.github.com/inductiveautomation/ignition-sdk-examples)


1. Apply the plugin to your `build.gradle`, or in the case of a multi-project build, to the root or parent project
.   *Note* that you should only apply the plugin to a single parent project in a multi-scope structure (e.g., one
 where you have separate source directories for `gateway` and `designer` code, for instance).

2. Configure your module through the `ignitionModule` configuration DSL.  See DSL properties section below for details. 

3. When depending on artifacts (dependencies) from the Ignition SDK, they should be specified as `compileOnly` dependencies as they will be provided by the Ignition platform at runtime.  Otherwise, your dependencies should be specified in accordance with the best practices described in Gradle's `java-library` plugin documentation, which is available [here](https://docs.gradle.org/current/userguide/java_library_plugin.html).  Dependencies marked with either `api` or `implementation` in any subproject of your module will be collected and included in the final modl file.  Test-only dependencies should not be marked with these configurations.

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
     * Gradle File property, resolving to the property file that contains key/value pairs which define the locations of and credentials for
     * module signing.  Optional if passing values as runtime flags, project props, or using the gradle.properties file
     * to store the required key:value pairs.
     *
     * In a property file, the following keys are required, shown with example values:
     * ```
     * ignition.signing.keystoreFile=/path/to/keystorefile.jks
     * ignition.signing.keystorePassword=somepassword
     * ignition.signing.certFile=/path/to/certfile.pem
     * ignition.signing.certPassword=somepassword
     * ignition.signing.alias=selfsigned
     * ```
     * The actual value must resolve to a File type, either via `new File()`, or using
     * gradle's file methods as shown below.  More info on gradle file handling and
     * file types available in the [Gradle Docs](https://docs.gradle.org/current/userguide/working_with_files.html#sec:locating_files)
     *
     */
     propertyFile = project.file("/path/to/my/signing.properties")
}

```

 # Tasks
 
 To see the tasks available, run the `tasks` gradle command.
