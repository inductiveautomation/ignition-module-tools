package io.ia.sdk.gradle.modl.extension

import org.gradle.api.Named
import org.gradle.api.tasks.Input
import java.io.Serializable

abstract class ModuleDependencySpec : Named, Serializable {
    @get:Input
    var scope: String = ""

    @get:Input
    var required: Boolean = false
}
