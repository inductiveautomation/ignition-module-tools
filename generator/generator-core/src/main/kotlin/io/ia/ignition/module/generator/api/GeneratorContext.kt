package io.ia.ignition.module.generator.api

import java.nio.file.Path

interface GeneratorContext {
    val config: GeneratorConfig

    /**
     * Map of [TemplateMarker.key] to appropriate replacement value
     */
    fun getTemplateReplacements(): Map<String, String>

    /**
     * The root 'workspace' directory, aka where the settings.gradle and root build.gradle will be created
     */
    fun getRootDirectory(): Path

    /**
     * The name of the class hook, generated from the scope and the class prefix such that a 'gateway' scope, and a
     * validated module name of 'Splitting Historizer' would resolve to _SplittingHistorizerGatewayHook_
     */
    fun getHookClassName(scope: ProjectScope): String

    /**
     * Returns the valid class name created from the validated module name.
     *
     * ### Examples:
     *
     * * _Some Functionality_ becomes _SomeFunctionality_
     * * _some functionality_ becomes _SomeFunctionality_
     * * _Do The 123 Things_ becomes _DoThe123Things_
     * * _I do cool stuff_ becomes _IDoCoolStuff_
     * * _More Test Strings_ becomes _MoreTestStrings_
     *
     */
    fun getClassPrefix(): String

    /**
     * The name of the build file, generally determined by the build script type set in the [GeneratorConfig]
     */
    fun getBuildScriptFilename(): String

    /**
     * The name of the build file, generally determined by the settings script type set in the [GeneratorConfig]
     */
    fun getSettingFileName(): String

    /**
     * Returns the resource path for the boilerplate stub implementation of an Ignition hook class, or in the case of
     * a 'common' project, a simple empty `<moduleName>Module.java`.
     *
     * @param scope for the hook/stub class being generated
     */
    fun getHookResourcePath(scope: ProjectScope): String

    /**
     * Returns the module id, which by default is "[GeneratorConfig.packageName] + '.' + [getClassPrefix]"
     */
    fun getModuleId(): String
}
