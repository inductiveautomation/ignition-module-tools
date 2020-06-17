package io.ia.sdk.gradle.modl.extension

import io.ia.sdk.gradle.modl.task.ZipModule
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

public const val EXTENSION_NAME = "ignitionModule"

/**
 * A data class representing the configuration of an Ignition module that is assembled by the gradle plugin.
 *
 * This configuration generally plays two roles:
 *
 *     1. It specifies meta-information about your module such as 'name' and 'version'.  This meta-info is ultimately
 *        reflected in the module.xml file that is generated and placed in the root of your assembled module by the plugin
 *     2. It identifies the project or subprojects that are to be included by your
 */
@Suppress("UnstableApiUsage")
open class ModuleSettings @javax.inject.Inject constructor(objects: ObjectFactory, layout: ProjectLayout) {
    /**
     * The 'name' of your module as is displayed in the Ignition Gateway configuration page when the module is installed.
     */
    var name: Property<String> = objects.property(String::class.java)

    /**
     * Name of the resulting .modl file.  Defaults to &lt;MODULENAME&gt;-&lt;VERSION&gt;.modl if not specified.
     *
     * Optional
     */
    var fileName: Property<String> = objects.property(String::class.java)

    /**
     * The unique module id.  It is by this ID that an installed module instance can be retrieved from Ignition's
     * {@link ModuleManager}To avoid the potential for collisions, it is suggested to follow Java's package name
     * rules.  For example: 'com.inductiveautomation.vision'
     */
    var id: Property<String> = objects.property(String::class.java)

    /**
     * The version of the module.  Defaults to the version of the Gradle subproject this plugin is applied to.
     */
    var moduleVersion: Property<String> = objects.property(String::class.java)

    /**
     * Short sentence that summarizes the functionality of the module, as would be useful when displayed in a tooltip.
     */
    var moduleDescription: Property<String> = objects.property(String::class.java)

    /**
     * Minimum version of Ignition that this module is compatible with.
     */
    var requiredIgnitionVersion: Property<String> = objects.property(String::class.java)

    /**
     * Map of Gradle Project path(s) to one or more Ignition Scopes, where the scopes are defined by
     */
    var projectScopes: MapProperty<String, String> =
        objects.mapProperty(String::class.java, String::class.java)

    /**
     * List of module dependencies, which declare one or more modules you are dependent on, as well as the scope in
     * which you depend on them, key'd on the module ID of the module depended-on, with shorthand scope value of the
     * scope in which the module is depended.
     *
     * ### Examples:
     *
     * _Groovy_
     * `  moduleDependencies = [ "com.inductiveautomation.vision" : "GCD"]`
     *
     * _Kotlin_
     * `  moduleDependencies = mapOf("com.inductiveautomation.vision" to "GCD")`
     */
    var moduleDependencies: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)

    /**
     * Map of Ignition Scope to fully qualified hook class to, where scope is one of "C", "D", "G" for "vision Client",
     * "Designer", and "Gateway" respectively.
     *
     * ### Examples:
     *
     * _Groovy_
     *`  hooks = ["G": "com.example.gateway.GatewayModuleHook"]`
     *
     * _Kotlin_
     *`  hooks = mapOf("D" to "com.example.designer.MyDesignerHook")`
     */
    var hooks: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)

    /**
     * If the module is a 'free' (non-commercial) module, without licensing restrictions, set this
     * value to Boolean.TRUE.  Default is [Boolean.FALSE]
     *
     * Optional
     */
    var freeModule: Property<Boolean> = objects.property(Boolean::class.javaObjectType)

    /**
     * The path to the .html file containing your license information.
     *
     * Optional
     */
    var license: Property<String> = objects.property(String::class.java)

    /**
     * The name of the root/parent documentation file for your module.  Documentation, if
     * provided, should be placed in a directory named 'doc' at the root of your build.
     *
     * Optional
     */
    var documentation: Property<String> = objects.property(String::class.java)

    /**
     * Optional Ignition platform requirements which fulfill the Ignition 8.0  ability to specify optional dependencies
     * which may be provided by the platform.  Current example is jxbrowser being added to client scope at runtime when
     * the module.xml contains an entry such as:
     *
     * ```
     *<require scope="CD">jxbrowser</require>;
     * ```
     *
     * Such an entry tells Ignition "this module requires the jxbrowser library to be provided from the Ignition
     * platform to correctly function in the Client and Designer environments.
     */
    var requireFromPlatform: MapProperty<String, String> =
        objects.mapProperty(String::class.java, String::class.java)

    /**
     * Framework version.
     */
    var requiredFrameworkVersion: Property<String> = objects.property(String::class.java)

    /**
     * Path to the property file that contains key/value pairs which define the locations of and credentials for
     * module signing.  Optional if passing values as runtime flags, project props, or using the gradle.properties file
     * to store the required key:value pairs.
     *
     * As a RegularFileProperty, the value should be a Provider<File>, which is easiest to do by simply assigning
     * the value using Gradle's [org.gradle.api.Project.file] method:
     *
     * ```
     *     propertyFile = project.file("/users/someone/home/someprops.properties")
     * ```
     *
     */
    var propertyFile: RegularFileProperty = objects.fileProperty()

    init {
        license.convention("")
        freeModule.convention(java.lang.Boolean.FALSE)
        moduleDescription.convention("")
        requireFromPlatform.convention(emptyMap())
        requiredIgnitionVersion.convention("8.0.0")
        requiredFrameworkVersion.convention("8")
        propertyFile.convention(
            layout.projectDirectory.file("${System.getProperty("user.home")}/signing.properties")
        )
        fileName.convention(
            name.map { it.split(" ").joinToString(separator = "-", postfix = ".") {
                it.capitalize() } + ZipModule.UNSIGNED_EXTENSION
            }
        )
    }
}
