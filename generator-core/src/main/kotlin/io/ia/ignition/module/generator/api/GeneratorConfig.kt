package io.ia.ignition.module.generator.api

import io.ia.ignition.module.generator.api.GradleDsl.GROOVY
import io.ia.ignition.module.generator.api.SupportedLanguage.JAVA
import java.nio.file.Path
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Settings object that the generator uses to construct the module structure.   Create via calling the builder
 *
 * @constructor private as instances must be created via the ConfigBuilder
 * @param moduleName A human-readable name for the module.  Should only contain letter characters and spaces.
 * @param parentDir the path to the directory in which the module will be constructed.  Must already exist.
 * @param packageName the base package path, in reverse URL format consistent with Java/Kotlin package naming rules and
 *                    conventions
 * @param scopes string containing the scope
 *
 */
class GeneratorConfig constructor(
    /* NOTE - UPDATE .equals(), hashcode(), and toString() IF ANY CHANGE IS MADE TO THE MEMBERS OF THIS CLASS */
    /**
     * Name of the module
     */
    val moduleName: String,

    /**
     * The package name which all subprojects will be based on.
     */
    val packageName: String,

    /**
     * The shorthand initials for the scopes that this module should be created with
     */
    val scopes: String,

    /**
     * Where the new root directory for the module should be created.
     */
    val parentDir: Path,

    /**
     * Language the Gradle settings file should be created with.
     */
    val settingsDsl: GradleDsl = GROOVY,

    /**
     * Which language/dsl the Gradle build files should be created with.
     */
    val buildDsl: GradleDsl = GROOVY,

    /**
     * Which language the module should be written in.
     */
    val projectLanguage: SupportedLanguage = JAVA,

    /**
     * Version of gradle wrapper to create the project with.  Must be a version that has appropriate gradle scripts
     * and wrapper jars available in the resources.
     */
    val gradleWrapperVersion: String = GRADLE_VERSION,

    /**
     * Whether to inject debuggable (`mavenLocal()`) pluginMangament repository sources.
     */
    val debugPluginConfig: Boolean = false,

    /**
     * Value applied to the root plugin conifiguration.  Overrides the default (which includes only the modl plugin by
     * default)
     */
    val rootPluginConfig: String = ROOT_PLUGIN_CONFIG,

    /**
     * String value that will compile to and resolve a property file.
     * For example: "\"project.file('myprops.properties')\""
     **/
    val signingCredentialPropertyFile: String = PROP_FILE_DEFAULT_VALUE,

    /**
     * Map of strings that will be replaced if found in any of the resources file templates used to create a module
     * project.  These will override any defaults or template replacement values, if present.  Care should be taken
     * so that these replacements don't result in errors by unintentionally overriding the TemplateReplacement values
     * used internal by the generator.
     *
     * Generally only used in testing and development.
     */
    val customReplacements: Map<String, String> = emptyMap()
) {

    companion object {
        val log: Logger = LoggerFactory.getLogger("GeneratorConfiguration")

        // default values used for optional arguments
        const val PROP_FILE_DEFAULT_VALUE = "project.file(\"\${System.getProperty(\"user.home\")}/signing.properties\")"
        const val GRADLE_VERSION = "6.5"
        const val ROOT_PLUGIN_CONFIG = "id('io.ia.sdk.modl')"
    }

    override fun toString(): String {
        return """\
            |GeneratorConfig(
            |  buildDsl=$buildDsl
            |  debugPluginConfig=$debugPluginConfig
            |  gradleWrapperVersion=$gradleWrapperVersion
            |  moduleName='$moduleName'
            |  packageName='$packageName' 
            |  parentDir=$parentDir
            |  projectLanguage=$projectLanguage
            |  scopes='$scopes'
            |  settingsDsl=$settingsDsl
            |  rootPluginConfig=$rootPluginConfig
            |  signingCredentialPropertyFile=$signingCredentialPropertyFile
            |  customReplacements=$customReplacements
            |)""".trimMargin("|")
    }

    override fun equals(other: Any?): Boolean =
        (other is GeneratorConfig) &&
            buildDsl == other.buildDsl &&
            moduleName == other.moduleName &&
            packageName == other.packageName &&
            parentDir == other.parentDir &&
            projectLanguage == other.projectLanguage &&
            rootPluginConfig == other.rootPluginConfig &&
            scopes == other.scopes &&
            settingsDsl == other.settingsDsl &&
            debugPluginConfig == other.debugPluginConfig &&
            gradleWrapperVersion == other.gradleWrapperVersion &&
            signingCredentialPropertyFile == other.signingCredentialPropertyFile &&
            customReplacements == other.customReplacements

    override fun hashCode(): Int {
        var result = moduleName.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + gradleWrapperVersion.hashCode()
        result = 31 * result + scopes.hashCode()
        result = 31 * result + parentDir.hashCode()
        result = 31 * result + settingsDsl.hashCode()
        result = 31 * result + buildDsl.hashCode()
        result = 31 * result + projectLanguage.hashCode()
        result = 31 * result + debugPluginConfig.hashCode()
        result = 31 * result + rootPluginConfig.hashCode()
        result = 31 * result + signingCredentialPropertyFile.hashCode()
        result = 31 * result + customReplacements.hashCode()
        return result
    }
}
