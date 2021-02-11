package io.ia.sdk.gradle.modl.model

import io.ia.sdk.gradle.modl.api.ModuleConfigException
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty

/**
 * Simple container representing a module dependency, where the scope is a string signifying the Ignition scope in which
 * the dependency should apply, and the moduleId is that of the module that is being depended on for that scope.
 */
data class ModuleDependency(val moduleId: String, val scopes: List<IgnitionScope>)

@Suppress("UnstableApiUsage")
open class ProjectScopeContainer(private val scope: IgnitionScope, private val factory: ObjectFactory) {
    private var projects: ListProperty<Project> = this.factory.listProperty(Project::class.java)

    public fun getScope(): IgnitionScope {
        return this.scope
    }
}

enum class IgnitionScope(val code: String) {
    DESIGNER("D"),
    VISION_CLIENT("C"),
    GATEWAY("G"),
    DESIGNER_VISION_CLIENT("CD"),
    GATEWAY_DESIGNER("DG"),
    GATEWAY_DESIGNER_VISION_CLIENT("CDG"),
    GATEWAY_VISION_CLIENT("CG"),
    NONE("");

    companion object {
        private val VALID_SCOPES = Regex("^[GCD]+\$")

        /**
         * Applies simplistic validation to the string and returns the corresponding scope if it can be determined.
         */
        @JvmStatic
        @Throws(ModuleConfigException::class)
        fun forShorthand(code: String?): IgnitionScope {
            if (code == null) {
                throw Exception("Null is not a valid value for IgnitionScope code.")
            }

            val capped = code.toCharArray().sorted().joinToString("")

            if (!capped.matches(VALID_SCOPES)) {
                throw ModuleConfigException(
                    "'$code' is not a valid Ignition scope value, should be a string of " +
                        "one or more of G, C, or D (without repeats)."
                )
            }

            return when (capped) {
                "C" -> VISION_CLIENT
                "D" -> DESIGNER
                "G" -> GATEWAY
                "CD" -> DESIGNER_VISION_CLIENT
                "CDG" -> GATEWAY_DESIGNER_VISION_CLIENT
                "DG" -> GATEWAY_DESIGNER
                "CG" -> GATEWAY_VISION_CLIENT
                else -> throw Exception("Could not determine IgnitionScope for shorthand '$code'")
            }
        }
    }
}
