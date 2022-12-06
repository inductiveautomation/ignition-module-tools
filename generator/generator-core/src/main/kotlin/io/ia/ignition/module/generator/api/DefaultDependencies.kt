package io.ia.ignition.module.generator.api

import io.ia.ignition.module.generator.api.TemplateMarker.SDK_VERSION_PLACEHOLDER

object DefaultDependencies {

    // default gradle version
    const val GRADLE_VERSION = "7.5.1"

    // default plugin configuration for the root build.gradle
    val MODL_PLUGIN: String = "id(\"io.ia.sdk.modl\") version(\"${TemplateMarker.MODL_PLUGIN_VERSION.key}\")"

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

    fun Set<String>.toDependencyFormat(
        dsl: GradleDsl,
        configuration: String = "compileOnly"
    ): String {
        return map { artifact ->
            val version = dsl.artifactSdkVersion()
            "$configuration(\"${artifact.replace(SDK_VERSION_PLACEHOLDER.toString(), version)}\")"
        }.joinToString(separator = "\n    ")
    }
}
