package io.ia.ignition.module.generator.api

object DefaultDependencies {

    // default gradle version
    const val GRADLE_VERSION = "9.3.1"

    // default Ignition SDK / platform version for generated projects (matches ignition-sdk-examples 8.3 line)
    const val IGNITION_SDK_VERSION = "8.3.0"

    // default plugin configuration for the root build.gradle
    val MODL_PLUGIN: String = "id(\"io.ia.sdk.modl\") version(\"${TemplateMarker.MODL_PLUGIN_VERSION.key}\")"

    /**
     * Version-catalog aliases for each scope, matching entries in
     * `templates/version-catalog/libs.versions.toml` (and ignition-sdk-examples).
     * Hyphens in the TOML library key become dots: ignition-gateway-api -> libs.ignition.gateway.api
     */
    val CATALOG_LIBS: Map<ProjectScope, Set<String>> = mapOf(
        ProjectScope.CLIENT to setOf(
            "libs.ignition.client.api",
            "libs.ignition.vision.client.api",
            "libs.ignition.common",
        ),
        ProjectScope.COMMON to setOf(
            "libs.ignition.common",
        ),
        ProjectScope.DESIGNER to setOf(
            "libs.ignition.designer.api",
            "libs.ignition.common",
        ),
        ProjectScope.GATEWAY to setOf(
            "libs.ignition.common",
            "libs.ignition.gateway.api",
        ),
    )

    fun Set<String>.toDependencyFormat(
        @Suppress("UNUSED_PARAMETER") dsl: GradleDsl,
        configuration: String = "compileOnly",
    ): String = map { catalogAlias ->
        "$configuration($catalogAlias)"
    }.joinToString(separator = "\n    ")
}
