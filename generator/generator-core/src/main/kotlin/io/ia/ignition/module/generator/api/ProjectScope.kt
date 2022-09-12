package io.ia.ignition.module.generator.api

enum class ProjectScope(val folderName: String) {
    CLIENT("client"),
    DESIGNER("designer"),
    GATEWAY("gateway"),
    COMMON("common"),
    // not an ignition scope, but represents the root build gradle project scope
    ROOT("");

    companion object {

        /**
         * Returns a list of valid ProjectScope elements according the the string.  Lower case letters treated as
         * upper case, and invalid scope characters in the String are ignored.  Common scope is not represented in
         * the resulting list.  Use [effectiveScopesFromShorthand] if needing the effective project scopes given a
         * shorthand string.
         */
        fun scopesFromShorthand(scopes: String): List<ProjectScope> {
            return scopes.toUpperCase()
                .map {
                    when (it) {
                        'C' -> CLIENT
                        'D' -> DESIGNER
                        'G' -> GATEWAY
                        else -> null
                    }
                }
                .filterNotNull()
        }

        /**
         * Returns a list of valid ProjectScope elements according the the string.  Lower case letters treated as
         * upper case, and invalid scope characters in the String are ignored.  Common scope *is*  represented in
         * the resulting list if the list includes more than one scope value..  Use [scopesFromShorthand] if needing
         * the exact project scopes given a shorthand string.
         */
        fun effectiveScopesFromShorthand(scopes: String): List<ProjectScope> {
            val asParsed = scopesFromShorthand(scopes)

            return if (asParsed.size > 1) listOf(COMMON) + asParsed else asParsed
        }
    }

    /**
     * Builds a string containing the default SDK dependencies for a scope.  The default dependencies string
     * will take contain [compileOnly]() entries for the most typical sdk dependencies
     */
    fun defaultDependencies(
        customizer: ((artifactStubs: Set<String>) -> String)? = null
    ): String {
        return DefaultSdkDependencies.ARTIFACTS[this]?.let {
            if (customizer != null) {
                customizer(it)
            } else {
                it.joinToString(separator = "\n    ", postfix = "\n") { "compileOnly(\"${'$'}{ignitionSdkVersion}\")" }
            }
        } ?: throw Exception("Default dependencies failed to resolve for ProjectScope '$this'")
    }
}
