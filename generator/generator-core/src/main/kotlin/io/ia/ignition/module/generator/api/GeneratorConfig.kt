package io.ia.ignition.module.generator.api

import io.ia.ignition.module.generator.api.DefaultDependencies.GRADLE_VERSION
import io.ia.ignition.module.generator.api.GradleDsl.KOTLIN
import io.ia.ignition.module.generator.api.SourceFileType.JAVA
import java.nio.file.Path

/**
 * Configuration object that the generator uses to construct the module structure.  Create via the
 * [GeneratorConfigBuilder].
 */
data class GeneratorConfig constructor(
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
    val settingsDsl: GradleDsl = KOTLIN,

    /**
     * Which language/dsl the Gradle build files should be created with.
     */
    val buildDsl: GradleDsl = KOTLIN,

    /**
     * Which language the module should be written in.
     */
    val projectLanguage: SourceFileType = JAVA,

    /**
     * Version of gradle wrapper to create the project with.  Must be a version that has appropriate gradle scripts
     * and wrapper jars available in the resources.
     */
    val gradleWrapperVersion: String = GRADLE_VERSION,

    /**
     * Whether to inject debuggable (`mavenLocal()`) pluginManagament repository sources.
     */
    val debugPluginConfig: Boolean = false,

    /**
     * Value applied to the root plugin configuration.
     *
     * By default, a multi-scope (or single-scope with multi-project structure) root buildscript file will have only
     * the module plugin applied.  In a single directory/single scope project structure, it will also have the
     * appropriate java/kotlin plugins (`java-library` or `kotlin("jvm")`) applied, according to the project source
     * language.
     *
     * Setting this value to a non-empty string will override the default plugin configuration, replacing it entirely.
     */
    val rootPluginConfig: String = "",

    /**
     * Map of strings that will be replaced if found in any of the resources file templates used to create a module
     * project.  These will override any defaults or template replacement values, if present.  Care should be taken
     * so that these replacements don't result in errors by unintentionally overriding the TemplateReplacement values
     * used internal by the generator.
     *
     * Generally only used in testing and development.
     */
    val customReplacements: Map<String, String> = emptyMap(),

    /**
     * If a single Ignition scope is declared, the generator defaults to the more flexible structure of having an
     * independent subproject, even in the case of a single scope.  Set this value to `true` in order to create a
     * project which uses the root directory as the location for source code files as well.
     *
     * Example directory structures:
     * ```
     * // when value is set to true for a G scoped module
     *
     * my-project/
     *     src/
     *        main/
     *             java/.../MyProjectGatewayHook.java
     *             resources/someResource.jpg
     *     settings.gradle
     *     build.gradle
     *
     *
     * // default structure, when value set to false
     *
     * my-project/
     *     settings.gradle
     *     build.gradle
     *     gateway/
     *         src/
     *             main/
     *                 java/.../MyProjectGatewayHook.java
     *                 resources/something.jpg
     *         build.gradle
     * ```
     */
    val useRootProjectWhenSingleScope: Boolean = false,

    /**
     * Version of the module plugin to be used for the project.  Is excluded when 'dev mode' module structure is
     * generated, as it is assumed the plugin will be established via 'includeBuild' in settings.gradle
     * pluginManagement.
     */
    val modulePluginVersion: String = "0.4.0",

    /**
     * If signing the module should be required, set to false.  Set to true by default to allow building the
     * generated module without needing to establish signing certificate configuration.  Should be set to 'false'
     * in the generated project when building modules intended for production use.
     */
    val skipModuleSigning: Boolean = true

)
