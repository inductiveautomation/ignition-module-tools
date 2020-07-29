package io.ia.ignition.module.generator.api

import io.ia.ignition.module.generator.api.Defaults.GRADLE_VERSION
import io.ia.ignition.module.generator.api.Defaults.PROP_FILE_DEFAULT_VALUE
import io.ia.ignition.module.generator.api.GradleDsl.GROOVY
import io.ia.ignition.module.generator.api.SupportedLanguage.JAVA
import java.io.File
import java.nio.file.Path
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GeneratorConfigBuilder {
    companion object {
        val log: Logger = LoggerFactory.getLogger("GeneratorConfigBuilder")
    }

    /* NOTE - UPDATE .equals(), hashcode(), and toString() IF ANY CHANGE IS MADE TO THE MEMBERS OF THIS CLASS */
    private lateinit var moduleName: String
    private lateinit var packageName: String
    private lateinit var parentDir: Path
    private lateinit var scopes: String
    private var customReplacments: Map<String, String> = emptyMap()
    private var buildDsl: GradleDsl = GROOVY
    private var projectLanguage: SupportedLanguage = JAVA
    private var settingsDsl: GradleDsl = GROOVY
    private var gradleWrapperVersion: String = GRADLE_VERSION
    // todo: default to false for stable release
    private var debugPluginConfig: Boolean = true
    private var rootPluginConfig: String = ""
    private var signingCredentialPropertyFile: String = PROP_FILE_DEFAULT_VALUE
    private var useRootForSingleProjectScope: Boolean = false

    // builder methods
    fun buildDSL(buildDsl: GradleDsl) = apply { this.buildDsl = buildDsl }
    fun debugPluginConfig(enable: Boolean) = apply { this.debugPluginConfig = enable }
    fun gradleWrapperVersion(version: String) = apply { this.gradleWrapperVersion = version }
    fun moduleName(name: String?) = apply { this.moduleName = name ?: "Example" }
    fun packageName(packageName: String?) = apply { this.packageName = packageName ?: "le.examp" }
    fun parentDir(dir: Path?) = apply { this.parentDir = dir ?: File("").toPath() }
    fun projectLanguage(language: String) = apply {
        this.projectLanguage = SupportedLanguage.valueOf(language.toLowerCase())
    }
    fun rootPluginConfig(config: String) = apply { this.rootPluginConfig = config }
    fun scopes(scopes: String?) = apply { this.scopes = scopes ?: "" }
    fun settingsDSL(settingsDsl: GradleDsl) = apply { this.settingsDsl = settingsDsl }
    fun signingCredentialPropertyFile(signingCredentialPropertyFile: String) = apply {
        this.signingCredentialPropertyFile = signingCredentialPropertyFile
    }
    // map of scope
    fun customReplacements(customReplacements: Map<String, String>) = apply {
        this.customReplacments = customReplacements
    }
    fun useRootForSingleScopeProject(value: Boolean) = apply {
        this.useRootForSingleProjectScope = value
    }
    // creates the config object
    fun build(): GeneratorConfig {
        log.debug("Construction GeneratorConfig...")
        return GeneratorConfig(moduleName, packageName, scopes, parentDir, settingsDsl, buildDsl, projectLanguage,
            gradleWrapperVersion, debugPluginConfig, rootPluginConfig, signingCredentialPropertyFile,
            this.customReplacments, useRootForSingleProjectScope)
    }
}
