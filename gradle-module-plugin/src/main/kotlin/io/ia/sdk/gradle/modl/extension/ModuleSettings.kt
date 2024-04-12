package io.ia.sdk.gradle.modl.extension

import io.ia.sdk.gradle.modl.task.HashAlgorithm
import io.ia.sdk.gradle.modl.task.ZipModule
import io.ia.sdk.gradle.modl.util.capitalize
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

const val EXTENSION_NAME = "ignitionModule"

/**
 * A data class representing the configuration of an Ignition module that is assembled by the gradle plugin.
 *
 * This configuration generally plays two roles:
 *
 *     1. It specifies meta-information about your module such as 'name' and 'version'.  This meta-info is ultimately
 *        reflected in the module.xml file that is generated and placed in the root of your assembled module by the plugin
 *     2. It identifies the project or subprojects that are to be included by your
 */
open class ModuleSettings @javax.inject.Inject constructor(objects: ObjectFactory) {

    /**
     * The 'name' of your module as is displayed in the Ignition Gateway configuration page when the module is installed.
     */
    val name: Property<String> = objects.property(String::class.java)

    /**
     * Name of the resulting .modl file.  Defaults to &lt;MODULENAME&gt;-&lt;VERSION&gt;.modl if not specified.
     *
     * Optional
     */
    val fileName: Property<String> = objects.property(String::class.java)

    /**
     * The unique module id.  It is by this ID that an installed module instance can be retrieved from Ignition's
     * {@link ModuleManager}To avoid the potential for collisions, it is suggested to follow Java's package name
     * rules.  For example: 'com.inductiveautomation.vision'
     */
    val id: Property<String> = objects.property(String::class.java)

    /**
     * The version of the module.  Defaults to the version of the Gradle subproject this plugin is applied to.
     */
    val moduleVersion: Property<String> = objects.property(String::class.java)

    /**
     * Short sentence that summarizes the functionality of the module, as would be useful when displayed in a tooltip.
     */
    val moduleDescription: Property<String> = objects.property(String::class.java)

    /**
     * Minimum version of Ignition that this module is compatible with.
     */
    val requiredIgnitionVersion: Property<String> = objects.property(String::class.java)

    /**
     * Map of Gradle Project path(s) to one or more Ignition Scopes, where the scopes are defined by
     */
    val projectScopes: MapProperty<String, String> =
        objects.mapProperty(String::class.java, String::class.java)

    /**
     * Map that serves no specific purpose in the plugin itself, but may be useful to build authors to support
     * functionality of pre or post module builds. The information in this map is included in the `buildResult.json`
     * file that is created when a module is assembled.
     */
    val metaInfo: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)

    /**
     * List of module dependencies, which declare one or more modules you are dependent on, as well as the scope in
     * which you depend on them, keyed on the module ID of the module depended-on, with shorthand scope value of the
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
    val moduleDependencies: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)

    /**
     * New version of moduleDependencies for 8.3+ which allows for the addition of a "required" flag to be specified.
     * Uses a builder to construct the property so the RequiredModuleDependency piece can be extrapolated away.
     * Note: This required flag will only be put in the XML if requiredIgnitionVersion is explicitly set to 8.3+.
     *
     * ### Examples:
     *
     * _Groovy_
     *   requiredModuleDependencies = [
     *      moduleId("com.inductiveautomation.vision") {
     *          it.scope = "GCD"
     *          it.required = true
     *      }
     *   ]
     *
     * _Kotlin_
     *   requiredModuleDependencies = setOf(
     *      moduleId("com.inductiveautomation.vision") {
     *          scope = "GCD"
     *          required = true
     *      }
     *   )
     */
    val requiredModuleDependencies: SetProperty<RequiredModuleDependency> = objects.setProperty(
        RequiredModuleDependency::class.java
    ).convention(
        moduleDependencies.map {
            it.map { (moduleId, scope) -> RequiredModuleDependency(moduleId, scope, false) }
        }
    )

    /**
     * Helper DSL function for requiredModuleDependencies.
     */
    fun moduleId(moduleId: String, block: ModuleDependencyBuilder.() -> Unit): RequiredModuleDependency =
        ModuleDependencyBuilder(moduleId).apply(block).build()

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
    val hooks: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)

    /**
     * If the module is a 'free' (non-commercial) module, without licensing restrictions, set this
     * value to `true`.  Default is `false`
     *
     * Optional
     */
    val freeModule: Property<Boolean> = objects.property(Boolean::class.javaObjectType)

    /**
     * The path to the .html file containing your license information.
     *
     * Optional
     */
    val license: Property<String> = objects.property(String::class.java)

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
    val requireFromPlatform: MapProperty<String, String> =
        objects.mapProperty(String::class.java, String::class.java)

    /**
     * Framework version.
     */
    val requiredFrameworkVersion: Property<String> = objects.property(String::class.java)

    /**
     * If the plugin should apply the inductive maven artifact repository as a source for
     */
    val applyInductiveArtifactRepo: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    /**
     * Algorithm used to generate a checksum digest for the signed module file.  Defaults to SHA-256.
     */
    val checksumAlgorithm: Property<HashAlgorithm> = objects.property(HashAlgorithm::class.java)
        .convention(HashAlgorithm.SHA256)

    /**
     * Files that will be added to the `docs/` directory inside the module.  Use [documentationIndex] to set the path
     * inside the `docs/` dir to the index.html file.
     */
    val documentationFiles: ConfigurableFileCollection = objects.fileCollection()

    /**
     * The path to the index html file for the optional module documentation , within the context of the assembled
     * module's 'docs' directory.  Default's to `index.html`, meaning that the resolved file would exist in the module
     * at `doc/index.html`.  Only gets used if there are one or more files associated with the [documentationFiles]
     * collection.
     */
    val documentationIndex: Property<String> = objects.property(String::class.java)

    /**
     * If `true` the signing task is not executed on the module and the unsigned module is used for all downstream
     * tasks. The default for this is `false` and should be in most scenarios. The exception being if you would like to
     * build an unsigned module for a development gateway without the signing keyfile present on the filesystem.
     */
    val skipModlSigning: Property<Boolean> = objects.property(Boolean::class.javaObjectType).convention(false)

    init {
        license.convention("")
        freeModule.convention(false)
        moduleDescription.convention("")
        requireFromPlatform.convention(emptyMap())
        requiredIgnitionVersion.convention("8.0.0")
        requiredFrameworkVersion.convention("8")
        fileName.convention(
            name.map {
                it.split(" ").joinToString(separator = "-", postfix = ".") { s ->
                    s.capitalize()
                } + ZipModule.UNSIGNED_EXTENSION
            }
        )
    }
}
