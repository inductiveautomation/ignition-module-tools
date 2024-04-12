package io.ia.sdk.gradle.modl.extension

import java.io.Serializable

class RequiredModuleDependency(
    val moduleId: String,
    val scope: String,
    val required: Boolean
) : Serializable

@DslMarker
annotation class ResourceDsl

@ResourceDsl
class ModuleDependencyBuilder(var moduleId: String) {

    var scope: String = ""
    var required: Boolean = false

    fun build(): RequiredModuleDependency = RequiredModuleDependency(moduleId, scope, required)
}
