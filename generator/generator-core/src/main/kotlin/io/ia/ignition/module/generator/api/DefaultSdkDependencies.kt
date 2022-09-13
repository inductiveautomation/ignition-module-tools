package io.ia.ignition.module.generator.api

object DefaultSdkDependencies {

    // default gradle version
    const val GRADLE_VERSION = "7.2"

    // default plugin configuration for the root build.gradle
    const val ROOT_PLUGIN_CONFIG = "id(\"io.ia.sdk.modl\")"

    const val SDK_VERSION_PLACEHOLDER = "<SDK_VERSION>"

    // example
    // "com.inductiveautomation.ignitionsdk:client-api:${'$'}{sdk_version}"
    val ARTIFACTS: Map<ProjectScope, Set<String>> = mapOf(
        ProjectScope.CLIENT to setOf(
            "com.inductiveautomation.ignitionsdk:client-api:$SDK_VERSION_PLACEHOLDER",
            "com.inductiveautomation.ignitionsdk:vision-client-api:$SDK_VERSION_PLACEHOLDER",
            "com.inductiveautomation.ignitionsdk:ignition-common:$SDK_VERSION_PLACEHOLDER"
        ),
        ProjectScope.COMMON to setOf(
            "com.inductiveautomation.ignitionsdk:ignition-common:$SDK_VERSION_PLACEHOLDER"
        ),
        ProjectScope.DESIGNER to setOf(
            "com.inductiveautomation.ignitionsdk:designer-api:$SDK_VERSION_PLACEHOLDER",
            "com.inductiveautomation.ignitionsdk:ignition-common:$SDK_VERSION_PLACEHOLDER"
        ),
        ProjectScope.GATEWAY to setOf(
            "com.inductiveautomation.ignitionsdk:ignition-common:$SDK_VERSION_PLACEHOLDER",
            "com.inductiveautomation.ignitionsdk:gateway-api:$SDK_VERSION_PLACEHOLDER"
        )
    )

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
