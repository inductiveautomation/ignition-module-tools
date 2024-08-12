package io.ia.ignition.module.generator.api

import io.ia.ignition.module.generator.api.DefaultDependencies.GRADLE_VERSION
import io.ia.ignition.module.generator.api.GradleDsl.GROOVY
import io.ia.ignition.module.generator.api.SourceFileType.JAVA
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path

class GeneratorConfigBuilder {
    companion object {
        @JvmStatic
        val log: Logger = LoggerFactory.getLogger("GeneratorConfigBuilder")
    }

    private lateinit var moduleName: String
    private lateinit var packageName: String
    private lateinit var parentDir: Path
    private lateinit var scopes: String
    private var customReplacements: Map<String, String> = emptyMap()
    private var buildDsl: GradleDsl = GROOVY
    private var projectLanguage: SourceFileType = JAVA
    private var settingsDsl: GradleDsl = GROOVY
    private var gradleWrapperVersion: String = GRADLE_VERSION
    private var debugPluginConfig: Boolean = false
    private var rootPluginConfig: String = ""
    private var useRootForSingleProjectScope: Boolean = false
    private var modulePluginVersion: String = "0.4.0-SNAPSHOT"
    private var allowUnsignedModules: Boolean = false

    // builder methods
    fun buildscriptDsl(buildDsl: GradleDsl) = apply { this.buildDsl = buildDsl }
    fun debugPluginConfig(enable: Boolean) = apply { this.debugPluginConfig = enable }
    fun gradleWrapperVersion(version: String) = apply { this.gradleWrapperVersion = version }
    fun moduleName(name: String?) = apply { this.moduleName = name ?: "Example" }
    fun packageName(packageName: String?) = apply { this.packageName = packageName ?: "le.examp" }
    fun parentDir(dir: Path?) = apply { this.parentDir = dir ?: File("").toPath() }
    fun projectLanguage(language: String) = apply {
        this.projectLanguage = SourceFileType.valueOf(language.lowercase())
    }

    fun rootPluginConfig(config: String) = apply { this.rootPluginConfig = config }
    fun scopes(scopes: String?) = apply { this.scopes = scopes ?: "" }
    fun settingsDsl(settingsDsl: GradleDsl) = apply { this.settingsDsl = settingsDsl }

    fun customReplacements(customReplacements: Map<String, String>) = apply {
        this.customReplacements = customReplacements
    }

    fun useRootForSingleScopeProject(value: Boolean) = apply {
        this.useRootForSingleProjectScope = value
    }

    fun allowUnsignedModules(allow: Boolean = true) = apply {
        this.allowUnsignedModules = allow
    }

    // creates the config object
    fun build(): GeneratorConfig {
        log.debug("Construction GeneratorConfig...")
        return GeneratorConfig(
            moduleName = moduleName,
            packageName = packageName,
            scopes = scopes,
            parentDir = parentDir,
            settingsDsl = settingsDsl,
            buildDsl = buildDsl,
            projectLanguage = projectLanguage,
            gradleWrapperVersion = gradleWrapperVersion,
            debugPluginConfig = debugPluginConfig,
            rootPluginConfig = rootPluginConfig,
            customReplacements = this.customReplacements,
            useRootProjectWhenSingleScope = useRootForSingleProjectScope,
            modulePluginVersion = modulePluginVersion,
            skipModuleSigning = allowUnsignedModules
        )
    }
}
