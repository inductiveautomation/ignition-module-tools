package io.ia.ignition.module.generator.api

import io.ia.ignition.module.generator.api.ProjectScope.CLIENT
import io.ia.ignition.module.generator.api.ProjectScope.COMMON
import io.ia.ignition.module.generator.api.ProjectScope.DESIGNER
import io.ia.ignition.module.generator.api.ProjectScope.GATEWAY

/**
 * Markers used in the various template files that are string replaced during assembly in order to create the
 * appropriately named project elements.
 *
 * The values are set during initialization of the [GeneratorContext], according to data derived from the
 * [GeneratorConfig]
 */
enum class TemplateMarker(val key: String) {

    /**
     * The 'plain text' name of the module as displayed in the gateway module configuration pages
     */
    MODULE_NAME("<MODULE_NAME>"),

    /**
     * Name of the modl file that is created
     */
    MODULE_FILENAME("<MODULE_FILENAME>"),

    /**
     * Value that is replaced by the 'valid class name' formatted name
     */
    MODULE_CLASSNAME("<MODULE_CLASSNAME>"),

    /**
     * Value that is replace by the 'package root' value provided to the Config by the creator
     */
    PACKAGE_ROOT("<PACKAGE_ROOT>"),

    /**
     * Value that is replaced by the derived Module Id
     */
    MODULE_ID("<MODULE_ID>"),

    /**
     * Value that is replaced by the appropriate array of projects and scopes
     */
    PROJECT_SCOPES("<PROJECT_SCOPE_CONFIG>"),

    /**
     * Value that is replaced by the name of the root project, e.g. value for _rootProject.name = 'my-project'_ in the
     * settings.gradle.
     */
    ROOT_PROJECT_NAME("<ROOT_PROJECT_NAME>"),

    /**
     * Value that is replaced by a list of comma-separated Strings that consist of each subproject that is
     * registered by the settings.gradle or settings.gradle.kts file (syntax as appropriate)
     */
    SUBPROJECT_INCLUDES("<SUBPROJECT_INCLUDES>"),

    /**
     * Value replaced by the appropriate hook class references for each scope.
     */
    HOOK_CLASS_CONFIG("<HOOK_CLASS_CONFIG>"),

    /**
     * Value at the head of the settings.gradle file, to allow injecting things into the first line of the file
     */
    SETTINGS_HEADER("//<SETTINGS_HEADER>"),

    /*
     * The plugin configuration to apply to the root project of the generated module.
     */
    ROOT_PLUGIN_CONFIGURATION("<ROOT_PLUGIN_CONFIGURATION>"),

    /**
     * Value used in the `dependencies { }` block of client scoped subproject
     */
    CLIENT_DEPENDENCIES("//<CLIENT_DEPENDENCIES>"),

    /**
     * Value used in the `dependencies` block of a common scoped subproject
     */
    COMMON_DEPENDENCIES("//<COMMON_DEPENDENCIES>"),

    /**
     * Value used in the `dependencies { }` block of gateway scoped subproject
     */
    GATEWAY_DEPENDENCIES("//<GATEWAY_DEPENDENCIES>"),

    /**
     * Value used in the `dependencies { }` block of designer scoped subproject
     */
    DESIGNER_DEPENDENCIES("//<DESIGNER_DEPENDENCIES>"),

    JAVA_TOOLING_CONFIG("//<JAVA_TOOLING>"),

    SKIP_SIGNING_CONFIG("//<SKIP_MODULE_SIGNING>"),

    MODL_PLUGIN_VERSION("<MODL_PLUGIN_VERSION>"),

    SDK_VERSION_PLACEHOLDER("<SDK_VERSION>");

    fun keys(): List<String> {
        return values().map { it.key }
    }

    override fun toString(): String {
        return key
    }

    companion object {
        fun dependencyKeyForScope(scope: ProjectScope): TemplateMarker? {
            return when (scope) {
                CLIENT -> TemplateMarker.CLIENT_DEPENDENCIES
                DESIGNER -> TemplateMarker.DESIGNER_DEPENDENCIES
                GATEWAY -> TemplateMarker.GATEWAY_DEPENDENCIES
                COMMON -> TemplateMarker.COMMON_DEPENDENCIES
                else -> null
            }
        }
    }
}
