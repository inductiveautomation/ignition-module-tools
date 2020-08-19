package io.ia.ignition.module.generator.data

import io.ia.ignition.module.generator.api.Defaults
import io.ia.ignition.module.generator.api.GeneratorConfig
import io.ia.ignition.module.generator.api.GeneratorContext
import io.ia.ignition.module.generator.api.GradleDsl
import io.ia.ignition.module.generator.api.ProjectScope
import io.ia.ignition.module.generator.api.ProjectScope.CLIENT
import io.ia.ignition.module.generator.api.ProjectScope.COMMON
import io.ia.ignition.module.generator.api.ProjectScope.DESIGNER
import io.ia.ignition.module.generator.api.ProjectScope.GATEWAY
import io.ia.ignition.module.generator.api.SupportedLanguage.JAVA
import io.ia.ignition.module.generator.api.SupportedLanguage.KOTLIN
import io.ia.ignition.module.generator.api.TemplateMarker
import io.ia.ignition.module.generator.util.logger
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Object which holds all the fully-validated state that may be used/useful in creating a module
 */
class ModuleGeneratorContext(override val config: GeneratorConfig) : GeneratorContext {
    private val scopes: List<ProjectScope> = ProjectScope.scopesFromShorthand(config.scopes)
    private val effectiveScopes: List<ProjectScope> = ProjectScope.effectiveScopesFromShorthand(config.scopes)
    private val replacements = mutableMapOf<String, String>()
    private val classPrefix: String = config.moduleName.split(" ")
        .joinToString("") { it.capitalize() }

    private val rootFolderName: String by lazy {
        config.moduleName.split(" ").joinToString("-") { it.toLowerCase() }
    }

    init {
        // initialize the values that will be injected into the template resource files
        replacements[TemplateMarker.MODULE_NAME.key] = config.moduleName
        replacements[TemplateMarker.MODULE_FILENAME.key] =
            "${config.moduleName.replace(" ", "-")}.modl"
        replacements[TemplateMarker.MODULE_ID.key] = "${config.packageName}.$classPrefix"
        replacements[TemplateMarker.MODULE_CLASSNAME.key] = classPrefix
        replacements[TemplateMarker.PACKAGE_ROOT.key] = config.packageName
        replacements[TemplateMarker.PROJECT_SCOPES.key] = buildProjectScopeConfiguration()
        replacements[TemplateMarker.ROOT_PROJECT_NAME.key] = rootFolderName
        replacements[TemplateMarker.SUBPROJECT_INCLUDES.key] =
            // if a single scope, single folder project, we don't have subprojects to include
            if (config.scopes.length == 1 && config.useRootProjectWhenSingleScope) {
                "\":\""
            } else {
                "\":\",\n" + effectiveScopes.joinToString(
                    separator = ",\n    ",
                    prefix = "    ",
                    postfix = ""
                ) { "\":${it.folderName}\"" }
            }
        replacements[TemplateMarker.HOOK_CLASS_CONFIG.key] = buildHookEntry()
        replacements[TemplateMarker.SETTINGS_HEADER.key] = if (!config.debugPluginConfig) "" else {
            """
                |pluginManagement {
                |    repositories {
                |        mavenLocal()
                |        gradlePluginPortal()
                |    }
                |}
            """.trimMargin("|")
        }
        replacements[TemplateMarker.ROOT_PLUGIN_CONFIGURATION.key] = when {
            config.rootPluginConfig.isNotEmpty() -> {
                config.rootPluginConfig
            }
            isSingleDirProject() -> {
                logger.info("Detected single dir project, applying jvm plugins to root buildscript...")
                when (config.projectLanguage) {
                    JAVA -> Defaults.ROOT_PLUGIN_CONFIG + "\n    id(\"java-library\")"
                    KOTLIN -> Defaults.ROOT_PLUGIN_CONFIG + "\n    `java-library`\n    " +
                        "id(\"org.jetbrains.kotlin.jvm\") version \"1.4.0\""
                }
            }
            else -> {
                Defaults.ROOT_PLUGIN_CONFIG
            }
        }
        replacements[TemplateMarker.SIGNING_PROPERTY_FILE.key] = config.signingCredentialPropertyFile
        replacements[TemplateMarker.CLIENT_DEPENDENCIES.key] = Defaults.CLIENT_SCOPE_DEPENDENCIES
        replacements[TemplateMarker.DESIGNER_DEPENDENCIES.key] = Defaults.DESIGNER_SCOPE_DEPENDENCIES
        replacements[TemplateMarker.GATEWAY_DEPENDENCIES.key] = Defaults.GATEWAY_SCOPE_DEPENDENCIES

        // this is a quick hack to support arbitrary replacements for resource files.  Works for now as all formal
        // template replacements are enclosed in < > characters, making collisions unlikely.
        if (config.customReplacements.isNotEmpty()) {
            config.customReplacements.forEach { (k, v) -> replacements[k] = v }
        }
    }

    private fun buildHookEntry(): String {
        val hookEntry = scopes.map {
            logger.trace("Creating module hook configuration entry for '$it' scope")
            when (it) {
                DESIGNER -> "\"${config.packageName}.designer.${getHookClassName(it)}\" ${associator()} \"${it.name.first().toUpperCase()}\""
                CLIENT -> "\"${config.packageName}.client.${getHookClassName(it)}\" ${associator()} \"${it.name.first().toUpperCase()}\""
                GATEWAY -> "\"${config.packageName}.gateway.${getHookClassName(it)}\" ${associator()} \"${it.name.first().toUpperCase()}\""
                else -> {
                    logger.warn("Unknown scope '$it', hook configuration entry skipped.")
                    null
                }
            }
        }.joinToString(prefix = "    ", separator = ",\n        ")

        logger.debug("Created hook entry configuration of:\n$hookEntry")
        return hookEntry
    }

    private fun associator(): String {
        return when (config.buildDsl) {
            GradleDsl.KOTLIN -> "to"
            GradleDsl.GROOVY -> ":"
        }
    }

    /**
     * Emits a String that is a valid project scope configuration, consistent with the buildscript dsl type.
     */
    private fun buildProjectScopeConfiguration(): String {
        val associator = associator()

        // single dir project will only have a single scope, so just return the initial
        if (isSingleDirProject()) {
            val scopeName = scopes[0].name

            return "\":\"" + associator + " \"${scopeName[0]}\""
        }

        // create the string to populate the `scopes` in the ignitionModule configuration DSL, each of which being
        // a literal value for a Map<org.gradle.api.Project, String> element:
        // in groovy: `":folder": "<scope abbreviation>"`
        // kotlin: `":folder" to "<scope abbreviation>"`
        return effectiveScopes.map { ps ->
            val scope = when (ps) {
                // common should be available in all scopes the project is created for
                COMMON -> scopes.joinToString(separator = "") { sc ->
                    sc.folderName[0].toUpperCase().toString()
                }
                // client scope implies availability in the designer, if designer scope is present
                CLIENT -> if (scopes.contains(DESIGNER)) "CD" else "C"
                else -> {
                    ps.folderName[0].toUpperCase().toString()
                }
            }

            "\":${ps.folderName}\" $associator \"$scope\""
        }.sorted().joinToString(separator = ",\n        ", prefix = "    ", postfix = "")
    }

    override fun getTemplateReplacements(): Map<String, String> {
        return replacements.toMap()
    }

    private val rootDirectory: Path = Paths.get(config.parentDir.toString(), rootFolderName).toAbsolutePath()

    private val buildScriptFilename: String =
        if (config.buildDsl == GradleDsl.GROOVY) "build.gradle" else "build.gradle.kts"

    private val settingsFileName: String =
        if (config.settingsDsl == GradleDsl.GROOVY) "settings.gradle" else "settings.gradle.kts"

    override fun getRootDirectory(): Path {
        return rootDirectory
    }

    // if the root project dir is also the only project dir and should allow sourcecode files
    fun isSingleDirProject(): Boolean {
        return scopes.size == 1 && config.useRootProjectWhenSingleScope
    }

    override fun getHookClassName(scope: ProjectScope): String {
        return when (scope) {
            DESIGNER -> "${getClassPrefix()}DesignerHook"
            GATEWAY -> "${getClassPrefix()}GatewayHook"
            CLIENT -> "${getClassPrefix()}ClientHook"
            COMMON -> "${getClassPrefix()}Module"
        }
    }

    override fun getClassPrefix(): String {
        return classPrefix
    }

    override fun getBuildScriptFilename(): String {
        return buildScriptFilename
    }

    override fun getSettingFileName(): String {
        return settingsFileName
    }

    /**
     * Returns the resource path for the boilerplate stub implementation of an Ignition hook class, or in the case of
     * a 'common' project, a simple empty `<moduleName>Module.java`.
     *
     * @param scope for the hook/stub class being generated
     */
    override fun getHookResourcePath(scope: ProjectScope): String {
        val fileExtension = config.projectLanguage.sourceCodeFileExtension()

        return if (scope == COMMON) {
            "hook/Module.$fileExtension"
        } else {
            "hook/${scope.name.toLowerCase().capitalize()}Hook.$fileExtension"
        }
    }

    /**
     * Returns the resource path for the appropriate gradle settings file template
     */
    fun settingsFilename(): String {
        return config.settingsDsl.settingsFilename()
    }

    override fun getModuleId(): String {
        return "${config.packageName}.${getClassPrefix().toLowerCase()}"
    }
}
