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
            return scopes.uppercase()
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
}
