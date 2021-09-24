package io.ia.sdk.gradle.modl.util

import io.ia.sdk.gradle.modl.IgnitionModlPlugin
import org.gradle.api.Project

fun Project.hasOptedOutOfModule(): Boolean {
    val propertyValue: String =
        this.properties.getOrDefault(IgnitionModlPlugin.PROJECT_OPT_OUT, "false").toString()

    return propertyValue == "true"
}

fun String.capitalize(): String {
    return this.replaceFirstChar { it.uppercase() }
}
