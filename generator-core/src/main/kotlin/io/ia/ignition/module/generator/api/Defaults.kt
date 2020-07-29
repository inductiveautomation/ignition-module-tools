package io.ia.ignition.module.generator.api

object Defaults {
    // default values used for optional arguments
    const val PROP_FILE_DEFAULT_VALUE = "project.file(\"\${System.getProperty(\"user.home\")}/signing.properties\")"

    // default gradle version
    const val GRADLE_VERSION = "6.5.1"

    // default plugin configuration for the root build.gradle
    const val ROOT_PLUGIN_CONFIG = "id(\"io.ia.sdk.modl\")"

    /**
     * Default `dependencies` closure entries for gateway scoped projects/subprojects
     */
    const val GATEWAY_SCOPE_DEPENDENCIES: String = """
        compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:${'$'}{sdk_version}")
        compileOnly("com.inductiveautomation.ignitionsdk:gateway-api:${'$'}{sdk_version}")
    """

    /**
     * Default `dependencies` closure entries for vision client scoped projects/subprojects
     */
    const val CLIENT_SCOPE_DEPENDENCIES: String = """
        compileOnly("com.inductiveautomation.ignitionsdk:client-api:${'$'}{sdk_version}")
        compileOnly("com.inductiveautomation.ignitionsdk:vision-client-api:${'$'}{sdk_version}")
        compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:${'$'}{sdk_version}")
    """

    /**
     * Default `dependencies` closure entries for designer scoped projects/subprojects
     */
    const val DESIGNER_SCOPE_DEPENDENCIES: String = """
        compileOnly("com.inductiveautomation.ignitionsdk:designer-api:${'$'}{sdk_version}")
        compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:${'$'}{sdk_version}")
    """

    /**
     * Default `dependencies` closure entries for common projects/subprojects
     */
    const val COMMON_SCOPE_DEPENDENCIES: String = ""
}
