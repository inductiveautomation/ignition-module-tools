package io.ia.ignition.module.generator.data

import io.ia.ignition.module.generator.api.DefaultDependencies
import io.ia.ignition.module.generator.api.DefaultDependencies.toDependencyFormat
import io.ia.ignition.module.generator.api.GeneratorConfig
import io.ia.ignition.module.generator.api.GeneratorContext
import io.ia.ignition.module.generator.api.GradleDsl
import io.ia.ignition.module.generator.api.GradleDsl.GROOVY
import io.ia.ignition.module.generator.api.ProjectScope
import io.ia.ignition.module.generator.api.ProjectScope.CLIENT
import io.ia.ignition.module.generator.api.ProjectScope.COMMON
import io.ia.ignition.module.generator.api.ProjectScope.DESIGNER
import io.ia.ignition.module.generator.api.ProjectScope.GATEWAY
import io.ia.ignition.module.generator.api.SourceFileType.JAVA
import io.ia.ignition.module.generator.api.SourceFileType.KOTLIN
import io.ia.ignition.module.generator.api.TemplateMarker
import io.ia.ignition.module.generator.api.TemplateMarker.HOOK_CLASS_CONFIG
import io.ia.ignition.module.generator.api.TemplateMarker.MODULE_CLASSNAME
import io.ia.ignition.module.generator.api.TemplateMarker.MODULE_FILENAME
import io.ia.ignition.module.generator.api.TemplateMarker.MODULE_ID
import io.ia.ignition.module.generator.api.TemplateMarker.MODULE_NAME
import io.ia.ignition.module.generator.api.TemplateMarker.PACKAGE_ROOT
import io.ia.ignition.module.generator.api.TemplateMarker.PROJECT_SCOPES
import io.ia.ignition.module.generator.api.TemplateMarker.ROOT_PLUGIN_CONFIGURATION
import io.ia.ignition.module.generator.api.TemplateMarker.ROOT_PROJECT_NAME
import io.ia.ignition.module.generator.api.TemplateMarker.SETTINGS_HEADER
import io.ia.ignition.module.generator.api.TemplateMarker.SUBPROJECT_INCLUDES
import io.ia.ignition.module.generator.util.capitalize
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
        .joinToString("") { name -> name.capitalize() }

    private val rootFolderName: String by lazy {
        config.moduleName.split(" ").joinToString("-") { it.lowercase() }
    }

    init {
        // initialize the values that will be injected into the template resource files
        replacements[MODULE_NAME.key] = config.moduleName
        replacements[MODULE_FILENAME.key] = config.moduleName.replace(" ", "-")
        replacements[MODULE_ID.key] = "${config.packageName}.$classPrefix"
        replacements[MODULE_CLASSNAME.key] = classPrefix
        replacements[PACKAGE_ROOT.key] = config.packageName
        replacements[PROJECT_SCOPES.key] = buildProjectScopeConfiguration()
        replacements[ROOT_PROJECT_NAME.key] = rootFolderName
        replacements[SUBPROJECT_INCLUDES.key] =
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
        replacements[HOOK_CLASS_CONFIG.key] = buildHookEntry()

        replacements[TemplateMarker.SKIP_SIGNING_CONFIG.key] = config.buildDsl.skipSigningConfig(config.skipModuleSigning)
        settingsHeaderReplacement()
        rootPluginReplacement()
        // populate the dependency replacements
        replacements.putAll(buildDependencyEntries(effectiveScopes))

        // this is a quick hack to support arbitrary replacements for resource files.  Works for now as all formal
        // template replacements are enclosed in < > characters, making collisions unlikely.
        if (config.customReplacements.isNotEmpty()) {
            config.customReplacements.forEach { (k, v) -> replacements[k] = v }
        }
    }

    private fun buildDependencyEntries(scopes: List<ProjectScope>): Map<String, String> {
        return mutableMapOf<String, String>().apply {
            scopes.forEach { scope ->
                TemplateMarker.dependencyKeyForScope(scope)?.let { tm ->
                    this[tm.key] = DefaultDependencies.ARTIFACTS[scope]?.toDependencyFormat(config.buildDsl) ?: ""
                    // if not a common scope and there is a common project, add it as a dependency to other scopes
                    if (scope != COMMON && scopes.size > 1) {
                        this[tm.key] = "${this[tm.key]}\n    compileOnly(project(\":common\"))"
                    }
                }
            }
        }
    }

    /**
     * Establishes the pluginManagement, so that the plugin can resolve the module signer lib from nexus
     */
    private fun settingsHeaderReplacement() {
        replacements[SETTINGS_HEADER.key] = """
                |pluginManagement {
                |    repositories {
                |        gradlePluginPortal()
                |        maven {
                |            url = uri("https://nexus.inductiveautomation.com/repository/public")
                |        }
                |    }
                |}
        """.trimMargin("|").let { pluginBlock ->
            var block = if (config.settingsDsl == GROOVY) {
                pluginBlock.replace(
                    """url = uri("https://nexus.inductiveautomation.com/repository/public")""",
                    """url = "https://nexus.inductiveautomation.com/repository/public" """
                )
            } else pluginBlock

            block.let {
                if (config.debugPluginConfig) {
                    it.replace(
                        "gradlePluginPortal()",
                        "mavenLocal()\n        gradlePluginPortal()\n"
                    )
                } else it
            }
        }
    }

    /**
     *  Populates the replacement map with the plugin entries.  If the the rootPluginConfig is provided by the
     *  [GeneratorConfig] then it will be used, otherwise we fall back to just applying the modl plugin.  We also
     *  apply java-lib plugin if we're dealing with a single-directory project (where the root directory has sources
     *  for a single scope module).
     */
    private fun rootPluginReplacement() {
        replacements[ROOT_PLUGIN_CONFIGURATION.key] = config.rootPluginConfig.ifEmpty {
            DefaultDependencies.MODL_PLUGIN.replace(
                TemplateMarker.MODL_PLUGIN_VERSION.key,
                config.modulePluginVersion
            )
        } + when {
            isSingleDirProject() -> {
                logger.info("Detected single dir project, applying jvm plugins to root buildscript...")
                when (config.projectLanguage) {
                    JAVA -> "\n    id(\"java-library\")"
                    KOTLIN -> if (config.buildDsl == GradleDsl.KOTLIN) {
                        "`java-library`\n    kotlin(\"jvm\") version(\"1.6.21\")"
                    } else "id(\"java-library\")\n    id(\"org.jetbrains.kotlin.jvm\") version \"1.6.21\""
                    else -> ""
                }
            }
            else -> ""
        }
    }

    private fun buildHookEntry(): String {
        val hookEntry = scopes.map {
            logger.trace("Creating module hook configuration entry for '$it' scope")
            val scopeInitial = it.name.first().uppercaseChar()
            val pkgName = config.packageName
            when (it) {
                DESIGNER -> "\"$pkgName.designer.${getHookClassName(it)}\" ${associator()} \"$scopeInitial\""
                CLIENT -> "\"$pkgName.client.${getHookClassName(it)}\" ${associator()} \"$scopeInitial\""
                GATEWAY -> "\"$pkgName.gateway.${getHookClassName(it)}\" ${associator()} \"$scopeInitial\""
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
        return config.buildDsl.mapAssociator()
    }

    /**
     * Emits a String that is a valid project scope configuration, consistent with the buildscript dsl type.
     */
    private fun buildProjectScopeConfiguration(): String {

        // single dir project will only have a single scope, so just return the initial
        if (isSingleDirProject()) {
            val scopeName = scopes[0].name

            return "\":\" ${associator()} \"${scopeName[0]}\""
        }

        // create the string to populate the `scopes` in the ignitionModule configuration DSL, each of which being
        // a literal value for a Map<org.gradle.api.Project, String> element:
        // in groovy: `":folder": "<scope abbreviation>"`
        // kotlin: `":folder" to "<scope abbreviation>"`
        return effectiveScopes.map { ps ->
            val scope = when (ps) {
                // common should be available in all scopes the project is created for
                COMMON -> scopes.joinToString(separator = "") { sc ->
                    sc.folderName[0].uppercaseChar().toString()
                }
                // client scope implies availability in the designer, if designer scope is present
                CLIENT -> if (scopes.contains(DESIGNER)) "CD" else "C"
                else -> {
                    ps.folderName[0].uppercaseChar().toString()
                }
            }

            "\":${ps.folderName}\" ${associator()} \"$scope\""
        }.sorted().joinToString(separator = ",\n        ", prefix = "    ", postfix = "")
    }

    override fun getTemplateReplacements(): Map<String, String> {
        return replacements.toMap()
    }

    private val rootDirectory: Path = Paths.get(config.parentDir.toString(), rootFolderName).toAbsolutePath()

    private val buildScriptFilename: String = config.buildDsl.buildScriptFilename()

    private val settingsFileName: String =
        if (config.settingsDsl == GROOVY) "settings.gradle" else "settings.gradle.kts"

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
            else -> throw Exception("Generator encounted unknown Project Scope '$scope'!")
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
            "hook/${scope.name.lowercase().capitalize()}Hook.$fileExtension"
        }
    }

    /**
     * Returns the resource path for the appropriate gradle settings file template
     */
    fun settingsFilename(): String {
        return config.settingsDsl.settingsFilename()
    }

    override fun getModuleId(): String {
        return "${config.packageName}.${getClassPrefix().lowercase()}"
    }
}
